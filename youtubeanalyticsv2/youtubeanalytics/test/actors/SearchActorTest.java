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

        @Before
        public void setup() {
                system = ActorSystem.create();
                MockitoAnnotations.initMocks(this);
        }

        @After
        public void teardown() {
                TestKit.shutdownActorSystem(system);
                system = null;
        }

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

                                // Create the SearchActor
                                ActorRef searchActor = system.actorOf(SearchActor.props(
                                                mockWsClient,
                                                "test query",
                                                mockCache,
                                                mockReadabilityCalculatorActor,
                                                mockSentimentAnalysisActor,
                                                mockWordStatsActor), "searchActor");

                                // Create a test probe to receive messages
                                TestKit probe = new TestKit(system);

                                // Send a Tick message to trigger handleSearch
                                searchActor.tell(new SearchActor.Tick("test query"), probe.getRef());

                                // Verify that no search is performed (no WSClient calls)
                                verify(mockWsClient, never()).url(anyString());
                        }
                };
        }

        @Test
        public void testSearchActorWorkflow() {
                System.out.println("SearchActorTest.testSearchActorWorkflow");
                new TestKit(system) {
                        {
                                // Create test probes to simulate readability and sentiment actors
                                TestKit readabilityProbe = new TestKit(system);
                                TestKit sentimentProbe = new TestKit(system);

                                TestKit wordStatProbe = new TestKit(system);

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
                                                wordStatProbe.getRef()), "searchActor");

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

        // public static final class RegisterMsg {

        // private final String query;

        // public RegisterMsg(String query) {
        // this.query = query;
        // }

        // public String getQuery() {
        // return query;
        // }
        // }

        // public static class SearchResponse {
        // final String query;
        // final ObjectNode response;

        // public SearchResponse(String query, ObjectNode response) {
        // this.query = query;
        // this.response = response;
        // }
        // }

        // // Simulate message classes from other actors
        // public static class ReadabilityCalculator {
        // public static class initReadabilityCalculatorService {
        // public final String videoId;
        // public final String description;

        // public initReadabilityCalculatorService(String videoId, String description) {
        // this.videoId = videoId;
        // this.description = description;
        // }
        // }

        // public static class ReadabilityResults {
        // public final String videoId;
        // public final double gradeLevel;
        // public final double readingScore;

        // public ReadabilityResults(String videoId, double gradeLevel, double
        // readingScore) {
        // this.videoId = videoId;
        // this.gradeLevel = gradeLevel;
        // this.readingScore = readingScore;
        // }
        // }
        // }

        // public static class SentimentAnalysisActor {
        // public static class initSentimentAnalyzerService {
        // public final String query;
        // public final java.util.List<String> descriptions;

        // public initSentimentAnalyzerService(String query, java.util.List<String>
        // descriptions) {
        // this.query = query;
        // this.descriptions = descriptions;
        // }
        // }

        // public static class SentimentAnalysisResults {
        // public final String sentiment;

        // public SentimentAnalysisResults(String sentiment) {
        // this.sentiment = sentiment;
        // }
        // }
        // }
}