package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.japi.pf.DeciderBuilder;
import messages.Messages.TerminateActor;
import scala.concurrent.duration.Duration;
import akka.actor.ActorSystem;
import play.libs.streams.ActorFlow;
import play.libs.ws.WSClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import services.ReadabilityCalculator;

public class SupervisorActor extends AbstractActor {

    // private final ActorRef searchActor;
    // private final ActorRef helperActor;
    // private final ActorRef sentimentAnalysisActor;
    // private final ActorRef readibilityCalculatorActor;
    // private final ActorRef wordStatsActor;
    private final List<ActorRef> actorRefs = new ArrayList<>();
    
    public static Props props(ActorSystem system, WSClient ws) {
        return Props.create(SupervisorActor.class, system, ws);
    }

    public SupervisorActor(ActorSystem system, WSClient ws) {

        // searchActor = getContext().actorOf(SearchActor.props(ws, "test query", null, null, null, null), "searchActor");
        // helperActor = getContext().actorOf(HelperActor.props(system, ws), "helperActor");
        // sentimentAnalysisActor = getContext().actorOf(SentimentAnalysisActor.props(), "sentimentAnalysisActor");
        // readibilityCalculatorActor = getContext().actorOf(ReadabilityCalculator.props(), "readibilityCalculatorActor");
        // wordStatsActor = getContext().actorOf(WordStatsActor.props(), "wordStatsActor");

        // getContext().watch(searchActor);
        // getContext().watch(helperActor);
        // getContext().watch(sentimentAnalysisActor);
        // getContext().watch(readibilityCalculatorActor);
        // getContext().watch(wordStatsActor);
        
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                // .match(Terminated.class, t -> {
                //     if (t.getActor().equals(searchActor)) {
                //         System.out.println("Search actor terminated");
                //         getContext().actorOf(SearchActor.props(null, "test query", null, null, null, null), "searchActor");
                //     } else if (t.getActor().equals(helperActor)) {
                //         System.out.println("Helper actor terminated");
                //         getContext().actorOf(HelperActor.props(null, null), "helperActor");
                //     } else if (t.getActor().equals(sentimentAnalysisActor)) {
                //         System.out.println("Sentiment analysis actor terminated");
                //         getContext().actorOf(SentimentAnalysisActor.props(), "sentimentAnalysisActor");
                //     }
                //     else if (t.getActor().equals(readibilityCalculatorActor)) {
                //         System.out.println("Readibility calculator actor terminated");
                //         getContext().actorOf(ReadabilityCalculator.props(), "readibilityCalculatorActor");
                //     }
                //     else if (t.getActor().equals(wordStatsActor)) {
                //         System.out.println("Word stats actor terminated");
                //         getContext().actorOf(WordStatsActor.props(), "wordStatsActor");
                //     }
                .match(AddActor.class, this::handleAddActor)
                .match(Terminated.class, this::handleTerminated)
                .build();
    }

    private void handleAddActor(AddActor message) {
        ActorRef actorRef = message.actorRef;
        actorRefs.add(actorRef);
        getContext().watch(actorRef);

        System.out.println("Actor added: " + actorRef.path().name());
    }

     private void handleTerminated(Terminated terminated) {
        ActorRef terminatedActor = terminated.getActor();
        for (ActorRef actorRef : actorRefs) {
            if (actorRef.equals(terminatedActor)) {
                System.out.println(actorRef.path().name() + " terminated");
                // Send termination message instead of killing the actor
                actorRef.tell(new TerminateActor(), getSelf());
                break;
            }
        }
    }


    public static class AddActor {
        public final ActorRef actorRef;

        public AddActor(ActorRef actorRef) {
            this.actorRef = actorRef;
        }
    }


    @Override
    public SupervisorStrategy supervisorStrategy() {
        System.out.println("Exception - restarting");
        return new OneForOneStrategy(
                10,
                Duration.create(2, TimeUnit.SECONDS),
                DeciderBuilder
                        .match(Exception.class, e -> SupervisorStrategy.restart())
                        .build()
        );
    }
}