package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import play.libs.ws.*;
import play.mvc.*;
import services.ReadabilityCalculator;
import services.SentimentAnalyzer;

public class YoutubeController extends Controller {

	private final WSClient ws;
	private static final String query = "";
	private final List<String> descriptions;
	private static final String YOUTUBE_API_KEY =
		"AIzaSyBn3hOC9y7PsDrQ62Xuj5M_P83ASq6GZRY";
	private static final String YOUTUBE_URL =
		"https://www.googleapis.com/youtube/v3";

	@Inject
	public YoutubeController(WSClient ws) {
		this.ws = ws;
		this.descriptions = new ArrayList<>();
	}

	public ObjectNode modifyHeading() {
		ObjectNode heading = JsonNodeFactory.instance.objectNode();
		heading.put("search_terms", query);
        return heading;
    }

	public ObjectNode modifyResponse(ObjectNode youtubeResponse) {
		if (youtubeResponse.has("items")) {
			JsonNode items = youtubeResponse.get("items");
			ObjectNode modifiedResponse =
                    youtubeResponse.deepCopy();
			ArrayNode modifiedItems = JsonNodeFactory.instance.arrayNode();
			StreamSupport.stream(items.spliterator(), false)
				.map(item -> {
					ObjectNode videoNode = (ObjectNode) item;
					String videoId = videoNode
						.get("id")
						.get("videoId")
						.asText();
					String description =
							ws.url(YOUTUBE_URL + "/videos")
							.addQueryParameter("part", "snippet")
							.addQueryParameter("id", videoId)
							.addQueryParameter("key", YOUTUBE_API_KEY)
							.get()
							.thenApply(response -> {
								if (response.getStatus() == 200) {
									System.out.println(response.asJson());
									System.out.println("hi");
								 	return response.asJson()
													.get("items")
													.get(0)
										 			.get("snippets")
													.get("description")
													.asText();
							}
							return null;
						}).toString();
//					System.out.println(description);
					descriptions.add(description);

					String sentiment = SentimentAnalyzer.analyzeDescription(
							description
					);

					double fleschKincaidGradeLevel = ReadabilityCalculator.calculateFleschKincaidGradeLevel(
							description
					);
					double fleschReadingScore = ReadabilityCalculator.calculateFleschReadingScore(
							description
					);

					videoNode.put("sentiment", sentiment);
					videoNode.put("fleschKincaidGradeLevel", String.format("%.2f", fleschKincaidGradeLevel));
					videoNode.put("fleschReadingScore", String.format("%.2f", fleschReadingScore));
					return videoNode;
				})
				.forEach(modifiedItems::add);

//			modifiedResponse.set("heading", modifyHeading());
			modifiedResponse.set("items", modifiedItems);
			return modifiedResponse;
		}

		return youtubeResponse;
	}

	public CompletionStage<Result> searchVideos(String query) {
		query = query.trim();
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
