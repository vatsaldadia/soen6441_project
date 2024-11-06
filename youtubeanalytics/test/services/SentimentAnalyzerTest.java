import static org.junit.Assert.*;
import static play.test.Helpers.*;

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
import org.junit.Test;
import play.libs.ws.*;
import play.mvc.*;
import play.test.WithApplication;
import services.ReadabilityCalculator;
import services.SentimentAnalyzer;

public class SentimentAnalyzerTest extends WithApplication {

	private SentimentAnalyzer sentimentAnalyzer;
	private WSClient ws;

	@Before
	public void setup() {
		// Initialize the service
		ws = app.injector().instanceOf(WSClient.class);
		youtubeService = new YouTubeService(ws);
	}

	// @Test
	// public void testSearchVideos() {
	// 	// Test searching videos
	// 	CompletionStage<ObjectNode> futureResponse =
	// 		youtubeService.searchVideos("test query");

	// 	ObjectNode response = futureResponse.toCompletableFuture().join();

	// 	assertNotNull(response);
	// 	assertTrue(response.has("items"));
	// }

	// @Test
	// public void testModifyResponse() {
	// 	// Create a sample response
	// 	ObjectNode sampleResponse = Json.newObject();
	// 	ArrayNode items = Json.newArray();
	// 	ObjectNode item = Json.newObject();
	// 	// Add necessary fields to item
	// 	items.add(item);
	// 	sampleResponse.set("items", items);

	// 	CompletionStage<ObjectNode> futureModified =
	// 		youtubeService.modifyResponse(sampleResponse);

	// 	ObjectNode modified = futureModified.toCompletableFuture().join();

	// 	assertNotNull(modified);
	// 	assertTrue(modified.has("items"));
	// 	// Add more specific assertions
	// }

	@Test
	public void testSentimentAnalysis() {
		String description = "This is a happy wonderful amazing video";
		String sentiment = youtubeService.analyzeSentiment(description);

		assertEquals(":-)", sentiment);
	}

	@Test
	public void testReadabilityScore() {
		String description = "This is a test description.";
		double score = youtubeService.calculateReadabilityScore(description);

		assertTrue(score >= 0);
	}
}
