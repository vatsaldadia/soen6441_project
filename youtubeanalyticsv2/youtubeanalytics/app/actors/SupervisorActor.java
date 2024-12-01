package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.japi.pf.DeciderBuilder;
import scala.concurrent.duration.Duration;
import akka.actor.ActorSystem;
import play.libs.streams.ActorFlow;
import play.libs.ws.WSClient;
import services.ChannelProfileService;
import java.util.concurrent.TimeUnit;

import services.ReadabilityCalculator;

/**
 * SupervisorActor is responsible for supervising other actors and restarting them if they fail.
 * @author Mohnish Mirchandani
 */
public class SupervisorActor extends AbstractActor {

    private final ActorRef searchActor;
    private final ActorRef helperActor;
    private final ActorRef sentimentAnalysisActor;
    private final ActorRef readibilityCalculatorActor;
    private final ActorRef wordStatsActor;
    private final ActorRef channelProfileActor;


    /**
     * Creates Props for an actor of this type.
     *
     * @param system The ActorSystem to create actors in.
     * @param ws The WSClient for making HTTP requests.
     * @return A Props for creating this actor.
     * @author Mohnish Mirchandani
     */
    public static Props props(ActorSystem system, WSClient ws) {
        return Props.create(SupervisorActor.class, system, ws);
    }

    /**
     * Creates Props for an actor of this type.
     *
     * @param system The ActorSystem to create actors in.
     * @param ws The WSClient for making HTTP requests.
     * @return A Props for creating this actor.
     * @author Mohnish Mirchandani
     */
    public SupervisorActor(ActorSystem system, WSClient ws) {
        searchActor = getContext().actorOf(SearchActor.props(ws, "test query", null, null, null, null, null), "searchActor");
        helperActor = getContext().actorOf(HelperActor.props(system, ws), "helperActor");
        sentimentAnalysisActor = getContext().actorOf(SentimentAnalysisActor.props(), "sentimentAnalysisActor");
        readibilityCalculatorActor = getContext().actorOf(ReadabilityCalculator.props(), "readibilityCalculatorActor");
        wordStatsActor = getContext().actorOf(WordStatsActor.props(), "wordStatsActor");
        channelProfileActor = getContext().actorOf(ChannelProfileActor.props(new ChannelProfileService(ws)), "channelProfileActor");
        getContext().watch(searchActor);
        getContext().watch(helperActor);
        getContext().watch(sentimentAnalysisActor);
        getContext().watch(readibilityCalculatorActor);
        getContext().watch(wordStatsActor);
        getContext().watch(channelProfileActor);
    }

    /**
    * Create receive for SupervisorActor
    * @return Receive
     * @author Mohnish Mirchandani
    */

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Terminated.class, t -> {
                    if (t.getActor().equals(searchActor)) {
                        System.out.println("Search actor terminated");
                        getContext().actorOf(SearchActor.props(null, "test query", null, null, null, null, null), "searchActor");
                    } else if (t.getActor().equals(helperActor)) {
                        System.out.println("Helper actor terminated");
                        getContext().actorOf(HelperActor.props(null, null), "helperActor");
                    } else if (t.getActor().equals(sentimentAnalysisActor)) {
                        System.out.println("Sentiment analysis actor terminated");
                        getContext().actorOf(SentimentAnalysisActor.props(), "sentimentAnalysisActor");
                    }
                    else if (t.getActor().equals(readibilityCalculatorActor)) {
                        System.out.println("Readibility calculator actor terminated");
                        getContext().actorOf(ReadabilityCalculator.props(), "readibilityCalculatorActor");
                    }
                    else if (t.getActor().equals(wordStatsActor)) {
                        System.out.println("Word stats actor terminated");
                        getContext().actorOf(WordStatsActor.props(), "wordStatsActor");
                    }
                    else if (t.getActor().equals(channelProfileActor)) {
                        System.out.println("Channel profile actor terminated, restarting...");
                        getContext().actorOf(ChannelProfileActor.props(new ChannelProfileService(null)), "channelProfileActor");
                    }
                })
                .build();
    }

    /**
     * Supervisor strategy for SupervisorActor
     * @return strategy
     * @author Mohnish Mirchandani
     */

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(
                10,
                Duration.create(2, TimeUnit.SECONDS),
                DeciderBuilder
                        .match(Exception.class, e -> SupervisorStrategy.restart())
                        .build()
        );
    }
}