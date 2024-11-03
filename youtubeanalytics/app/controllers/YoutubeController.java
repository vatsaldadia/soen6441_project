package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.CompletionStage;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import play.libs.ws.*;
import play.mvc.*;
import services.SentimentAnalyzer;

public class YoutubeController extends Controller {

	private final WSClient ws;
	private static final String YOUTUBE_API_KEY =
		"AIzaSyBn3hOC9y7PsDrQ62Xuj5M_P83ASq6GZRY";
	private static final String YOUTUBE_URL =
		"https://www.googleapis.com/youtube/v3/search";

	@Inject
	public YoutubeController(WSClient ws) {
		this.ws = ws;
	}

	public ObjectNode modifyResponse(ObjectNode youtubeResponse) {
		if (youtubeResponse.has("items")) {
			JsonNode items = (ArrayNode) youtubeResponse.get("items");
			ObjectNode modifiedResponse =
				(ObjectNode) youtubeResponse.deepCopy();
			ArrayNode modifiedItems = JsonNodeFactory.instance.arrayNode();
			StreamSupport.stream(items.spliterator(), false)
				.map(item -> {
					ObjectNode videoNode = (ObjectNode) item;
					String description = videoNode
						.get("snippet")
						.get("description")
						.asText();
					String sentiment = SentimentAnalyzer.analyzeDescription(
						description
					);
					System.out.println(sentiment);
					videoNode.put("sentiment", sentiment);
					return videoNode;
				})
				.forEach(modifiedItems::add);

			modifiedResponse.set("items", modifiedItems);
			return modifiedResponse;
		}

		return youtubeResponse;
	}

	public CompletionStage<Result> searchVideos(String query) {
		return ws
			.url(YOUTUBE_URL)
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
