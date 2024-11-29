package controllers;

import actors.SearchActor;
import actors.SupervisorActor;
import actors.WordStatsActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.libs.ws.WSClient;
import play.mvc.Result;
import play.mvc.Http;
import play.test.Helpers;
import play.test.WithApplication;
import scala.concurrent.duration.Duration;
import services.ReadabilityCalculator;
import actors.SentimentAnalysisActor;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.test.Helpers.contentAsString;

public class YoutubeControllerTest extends WithApplication {

    private ActorSystem actorSystem;
    private Materializer materializer;
    private WSClient ws;
    private YoutubeController youtubeController;

    @Before
    public void setup() {
        actorSystem = ActorSystem.create();
        materializer = mock(Materializer.class);
        ws = mock(WSClient.class);
        youtubeController = new YoutubeController(ws, actorSystem, materializer);
    }

    @After
    public void teardown() {
        Helpers.stop(play.test.Helpers.fakeApplication());
        actorSystem.terminate();
    }

    @Test
    public void testIndex() {
        Result result = youtubeController.index();
        assertEquals(200, result.status());
    }

    @Test
    public void testGetSearchActor() {
        String query = "test query";
        ActorRef searchActor = youtubeController.getSearchActor(query);
        assertNotNull(searchActor);
        assertEquals(searchActor, youtubeController.getSearchActor(query));
    }

    @Test
    public void testActorsCreation() {
        assertNotNull(youtubeController.getSearchActor("test query"));
        assertNotNull(youtubeController.getSearchActor("another query"));
    }

    @Test
    public void testSupervisorActor() {
        ActorRef supervisorActor = actorSystem.actorOf(SupervisorActor.props(actorSystem, ws), "supervisor");
        assertNotNull(supervisorActor);
    }

    @Test
    public void testReadabilityCalculatorActor() {
        ActorRef readabilityCalculatorActor = actorSystem.actorOf(ReadabilityCalculator.props(), "readibilityCalculatorActor");
        assertNotNull(readabilityCalculatorActor);
    }

    @Test
    public void testSentimentAnalysisActor() {
        ActorRef sentimentAnalysisActor = actorSystem.actorOf(SentimentAnalysisActor.props(), "sentimentAnalysisActor");
        assertNotNull(sentimentAnalysisActor);
    }

    @Test
    public void testGetWordStats() {
        // Prepare test data
        String query = "testQuery";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode wordStats = mapper.createObjectNode().put("word", 1);
        WordStatsActor.wordStatsMap.put(query, wordStats);

        // Call the method
        Result result = youtubeController.getWordStats(query);

        // Verify the result
        assertEquals(200, result.status());
        assertNotNull(result.contentType());
        assertTrue(result.contentType().isPresent());
        assertEquals("text/html", result.contentType().get());
        assertTrue(contentAsString(result).contains("testQuery"));
    }
}