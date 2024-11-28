package actors;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import play.cache.AsyncCacheApi;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import scala.concurrent.duration.Duration;
import static org.mockito.ArgumentMatchers.*;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchActorTest {
    private static ActorSystem system;

    @Mock
    private WSClient mockWsClient;

    @Mock
    private WSRequest mockWSRequest;

    @Mock
    private WSResponse mockWSResponse;

    @Mock
    private AsyncCacheApi mockCache;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSearchActorWorkflow() {
        new TestKit(system) {{
            // Create test probes to simulate readability and sentiment actors
            TestKit readabilityProbe = new TestKit(system);
            TestKit sentimentProbe = new TestKit(system);

            // Prepare mock YouTube search response
            ObjectNode searchResponseNode = Json.newObject();
            ObjectNode itemId = Json.newObject();
            itemId.put("videoId", "test-video-id");
            
            ObjectNode searchItem = Json.newObject();
            searchItem.set("id", itemId);
            
            searchResponseNode.putArray("items").add(searchItem);

            // Prepare mock video details response
            ObjectNode videoResponseNode = Json.newObject();
            ObjectNode videoSnippet = Json.newObject();
            videoSnippet.put("description", "Test video description");
            
            ObjectNode videoItem = Json.newObject();
            videoItem.set("snippet", videoSnippet);
            videoResponseNode.putArray("items").add(videoItem);

                        
                    
            // Mock WSClient behavior for "/search"
            when(mockWsClient.url(contains("/search"))).thenReturn(mockWSRequest);
            when(mockWSRequest.addQueryParameter(anyString(), anyString())).thenReturn(mockWSRequest);
            when(mockWSRequest.get()).thenReturn(CompletableFuture.completedFuture(mockWSResponse));
            when(mockWSResponse.asJson()).thenReturn(searchResponseNode);

            // Mock WSClient behavior for "/videos"
            when(mockWsClient.url(contains("/videos"))).thenReturn(mockWSRequest);
            when(mockWSRequest.addQueryParameter(anyString(), anyString())).thenReturn(mockWSRequest);
            when(mockWSRequest.get()).thenReturn(CompletableFuture.completedFuture(mockWSResponse));
            when(mockWSResponse.asJson()).thenReturn(videoResponseNode);

            // Prepare Props with test probe actors
            Props props = SearchActor.props(
                mockWsClient, 
                "test query", 
                mockCache, 
                readabilityProbe.getRef(), 
                sentimentProbe.getRef()
            );
            ActorRef searchActor = system.actorOf(props);

            // Create a test probe to receive messages
            TestKit probe = new TestKit(system);

          
            // Register the probe as a user actor
            searchActor.tell(new RegisterMsg("test query"), probe.getRef());

            // // Simulate readability calculation response
            // readabilityProbe.expectMsgClass(
            //     Duration.create(20, TimeUnit.SECONDS), 
            //     ReadabilityCalculator.initReadabilityCalculatorService.class
            // );
            // readabilityProbe.reply(
            //     new ReadabilityCalculator.ReadabilityResults("test-video-id", 8.5, 60.0)
            // );

            // // Simulate sentiment analysis response
            // sentimentProbe.expectMsgClass(
            //     Duration.create(20, TimeUnit.SECONDS), 
            //     SentimentAnalysisActor.initSentimentAnalyzerService.class
            // );
            // sentimentProbe.reply(
            //     new SentimentAnalysisActor.SentimentAnalysisResults("positive")
            // );

            // // Expect a SearchResponse
            // SearchResponse response = probe.expectMsgClass(
            //     Duration.create(10, TimeUnit.SECONDS), 
            //     SearchResponse.class
            // );

            // Additional assertions can be added here to verify the response
        }};
    }


    // Helper method to prepare mock search response
    private ObjectNode prepareMockSearchResponse() {
        ObjectNode searchResponseNode = Json.newObject();
        ObjectNode itemId = Json.newObject();
        itemId.put("videoId", "test-video-id");
        
        ObjectNode searchItem = Json.newObject();
        searchItem.set("id", itemId);
        
        searchResponseNode.putArray("items").add(searchItem);
        return searchResponseNode;
    }

    // Helper method to prepare mock video response
    private ObjectNode prepareMockVideoResponse() {
        ObjectNode videoResponseNode = Json.newObject();
        ObjectNode videoSnippet = Json.newObject();
        videoSnippet.put("description", "Test video description");
        
        ObjectNode videoItem = Json.newObject();
        videoItem.set("snippet", videoSnippet);
        videoResponseNode.putArray("items").add(videoItem);
        return videoResponseNode;
    }

    // Inner classes to match the original actor's message types
    public static class RegisterMsg {
        private final String query;

        public RegisterMsg(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }

    public static class SearchResponse {
        final String query;
        final ObjectNode response;

        public SearchResponse(String query, ObjectNode response) {
            this.query = query;
            this.response = response;
        }
    }

    // Simulate message classes from other actors
    public static class ReadabilityCalculator {
        public static class initReadabilityCalculatorService {
            public final String videoId;
            public final String description;

            public initReadabilityCalculatorService(String videoId, String description) {
                this.videoId = videoId;
                this.description = description;
            }
        }

        public static class ReadabilityResults {
            public final String videoId;
            public final double gradeLevel;
            public final double readingScore;

            public ReadabilityResults(String videoId, double gradeLevel, double readingScore) {
                this.videoId = videoId;
                this.gradeLevel = gradeLevel;
                this.readingScore = readingScore;
            }
        }
    }

    public static class SentimentAnalysisActor {
        public static class initSentimentAnalyzerService {
            public final String query;
            public final java.util.List<String> descriptions;

            public initSentimentAnalyzerService(String query, java.util.List<String> descriptions) {
                this.query = query;
                this.descriptions = descriptions;
            }
        }

        public static class SentimentAnalysisResults {
            public final String sentiment;

            public SentimentAnalysisResults(String sentiment) {
                this.sentiment = sentiment;
            }
        }
    }
}