package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import services.WordStatsService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WordStatsActorTest {

    static ActorSystem system;

    @BeforeAll
    static void setup() {
        system = ActorSystem.create();
    }

    @AfterAll
    static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

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
}
