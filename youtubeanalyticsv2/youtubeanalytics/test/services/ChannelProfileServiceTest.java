package services;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import akka.actor.ActorRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import play.libs.ws.*;

public class ChannelProfileServiceTest {

    @Mock
    private WSClient wsClient;

    @Mock
    private WSRequest wsRequest;

    @Mock
    private WSResponse wsResponse;

    private ChannelProfileService service;
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        service = new ChannelProfileService(wsClient);
    }

    @Test
    public void testFetchChannelDetails_Success() throws Exception {
        // Prepare mock data
        String channelId = "channel123";
        String mockResponseJson = "{"
                + "\"items\": [{"
                + "\"id\": \"channel123\","
                + "\"snippet\": {"
                + "\"title\": \"Test Channel\","
                + "\"description\": \"This is a mock channel description.\","
                + "\"thumbnails\": {\"default\": {\"url\": \"http://mock.com\"}},"
                + "\"country\": \"US\""
                + "},"
                + "\"statistics\": {"
                + "\"subscriberCount\": \"1000\","
                + "\"viewCount\": \"5000\","
                + "\"videoCount\": \"100\""
                + "}"
                + "}]"
                + "}";
        JsonNode mockResponse = objectMapper.readTree(mockResponseJson);

        // Setup mocks
        when(wsClient.url(contains("/youtube/v3/channels"))).thenReturn(wsRequest);
        when(wsRequest.addQueryParameter(anyString(), anyString())).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
        when(wsResponse.asJson()).thenReturn(mockResponse);

        // Execute service method
        CompletionStage<JsonNode> resultFuture = service.fetchChannelDetails(channelId);
        JsonNode profile = resultFuture.toCompletableFuture().get();

        // Verify results
        assertNotNull(profile);
        assertEquals("Test Channel", profile.get("items").get(0).get("snippet").get("title").asText());
        assertEquals("This is a mock channel description.", profile.get("items").get(0).get("snippet").get("description").asText());
        assertEquals("1000", profile.get("items").get(0).get("statistics").get("subscriberCount").asText());
        assertEquals("5000", profile.get("items").get(0).get("statistics").get("viewCount").asText());
        assertEquals("100", profile.get("items").get(0).get("statistics").get("videoCount").asText());

        // Verify interactions
        verify(wsClient).url(contains("/youtube/v3/channels"));
        verify(wsRequest).addQueryParameter(eq("id"), eq(channelId));
        verify(wsRequest).addQueryParameter(eq("part"), eq("snippet,statistics"));
        verify(wsRequest).addQueryParameter(eq("key"), anyString());
        verify(wsRequest).get();
        verify(wsResponse).asJson();
    }

    @Test
    public void testFetchChannelDetails_NotFound() throws Exception {
        // Prepare mock empty response
        String channelId = "invalidChannel";
        String mockResponseJson = "{\"items\": []}";
        JsonNode mockResponse = objectMapper.readTree(mockResponseJson);

        // Setup mocks
        when(wsClient.url(contains("/youtube/v3/channels"))).thenReturn(wsRequest);
        when(wsRequest.addQueryParameter(anyString(), anyString())).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
        when(wsResponse.getStatus()).thenReturn(200); // Ensure the status is 200 OK
        when(wsResponse.asJson()).thenReturn(mockResponse);

        // Execute service method
        CompletionStage<JsonNode> resultFuture = service.fetchChannelDetails(channelId);
        JsonNode profile = resultFuture.toCompletableFuture().get();

        // Verify results
        assertTrue(profile.get("items").isEmpty());

        // Verify interactions
        verify(wsClient).url(contains("/youtube/v3/channels"));
        verify(wsRequest).addQueryParameter(eq("id"), eq(channelId));
        verify(wsRequest).addQueryParameter(eq("part"), eq("snippet,statistics"));
        verify(wsRequest).addQueryParameter(eq("key"), anyString());
        verify(wsRequest).get();
        verify(wsResponse).asJson();
    }

    @Test
    public void testFetchChannelDetails_Failure() throws Exception {
        // Prepare mock error response
        String channelId = "channel123";
        when(wsClient.url(contains("/youtube/v3/channels"))).thenReturn(wsRequest);
        when(wsRequest.addQueryParameter(anyString(), anyString())).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
        when(wsResponse.getStatus()).thenReturn(500); // Simulate server error

        // Execute service method
        CompletionStage<JsonNode> resultFuture = service.fetchChannelDetails(channelId);

        // Verify interactions
        verify(wsClient).url(contains("/youtube/v3/channels"));
        verify(wsRequest).addQueryParameter(eq("id"), eq(channelId));
        verify(wsRequest).addQueryParameter(eq("part"), eq("snippet,statistics"));
        verify(wsRequest).addQueryParameter(eq("key"), anyString());
        verify(wsRequest).get();
    }

    @Test
    public void testParseChannelDetails() {
        // Prepare mock data
        String mockResponseJson = "{"
                + "\"items\": [{"
                + "\"id\": \"channel123\","
                + "\"snippet\": {"
                + "\"title\": \"Test Channel\","
                + "\"description\": \"This is a mock channel description.\","
                + "\"thumbnails\": {\"default\": {\"url\": \"http://mock.com\"}},"
                + "\"country\": \"US\""
                + "},"
                + "\"statistics\": {"
                + "\"subscriberCount\": \"1000\","
                + "\"viewCount\": \"5000\","
                + "\"videoCount\": \"100\""
                + "}"
                + "}]"
                + "}";
        JsonNode mockResponse;
        try {
            mockResponse = objectMapper.readTree(mockResponseJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse mock response JSON", e);
        }

        // Execute service method
        Map<String, String> channelDetails = service.parseChannelDetails(mockResponse);

        // Verify results
        assertEquals("channel123", channelDetails.get("id"));
        assertEquals("Test Channel", channelDetails.get("title"));
        assertEquals("This is a mock channel description.", channelDetails.get("description"));
        assertEquals("US", channelDetails.get("country"));
        assertEquals("http://mock.com", channelDetails.get("thumbnailDefault"));
        assertEquals("1000", channelDetails.get("subscriberCount"));
        assertEquals("5000", channelDetails.get("viewCount"));
        assertEquals("100", channelDetails.get("videoCount"));
    }

    @Test
    public void testParseLatestVideos() throws JsonProcessingException {
        // Prepare mock data
        String mockResponseJson = "{"
                + "\"items\": [{"
                + "\"id\": {\"videoId\": \"video123\"},"
                + "\"snippet\": {"
                + "\"title\": \"Test Video\","
                + "\"description\": \"This is a mock video description.\","
                + "\"thumbnails\": {\"default\": {\"url\": \"http://mock.com\"}}"
                + "}"
                + "}]"
                + "}";
        JsonNode mockResponse = objectMapper.readTree(mockResponseJson);

        // Execute service method
        ArrayNode latestVideos = service.parseLatestVideos(mockResponse);

        // Verify results
        assertEquals(1, latestVideos.size());
        JsonNode video = latestVideos.get(0);
        assertEquals("video123", video.get("videoId").asText());
        assertEquals("Test Video", video.get("title").asText());
        assertEquals("This is a mock video description.", video.get("description").asText());
        assertEquals("http://mock.com", video.get("thumbnailUrl").asText());
    }

}