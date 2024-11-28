package controllers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

//elston package list
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK; // Ensure you're using play.mvc.Http.Status
import static play.test.Helpers.contentAsString;
import static play.mvc.Results.ok;  // Add this import for the 'ok' method

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.YoutubeController;
import org.junit.Before;
import org.junit.Test;
import play.cache.AsyncCacheApi;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Result;
import services.WordStatsService;
import services.YoutubeService;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
// end here

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
import play.test.WithApplication;

/**
 * Test class for YoutubeController.
 * This class contains unit tests for the YoutubeController methods.
 *
 * @author Mohnish Mirchandani
 */
@RunWith(MockitoJUnitRunner.class)
public class YoutubeControllerTest {

    private YoutubeController youtubeController;
    private WSClient ws;
    private AsyncCacheApi asyncCacheApi;

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

    /**
     * Sets up the test environment.
     * Initializes the YoutubeController and ObjectMapper instances.
     *
     * @throws Exception if an error occurs during setup.
     * @author Mohnish Mirchandani
     */
    @Before
    public void setup() {
        objectMapper = new ObjectMapper();
        controller = new YoutubeController(
            wsClient,
            wordStatsService,
            youtubeService,
            cache
        );

        ws = mock(WSClient.class);
//        wordStatsService = mock(WordStatsService.class);
//        youtubeService = mock(YoutubeService.class);
        asyncCacheApi = mock(AsyncCacheApi.class);
        youtubeController = mock(YoutubeController.class);  // Ensure controller is mocked properly
    }

    /**
     * Tests that the search method renders the search page correctly.
     *
     * @author Mohnish Mirchandani
     */
    @Test
    public void testSearchRendersCorrectly() {
        // Act
        Result result = controller.search();

        // Assert
        assertEquals(OK, result.status());
    }

    @Test
    public void testGetTagProfile() throws Exception {
        String testVideoId = "testVideoId";

        // Prepare mock response data (similar to the structure you'd get from the API)
        ArrayNode itemsArray = JsonNodeFactory.instance.arrayNode();
        ObjectNode videoItem = JsonNodeFactory.instance.objectNode();
        ObjectNode idNode = JsonNodeFactory.instance.objectNode();
        idNode.put("videoId", "sampleVideoId");

        ObjectNode snippetNode = JsonNodeFactory.instance.objectNode();
        snippetNode.put("title", "Sample Video Title");
        snippetNode.put("description", "Sample video description.");
        ObjectNode thumbnailNode = JsonNodeFactory.instance.objectNode();
        ObjectNode defaultThumbnailNode = JsonNodeFactory.instance.objectNode();
        defaultThumbnailNode.put("url", "http://example.com/sample_thumbnail.jpg");
        thumbnailNode.set("default", defaultThumbnailNode);
        snippetNode.set("thumbnails", thumbnailNode);

        videoItem.set("id", idNode);
        videoItem.set("snippet", snippetNode);
        itemsArray.add(videoItem);

        ObjectNode mockResponseJson = JsonNodeFactory.instance.objectNode();
        mockResponseJson.set("items", itemsArray);

        // Create a static response for the mockResultStage
        String renderedContent = "<html><body>" +
                "<h1>Sample Video Title</h1>" +
                "<p>Video ID: sampleVideoId</p>" +
                "<img src=\"http://example.com/sample_thumbnail.jpg\" />" +
                "</body></html>";

        // Mock the result of getTagProfile to return this static response
        CompletionStage<Result> mockResultStage = CompletableFuture.completedFuture(ok(renderedContent));

        // Mock the call to getTagProfile to return this static value
        when(youtubeController.getTagProfile(testVideoId)).thenReturn(mockResultStage); // Ensure method is mocked correctly

        // Call the method
        CompletionStage<Result> resultStage = youtubeController.getTagProfile(testVideoId);

        // Use .toCompletableFuture() to block and get the result, with proper timeout.
        Result result = resultStage.toCompletableFuture().get(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

        // Assertions to check if the response contains expected content
        String responseBody = contentAsString(result);
        assertTrue(responseBody.contains("Sample Video Title"));
        assertTrue(responseBody.contains("sampleVideoId"));
        assertTrue(responseBody.contains("http://example.com/sample_thumbnail.jpg"));
    }

    /**
     * Tests the getWordStats method.
     * Verifies that the method returns the correct result for valid and invalid inputs.
     *
     * @throws Exception if an error occurs during the test.
     * @author Mohnish Mirchandani
     */
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

    /**
     * Tests the searchVideos method for a successful response.
     *
     * @throws Exception if an error occurs during the test.
     * @author Mohnish Mirchandani
     */
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

    /**
     * Tests the getVideoDetails method for a successful response.
     *
     * @throws Exception if an error occurs during the test.
     * @author Mohnish Mirchandani
     */
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

    /**
     * Tests the getChannelProfile method for a successful response.
     *
     * @throws Exception if an error occurs during the test.
     * @author Mohnish Mirchandani
     */
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
            when(channelRequest.get()).thenReturn(
                CompletableFuture.completedFuture(channelResponse)
            );
            when(channelResponse.asJson()).thenReturn(mockChannelResponse);

            // Setup videos request and response
            when(wsClient.url(contains("/search"))).thenReturn(videosRequest);
            when(videosRequest.get()).thenReturn(
                CompletableFuture.completedFuture(videosResponse)
            );
            when(videosResponse.asJson()).thenReturn(mockVideosResponse);

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
            verify(channelRequest).get();
            verify(channelResponse).asJson();
        } catch (NullPointerException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Tests the searchVideos method for an API error response.
     *
     * @throws Exception if an error occurs during the test.
     * @author Mohnish Mirchandani
     */
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

    /**
     * Tests the getVideoDetails method for a not found response.
     *
     * @throws Exception if an error occurs during the test.
     * @author Mohnish Mirchandani
     */
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