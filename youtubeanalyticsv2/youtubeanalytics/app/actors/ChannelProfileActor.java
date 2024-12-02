package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.typed.javadsl.Receive;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import services.ChannelProfileService;
import play.libs.ws.WSClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class ChannelProfileActor extends AbstractActor {

    private final ChannelProfileService channelProfileService;
    public static Map<String, JsonNode> channelProfileCache = new HashMap<>();

    public ChannelProfileActor(ChannelProfileService channelProfileService) {
            this.channelProfileService = channelProfileService;
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(InitChannelProfileService.class, message -> {
                        String channelId = message.channelId;

                        CompletionStage<JsonNode> channelDetailsFuture = channelProfileService.fetchChannelDetails(channelId);
                        CompletionStage<JsonNode> latestVideosFuture = channelProfileService.fetchLatestVideos(channelId);
                        ActorRef sender = getSender();
                        channelDetailsFuture.thenCombine(latestVideosFuture, (channelDetails, latestVideos) -> {
                            ObjectNode response = JsonNodeFactory.instance.objectNode();
                            response.set("channelDetails", channelDetails);
                            response.set("latestVideos", latestVideos);
                            sender.tell(response, getSelf());
                            return response;
                            //return new ChannelProfileResults(channelId, response);
                        });
//                                .thenAccept(results -> getSender().tell(results, getSelf()));
                    })
                    .build();
        }

        public static Props props(ChannelProfileService channelProfileService) {
            return Props.create(ChannelProfileActor.class, () -> new ChannelProfileActor(channelProfileService));
        }
    public static class InitChannelProfileService {
        public final String channelId;

        public InitChannelProfileService(String channelId) {
            this.channelId = channelId;
        }
    }

    // Message class for results
    public static class ChannelProfileResults {
        public final String channelId;
        public final JsonNode channelProfile;

        public ChannelProfileResults(String channelId, JsonNode channelProfile) {
            this.channelId = channelId;
            this.channelProfile = channelProfile;
            System.out.println("Channel Profile for " + channelId + ": " + channelProfile);

        }
    }

}

    // Message class to initialize the service


