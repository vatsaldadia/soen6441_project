// package actors;

// import akka.actor.ActorRef;
// import akka.actor.ActorSystem;
// import akka.actor.Props;
// import akka.actor.Terminated;
// import akka.testkit.javadsl.TestKit;
// import com.fasterxml.jackson.databind.node.ObjectNode;
// import controllers.YoutubeController;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import play.libs.ws.WSClient;
// import play.libs.Json;
// import org.mockito.Mock;
// import play.cache.AsyncCacheApi;


// import scala.concurrent.duration.Duration;
// import services.ReadabilityCalculator;

// import static org.mockito.Mockito.mock;

// import java.util.concurrent.TimeUnit;

// /**
//  * Unit tests for the SupervisorActor class.
//  * @author Mohnish Mirchandani
//  */
// public class SupervisorActorTest {

//     private static ActorSystem system;

//     @Mock
//     private AsyncCacheApi mockCache;


//     /**
//      * Sets up the ActorSystem before each test.
//      * @author Mohnish Mirchandani
//      */
//     @Before
//     public void setup() {
//         system = ActorSystem.create();
//     }

//     /**
//      * Shuts down the ActorSystem after each test.
//      * @author Mohnish Mirchandani
//      */
//     @After
//     public void teardown() {
//         TestKit.shutdownActorSystem(system);
//         system = null;
//     }

//     /**
//      * Tests the SupervisorActor's behavior when child actors are terminated.
//      * @author Mohnish Mirchandani
//      */
//     @Test
//     public void testSupervisorActor() {
//         new TestKit(system) {{
//             // Mock dependencies
//             WSClient mockWsClient = mock(WSClient.class);
//             AsyncCacheApi mockCache = mock(AsyncCacheApi.class);
//             YoutubeController mockYoutubeController = mock(YoutubeController.class);

//             // Create the SupervisorActor
//             ActorRef supervisorActor = system.actorOf(
//                 SupervisorActor.props(system, mockWsClient),
//                 "supervisorActor"
//             );

//             // Watch the SupervisorActor
//             watch(supervisorActor);

//             // Simulate termination of child actors

//             // ActorRef searchActor = system.actorOf(SearchActor.props(mockWsClient, "test query", null, null, null, null, null), "searchActor");
//             ActorRef sentimentAnalysisActor = system.actorOf(SentimentAnalysisActor.props(), "sentimentAnalysisActor");
//             ActorRef readabilityCalculatorActor = system.actorOf(ReadabilityCalculator.props(), "readabilityCalculatorActor");
//             ActorRef searchActor = system.actorOf(SearchActor.props(mockWsClient, "test query", mockCache, readabilityCalculatorActor, sentimentAnalysisActor, wordStatsActor), "sActor");
            
//             supervisorActor.tell(new SupervisorActor.AddActor(helperActor), getRef());
//             supervisorActor.tell(new SupervisorActor.AddActor(sentimentAnalysisActor), getRef());
//             supervisorActor.tell(new SupervisorActor.AddActor(wordStatsActor), getRef());
//             supervisorActor.tell(new SupervisorActor.AddActor(readabilityCalculatorActor), getRef());
//             supervisorActor.tell(new SupervisorActor.AddActor(searchActor), getRef());


//             // supervisorActor.tell(new Terminated(searchActor, false, false), getRef());
//             // supervisorActor.tell(new Terminated(helperActor, false, false), getRef());
//             // supervisorActor.tell(new Terminated(sentimentAnalysisActor, false, false), getRef());
//             // supervisorActor.tell(new Terminated(wordStatsActor, false, false), getRef());
//             // supervisorActor.tell(new Terminated(readabilityCalculatorActor, false, false), getRef());

//             // // Expect the SupervisorActor to restart the terminated actors
//             // expectTerminated(Duration.create(10, TimeUnit.SECONDS), searchActor);
//             // expectTerminated(Duration.create(10, TimeUnit.SECONDS), helperActor);
//             // expectTerminated(Duration.create(10, TimeUnit.SECONDS), sentimentAnalysisActor);

//             // Simulate an exception in the searchActor
//             searchActor.tell(new Exception("Simulated exception"), getRef());

//             // Verify that the actors are restarted
//             // ActorRef restartedHelperActor = system.actorSelection("/user/supervisorActor/helperActor").resolveOne(java.time.Duration.ofSeconds(3)).toCompletableFuture().join();
//             // ActorRef restartedSentimentAnalysisActor = system.actorSelection("/user/supervisorActor/sentimentAnalysisActor").resolveOne(java.time.Duration.ofSeconds(3)).toCompletableFuture().join();
//             // ActorRef restartedWordStatsActor = system.actorSelection("/user/supervisorActor/wordStatsActor").resolveOne(java.time.Duration.ofSeconds(3)).toCompletableFuture().join();
//             // ActorRef restartedReadabilityCalculatorActor = system.actorSelection("/user/supervisorActor/readabilityCalculatorActor").resolveOne(java.time.Duration.ofSeconds(3)).toCompletableFuture().join();
//             ActorRef restartedSearchActor = system.actorSelection("/user/supervisorActor/searchActor").resolveOne(java.time.Duration.ofSeconds(6)).toCompletableFuture().join();

//             assert(restartedSearchActor != null);
//             // assert(restartedHelperActor != null);
//             // assert(restartedSentimentAnalysisActor != null);
//             // assert(restartedReadabilityCalculatorActor != null);
//             // assert(restartedWordStatsActor != null);
            
//         }};
//     }
// }


package actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.libs.ws.WSClient;
import play.libs.Json;
import org.mockito.Mock;
import play.cache.AsyncCacheApi;
import services.ChannelProfileService;

import static org.junit.Assert.*;

import scala.concurrent.duration.Duration;
import services.ReadabilityCalculator;

import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

public class SupervisorActorTest {

    private static ActorSystem system;

    @Mock
    private AsyncCacheApi mockCache;

    @Before
    public void setup() {
        system = ActorSystem.create();
    }

    @After
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testSupervisorActor() {
        new TestKit(system) {{
            // Mock dependencies
            WSClient mockWsClient = mock(WSClient.class);
            AsyncCacheApi mockCache = mock(AsyncCacheApi.class);

            // Create the SupervisorActor
            ActorRef supervisorActor = system.actorOf(
                SupervisorActor.props(system, mockWsClient),
                "supervisorActor"
            );

            // Watch the SupervisorActor
            watch(supervisorActor);

            // Create and add child actors
            ActorRef helperActor = system.actorOf(HelperActor.props(system, mockWsClient), "helperActor");
            ActorRef sentimentAnalysisActor = system.actorOf(SentimentAnalysisActor.props(), "sentimentAnalysisActor");
            ActorRef wordStatsActor = system.actorOf(WordStatsActor.props(), "wordStatsActor");
            ActorRef readabilityCalculatorActor = system.actorOf(ReadabilityCalculator.props(), "readabilityCalculatorActor");
            ActorRef channelProfileActor = system.actorOf(ChannelProfileActor.props(new ChannelProfileService(mockWsClient)), "channelProfileActor");
            
            ActorRef searchActor = system.actorOf(SearchActor.props(mockWsClient, "test query", mockCache, readabilityCalculatorActor, sentimentAnalysisActor, wordStatsActor, channelProfileActor), "searchActor");

             // Create a TestProbe to watch the actors
            TestKit probe = new TestKit(system);
            probe.watch(searchActor);
            probe.watch(helperActor);
            probe.watch(sentimentAnalysisActor);
            probe.watch(wordStatsActor);
            probe.watch(readabilityCalculatorActor);

            // Add actors to the SupervisorActor
            supervisorActor.tell(new SupervisorActor.AddActor(helperActor), getRef());
            supervisorActor.tell(new SupervisorActor.AddActor(sentimentAnalysisActor), getRef());
            supervisorActor.tell(new SupervisorActor.AddActor(wordStatsActor), getRef());
            supervisorActor.tell(new SupervisorActor.AddActor(readabilityCalculatorActor), getRef());
            supervisorActor.tell(new SupervisorActor.AddActor(searchActor), getRef());

            // Verify that the actors are added and watched
            expectNoMessage(Duration.create(1, TimeUnit.SECONDS));


            // Simulate termination of child actors
            system.stop(searchActor);
            probe.expectTerminated(Duration.create(10, TimeUnit.SECONDS), searchActor);
           
            
            system.stop(sentimentAnalysisActor);
            probe.expectTerminated(Duration.create(10, TimeUnit.SECONDS), sentimentAnalysisActor);
            
            system.stop(wordStatsActor);
            probe.expectTerminated(Duration.create(10, TimeUnit.SECONDS), wordStatsActor);
            
            system.stop(readabilityCalculatorActor);
            probe.expectTerminated(Duration.create(10, TimeUnit.SECONDS), readabilityCalculatorActor);

        }};
    }
}