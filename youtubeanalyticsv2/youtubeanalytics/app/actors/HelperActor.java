package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import play.libs.ws.WSClient;

import java.util.HashSet;
import java.util.Set;


public class HelperActor extends AbstractActor {

    private Set<String> queries = new HashSet<>();

    private ActorSystem system;
    private WSClient ws;

    public static Props props(ActorSystem system, WSClient ws){
        return Props.create(HelperActor.class, system, ws);
    }

    public HelperActor(ActorSystem system, WSClient ws) {
        this.system = system;
        this.ws = ws;
    }

    public static class createActor {

        private final String query;
        public createActor(String query) {
//            System.out.println("hgv\nj");
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(createActor.class, object -> {
                    System.out.println("Received message of type: " + object.getClass());
                    if (!queries.contains(object.getQuery())) {
//                        system.actorOf(SearchActor.props(ws, object.getQuery(), cache));
                        queries.add(object.getQuery());
                    }
                    sender().tell(new SearchActor.RegisterMsg(object.getQuery()), getSender());
                })
                .build();
    }
}
