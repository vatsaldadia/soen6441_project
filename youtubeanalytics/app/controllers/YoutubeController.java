package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;
import play.libs.ws.*;
import play.mvc.*;
import services.ReadabilityCalculator;
import services.WordStatsService;
import models.YoutubeVideo;
import java.util.Arrays;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

public class YoutubeController extends Controller {

	private final WSClient ws;
	private final WordStatsService wordStatsService;
	private static final String YOUTUBE_API_KEY =
		"AIzaSyA1mnyPEMB5J33g-zOOPSbJPzWq1d4Qczs";
	private static final String YOUTUBE_URL =
		"https://www.googleapis.com/youtube/v3";

	@Inject
	public YoutubeController(WSClient ws, WordStatsService wordStatsService) {
		this.ws = ws;
		this.wordStatsService = wordStatsService;
	}

	public ObjectNode modifyResponse(ObjectNode youtubeResponse) {
		if (youtubeResponse.has("items")) {
			JsonNode items = youtubeResponse.get("items");
			ObjectNode modifiedResponse = youtubeResponse.deepCopy();
			ArrayNode modifiedItems = JsonNodeFactory.instance.arrayNode();
			List<CompletableFuture<String>> futures = new ArrayList<>();
			for (JsonNode item : items) {
				ObjectNode videoNode = (ObjectNode) item;
				String videoId = videoNode.get("id").get("videoId").asText();
				CompletableFuture<String> future = ws
					.url(YOUTUBE_URL + "/videos")
					.addQueryParameter("part", "snippet")
					.addQueryParameter("id", videoId)
					.addQueryParameter("key", YOUTUBE_API_KEY)
					.get()
					.thenApply(response -> {
						if (response.getStatus() == 200) {
							return response
								.asJson()
								.get("items")
								.get(0)
								.get("snippet")
								.get("description")
								.asText();
						}
						return null;
					})
					.toCompletableFuture();
				futures.add(future);
				modifiedItems.add(videoNode);
			}

			List<String> descriptions = futures
				.stream()
				.map(CompletableFuture::join)
				.collect(Collectors.toList());

			List<Double> grade = descriptions
				.stream()
				.map(ReadabilityCalculator::calculateFleschKincaidGradeLevel)
				.collect(Collectors.toList());

			List<Double> score = descriptions
				.stream()
				.map(ReadabilityCalculator::calculateFleschReadingScore)
				.collect(Collectors.toList());

			double gradeAvg = grade
				.stream()
				.mapToDouble(Double::doubleValue)
				.average()
				.orElse(0.0);

			double scoreAvg = score
				.stream()
				.mapToDouble(Double::doubleValue)
				.average()
				.orElse(0.0);

			for (int i = 0; i < descriptions.size(); i++) {
				ObjectNode videoNode = (ObjectNode) modifiedItems.get(i);
				videoNode.put("description", descriptions.get(i));
				videoNode.put(
					"fleschKincaidGradeLevel",
					String.format("%.2f", grade.get(i))
				);
				videoNode.put(
					"fleschReadingScore",
					String.format("%.2f", score.get(i))
				);
			}
			modifiedResponse.set("items", modifiedItems);
			modifiedResponse.put(
				"fleschKincaidGradeLevelAvg",
				String.format("%.2f", gradeAvg)
			);
			modifiedResponse.put(
				"fleschReadingScoreAvg",
				String.format("%.2f", scoreAvg)
			);

			return modifiedResponse;
		}

		return youtubeResponse;
	}

	public CompletionStage<Result> searchVideos(String query) {
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
					ObjectNode modifiedResponse = modifyResponse(
						(ObjectNode) response.asJson()
					);

					return ok(modifiedResponse);
				} else {
					return internalServerError(
						"YouTube API error: " + response.getBody()
					);
				}
			});
	}

	// New method to get word stats
	public Result getWordStats(String query) {
		// Fetch the list of videos by search query
		List<YoutubeVideo> videos = fetchVideosBySearchTerm(query);

		// Concatenate all descriptions into a single string
		List<String> allDescriptions = videos.stream()
				.map(YoutubeVideo::getDescription)
				.collect(Collectors.toList());

		// Split the string into words, normalize to lowercase, and count word frequencies
		Map<String, Long> sortedWordCount = wordStatsService.calculateWordStats(allDescriptions);

		// Pass the sorted word stats map and search query to the view
		return ok(views.html.wordstats.render(sortedWordCount, query));
	}

	private List<YoutubeVideo> fetchVideosBySearchTerm(String query) {
		// Create an empty list to store the fetched video details
		List<YoutubeVideo> videoList = new ArrayList<>();

		// Perform YouTube search similar to searchVideos method
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
						return null; // Handle the error case
					}
				});

		// Wait for the response to complete
		JsonNode response = responseStage.toCompletableFuture().join();

		if (response != null && response.has("items")) {
			JsonNode items = response.get("items");

			// Loop through the search results and fetch details
			for (JsonNode item : items) {
				JsonNode snippet = item.get("snippet");
				String videoId = item.get("id").get("videoId").asText();
				String title = snippet.get("title").asText();
				String description = snippet.get("description").asText();
				String thumbnailUrl = snippet.get("thumbnails").get("default").get("url").asText();
				String channelTitle = snippet.get("channelTitle").asText();
				String publishedAt = snippet.get("publishedAt").asText();

				// Fetch additional video stats (like viewCount)
				CompletionStage<JsonNode> videoDetailsStage = ws
						.url(YOUTUBE_URL + "/videos")
						.addQueryParameter("part", "statistics")
						.addQueryParameter("id", videoId)
						.addQueryParameter("key", YOUTUBE_API_KEY)
						.get()
						.thenApply(videoResponse -> {
							if (videoResponse.getStatus() == 200) {
								return videoResponse.asJson();
							} else {
								return null;
							}
						});

				JsonNode videoDetails = videoDetailsStage.toCompletableFuture().join();
				Long viewCount = null;
				// Create a YoutubeVideo object and add it to the list
				YoutubeVideo video = new YoutubeVideo(
						videoId, title, description, thumbnailUrl, channelTitle, publishedAt, viewCount
				);
				videoList.add(video);
			}
		}

		return videoList; // Return the list of videos
	}

}
