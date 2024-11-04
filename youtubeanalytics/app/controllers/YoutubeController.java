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

public class YoutubeController extends Controller {

	private final WSClient ws;
	private static final String YOUTUBE_API_KEY =
		"AIzaSyBn3hOC9y7PsDrQ62Xuj5M_P83ASq6GZRY";
	private static final String YOUTUBE_URL =
		"https://www.googleapis.com/youtube/v3";

	@Inject
	public YoutubeController(WSClient ws) {
		this.ws = ws;
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
}
