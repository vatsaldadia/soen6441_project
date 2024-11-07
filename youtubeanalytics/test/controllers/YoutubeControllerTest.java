package controllers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import models.YoutubeVideo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Result;
import services.ReadabilityCalculator;
import services.WordStatsService;
import views.html.search;
import views.html.wordstats;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
@MockitoSettings(strictness = Strictness.LENIENT)
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
//    private static final String CHANNEL_API_URL =

    @BeforeEach
    public void setUp() {
        when(wsClient.url(eq(SEARCH_API_URL))).thenReturn(searchRequest);

        // Mock the search API request and response
        when(searchRequest.addQueryParameter(any(String.class), any(String.class))).thenReturn(searchRequest);
        when(searchRequest.get()).thenReturn(CompletableFuture.completedFuture(searchResponse));

    }

    @Test
    public void testSearch() {
        // Execute the search() method
        Result result = youtubeController.search();

        // Verify the status is OK
        assertEquals(OK, result.status());

        // Verify that the rendered content is as expected
        String renderedContent = contentAsString(result);
        assertEquals(search.render().body(), renderedContent);
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
        emptyResponse.put("sentiment", ":-|");

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

    @Test
    public void testFetchVideosBySearchTerm() throws Exception {
        // Prepare a mock JSON response
        ObjectNode mockResponse = JsonNodeFactory.instance.objectNode();
        ArrayNode itemsArray = mockResponse.putArray("items");

        // Add a video entry to the items array
        ObjectNode videoItem = itemsArray.addObject();
        videoItem.putObject("id").put("videoId", "12345");
        ObjectNode snippet = videoItem.putObject("snippet");
        snippet.put("title", "Sample Video Title");
        snippet.put("description", "Sample Video Description");
        snippet.putObject("thumbnails").putObject("default").put("url", "http://sample.thumbnail.url");
        snippet.put("channelTitle", "Sample Channel Title");
        snippet.put("publishedAt", "2023-01-01T00:00:00Z");
        snippet.put("viewCount", 1000);

        // Mock the response from WSClient
        when(searchRequest.get()).thenReturn(CompletableFuture.completedFuture(searchResponse));
        when(searchResponse.getStatus()).thenReturn(200);
        when(searchResponse.asJson()).thenReturn(mockResponse);

        // Execute the fetchVideosBySearchTerm method
        List<YoutubeVideo> videos = youtubeController.fetchVideosBySearchTerm(MOCK_QUERY);

        // Verify the parsed result
        assertEquals(1, videos.size());
        YoutubeVideo video = videos.get(0);
        assertEquals("12345", video.getVideoId());
        assertEquals("Sample Video Title", video.getTitle());
        assertEquals("Sample Video Description", video.getDescription());
        assertEquals("http://sample.thumbnail.url", video.getThumbnailUrl());
        assertEquals("Sample Channel Title", video.getChannelTitle());
        assertEquals("2023-01-01T00:00:00Z", video.getPublishedAt());
        assertEquals(1000L, video.getViewCount());
    }

}
