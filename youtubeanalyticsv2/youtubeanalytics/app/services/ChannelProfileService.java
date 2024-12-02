package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;


import akka.actor.ActorRef;
import akka.pattern.Patterns;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

public class ChannelProfileService {

    private final WSClient ws;
    private final String YOUTUBE_API_KEY = "AIzaSyD_v7xmCw01JMvpN4pSHlQQPeGhxhAhqCY";
    private final String YOUTUBE_URL = "https://www.googleapis.com/youtube/v3";

    public ChannelProfileService(WSClient ws) {
        this.ws = ws;
    }

    /**
     * Fetches channel details from YouTube API.
     *
     * @param channelId The ID of the channel.
     * @return A CompletionStage with the channel details in JSON format.
     */
    public CompletionStage<JsonNode> fetchChannelDetails(String channelId) {
        return ws.url(YOUTUBE_URL + "/channels")
                .addQueryParameter("part", "snippet,statistics")
                .addQueryParameter("id", channelId)
                .addQueryParameter("key", "AIzaSyD_v7xmCw01JMvpN4pSHlQQPeGhxhAhqCY")
                .get()
                .thenApply(WSResponse::asJson);
    }

    /**
     * Fetches the latest videos from a channel using YouTube API.
     *
     * @param channelId The ID of the channel.
     * @return A CompletionStage with the video details in JSON format.
     */
    public CompletionStage<JsonNode> fetchLatestVideos(String channelId) {
        return ws.url(YOUTUBE_URL + "/search")
                .addQueryParameter("part", "snippet")
                .addQueryParameter("channelId", channelId)
                .addQueryParameter("maxResults", "10")
                .addQueryParameter("order", "date")
                .addQueryParameter("type", "video")
                .addQueryParameter("key", "AIzaSyD_v7xmCw01JMvpN4pSHlQQPeGhxhAhqCY")
                .get()
                .thenApply(WSResponse::asJson);
    }

    /**
     * Parses channel details from the fetched JSON response.
     *
     * @param channelData JSON response containing channel data.
     * @return A map with channel details.
     */
    public Map<String, String> parseChannelDetails(JsonNode channelData) {
        Map<String, String> channelDetails = new HashMap<>();

        if (channelData.path("items").isArray() && channelData.path("items").size() > 0) {
            JsonNode channelJson = channelData.path("items").get(0);
            channelDetails.put("id", channelJson.path("id").asText("N/A"));

            JsonNode snippet = channelJson.path("snippet");
            channelDetails.put("title", snippet.path("title").asText("No title"));
            channelDetails.put("description", snippet.path("description").asText("No description"));
            channelDetails.put("country", snippet.path("country").asText("N/A"));

            JsonNode thumbnails = snippet.path("thumbnails").path("default");
            channelDetails.put("thumbnailDefault", thumbnails.path("url").asText("N/A"));

            JsonNode statistics = channelJson.path("statistics");
            channelDetails.put("subscriberCount", statistics.path("subscriberCount").asText("0"));
            channelDetails.put("viewCount", statistics.path("viewCount").asText("0"));
            channelDetails.put("videoCount", statistics.path("videoCount").asText("0"));
        }

        return channelDetails;
    }

    /**
     * Parses the latest videos from the fetched JSON response.
     *
     * @param videosData JSON response containing video data.
     * @return An ArrayNode with video details.
     */
    public ArrayNode parseLatestVideos(JsonNode videosData) {
        ArrayNode latestVideos = JsonNodeFactory.instance.arrayNode();

        if (videosData.path("items").isArray()) {
            for (JsonNode item : videosData.path("items")) {
                ObjectNode videoNode = JsonNodeFactory.instance.objectNode();
                String videoId = item.path("id").path("videoId").asText("N/A");
                JsonNode snippet = item.path("snippet");

                videoNode.put("videoId", videoId);
                videoNode.put("title", snippet.path("title").asText("No title"));
                videoNode.put("description", snippet.path("description").asText("No description"));
                videoNode.put("thumbnailUrl", snippet.path("thumbnails").path("default").path("url").asText("N/A"));

                latestVideos.add(videoNode);
            }
        }

        return latestVideos;
    }

    /**
     * Sends the channel profile response to the requesting actor.
     *
     * @param channelId         The ID of the channel.
     * @param requestingActor   The actor requesting the channel profile.
     */
    public void sendChannelProfile(String channelId, ActorRef requestingActor) {
        fetchChannelDetails(channelId)
                .thenCombine(fetchLatestVideos(channelId), (channelData, videosData) -> {
                    Map<String, String> channelDetails = parseChannelDetails(channelData);
                    ArrayNode latestVideos = parseLatestVideos(videosData);

                    ObjectNode response = JsonNodeFactory.instance.objectNode();
                    response.putPOJO("channelDetails", channelDetails);
                    response.set("latestVideos", latestVideos);

                    return response;
                })
                .thenAccept(response -> requestingActor.tell(response, ActorRef.noSender()));
    }
}
