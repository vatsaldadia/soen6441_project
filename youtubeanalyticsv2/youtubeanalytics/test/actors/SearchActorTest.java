package actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.After;
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
import services.ReadabilityCalculator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the SearchActor class.
 * @author Mohnish Mirchandani
 */
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

        /**
         * Sets up the ActorSystem and initializes mocks before each test.
         * @author Mohnish Mirchandani
         */
        @Before
        public void setup() {
                system = ActorSystem.create();
                MockitoAnnotations.initMocks(this);
        }

        /**
         * Shuts down the ActorSystem after each test.
         */
        @After
        public void teardown() {
                TestKit.shutdownActorSystem(system);
                system = null;
        }

        /**
         * Tests that the SearchActor does not perform a search when the user list is empty.
         * @author Mohnish Mirchandani
         */
        @Test
        public void testHandleSearchWithEmptyUserList() {
                new TestKit(system) {
                        {
                                // Mock dependencies
                                WSClient mockWsClient = mock(WSClient.class);
                                AsyncCacheApi mockCache = mock(AsyncCacheApi.class);
                                ActorRef mockReadabilityCalculatorActor = mock(ActorRef.class);
                                ActorRef mockSentimentAnalysisActor = mock(ActorRef.class);
                                ActorRef mockWordStatsActor = mock(ActorRef.class);
                                ActorRef channelProfileActor = mock(ActorRef.class);
                                

                                // Create the SearchActor
                                ActorRef searchActor = system.actorOf(SearchActor.props(
                                                mockWsClient,
                                                "test query",
                                                mockCache,
                                                mockReadabilityCalculatorActor,
                                                mockSentimentAnalysisActor,
                                                mockWordStatsActor, 
                                                channelProfileActor), "searchActor");

                                // Create a test probe to receive messages
                                TestKit probe = new TestKit(system);

                                // Send a Tick message to trigger handleSearch
                                searchActor.tell(new SearchActor.Tick("test query"), probe.getRef());

                                // Verify that no search is performed (no WSClient calls)
                                verify(mockWsClient, never()).url(anyString());
                        }
                };
        }

        /**
         * Tests the complete workflow of the SearchActor, including interactions with other actors.
         * @author Mohnish Mirchandani
         */
        @Test
        public void testSearchActorWorkflow() {
                System.out.println("SearchActorTest.testSearchActorWorkflow");
                new TestKit(system) {
                        {
                                // Create test probes to simulate readability and sentiment actors
                                TestKit readabilityProbe = new TestKit(system);
                                TestKit sentimentProbe = new TestKit(system);
                                TestKit channelProfileProbe = new TestKit(system);

                                TestKit wordStatProbe = new TestKit(system);

                                // Prepare mock YouTube search response
                                ObjectNode searchResponseNode = Json.newObject();
                                ObjectNode itemId = Json.newObject();
                                ObjectNode snippet = Json.newObject();
                                
                                itemId.put("videoId", "test-video-id");
                                snippet.put("channelId", "test-channel-id");
                                

                                ObjectNode searchItem = Json.newObject();
                                searchItem.set("id", itemId);
                                searchItem.set("snippet", snippet);
                                searchResponseNode.putArray("items").add(searchItem);

                                // Prepare mock video details response
                                ObjectNode videoResponseNode = Json.newObject();
                                ObjectNode videoSnippet = Json.newObject();
                                videoSnippet.put("description", "Test video description");

                                ObjectNode videoItem = Json.newObject();
                                videoItem.set("snippet", videoSnippet);
                                videoResponseNode.putArray("items").add(videoItem);

                                // Mock WSClient behavior for "/videos"
                                when(mockWsClient.url(anyString())).thenReturn(mockWSRequest);
                                when(mockWSRequest.addQueryParameter(anyString(), anyString()))
                                                .thenReturn(mockWSRequest);
                                when(mockWSRequest.get()).thenReturn(CompletableFuture.completedFuture(mockWSResponse));
                                when(mockWSResponse.asJson()).thenReturn(searchResponseNode)
                                                .thenReturn(videoResponseNode);

                                System.out.println(searchResponseNode);

                                System.out.println(videoResponseNode);

                                // Prepare Props with test probe actors

                                ActorRef searchActor = system.actorOf(SearchActor.props(
                                                mockWsClient,
                                                "test query",
                                                mockCache,
                                                readabilityProbe.getRef(),
                                                sentimentProbe.getRef(),
                                                wordStatProbe.getRef(),
                                                channelProfileProbe.getRef()), "searchActor");

                                System.out.println(searchActor);

                                // Create a test probe to receive messages
                                TestKit probe = new TestKit(system);
                                // Register the probe as a user actor
                                searchActor.tell(new SearchActor.RegisterMsg("test query"), probe.getRef());

                                // Simulate readability calculation response
                                readabilityProbe.expectMsgClass(
                                                Duration.create(20, TimeUnit.SECONDS),
                                                ReadabilityCalculator.initReadabilityCalculatorService.class);
                                readabilityProbe.reply(
                                                new ReadabilityCalculator.ReadabilityResults("test-video-id", 8.5,
                                                                60.0));

                                // Simulate sentiment analysis response
                                sentimentProbe.expectMsgClass(
                                                SentimentAnalysisActor.initSentimentAnalyzerService.class);
                                sentimentProbe.reply(
                                                new SentimentAnalysisActor.SentimentAnalysisResults("test query",
                                                                ":-||"));

                                wordStatProbe.expectMsgClass(
                                                WordStatsActor.InitWordStatsService.class);
                                Map<String, Long> wordStats = new HashMap<>();
                                wordStats.put("test", 1L);
                                wordStatProbe.reply(
                                                new WordStatsActor.WordStatsResults("test-video-id", wordStats));

                                // Expect a SearchResponse
                                probe.expectMsgClass(
                                                SearchActor.SearchResponse.class);

                                // Additional assertions can be added here to verify the response
                        }
                };
        }

}