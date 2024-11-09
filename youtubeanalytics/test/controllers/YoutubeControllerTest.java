package controllers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import play.cache.AsyncCacheApi;
import play.libs.ws.*;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Result;
import play.test.WithApplication;
import services.WordStatsService;
import services.YoutubeService;

@RunWith(MockitoJUnitRunner.class)
public class YoutubeControllerTest {

	@Mock
	private WSClient wsClient;

	@Mock
	private WSRequest wsRequest;

	@Mock
	private WSResponse wsResponse;

	@Mock
	private WordStatsService wordStatsService;

	@Mock
	private YoutubeService youtubeService;

	@Mock
	private AsyncCacheApi cache;

	private YoutubeController controller;
	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = new ObjectMapper();
		controller = new YoutubeController(
			wsClient,
			wordStatsService,
			youtubeService,
			cache
		);
	}

	@Test
	public void testSearchRendersCorrectly() {
		// Act
		Result result = controller.search();

		// Assert
		assertEquals(OK, result.status());
	}

	@Test
	public void testWordStats() throws Exception {
		// Arrange
		YoutubeController mockController = mock(YoutubeController.class);
		String jsonResponse =
			"{" +
			"\"items\": [{" +
			"\"id\": {\"videoId\": \"123\"}," +
			"\"snippet\": {" +
			"\"title\": \"Test Video\"," +
			"\"description\": \"Hello world hello\"," +
			"\"thumbnails\": {\"default\": {\"url\": \"http://example.com/thumb.jpg\"}}," +
			"\"channelTitle\": \"Test Channel\"," +
			"\"publishedAt\": \"2023-01-01T00:00:00Z\"" +
			"}}]}";

		// Mock the searchVideoCall response
		when(wsResponse.getStatus()).thenReturn(200);
		when(wsResponse.asJson()).thenReturn(
			objectMapper.readTree(jsonResponse)
		);
		when(mockController.searchVideoCall("test")).thenReturn(
			CompletableFuture.completedFuture(wsResponse)
		);
		when(mockController.getWordStats("test")).thenCallRealMethod();

		// Act
		CompletionStage<Result> result = mockController.getWordStats("test");
		Result resultValue = result.toCompletableFuture().get();

		// Assert
		assertEquals(OK, resultValue.status());
		verify(wsResponse).getStatus();
		verify(wsResponse).asJson();

		// Failed case
		when(wsResponse.getStatus()).thenReturn(400);
		result = mockController.getWordStats("test");
		resultValue = result.toCompletableFuture().get();

		assertNull(resultValue);
		verify(wsResponse, times(2)).getStatus();
	}

	@Test
	public void testSearchVideos_Success() throws Exception {
		// Prepare test data
		String query = "test query";
		String mockResponseJson =
			"{\"items\": [{\"id\": {\"videoId\": \"123\"}, \"snippet\": {\"title\": \"Test Video\"}}]}";
		JsonNode mockJsonResponse = objectMapper.readTree(mockResponseJson);
		ObjectNode modifiedResponse = objectMapper.createObjectNode();
		modifiedResponse.put("status", "success");

		when(cache.getOrElseUpdate(eq(query), any(), anyInt())).thenAnswer(
			invocation -> {
				// Execute the Callable passed to getOrElseUpdate
				java.util.concurrent.Callable<?> callable =
					invocation.getArgument(1);
				return callable.call();
			}
		);

		// Setup mocks
		when(wsClient.url(anyString())).thenReturn(wsRequest);
		when(wsRequest.addQueryParameter(anyString(), anyString())).thenReturn(
			wsRequest
		);
		when(wsRequest.get()).thenReturn(
			CompletableFuture.completedFuture(wsResponse)
		);
		when(wsResponse.getStatus()).thenReturn(200);
		when(wsResponse.asJson()).thenReturn(mockJsonResponse);
		when(youtubeService.modifyResponse(any(ObjectNode.class))).thenReturn(
			CompletableFuture.completedFuture(modifiedResponse)
		);

		// Execute test
		CompletionStage<Result> resultStage = controller.searchVideos(query);
		Result result = resultStage.toCompletableFuture().get();

		// Verify
		assertEquals(200, result.status());
		verify(wsClient).url(contains("/youtube/v3/search"));
		verify(youtubeService).modifyResponse(any(ObjectNode.class));
		verify(cache).getOrElseUpdate(eq(query), any(), anyInt());
	}

	@Test
	public void testGetVideoDetails_Success() throws Exception {
		// Prepare test data
		String videoId = "test123";
		String mockResponseJson =
			"{" +
			"\"items\": [{" +
			"\"snippet\": {" +
			"\"title\": \"Test Video\"," +
			"\"description\": \"Test Description\"," +
			"\"thumbnails\": {\"default\": {\"url\": \"http://test.com\"}}," +
			"\"channelTitle\": \"Test Channel\"," +
			"\"publishedAt\": \"2023-01-01\"," +
			"\"tags\": [\"tag1\", \"tag2\"]" +
			"}," +
			"\"statistics\": {\"viewCount\": \"1000\"}" +
			"}]}";
		JsonNode mockJsonResponse = objectMapper.readTree(mockResponseJson);

		// Setup mocks
		when(youtubeService.getVideo(videoId)).thenReturn(
			CompletableFuture.completedFuture(wsResponse)
		);
		when(wsResponse.getStatus()).thenReturn(200);
		when(wsResponse.asJson()).thenReturn(mockJsonResponse);

		// Execute test
		CompletionStage<Result> resultStage = controller.getVideoDetails(
			videoId
		);
		Result result = resultStage.toCompletableFuture().get();

		// Verify
		assertEquals(200, result.status());
		verify(youtubeService).getVideo(videoId);
		verify(wsResponse).getStatus();
		verify(wsResponse).asJson();
	}

	@Test
	public void testGetChannelProfile_Success() throws Exception {
		try {
			// Prepare test data
			String channelId = "channel123";
			String mockChannelJson =
				"{" +
				"\"items\": [{" +
				"\"id\": \"channel123\"," +
				"\"snippet\": {" +
				"\"title\": \"Test Channel\"," +
				"\"description\": \"Test Description\"," +
				"\"thumbnails\": {\"default\": {\"url\": \"http://test.com\"}}," +
				"\"country\": \"US\"" +
				"}," +
				"\"statistics\": {" +
				"\"subscriberCount\": \"1000\"," +
				"\"viewCount\": \"5000\"," +
				"\"videoCount\": \"100\"" +
				"}" +
				"}]}";

			String mockVideosJson = "{\"items\": []}";
			JsonNode mockChannelResponse = objectMapper.readTree(
				mockChannelJson
			);
			JsonNode mockVideosResponse = objectMapper.readTree(mockVideosJson);

			// Create separate response mocks for each call
			WSResponse channelResponse = mock(WSResponse.class);
			WSResponse videosResponse = mock(WSResponse.class);

			// Create separate request mocks for each call
			WSRequest channelRequest = mock(WSRequest.class);
			WSRequest videosRequest = mock(WSRequest.class);

			// Setup channel request and response
			when(wsClient.url(contains("/channels"))).thenReturn(
				channelRequest
			);
			// when(
			// 	channelRequest.addQueryParameter(anyString(), anyString())
			// ).thenReturn(channelRequest);
			when(channelRequest.get()).thenReturn(
				CompletableFuture.completedFuture(channelResponse)
			);
			when(channelResponse.asJson()).thenReturn(mockChannelResponse);
			// when(channelResponse.getStatus()).thenReturn(200);

			// Setup videos request and response
			when(wsClient.url(contains("/search"))).thenReturn(videosRequest);
			// when(
			// 	videosRequest.addQueryParameter(anyString(), anyString())
			// ).thenReturn(videosRequest);
			when(videosRequest.get()).thenReturn(
				CompletableFuture.completedFuture(videosResponse)
			);
			when(videosResponse.asJson()).thenReturn(mockVideosResponse);
			// when(videosResponse.getStatus()).thenReturn(200);

			// Execute test
			CompletionStage<Result> resultStage = controller.getChannelProfile(
				channelId
			);
			Result result = resultStage.toCompletableFuture().get();

			// Verify
			assertEquals(200, result.status());

			// Verify API calls
			verify(wsClient).url(contains("/channels"));
			verify(wsClient).url(contains("/search"));
			// verify(channelRequest, atLeastOnce()).addQueryParameter("", "");
			// verify(videosRequest, atLeastOnce()).addQueryParameter("", "");
			verify(channelRequest).get();
			// verify(videosRequest).get();
			verify(channelResponse).asJson();
			// verify(videosResponse).asJson();
		} catch (NullPointerException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void testSearchVideos_ApiError() throws Exception {
		String query = "query";
		// Setup cache mock
		when(cache.getOrElseUpdate(eq(query), any(), anyInt())).thenAnswer(
			invocation -> {
				// Execute the Callable passed to getOrElseUpdate
				java.util.concurrent.Callable<?> callable =
					invocation.getArgument(1);
				return callable.call();
			}
		);
		// Setup mocks for error scenario
		when(wsClient.url(anyString())).thenReturn(wsRequest);
		when(wsRequest.addQueryParameter(anyString(), anyString())).thenReturn(
			wsRequest
		);
		when(wsRequest.get()).thenReturn(
			CompletableFuture.completedFuture(wsResponse)
		);
		when(wsResponse.getStatus()).thenReturn(500);
		when(wsResponse.getBody()).thenReturn("API Error");

		// Execute test
		CompletionStage<Result> resultStage = controller.searchVideos("query");
		Result result = resultStage.toCompletableFuture().get();

		// Verify
		assertEquals(500, result.status());
		verify(wsClient).url(contains("/youtube/v3/search"));
		verify(cache).getOrElseUpdate(eq(query), any(), anyInt());
		verify(wsResponse).getBody();
	}

	@Test
	public void testGetVideoDetails_NotFound() throws Exception {
		String videoId = "nonexistent";
		String mockResponseJson = "{\"items\": []}";
		JsonNode mockJsonResponse = objectMapper.readTree(mockResponseJson);

		when(youtubeService.getVideo(videoId)).thenReturn(
			CompletableFuture.completedFuture(wsResponse)
		);
		when(wsResponse.getStatus()).thenReturn(200);
		when(wsResponse.asJson()).thenReturn(mockJsonResponse);

		CompletionStage<Result> resultStage = controller.getVideoDetails(
			videoId
		);
		Result result = resultStage.toCompletableFuture().get();

		assertEquals(404, result.status());
	}
}
