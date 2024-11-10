package controllers;

import static services.WordStatsService.computeWordStats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import models.YoutubeVideo;
import play.cache.AsyncCacheApi;
import play.libs.ws.*;
import play.mvc.*;
import services.ReadabilityCalculator;
import services.SentimentAnalyzer;
import services.WordStatsService;
import services.YoutubeService;

/**
 * Controller class for handling YouTube-related requests.
 * This class provides methods to search for videos, get word statistics, and fetch video details.
 *
 * @author Mohnish Mirchandani, Vatsal Dadia, Rolwyn Raju, Pretty Kotian, Elston Farel
 */
public class YoutubeController extends Controller {

    private final WSClient ws;
    private static final String YOUTUBE_API_KEY = "AIzaSyDjXQxEHYcT9PBsFI6frudaxzd4fNxTWbs";
    private static final String YOUTUBE_URL = "https://www.googleapis.com/youtube/v3";

    private final YoutubeService youtubeService;
    private final AsyncCacheApi cache;

    /**
     * Constructor for YoutubeController.
     *
     * @param ws The WSClient for making HTTP requests.
     * @param wordStatsService The WordStatsService for calculating word statistics.
     * @param youtubeService The YoutubeService for interacting with the YouTube API.
     * @param cache The AsyncCacheApi for caching responses.
     * @author Mohnish Mirchandani
     */
    @Inject
    public YoutubeController(WSClient ws, WordStatsService wordStatsService, YoutubeService youtubeService, AsyncCacheApi cache) {
        this.ws = ws;
        this.youtubeService = youtubeService;
        this.cache = cache;
    }

    /**
     * Renders the search page.
     *
     * @return The search page result.
     * @author Mohnish Mirchandani
     */
    public Result search() {
        return ok(views.html.search.render());
    }

    /**
     * Searches for videos based on the query.
     *
     * @param query The search query.
     * @return A CompletionStage containing the search result as a JSON response.
     * @author Mohnish Mirchandani
     */
    public CompletionStage<Result> searchVideos(String query) {
        return searchVideoCall(query).thenCompose(response -> {
            if (response.getStatus() == 200) {
                return youtubeService.modifyResponse((ObjectNode) response.asJson()).thenApply(modifiedResponse -> ok(modifiedResponse));
            } else {
                return CompletableFuture.completedFuture(internalServerError("YouTube API error: " + response.getBody()));
            }
        });
    }

    /**
     * Makes an API call to search for videos.
     *
     * @param query The search query.
     * @return A CompletionStage containing the WSResponse with search results.
     * @author Mohnish Mirchandani, Vatsal Dadia
     */
    public CompletionStage<WSResponse> searchVideoCall(String query) {
        return cache.getOrElseUpdate(query, () -> {
            return ws.url(YOUTUBE_URL + "/search")
                .addQueryParameter("part", "snippet")
                .addQueryParameter("maxResults", "50")
                .addQueryParameter("q", query)
                .addQueryParameter("type", "video")
                .addQueryParameter("key", YOUTUBE_API_KEY)
                .get();
        }, 3600); // Cache for 1 hour (3600 seconds)
    }

    /**
     * Gets word statistics for the given query.
     *
     * @param query The search query.
     * @return A CompletionStage containing the word statistics result as a rendered view.
     * @author Rolwyn Raju
     */
    public CompletionStage<Result> getWordStats(String query) {
        List<YoutubeVideo> videoList = new ArrayList<>();

        return searchVideoCall(query).thenApply(r -> {
            if (r.getStatus() == 200) {
                JsonNode response = r.asJson();

                if (response != null && response.has("items")) {
                    JsonNode items = response.get("items");

                    for (JsonNode item : items) {
                        JsonNode snippet = item.get("snippet");
                        String videoId = item.get("id").get("videoId").asText();
                        String title = snippet.get("title").asText();
                        String description = snippet.get("description").asText();
                        String thumbnailUrl = snippet.get("thumbnails").get("default").get("url").asText();
                        String channelTitle = snippet.get("channelTitle").asText();
                        String publishedAt = snippet.get("publishedAt").asText();
                        Long viewCount = snippet.has("viewCount") ? snippet.get("viewCount").asLong() : 0L;

                        YoutubeVideo video = new YoutubeVideo(videoId, title, description, thumbnailUrl, channelTitle, publishedAt, viewCount);
                        videoList.add(video);
                    }
                }

                return ok(views.html.wordstats.render(computeWordStats(videoList), query));
            } else {
                return null;
            }
        });
    }

    /**
     * Gets the profile of a YouTube channel.
     *
     * @param channelId The ID of the channel.
     * @return A CompletionStage containing the channel profile result as a rendered view.
     * @author Pretty Kotian
     */
    public CompletionStage<Result> getChannelProfile(String channelId) {
        String channelUrl = YOUTUBE_URL + "/channels?part=snippet,statistics&id=" + channelId + "&key=" + YOUTUBE_API_KEY;

        return ws.url(channelUrl).get().thenApply(response -> {
            JsonNode channelData = response.asJson();

            if (channelData.has("items") && channelData.get("items").size() > 0) {
                JsonNode channelJson = channelData.get("items").get(0);

                Map<String, String> channelDetails = new HashMap<>();
                channelDetails.put("id", channelJson.has("id") ? channelJson.get("id").asText() : "N/A");

                JsonNode snippet = channelJson.get("snippet");
                if (snippet != null) {
                    channelDetails.put("title", snippet.has("title") ? snippet.get("title").asText() : "No title");
                    channelDetails.put("description", snippet.has("description") ? snippet.get("description").asText() : "No description");
                    channelDetails.put("country", snippet.has("country") ? snippet.get("country").asText() : "N/A");
                }

                JsonNode thumbnails = snippet.get("thumbnails");
                if (thumbnails != null) {
                    channelDetails.put("thumbnailDefault", thumbnails.get("default").get("url").asText());
                }

                JsonNode statistics = channelJson.get("statistics");
                if (statistics != null) {
                    channelDetails.put("subscriberCount", statistics.has("subscriberCount") ? statistics.get("subscriberCount").asText() : "0");
                    channelDetails.put("viewCount", statistics.has("viewCount") ? statistics.get("viewCount").asText() : "0");
                    channelDetails.put("videoCount", statistics.has("videoCount") ? statistics.get("videoCount").asText() : "0");
                }

                channelDetails.put("channelLink", "https://www.youtube.com/channel/" + channelId);

                String videosUrl = YOUTUBE_URL + "/search?part=snippet&channelId=" + channelId + "&maxResults=10&order=date&type=video&key=" + YOUTUBE_API_KEY;

                CompletionStage<JsonNode> videosResponseStage = ws.url(videosUrl).get().thenApply(videosResponse -> {
                    JsonNode videosData = videosResponse.asJson();
                    ArrayNode latestVideos = JsonNodeFactory.instance.arrayNode();

                    if (videosData.has("items")) {
                        for (JsonNode item : videosData.get("items")) {
                            String videoId = item.get("id").get("videoId").asText();

                            CompletionStage<JsonNode> videoDetailResponse = ws.url(YOUTUBE_URL + "/videos")
                                .addQueryParameter("part", "snippet")
                                .addQueryParameter("id", videoId)
                                .addQueryParameter("key", YOUTUBE_API_KEY)
                                .get()
                                .thenApply(videoDetail -> {
                                    JsonNode videoDetailData = videoDetail.asJson();
                                    if (videoDetailData.has("items") && videoDetailData.get("items").size() > 0) {
                                        JsonNode videoSnippet = videoDetailData.get("items").get(0).get("snippet");

                                        ObjectNode videoNode = JsonNodeFactory.instance.objectNode();
                                        videoNode.put("videoId", videoId);
                                        videoNode.put("title", videoSnippet.get("title").asText());
                                        videoNode.put("description", videoSnippet.get("description").asText());
                                        videoNode.put("thumbnailUrl", videoSnippet.get("thumbnails").get("default").get("url").asText());
                                        return videoNode;
                                    }
                                    return null;
                                });

                            JsonNode fullVideoDetail = videoDetailResponse.toCompletableFuture().join();
                            if (fullVideoDetail != null) {
                                latestVideos.add(fullVideoDetail);
                            }
                        }
                    }
                    return latestVideos;
                });

                videosResponseStage.toCompletableFuture().join();
                channelDetails.put("latestVideos", videosResponseStage.toCompletableFuture().join().toString());

                return ok(views.html.channelprofile.render(channelDetails));
            } else {
                return notFound("Channel not found");
            }
        });
    }

    /**
     * Gets the details of a YouTube video.
     *
     * @param video_id The ID of the video.
     * @return A CompletionStage containing the video details result as a rendered view.
     * @author Elston Farel
     */
    public CompletionStage<Result> getVideoDetails(String video_id) {
        return youtubeService.getVideo(video_id).thenCompose(response -> {
            if (response.getStatus() == 200) {
                JsonNode videoData = response.asJson();
                JsonNode items = videoData.get("items");

                if (items != null && items.size() > 0) {
                    JsonNode videoInfo = items.get(0);
                    String title = videoInfo.get("snippet").get("title").asText();
                    String description = videoInfo.get("snippet").get("description").asText();
                    String thumbnailUrl = videoInfo.get("snippet").get("thumbnails").get("default").get("url").asText();
                    String channelTitle = videoInfo.get("snippet").get("channelTitle").asText();
                    String publishedAt = videoInfo.get("snippet").get("publishedAt").asText();
                    Long viewCount = videoInfo.get("statistics").get("viewCount").asLong();
                    ArrayNode tagsNode = (ArrayNode) videoInfo.get("snippet").get("tags");

                    List<String> tags = new ArrayList<>();
                    if (tagsNode != null) {
                        for (JsonNode tagNode : tagsNode) {
                            tags.add(tagNode.asText());
                        }
                    }

                    YoutubeVideo video = new YoutubeVideo(video_id, title, description, thumbnailUrl, channelTitle, publishedAt, viewCount);

                    return CompletableFuture.completedFuture(ok(views.html.videoDetails.render(video, tags)));
                } else {
                    return CompletableFuture.completedFuture(notFound("Video not found"));
                }
            } else {
                return CompletableFuture.completedFuture(internalServerError("Error fetching video details: " + response.getBody()));
            }
        });
    }

    public CompletionStage<Result> getTagProfile(String video_id) {
        // Construct the YouTube API search URL for the given tag
        String encodedHashtag = URLEncoder.encode(video_id, StandardCharsets.UTF_8);
        String tagSearchUrl = YOUTUBE_URL +
                "/search?part=snippet&q=%23" + encodedHashtag + // Add %23 for the '#' symbol
                "&maxResults=10&type=video&key=" + YOUTUBE_API_KEY;

        return ws.url(tagSearchUrl)
                .get()
                .thenApply(response -> {
                    JsonNode tagData = response.asJson();
                    System.out.println("API Response for tag: " + tagData.toString());

                    ArrayNode videoList = JsonNodeFactory.instance.arrayNode();

                    // Loop through the returned videos and extract necessary data
                    if (tagData.has("items")) {
                        for (JsonNode item : tagData.get("items")) {
                            ObjectNode videoNode = JsonNodeFactory.instance.objectNode();
                            String videoId = item.get("id").get("videoId").asText();
                            JsonNode snippet = item.get("snippet");
                            videoNode.put("videoId", videoId);
                            videoNode.put("title", snippet.get("title").asText());
                            videoNode.put("description", snippet.get("description").asText());
                            videoNode.put("thumbnailUrl", snippet.get("thumbnails").get("default").get("url").asText());
                            videoList.add(videoNode);
                        }
                    }
                    // Render the tag profile view with the list of videos
                    return ok(views.html.tagprofile.render(video_id, videoList.toString()));
                });
    }

}
