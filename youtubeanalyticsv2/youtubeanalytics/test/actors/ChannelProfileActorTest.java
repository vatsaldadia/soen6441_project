package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import services.ChannelProfileService;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ChannelProfileActorTest {

    private static ActorSystem actorSystem;

    @BeforeClass
    public static void setUp() {
        actorSystem = ActorSystem.create();
    }

    @AfterClass
    public static void tearDown() {
        TestKit.shutdownActorSystem(actorSystem);
        actorSystem = null;
    }

    @Test
    public void testChannelProfileActor() {
        // Arrange
        TestKit testKit = new TestKit(actorSystem);
        ChannelProfileService channelProfileService = mock(ChannelProfileService.class);

        ActorRef channelProfileActor = actorSystem.actorOf(ChannelProfileActor.props(channelProfileService));

        // Mocking response
        JsonNode expectedResponse = new ObjectMapper().createObjectNode().put("channelName", "Actor Test Channel");
        when(channelProfileService.fetchChannelDetails("UC123456"))
                .thenReturn(CompletableFuture.completedFuture(expectedResponse));

        // Act
        channelProfileActor.tell(new ChannelProfileActor.InitChannelProfileService("UC123456"), testKit.getRef());

        // Assert
        JsonNode actualResponse = testKit.expectMsgClass(JsonNode.class);
        assertEquals(expectedResponse, actualResponse);

        verify(channelProfileService, times(1)).fetchChannelDetails("UC123456");
    }
}