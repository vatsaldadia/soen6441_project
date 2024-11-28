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

public class HelperActorTest {

    private static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

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
