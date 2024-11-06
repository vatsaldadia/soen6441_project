import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import play.libs.ws.*;
import play.mvc.Result;

public class YouTubeServiceTest {

	private YouTubeService youtubeService;

	@Mock
	private WSClient ws;

	@Mock
	private WSRequest wsRequest;

	@Mock
	private WSResponse wsResponse;

	@Before
	public void setup() {
		MockitoAnnotations.openMocks(this);

		// Mock the WS chain
		when(ws.url(anyString())).thenReturn(wsRequest);
		when(wsRequest.addQueryParameter(anyString(), anyString())).thenReturn(
			wsRequest
		);

		youtubeService = new YouTubeService(ws);
	}

	@Test
	public void testSearchVideos_Success()
		throws ExecutionException, InterruptedException {
		// Create mock response JSON
		ObjectNode mockResponse = JsonNodeFactory.instance.objectNode();
		ArrayNode items = mockResponse.putArray("items");
		ObjectNode item = items.addObject();
		item.put("id", "videoId");
		ObjectNode snippet = item.putObject("snippet");
		snippet.put("title", "Test Video");
		snippet.put("description", "Test Description");

		// Mock successful response
		when(wsResponse.getStatus()).thenReturn(200);
		when(wsResponse.asJson()).thenReturn(mockResponse);
		when(wsRequest.get()).thenReturn(
			CompletableFuture.completedFuture(wsResponse)
		);

		// Execute search
		CompletionStage<Result> futureResult = youtubeService.searchVideos(
			"test query"
		);
		Result result = futureResult.toCompletableFuture().get();

		// Verify response
		assertEquals(OK, result.status());
		verify(ws).url(contains("youtube.com/search"));
		verify(wsRequest).addQueryParameter("q", "test query");
		verify(wsRequest).addQueryParameter("maxResults", "10");
		verify(wsRequest).addQueryParameter("type", "video");
	}
}
