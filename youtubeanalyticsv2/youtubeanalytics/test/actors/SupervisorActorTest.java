package actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.YoutubeController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.libs.ws.WSClient;
import play.libs.Json;

import scala.concurrent.duration.Duration;

import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

/**
 * Unit tests for the SupervisorActor class.
 * @author Mohnish Mirchandani
 * @author Vatsal Dadia
 * @author Rolwyn Raju
 * @author Pretty Kotian
 * @author Elston Farel
 */
public class SupervisorActorTest {

    private static ActorSystem system;

    /**
     * Sets up the ActorSystem before each test.
     */
    @Before
    public void setup() {
        system = ActorSystem.create();
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
     * Tests the SupervisorActor's behavior when child actors are terminated.
     */
    @Test
    public void testSupervisorActor() {
        new TestKit(system) {{
            // Mock dependencies
            WSClient mockWsClient = mock(WSClient.class);
            YoutubeController mockYoutubeController = mock(YoutubeController.class);

            // Create the SupervisorActor
            ActorRef supervisorActor = system.actorOf(
                SupervisorActor.props(system, mockWsClient),
                "supervisorActor"
            );

            // Watch the SupervisorActor
            watch(supervisorActor);

            // Simulate termination of child actors
            ActorRef searchActor = system.actorOf(SearchActor.props(mockWsClient, "test query", null, null, null, null), "searchActor");
            ActorRef helperActor = system.actorOf(HelperActor.props(system, mockWsClient), "helperActor");
            ActorRef sentimentAnalysisActor = system.actorOf(SentimentAnalysisActor.props(), "sentimentAnalysisActor");
            ActorRef wordStatsActor = system.actorOf(WordStatsActor.props(), "wordStatsActor");

            supervisorActor.tell(new Terminated(searchActor, false, false), getRef());
            supervisorActor.tell(new Terminated(helperActor, false, false), getRef());
            supervisorActor.tell(new Terminated(sentimentAnalysisActor, false, false), getRef());
            supervisorActor.tell(new Terminated(wordStatsActor, false, false), getRef());

            // // Expect the SupervisorActor to restart the terminated actors
            // expectTerminated(Duration.create(10, TimeUnit.SECONDS), searchActor);
            // expectTerminated(Duration.create(10, TimeUnit.SECONDS), helperActor);
            // expectTerminated(Duration.create(10, TimeUnit.SECONDS), sentimentAnalysisActor);

            // Verify that the actors are restarted
            ActorRef restartedSearchActor = system.actorSelection("/user/supervisorActor/searchActor").resolveOne(java.time.Duration.ofSeconds(3)).toCompletableFuture().join();
            ActorRef restartedHelperActor = system.actorSelection("/user/supervisorActor/helperActor").resolveOne(java.time.Duration.ofSeconds(3)).toCompletableFuture().join();
            ActorRef restartedSentimentAnalysisActor = system.actorSelection("/user/supervisorActor/sentimentAnalysisActor").resolveOne(java.time.Duration.ofSeconds(3)).toCompletableFuture().join();
            ActorRef restartedWordStatsActor = system.actorSelection("/user/supervisorActor/wordStatsActor").resolveOne(java.time.Duration.ofSeconds(3)).toCompletableFuture().join();

            assert(restartedSearchActor != null);
            assert(restartedHelperActor != null);
            assert(restartedSentimentAnalysisActor != null);
        }};
    }
}