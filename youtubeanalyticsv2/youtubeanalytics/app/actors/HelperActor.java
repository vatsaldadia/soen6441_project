package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import play.libs.ws.WSClient;
import messages.Messages.TerminateActor;
import java.util.HashSet;
import java.util.Set;

/**
 * HelperActor is responsible for managing search queries and creating SearchActor instances.
 * @author Vatsal Dadia
 */
public class HelperActor extends AbstractActor {

    private Set<String> queries = new HashSet<>();

    private ActorSystem system;
    private WSClient ws;

    /**
     * Creates Props for an actor of this type.
     *
     * @param system The ActorSystem to create actors in.
     * @param ws The WSClient for making HTTP requests.
     * @return A Props for creating this actor.
     */
    public static Props props(ActorSystem system, WSClient ws){
        return Props.create(HelperActor.class, system, ws);
    }

    /**
     * Constructor for HelperActor.
     *
     * @param system The ActorSystem to create actors in.
     * @param ws The WSClient for making HTTP requests.
     */
    public HelperActor(ActorSystem system, WSClient ws) {
        this.system = system;
        this.ws = ws;
    }

    /**
     * Message class for creating a SearchActor.
     */
    public static class createActor {

        private final String query;

        /**
         * Constructor for createActor.
         *
         * @param query The search query.
         */
        public createActor(String query) {
//            System.out.println("hgv\nj");
            this.query = query;
        }

        /**
         * Gets the search query.
         *
         * @return The search query.
         */
        public String getQuery() {
            return query;
        }
    }

    
    
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(createActor.class, object -> {
                    // System.out.println("Received message of type: " + object.getClass());
                    if (!queries.contains(object.getQuery())) {
//                        system.actorOf(SearchActor.props(ws, object.getQuery(), cache));
                        queries.add(object.getQuery());
                    }
                    sender().tell(new SearchActor.RegisterMsg(object.getQuery()), getSender());
                    // System.out.println(sender());
                    // System.out.println("Message Sent");
                })
                .match(TerminateActor.class, message -> {
                    System.out.println("Terminating HelperActor");
                    getContext().stop(getSelf());
                })
                .build();
    }
}
