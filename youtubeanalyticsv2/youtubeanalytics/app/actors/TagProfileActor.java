package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.typed.javadsl.Receive;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import services.TagProfileService;
import play.libs.ws.WSClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class TagProfileActor extends AbstractActor {

    private final TagProfileService tagProfileService;
    public static Map<String, JsonNode> channelProfileCache = new HashMap<>();

    public TagProfileActor(TagProfileService tagProfileService) {
        this.tagProfileService = tagProfileService;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InitTagProfileService.class, message -> {
                    String tagId = message.tagId;

                    CompletionStage<JsonNode> tagDetailsFuture = tagProfileService.fetchVideosByTag(tagId);
                    CompletionStage<JsonNode> latestTagFuture = tagProfileService.fetchlatestTagFuture(tagId);



                    ActorRef sender = getSender();

                    ArrayNode videoList = JsonNodeFactory.instance.arrayNode();
                    latestTagFuture.thenApply(latestTag -> {
                        // Extract data from the JsonNode
                        JsonNode items = latestTag.get("items");
                        ObjectNode response = JsonNodeFactory.instance.objectNode();

                        if (items != null && items.isArray() && items.size() > 0) {
                            JsonNode firstItem = items.get(0);

                            // Extract video details
                            String videoId = firstItem.at("/id/videoId").asText();
                            String title = firstItem.at("/snippet/title").asText();
                            String channelTitle = firstItem.at("/snippet/channelTitle").asText();
                            String thumbnailUrl = firstItem.at("/snippet/thumbnails/high/url").asText();

                            // Construct response ObjectNode
                            response.put("videoId", videoId);
                            response.put("title", title);
                            response.put("channelTitle", channelTitle);
                            response.put("thumbnailUrl", thumbnailUrl);
                        } else {
                            response.put("error", "No items found in the response.");
                        }

                        // Send the ObjectNode as a response
                        sender.tell(response, getSelf());
                        return response;
                    });
//                                .thenAccept(results -> getSender().tell(results, getSelf()));
                })
                .build();
    }

    public static Props props(TagProfileService tagProfileService) {
        return Props.create(TagProfileActor.class, () -> new TagProfileActor(tagProfileService));
    }
    public static class InitTagProfileService {
        public final String tagId;

        public InitTagProfileService(String tagId) {
            this.tagId = tagId;
        }
    }

    // Message class for results
    public static class TagProfileResults {
        public final String tagId;
        public final JsonNode tagProfile;

        public TagProfileResults(String tagId, JsonNode tagProfile) {
            this.tagId = tagId;
            this.tagProfile = tagProfile;
            System.out.println("Tag Profile for " + tagId + ": " + tagProfile);

        }
    }

}