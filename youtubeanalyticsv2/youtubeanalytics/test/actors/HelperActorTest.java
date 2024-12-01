package actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import play.libs.ws.WSClient;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the HelperActor class.
 * @author Mohnish Mirchandani
 */
public class HelperActorTest {

    private static ActorSystem system;

    /**
     * Sets up the ActorSystem before any tests are run.
     */
    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    /**
     * Shuts down the ActorSystem after all tests have been run.
     */
    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    /**
     * Tests that the HelperActor correctly receives and processes a createActor message.
     */
    @Test
    public void testHelperActorReceivesCreateActorMessage() {
        // Create a mock WSClient
        WSClient mockWsClient = Mockito.mock(WSClient.class);

        // TestKit for the actor under test
        TestKit testProbe = new TestKit(system);

        // Create the HelperActor
        ActorRef helperActor = system.actorOf(HelperActor.props(system, mockWsClient));

        // Send a createActor message to HelperActor
        String testQuery = "testQuery";
        HelperActor.createActor createActorMessage = new HelperActor.createActor(testQuery);
        helperActor.tell(createActorMessage, testProbe.getRef());

        // // Expect a response message from HelperActor
        // testProbe.expectMsgClass(SearchActor.RegisterMsg.class);

        // Verify the content of the response
        SearchActor.RegisterMsg receivedMessage = testProbe.expectMsgClass(SearchActor.RegisterMsg.class);
        assertEquals(testQuery, receivedMessage.getQuery());
    }
}
