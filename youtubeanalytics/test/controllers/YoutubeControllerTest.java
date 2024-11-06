package controllers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Result;
import services.ReadabilityCalculator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;
import static play.test.Helpers.contentAsString;

@ExtendWith(MockitoExtension.class)
public class YoutubeControllerTest {

    @Mock
    private WSClient wsClient;

    @Mock
    private WSRequest searchRequest;

    @Mock
    private WSRequest videoDetailsRequest;

    @Mock
    private WSResponse searchResponse;

    @Mock
    private WSResponse videoDetailsResponse;

    @InjectMocks
    private YoutubeController youtubeController;

    private static final String MOCK_QUERY = "test query";
    private static final String MOCK_VIDEO_ID = "mockVideoId";
    private static final String SEARCH_API_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final String VIDEOS_API_URL = "https://www.googleapis.com/youtube/v3/videos";

    @BeforeEach
    public void setUp() {
        // Mock the WSClient to return different requests based on the URL
        when(wsClient.url(eq(SEARCH_API_URL))).thenReturn(searchRequest);
//        when(wsClient.url(eq(VIDEOS_API_URL))).thenReturn(videoDetailsRequest);

        // Mock the search API request and response
        when(searchRequest.addQueryParameter(any(String.class), any(String.class))).thenReturn(searchRequest);
        when(searchRequest.get()).thenReturn(CompletableFuture.completedFuture(searchResponse));

        // Mock the video details API request and response
//        when(videoDetailsRequest.addQueryParameter(any(String.class), any(String.class))).thenReturn(videoDetailsRequest);
//        when(videoDetailsRequest.get()).thenReturn(CompletableFuture.completedFuture(videoDetailsResponse));
    }

    @Test
    public void testSearchVideos_Success() throws Exception {

        when(wsClient.url(eq(VIDEOS_API_URL))).thenReturn(videoDetailsRequest);
        when(videoDetailsRequest.addQueryParameter(any(String.class), any(String.class))).thenReturn(videoDetailsRequest);
        when(videoDetailsRequest.get()).thenReturn(CompletableFuture.completedFuture(videoDetailsResponse));
        // Set up a mock response for the initial search API call
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode searchApiResponse = JsonNodeFactory.instance.objectNode();
        ObjectNode videoItem = JsonNodeFactory.instance.objectNode();
        ObjectNode idNode = JsonNodeFactory.instance.objectNode();
        idNode.put("videoId", MOCK_VIDEO_ID);
        videoItem.set("id", idNode);
        searchApiResponse.set("items", JsonNodeFactory.instance.arrayNode().add(videoItem));

        when(searchResponse.getStatus()).thenReturn(OK);
        when(searchResponse.asJson()).thenReturn(searchApiResponse);

        // Set up a mock response for the nested video details API call
        ObjectNode videoDetailsApiResponse = JsonNodeFactory.instance.objectNode();
        ObjectNode videoSnippet = JsonNodeFactory.instance.objectNode();
        videoSnippet.put("description", "Mock description");
        videoDetailsApiResponse.set("items", JsonNodeFactory.instance.arrayNode().add(
                JsonNodeFactory.instance.objectNode().set("snippet", videoSnippet)
        ));

        when(videoDetailsResponse.getStatus()).thenReturn(OK);
        when(videoDetailsResponse.asJson()).thenReturn(videoDetailsApiResponse);

        // Mock ReadabilityCalculator (assuming static methods)
        mockStatic(ReadabilityCalculator.class);
        when(ReadabilityCalculator.calculateFleschKincaidGradeLevel(anyString())).thenReturn(5.0);
        when(ReadabilityCalculator.calculateFleschReadingScore(anyString())).thenReturn(60.0);
        when(ReadabilityCalculator.calculateGradeAvg(anyList())).thenReturn(5.0);
        when(ReadabilityCalculator.calculateScoreAvg(anyList())).thenReturn(60.0);

        // Perform the searchVideos call
        CompletionStage<Result> resultStage = youtubeController.searchVideos(MOCK_QUERY);
        Result result = resultStage.toCompletableFuture().join();

        // Verify response and status
        assertEquals(OK, result.status());
        String responseBody = contentAsString(result);
        JsonNode responseJson = mapper.readTree(responseBody);

        assertEquals("mockVideoId", responseJson.get("items").get(0).get("id").get("videoId").asText());
        assertEquals("5.00", responseJson.get("fleschKincaidGradeLevelAvg").asText());
        assertEquals("60.00", responseJson.get("fleschReadingScoreAvg").asText());
    }

    @Test
    public void testSearchVideos_NoItemsInResponse() {
        // Mock search API response with no items
        ObjectNode emptyResponse = JsonNodeFactory.instance.objectNode();
        emptyResponse.set("items", JsonNodeFactory.instance.arrayNode());
        emptyResponse.put("fleschKincaidGradeLevelAvg", "0.00");
        emptyResponse.put("fleschReadingScoreAvg", "0.00");

        when(searchResponse.getStatus()).thenReturn(OK);
        when(searchResponse.asJson()).thenReturn(emptyResponse);
        when(searchRequest.get()).thenReturn(CompletableFuture.completedFuture(searchResponse));

        // Execute and verify
        CompletionStage<Result> resultStage = youtubeController.searchVideos(MOCK_QUERY);
        Result result = resultStage.toCompletableFuture().join();

        assertEquals(OK, result.status());
        String responseBody = contentAsString(result);
        assertEquals(emptyResponse.toString(), responseBody); // Expecting unchanged empty response
    }

    @Test
    public void testSearchVideos_ApiError() {
        // Simulate an error response from the search API
        when(searchResponse.getStatus()).thenReturn(INTERNAL_SERVER_ERROR);
        when(searchResponse.getBody()).thenReturn("YouTube API error");

        // Perform the searchVideos call
        CompletionStage<Result> resultStage = youtubeController.searchVideos(MOCK_QUERY);
        Result result = resultStage.toCompletableFuture().join();

        // Verify that the response is an internal server error
        assertEquals(INTERNAL_SERVER_ERROR, result.status());
        assertEquals("YouTube API error: YouTube API error", contentAsString(result));
    }
}
