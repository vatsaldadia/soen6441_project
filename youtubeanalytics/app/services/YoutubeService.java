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
import play.libs.ws.WSClient;

public class YoutubeService {

	private final WSClient ws;
	private final AsyncCacheApi cache;
	private static final String YOUTUBE_API_KEY =
		"AIzaSyCRTltXLPxW3IwxMf-WH0BagUipQy_7TuQ";
	private static final String YOUTUBE_URL =
		"https://www.googleapis.com/youtube/v3";

	@Inject
	public YoutubeService(WSClient ws, AsyncCacheApi cache) {
		this.ws = ws;
		this.cache = cache;
	}

	public CompletionStage<ObjectNode> modifyResponse(
		ObjectNode youtubeResponse
	) {
		if (youtubeResponse.has("items")) {
			JsonNode items = youtubeResponse.get("items");
			ObjectNode modifiedResponse = youtubeResponse.deepCopy();
			ArrayNode modifiedItems = JsonNodeFactory.instance.arrayNode();
			List<CompletableFuture<ObjectNode>> futures = new ArrayList<>();

			for (JsonNode item : items) {
				ObjectNode videoNode = (ObjectNode) item;
				String videoId = videoNode.get("id").get("videoId").asText();
				CompletionStage<ObjectNode> future = ws
					.url(YOUTUBE_URL + "/videos")
					.addQueryParameter("part", "snippet")
					.addQueryParameter("id", videoId)
					.addQueryParameter("key", YOUTUBE_API_KEY)
					.get()
					.thenApply(response -> {
						if (response.getStatus() == 200) {
							String description = response
								.asJson()
								.get("items")
								.get(0)
								.get("snippet")
								.get("description")
								.asText();

							double grade =
								ReadabilityCalculator.calculateFleschKincaidGradeLevel(
									description
								);
							double score =
								ReadabilityCalculator.calculateFleschReadingScore(
									description
								);
							double sentimentValue =
								SentimentAnalyzer.analyzeDescription(
									description
								);

							videoNode.put("description", description);
							videoNode.put(
								"fleschKincaidGradeLevel",
								String.format("%.2f", grade)
							);
							videoNode.put(
								"fleschReadingScore",
								String.format("%.2f", score)
							);
							return videoNode;
						}
						return videoNode;
					});
				futures.add(future.toCompletableFuture());
			}

			return CompletableFuture.allOf(
				futures.toArray(new CompletableFuture[0])
			).thenApply(v -> {
				futures
					.stream()
					.map(CompletableFuture::join)
					.forEach(modifiedItems::add);

				double gradeAvg = StreamSupport.stream(
					modifiedItems.spliterator(),
					false
				)
					.mapToDouble(item ->
						Double.parseDouble(
							item.get("fleschKincaidGradeLevel").asText()
						)
					)
					.average()
					.orElse(0.0);

				double scoreAvg = StreamSupport.stream(
					modifiedItems.spliterator(),
					false
				)
					.mapToDouble(item ->
						Double.parseDouble(
							item.get("fleschReadingScore").asText()
						)
					)
					.average()
					.orElse(0.0);

				List<String> descriptions = StreamSupport.stream(
					modifiedItems.spliterator(),
					false
				)
					.map(item -> item.get("description").asText())
					.collect(Collectors.toList());

				String sentiment = SentimentAnalyzer.analyzeSentiment(
					descriptions
				);

				modifiedResponse.put("sentiment", sentiment);
				modifiedResponse.put(
					"fleschKincaidGradeLevelAvg",
					String.format("%.2f", gradeAvg)
				);
				modifiedResponse.put(
					"fleschReadingScoreAvg",
					String.format("%.2f", scoreAvg)
				);
				modifiedResponse.put("items", modifiedItems);

				return modifiedResponse;
			});
		}
		return CompletableFuture.completedFuture(youtubeResponse);
	}
}
