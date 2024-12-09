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

                        for (JsonNode item : latestTag.get("items")) {
                            ObjectNode videoNode =
                                    JsonNodeFactory.instance.objectNode();
                            String videoId = item.get("id").get("videoId").asText();
                            JsonNode snippet = item.get("snippet");
                            videoNode.put("videoId", videoId);
                            videoNode.put("title", snippet.get("title").asText());
                            videoNode.put(
                                    "description",
                                    snippet.get("description").asText()
                            );
                            videoNode.put(
                                    "thumbnailUrl",
                                    snippet
                                            .get("thumbnails")
                                            .get("default")
                                            .get("url")
                                            .asText()
                            );
                            videoList.add(videoNode);
                        }

                        // Send the ObjectNode as a response
                        sender.tell(videoList, getSelf());
                        return videoList;
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