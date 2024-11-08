package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

public class YoutubeController extends Controller {

	private final WSClient ws;
	private final WordStatsService wordStatsService;
	private static final String YOUTUBE_API_KEY =
		"AIzaSyCRTltXLPxW3IwxMf-WH0BagUipQy_7TuQ";
	private static final String YOUTUBE_URL =
		"https://www.googleapis.com/youtube/v3";

	private final YoutubeService youtubeService;
	private final AsyncCacheApi cache;

	@Inject
	public YoutubeController(
		WSClient ws,
		WordStatsService wordStatsService,
		YoutubeService youtubeService,
		AsyncCacheApi cache
	) {
		this.ws = ws;
		this.wordStatsService = wordStatsService;
		this.youtubeService = youtubeService;
		this.cache = cache;
	}

	public Result search() {
		return ok(views.html.search.render());
	}

	public CompletionStage<Result> searchVideos(String query) {
		// Attempt to fetch from cache or perform the API call if not cached

		return ws
			.url(YOUTUBE_URL + "/search")
			.addQueryParameter("part", "snippet")
			.addQueryParameter("maxResults", "10")
			.addQueryParameter("q", query)
			.addQueryParameter("type", "video")
			.addQueryParameter("key", YOUTUBE_API_KEY)
			.get()
			.thenCompose(response -> {
				if (response.getStatus() == 200) {
					// Modify and return the response as JSON
					return youtubeService
						.modifyResponse((ObjectNode) response.asJson())
						.thenApply(modifiedResponse -> ok(modifiedResponse));
				} else {
					return CompletableFuture.completedFuture(
						internalServerError(
							"YouTube API error: " + response.getBody()
						)
					);
				}
			});
	}

	public CompletionStage<ObjectNode> getCachedYoutubeResponse(String query) {
		String cacheKey = "youtube_response_" + query;

		// Check if data is cached
		return cache
			.get(cacheKey)
			.thenCompose(cachedResponse -> {
				if (cachedResponse != null) {
					// If cached data exists, return it directly
					return CompletableFuture.completedFuture(
						(ObjectNode) cachedResponse
					);
				} else {
					// If no cached data exists, fetch it from the YouTube API
					return fetchYoutubeData(query).thenApply(response -> {
						// Cache the response before returning it
						cache.set(cacheKey, response, 3600); // Cache for 1 hour (3600 seconds)
						return response;
					});
				}
			});
	}

	private CompletionStage<ObjectNode> fetchYoutubeData(String query) {
		return ws
			.url(YOUTUBE_URL + "/search")
			.addQueryParameter("part", "snippet")
			.addQueryParameter("maxResults", "10")
			.addQueryParameter("q", query)
			.addQueryParameter("type", "video")
			.addQueryParameter("key", YOUTUBE_API_KEY)
			.get()
			.thenApply(response -> {
				if (response.getStatus() == 200) {
					return response;
				} else {
					// Handle error or return a default response
					return null;
				}
			});
	}

	public Result getWordStats(String query) {
		// Fetch the list of videos by search query
		List<YoutubeVideo> videos = fetchVideosBySearchTerm(query);

		// Concatenating all descriptions into a single string
		List<String> allDescriptions = videos
			.stream()
			.map(YoutubeVideo::getDescription)
			.collect(Collectors.toList());

		// Splitting the string into words, then to lowercase, and counting word frequencies
		Map<String, Long> sortedWordCount = wordStatsService.calculateWordStats(
			allDescriptions
		);

		// Passing the sorted word stats map and search query to the view
		return ok(views.html.wordstats.render(sortedWordCount, query));
	}

	private List<YoutubeVideo> fetchVideosBySearchTerm(String query) {
		// Creating an empty list to store the fetched video details
		List<YoutubeVideo> videoList = new ArrayList<>();

		CompletionStage<JsonNode> responseStage = ws
			.url(YOUTUBE_URL + "/search")
			.addQueryParameter("part", "snippet")
			.addQueryParameter("maxResults", "50")
			.addQueryParameter("q", query)
			.addQueryParameter("type", "video")
			.addQueryParameter("key", YOUTUBE_API_KEY)
			.get()
			.thenApply(response -> {
				if (response.getStatus() == 200) {
					return response.asJson();
				} else {
					return null;
				}
			});

		JsonNode response = responseStage.toCompletableFuture().join();

		if (response != null && response.has("items")) {
			JsonNode items = response.get("items");

			for (JsonNode item : items) {
				JsonNode snippet = item.get("snippet");
				String videoId = item.get("id").get("videoId").asText();
				String title = snippet.get("title").asText();
				String description = snippet.get("description").asText();
				String thumbnailUrl = snippet
					.get("thumbnails")
					.get("default")
					.get("url")
					.asText();
				String channelTitle = snippet.get("channelTitle").asText();
				String publishedAt = snippet.get("publishedAt").asText();
				Long viewCount = snippet.has("viewCount")
					? snippet.get("viewCount").asLong()
					: 0L;

				// Creating a YoutubeVideo object and add it to the list
				YoutubeVideo video = new YoutubeVideo(
					videoId,
					title,
					description,
					thumbnailUrl,
					channelTitle,
					publishedAt,
					viewCount
				);
				videoList.add(video);
			}
		}

		return videoList;
	}

	public CompletionStage<Result> getChannelProfile(String channelId) {
		String channelUrl =
			YOUTUBE_URL +
			"/channels?part=snippet,statistics&id=" +
			channelId +
			"&key=" +
			YOUTUBE_API_KEY;

		return ws
			.url(channelUrl)
			.get()
			.thenApply(response -> {
				JsonNode channelData = response.asJson();

				// Debug: Log the entire response to verify structure
				System.out.println("API Response: " + channelData.toString());

				// Check if "items" exists and has at least one element
				if (
					channelData.has("items") &&
					channelData.get("items").size() > 0
				) {
					JsonNode channelJson = channelData.get("items").get(0);

					// Debug: Log the channelJson to confirm access
					System.out.println(
						"Channel JSON: " + channelJson.toString()
					);

					Map<String, String> channelDetails = new HashMap<>();
					channelDetails.put(
						"id",
						channelJson.has("id")
							? channelJson.get("id").asText()
							: "N/A"
					);

					// Access the "snippet" part safely
					JsonNode snippet = channelJson.get("snippet");
					System.out.println(snippet);
					if (snippet != null) {
						channelDetails.put(
							"title",
							snippet.has("title")
								? snippet.get("title").asText()
								: "No title"
						);
						channelDetails.put(
							"description",
							snippet.has("description")
								? snippet.get("description").asText()
								: "No description"
						);
						channelDetails.put(
							"country",
							snippet.has("country")
								? snippet.get("country").asText()
								: "N/A"
						);
					} else {
						System.out.println(
							"Snippet is missing in the response."
						);
					}

					JsonNode thumbnails = snippet.get("thumbnails");
					if (thumbnails != null) {
						channelDetails.put(
							"thumbnailDefault",
							thumbnails.get("default").get("url").asText()
						);
					}

					// Access the "statistics" part safely
					JsonNode statistics = channelJson.get("statistics");
					if (statistics != null) {
						channelDetails.put(
							"subscriberCount",
							statistics.has("subscriberCount")
								? statistics.get("subscriberCount").asText()
								: "0"
						);
						channelDetails.put(
							"viewCount",
							statistics.has("viewCount")
								? statistics.get("viewCount").asText()
								: "0"
						);
						channelDetails.put(
							"videoCount",
							statistics.has("videoCount")
								? statistics.get("videoCount").asText()
								: "0"
						);
					} else {
						System.out.println(
							"Statistics is missing in the response."
						);
					}

					// Add the channel link to channelDetails
					channelDetails.put(
						"channelLink",
						"https://www.youtube.com/channel/" + channelId
					);

					String videosUrl =
						YOUTUBE_URL +
						"/search?part=snippet&channelId=" +
						channelId +
						"&maxResults=10&order=date&type=video&key=" +
						YOUTUBE_API_KEY;

					CompletionStage<JsonNode> videosResponseStage = ws
						.url(videosUrl)
						.get()
						.thenApply(videosResponse -> {
							JsonNode videosData = videosResponse.asJson();
							ArrayNode latestVideos =
								JsonNodeFactory.instance.arrayNode();

							// If videos are available, use the `videos` endpoint to get full descriptions
							if (videosData.has("items")) {
								for (JsonNode item : videosData.get("items")) {
									String videoId = item
										.get("id")
										.get("videoId")
										.asText();

									CompletionStage<
										JsonNode
									> videoDetailResponse = ws
										.url(YOUTUBE_URL + "/videos")
										.addQueryParameter("part", "snippet")
										.addQueryParameter("id", videoId)
										.addQueryParameter(
											"key",
											YOUTUBE_API_KEY
										)
										.get()
										.thenApply(videoDetail -> {
											JsonNode videoDetailData =
												videoDetail.asJson();
											if (
												videoDetailData.has("items") &&
												videoDetailData
													.get("items")
													.size() >
												0
											) {
												JsonNode videoSnippet =
													videoDetailData
														.get("items")
														.get(0)
														.get("snippet");

												// Use the full description from the video details
												ObjectNode videoNode =
													JsonNodeFactory.instance.objectNode();
												videoNode.put(
													"videoId",
													videoId
												);
												videoNode.put(
													"title",
													videoSnippet
														.get("title")
														.asText()
												);
												videoNode.put(
													"description",
													videoSnippet
														.get("description")
														.asText()
												); // Full description
												videoNode.put(
													"thumbnailUrl",
													videoSnippet
														.get("thumbnails")
														.get("default")
														.get("url")
														.asText()
												);
												return videoNode;
											}
											return null;
										});

									// Add the video detail to the latestVideos array if it was successfully retrieved
									JsonNode fullVideoDetail =
										videoDetailResponse
											.toCompletableFuture()
											.join();
									if (fullVideoDetail != null) {
										latestVideos.add(fullVideoDetail);
									}
								}
							}
							return latestVideos;
						});

					// Wait for the videos response to complete and add it to the channelDetails map
					videosResponseStage.toCompletableFuture().join();
					channelDetails.put(
						"latestVideos",
						videosResponseStage
							.toCompletableFuture()
							.join()
							.toString()
					);

					return ok(views.html.channelprofile.render(channelDetails));
				} else {
					System.out.println("No channel items found in response.");
					return notFound("Channel not found");
				}
			});
	}

	public CompletionStage<Result> getVideoDetails(String video_id) {
		return ws
			.url(YOUTUBE_URL + "/videos")
			.addQueryParameter("part", "snippet,contentDetails,statistics") // Get snippet (title, description), content details (tags), statistics (view count)
			.addQueryParameter("id", video_id)
			.addQueryParameter("key", YOUTUBE_API_KEY)
			.get()
			.thenCompose(response -> {
				if (response.getStatus() == 200) {
					JsonNode videoData = response.asJson();
					JsonNode items = videoData.get("items");

					if (items != null && items.size() > 0) {
						JsonNode videoInfo = items.get(0);
						String title = videoInfo
							.get("snippet")
							.get("title")
							.asText();
						String description = videoInfo
							.get("snippet")
							.get("description")
							.asText();
						String thumbnailUrl = videoInfo
							.get("snippet")
							.get("thumbnails")
							.get("default")
							.get("url")
							.asText();
						String channelTitle = videoInfo
							.get("snippet")
							.get("channelTitle")
							.asText();
						String publishedAt = videoInfo
							.get("snippet")
							.get("publishedAt")
							.asText();
						Long viewCount = videoInfo
							.get("statistics")
							.get("viewCount")
							.asLong();
						ArrayNode tagsNode = (ArrayNode) videoInfo
							.get("snippet")
							.get("tags");

						List<String> tags = new ArrayList<>();
						if (tagsNode != null) {
							for (JsonNode tagNode : tagsNode) {
								tags.add(tagNode.asText());
							}
						}

						YoutubeVideo video = new YoutubeVideo(
							video_id,
							title,
							description,
							thumbnailUrl,
							channelTitle,
							publishedAt,
							viewCount
						);

						return CompletableFuture.completedFuture(
							ok(views.html.videoDetails.render(video, tags))
						);
					} else {
						return CompletableFuture.completedFuture(
							notFound("Video not found")
						);
					}
				} else {
					return CompletableFuture.completedFuture(
						internalServerError(
							"Error fetching video details: " +
							response.getBody()
						)
					);
				}
			});
	}
}
