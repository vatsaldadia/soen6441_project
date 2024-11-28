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

import java.util.concurrent.TimeUnit;

import services.ReadabilityCalculator;

public class SupervisorActor extends AbstractActor {

    private final ActorRef searchActor;
    private final ActorRef helperActor;
    private final ActorRef sentimentAnalysisActor;
    private final ActorRef readibilityCalculatorActor;
    
    public static Props props(ActorSystem system, WSClient ws) {
        return Props.create(SupervisorActor.class, system, ws);
    }

    public SupervisorActor(ActorSystem system, WSClient ws) {
        searchActor = getContext().actorOf(SearchActor.props(ws, "test query", null, null, null, null), "searchActor");
        helperActor = getContext().actorOf(HelperActor.props(system, ws), "helperActor");
        sentimentAnalysisActor = getContext().actorOf(SentimentAnalysisActor.props(), "sentimentAnalysisActor");
        readibilityCalculatorActor = getContext().actorOf(ReadabilityCalculator.props(), "readibilityCalculatorActor");

        getContext().watch(searchActor);
        getContext().watch(helperActor);
        getContext().watch(sentimentAnalysisActor);
        getContext().watch(readibilityCalculatorActor);
        
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Terminated.class, t -> {
                    if (t.getActor().equals(searchActor)) {
                        System.out.println("Search actor terminated");
                        getContext().actorOf(SearchActor.props(null, "test query", null, null, null, null), "searchActor");
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
                })
                .build();
    }

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