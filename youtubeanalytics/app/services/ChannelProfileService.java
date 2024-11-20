package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

public class ChannelProfileService {

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
}
