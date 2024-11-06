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
import java.util.HashMap;
import java.util.Map;

public class YoutubeController extends Controller {

	private final WSClient ws;
	private final WordStatsService wordStatsService;
	private static final String YOUTUBE_API_KEY =
		"AIzaSyBn3hOC9y7PsDrQ62Xuj5M_P83ASq6GZRY";
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

			List<Double> grades = descriptions
				.stream()
				.map(ReadabilityCalculator::calculateFleschKincaidGradeLevel)
				.collect(Collectors.toList());

			List<Double> scores = descriptions
				.stream()
				.map(ReadabilityCalculator::calculateFleschReadingScore)
				.collect(Collectors.toList());

			double gradeAvg = ReadabilityCalculator.calculateGradeAvg(grades);

			double scoreAvg = ReadabilityCalculator.calculateScoreAvg(scores);

			for (int i = 0; i < descriptions.size(); i++) {
				ObjectNode videoNode = (ObjectNode) modifiedItems.get(i);
				videoNode.put("description", descriptions.get(i));
				videoNode.put(
					"fleschKincaidGradeLevel",
					String.format("%.2f", grades.get(i))
				);
				videoNode.put(
					"fleschReadingScore",
					String.format("%.2f", scores.get(i))
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

	public CompletionStage<Result> getChannelProfile(String channelId) {
		String channelUrl = YOUTUBE_URL + "/channels?part=snippet,statistics&id=" + channelId + "&key=" + YOUTUBE_API_KEY;

		return ws.url(channelUrl).get().thenApply(response -> {
			JsonNode channelData = response.asJson();

			// Debug: Log the entire response to verify structure
			System.out.println("API Response: " + channelData.toString());

			// Check if "items" exists and has at least one element
			if (channelData.has("items") && channelData.get("items").size() > 0) {
				JsonNode channelJson = channelData.get("items").get(0);

				// Debug: Log the channelJson to confirm access
				System.out.println("Channel JSON: " + channelJson.toString());

				Map<String, String> channelDetails = new HashMap<>();
				channelDetails.put("id", channelJson.has("id") ? channelJson.get("id").asText() : "N/A");

				// Access the "snippet" part safely
				JsonNode snippet = channelJson.get("snippet");
				System.out.println(snippet);
				if (snippet != null) {
					channelDetails.put("title", snippet.has("title") ? snippet.get("title").asText() : "No title");
					channelDetails.put("description", snippet.has("description") ? snippet.get("description").asText() : "No description");
					channelDetails.put("country", snippet.has("country") ? snippet.get("country").asText() : "N/A");

				} else {
					System.out.println("Snippet is missing in the response.");
				}

				JsonNode thumbnails = snippet.get("thumbnails");
				if (thumbnails != null) {
					channelDetails.put("thumbnailDefault", thumbnails.get("default").get("url").asText());
				}

				// Access the "statistics" part safely
				JsonNode statistics = channelJson.get("statistics");
				if (statistics != null) {
					channelDetails.put("subscriberCount", statistics.has("subscriberCount") ? statistics.get("subscriberCount").asText() : "0");
					channelDetails.put("viewCount", statistics.has("viewCount") ? statistics.get("viewCount").asText() : "0");
					channelDetails.put("videoCount", statistics.has("videoCount") ? statistics.get("videoCount").asText() : "0");
				} else {
					System.out.println("Statistics is missing in the response.");
				}

				// Add the channel link to channelDetails
				channelDetails.put("channelLink", "https://www.youtube.com/channel/" + channelId);


				String videosUrl = YOUTUBE_URL + "/search?part=snippet&channelId=" + channelId + "&maxResults=10&order=date&type=video&key=" + YOUTUBE_API_KEY;

				CompletionStage<JsonNode> videosResponseStage = ws.url(videosUrl).get().thenApply(videosResponse -> {
					JsonNode videosData = videosResponse.asJson();
					ArrayNode latestVideos = JsonNodeFactory.instance.arrayNode();

					// If videos are available, use the `videos` endpoint to get full descriptions
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

											// Use the full description from the video details
											ObjectNode videoNode = JsonNodeFactory.instance.objectNode();
											videoNode.put("videoId", videoId);
											videoNode.put("title", videoSnippet.get("title").asText());
											videoNode.put("description", videoSnippet.get("description").asText()); // Full description
											videoNode.put("thumbnailUrl", videoSnippet.get("thumbnails").get("default").get("url").asText());
											return videoNode;
										}
										return null;
									});

							// Add the video detail to the latestVideos array if it was successfully retrieved
							JsonNode fullVideoDetail = videoDetailResponse.toCompletableFuture().join();
							if (fullVideoDetail != null) {
								latestVideos.add(fullVideoDetail);
							}
						}
					}
					return latestVideos;
				});

// Wait for the videos response to complete and add it to the channelDetails map
				videosResponseStage.toCompletableFuture().join();
				channelDetails.put("latestVideos", videosResponseStage.toCompletableFuture().join().toString());


				return ok(views.html.channelprofile.render(channelDetails));
			} else {
				System.out.println("No channel items found in response.");
				return notFound("Channel not found");
			}
		});
	}

}
