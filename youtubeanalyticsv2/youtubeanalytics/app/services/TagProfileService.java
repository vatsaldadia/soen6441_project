package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


public class TagProfileService {
    private final WSClient ws;
    private final String YOUTUBE_API_KEY = "AIzaSyA1mnyPEMB5J33g-zOOPSbJPzWq1d4Qczs"; // Replace with your actual YouTube API key
    private static final String YOUTUBE_URL =
            "https://www.googleapis.com/youtube/v3";
    public TagProfileService(WSClient ws) {
        this.ws = ws;
    }

    /**
     * Fetches tags for a specific video by its video ID.
     *
     * @param videoId The YouTube video ID.
     * @return A CompletionStage with an ObjectNode containing the tags.
     */
    public CompletionStage<ObjectNode> getVideoTags(String videoId) {
        String url = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + videoId + "&key=" + YOUTUBE_API_KEY;

        return ws.url(url).get().thenApply(WSResponse::asJson).thenApply(response -> {
            ObjectNode result = new ObjectMapper().createObjectNode();
            ArrayNode tags = new ObjectMapper().createArrayNode();

            if (response.has("items") && response.get("items").size() > 0) {
                JsonNode snippet = response.get("items").get(0).get("snippet");
                if (snippet != null && snippet.has("tags")) {
                    for (JsonNode tag : snippet.get("tags")) {
                        tags.add(tag.asText());
                    }
                }
            }

            result.set("tags", tags);
            return result;
        });
    }

    public CompletionStage<JsonNode> fetchVideosByTag(String tagId) throws IOException, URISyntaxException, InterruptedException {
        String encodedTag = URLEncoder.encode(tagId, StandardCharsets.UTF_8);

        String urlString = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&q=" + tagId + "&key=" + YOUTUBE_API_KEY;

        URI uri = new URI(urlString);
        //HttpURLConnection conn = getHttpURLConnection(uri);
        //conn.setRequestMethod("GET");

        HttpClient client = HttpClient.newHttpClient();

        // Create a GET HttpRequest
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .GET()
                .build();

        HttpResponse<String> response1 = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response1.body());

        return CompletableFuture.completedFuture(jsonNode);
    }

    public CompletionStage<JsonNode> fetchlatestTagFuture(String etag) throws IOException, URISyntaxException, InterruptedException{
        String encodedTag = URLEncoder.encode(etag, StandardCharsets.UTF_8);

        String urlString = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&q=" + etag + "&key=" + YOUTUBE_API_KEY;


        HttpClient client = HttpClient.newHttpClient();

        // Create a GET HttpRequest
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .GET()
                .build();

        HttpResponse<String> response1 = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response1.body());

        return CompletableFuture.completedFuture(jsonNode);

        /*String encodedTag = URLEncoder.encode(etag, StandardCharsets.UTF_8);

        return ws.url(YOUTUBE_URL + "/search")
                .addQueryParameter("part", "snippet")
                .addQueryParameter("q", encodedTag) // Search by tag
                .addQueryParameter("maxResults", "10")
                .addQueryParameter("type", "video")
                .addQueryParameter("key", YOUTUBE_API_KEY)
                .get()
                .thenApply(response -> {
                    if (response.getStatus() == 200) {
                        return response.asJson();
                    } else {
                        throw new RuntimeException("Error fetching videos: " + response.getStatusText());
                    }
                });*/
    }

}