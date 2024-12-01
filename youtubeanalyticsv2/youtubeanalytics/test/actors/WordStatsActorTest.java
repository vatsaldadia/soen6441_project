package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.Messages;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import services.WordStatsService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test class for WordStatsActor.
 * This class contains unit tests for the WordStatsActor methods.
 * @author Rolwyn Raju
 */
class WordStatsActorTest {

    static ActorSystem system;

    /**
     * Sets up the ActorSystem before all tests.
     */
    @BeforeAll
    static void setup() {
        system = ActorSystem.create();
    }

    /**
     * Tears down the ActorSystem after all tests.
     */
    @AfterAll
    static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    /**
     * Tests the WordStatsActor by processing descriptions.
     * Verifies that the actor processes the descriptions and returns the expected results.
     */
    @Test
    void testWordStatsActor_ProcessDescriptions() {
        new TestKit(system) {{
            // Initialize actor
            ActorRef wordStatsActor = system.actorOf(WordStatsActor.props());

            // Prepare test data
            String videoId = "testVideo123";
            List<String> descriptions = Arrays.asList(
                    "This is a test description",
                    "Another description for testing"
            );

            // Send message to actor
            wordStatsActor.tell(new WordStatsActor.InitWordStatsService(videoId, descriptions), getRef());

            // Expect response
            WordStatsActor.WordStatsResults response = expectMsgClass(WordStatsActor.WordStatsResults.class);

            // Validate the results
            assertEquals(videoId, response.videoId);

            Map<String, Long> expectedStats = WordStatsService.calculateWordStats(descriptions);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode expectedJson = mapper.valueToTree(expectedStats);

            assertEquals(expectedJson, response.wordStats);
        }};
    }


    /**
     * Tests the WordStatsActor with an empty list of descriptions.
     * Verifies that the actor returns an empty word statistics map.
     */
    @Test
    void testWordStatsActor_EmptyDescriptions() {
        new TestKit(system) {{
            // Initialize actor
            ActorRef wordStatsActor = system.actorOf(WordStatsActor.props());

            // Prepare test data
            String videoId = "testVideo123";
            List<String> descriptions = Collections.emptyList();

            // Send message to actor
            wordStatsActor.tell(new WordStatsActor.InitWordStatsService(videoId, descriptions), getRef());

            // Expect response
            WordStatsActor.WordStatsResults response = expectMsgClass(WordStatsActor.WordStatsResults.class);

            // Validate the results
            assertEquals(videoId, response.videoId);
            assertTrue(response.wordStats.isEmpty());
        }};
    }


    /**
     * Tests the WordStatsActor with null descriptions.
     * Verifies that the actor does not respond.
     */
    @Test
    void testWordStatsActor_NullDescriptions() {
        new TestKit(system) {{
            // Initialize actor
            ActorRef wordStatsActor = system.actorOf(WordStatsActor.props());

            // Prepare test data
            String videoId = "testVideo123";

            // Send message to actor
            wordStatsActor.tell(new WordStatsActor.InitWordStatsService(videoId, null), getRef());

            // Expect no response due to null descriptions
            expectNoMessage();
        }};
    }


    /**
     * Tests the WordStatsActor with descriptions containing special characters.
     * Verifies that the actor processes the descriptions and ignores special characters.
     */
    @Test
    void testWordStatsActor_SpecialCharacters() {
        new TestKit(system) {{
            // Initialize actor
            ActorRef wordStatsActor = system.actorOf(WordStatsActor.props());

            // Prepare test data
            String videoId = "testVideo123";
            List<String> descriptions = Arrays.asList(
                    "This is a test description with special characters !@#$%^&*()"
            );

            // Send message to actor
            wordStatsActor.tell(new WordStatsActor.InitWordStatsService(videoId, descriptions), getRef());

            // Expect response
            WordStatsActor.WordStatsResults response = expectMsgClass(WordStatsActor.WordStatsResults.class);

            // Validate the results
            assertEquals(videoId, response.videoId);

            Map<String, Long> expectedStats = WordStatsService.calculateWordStats(descriptions);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode expectedJson = mapper.valueToTree(expectedStats);

            assertEquals(expectedJson, response.wordStats);
        }};
    }


    /**
     * Tests the WordStatsActor with mixed case descriptions.
     * Verifies that the actor processes the descriptions and ignores case.
     */
    @Test
    void testWordStatsActor_MixedCaseDescriptions() {
        new TestKit(system) {{
            // Initialize actor
            ActorRef wordStatsActor = system.actorOf(WordStatsActor.props());

            // Prepare test data
            String videoId = "testVideo123";
            List<String> descriptions = Arrays.asList(
                    "This is a Test Description",
                    "Another test description with more Words",
                    "test DESCRIPTION with some common words"
            );

            // Send message to actor
            wordStatsActor.tell(new WordStatsActor.InitWordStatsService(videoId, descriptions), getRef());

            // Expect response
            WordStatsActor.WordStatsResults response = expectMsgClass(WordStatsActor.WordStatsResults.class);

            // Validate the results
            assertEquals(videoId, response.videoId);

            Map<String, Long> expectedStats = WordStatsService.calculateWordStats(descriptions);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode expectedJson = mapper.valueToTree(expectedStats);

            assertEquals(expectedJson, response.wordStats);
        }};
    }


    /**
     * Tests the WordStatsActor termination.
     * Verifies that the actor stops and does not respond after termination.
     */
    @Test
    void testWordStatsActor_StopActor() {
        new TestKit(system) {{
            // Initialize actor
            ActorRef wordStatsActor = system.actorOf(WordStatsActor.props());

            // Send terminate message to actor
            wordStatsActor.tell(new Messages.TerminateActor(), getRef());

            // Expect no response after termination
            expectNoMessage();
        }};
    }
}
