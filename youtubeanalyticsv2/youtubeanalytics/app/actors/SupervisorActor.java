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

/**
 * 
 * Actor for SupervisorActor
 * @author Mohnish Mirchandani
 */
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
                .match(AddActor.class, this::handleAddActor)
                .match(Terminated.class, this::handleTerminated)
                .build();
    }

   /**
	 * supervisor actor handle add actor
	 * @return void
     * @param message
	 * @author Mohnish Mirchandani
	 */
    private void handleAddActor(AddActor message) {
        ActorRef actorRef = message.actorRef;
        actorRefs.add(actorRef);
        getContext().watch(actorRef);

        System.out.println("Actor added: " + actorRef.path().name());
    }

    /**
	 * Termination function for supervisor actor
	 * @return void
     * @param terminated
	 * @author Mohnish Mirchandani
	 */
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
                        .match(Terminated.class, e -> SupervisorStrategy.restart())
                        .match(RuntimeException.class, e -> SupervisorStrategy.restart())
                        .build()
        );
    }
}