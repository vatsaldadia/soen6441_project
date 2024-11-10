package services;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.YoutubeController;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.OngoingStubbing;
import play.cache.AsyncCacheApi;
import play.libs.ws.*;
import play.mvc.Result;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Test class for YoutubeService.
 * This class contains unit tests for the YoutubeService methods.
 *
 * @author Vatsal Dadia
 */
//@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@RunWith(MockitoJUnitRunner.class)
public class YoutubeServiceTest {

    @Mock
    private WSClient wsClient;

    @Mock
    private AsyncCacheApi cache;

    @Mock
    private WSRequest wsRequest;

    @Mock
    private WSResponse wsResponse;

    private YoutubeService youtubeService;
    private ObjectMapper mapper;

    /**
     * Sets up the test environment.
     * Initializes the YoutubeService and ObjectMapper instances.
     *
     * @author Vatsal Dadia
     */
    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        youtubeService = new YoutubeService(
                wsClient,
                cache
        );
    }

    /**
     * Tests the modifyResponse method.
     * Verifies that the method correctly modifies the YouTube API response.
     *
     * @throws Exception if an error occurs during the test.
     * @author Vatsal Dadia
     */
    @Test
    public void testModifyResponse() throws Exception {
        // Mock the YouTube API response
        ObjectNode youtubeResponse = mapper.createObjectNode();
        ArrayNode items = JsonNodeFactory.instance.arrayNode();

        ObjectNode item1 = mapper.createObjectNode();
        ObjectNode id1 = mapper.createObjectNode();
        id1.put("videoId", "video1");
        item1.set("id", id1);

        ObjectNode item2 = mapper.createObjectNode();
        ObjectNode id2 = mapper.createObjectNode();
        id2.put("videoId", "video2");
        item2.set("id", id2);

        items.add(item1);
        items.add(item2);
        youtubeResponse.set("items", items);

        when(cache.getOrElseUpdate(anyString(), any(), anyInt())).thenAnswer(
                invocation -> {
                    // Execute the Callable passed to getOrElseUpdate
                    java.util.concurrent.Callable<?> callable =
                            invocation.getArgument(1);
                    return callable.call();
                }
        );

        when(wsClient.url(anyString())).thenReturn(wsRequest);
        when(wsRequest.addQueryParameter(anyString(), anyString())).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));

        // Mock calls to getVideo method
        ObjectNode videoResponse1 = mapper.createObjectNode();
        ObjectNode snippet1 = mapper.createObjectNode();
        snippet1.put("description", "Test description for video 1.");
        ArrayNode items1 = JsonNodeFactory.instance.arrayNode();
        ObjectNode item1Response = mapper.createObjectNode();
        item1Response.set("snippet", snippet1);
        items1.add(item1Response);
        videoResponse1.set("items", items1);

        String jsonResponse1 = mapper.writeValueAsString(videoResponse1);

        // Configure WSResponse to return JSON
        when(wsResponse.getStatus()).thenReturn(200);
        when(wsResponse.asJson()).thenReturn(mapper.readTree(jsonResponse1));
        Mockito.lenient().when(youtubeService.getVideo("video1")).thenReturn(CompletableFuture.completedFuture(wsResponse));

        ObjectNode videoResponse2 = mapper.createObjectNode();
        ObjectNode snippet2 = mapper.createObjectNode();
        snippet2.put("description", "Test description for video 2.");
        ArrayNode items2 = JsonNodeFactory.instance.arrayNode();
        ObjectNode item2Response = mapper.createObjectNode();
        item2Response.set("snippet", snippet2);
        items2.add(item2Response);
        videoResponse2.set("items", items2);

        String jsonResponse2 = mapper.writeValueAsString(videoResponse2);

        // Configure WSResponse to return JSON
        when(wsResponse.getStatus()).thenReturn(200);
        when(wsResponse.asJson()).thenReturn(mapper.readTree(jsonResponse2));
        Mockito.lenient().when(youtubeService.getVideo("video2")).thenReturn(CompletableFuture.completedFuture(wsResponse));

        // Execute the method
        CompletionStage<ObjectNode> resultStage = youtubeService.modifyResponse(youtubeResponse);
        ObjectNode modifiedResponse = resultStage.toCompletableFuture().join();

        assertTrue(modifiedResponse.has("sentiment"));
        assertTrue(modifiedResponse.has("fleschKincaidGradeLevelAvg"));
        assertTrue(modifiedResponse.has("fleschReadingScoreAvg"));
        assertTrue(modifiedResponse.has("items"));

        ArrayNode modifiedItems = (ArrayNode) modifiedResponse.get("items");
        assertEquals(2, modifiedItems.size());

        JsonNode modifiedItem1 = modifiedItems.get(0);
        assertEquals("5.24", modifiedItem1.get("fleschKincaidGradeLevel").asText());
        assertEquals("66.40", modifiedItem1.get("fleschReadingScore").asText());

    }

}