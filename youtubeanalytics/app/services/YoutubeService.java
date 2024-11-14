package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import play.cache.AsyncCacheApi;
import play.libs.ws.*;
import play.libs.ws.WSClient;

/**
 * Service class for interacting with the YouTube API.
 * This class provides methods to fetch video details and modify responses.
 *
 * @author Mohnish Mirchandani, Vatsal Dadia
 */
public class YoutubeService {

    private final WSClient ws;
    private final AsyncCacheApi cache;
    private static final String YOUTUBE_API_KEY = "AIzaSyDjXQxEHYcT9PBsFI6frudaxzd4fNxTWbs";
    private static final String YOUTUBE_URL = "https://www.googleapis.com/youtube/v3";

    /**
     * Constructor for YoutubeService.
     *
     * @param ws The WSClient for making HTTP requests.
     * @param cache The AsyncCacheApi for caching responses.
     * @author Mohnish Mirchandani
     */
    @Inject
    public YoutubeService(WSClient ws, AsyncCacheApi cache) {
        this.ws = ws;
        this.cache = cache;
    }

    /**
     * Fetches video details from the YouTube API.
     *
     * @param video_id The ID of the video to fetch.
     * @return A CompletionStage containing the WSResponse with video details.
     * @author Mohnish Mirchandani
     */
    public CompletionStage<WSResponse> getVideo(String video_id) {
        // Try to fetch from cache first
        return cache.getOrElseUpdate(
            video_id,
            () -> {
                // If not cached, perform the API call
                return ws
                    .url(YOUTUBE_URL + "/videos")
                    .addQueryParameter("part", "snippet,contentDetails,statistics")
                    .addQueryParameter("id", video_id)
                    .addQueryParameter("key", YOUTUBE_API_KEY)
                    .get();
            },
            3600
        ); // Cache for 1 hour (3600 seconds)
    }

    /**
     * Modifies the YouTube API response to include additional information.
     *
     * @param youtubeResponse The original YouTube API response.
     * @return A CompletionStage containing the modified response as an ObjectNode.
     * @author Mohnish Mirchandani, Vatsal Dadia
     */
    public CompletionStage<ObjectNode> modifyResponse(ObjectNode youtubeResponse) {
        JsonNode items = youtubeResponse.get("items");
        ObjectNode modifiedResponse = youtubeResponse.deepCopy();
        ArrayNode modifiedItems = JsonNodeFactory.instance.arrayNode();
        List<CompletableFuture<ObjectNode>> futures = new ArrayList<>();
        List<Double> grades = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        for (JsonNode item : items) {
            ObjectNode videoNode = (ObjectNode) item;
            String videoId = videoNode.get("id").get("videoId").asText();
            CompletionStage<ObjectNode> future = getVideo(videoId).thenApply(response -> {
                if (response.getStatus() == 200) {
                    String description = response.asJson().get("items").get(0).get("snippet").get("description").asText();

                    double grade = ReadabilityCalculator.calculateFleschKincaidGradeLevel(description);
                    grades.add(grade);
                    double score = ReadabilityCalculator.calculateFleschReadingScore(description);
                    scores.add(score);
                    double sentimentValue = SentimentAnalyzer.analyzeDescription(description);

                    videoNode.put("description", description);
                    videoNode.put("fleschKincaidGradeLevel", String.format("%.2f", grade));
                    videoNode.put("fleschReadingScore", String.format("%.2f", score));
                    return videoNode;
                }
                return videoNode;
            });
            futures.add(future.toCompletableFuture());
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> {
            futures.stream()
                .map(CompletionStage::toCompletableFuture)
                .map(future -> future.getNow(null))
                .limit(10)
                .forEach(videoNode -> modifiedItems.add(videoNode));

            double gradeAvg = ReadabilityCalculator.calculateGradeAvg(grades);
            double scoreAvg = ReadabilityCalculator.calculateScoreAvg(scores);

            List<String> descriptions = StreamSupport.stream(modifiedItems.spliterator(), false)
                .map(item -> item.get("description").asText())
                .collect(Collectors.toList());

            String sentiment = SentimentAnalyzer.analyzeSentiment(descriptions);

            modifiedResponse.put("sentiment", sentiment);
            modifiedResponse.put("fleschKincaidGradeLevelAvg", String.format("%.2f", gradeAvg));
            modifiedResponse.put("fleschReadingScoreAvg", String.format("%.2f", scoreAvg));
            modifiedResponse.set("items", modifiedItems);

            return modifiedResponse;
        });
    }
}