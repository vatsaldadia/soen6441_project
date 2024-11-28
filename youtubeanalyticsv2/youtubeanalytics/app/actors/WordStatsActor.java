package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import services.WordStatsService;

import static services.WordStatsService.calculateWordStats;




public class WordStatsActor extends AbstractActor {

    public static Map<String, JsonNode> wordStatsMap = new HashMap<>();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InitWordStatsService.class, message -> {
                    Map<String, Long> wordStats = calculateWordStats(message.descriptions);
                    getSender().tell(new WordStatsResults(message.videoId, wordStats), getSelf());
                })
                .build();
    }

    public static Props props(){
        return Props.create(WordStatsActor.class);
    }

    public static class InitWordStatsService {
        public final String videoId;
        public final List<String> descriptions;

        public InitWordStatsService(String videoId, List<String> descriptions) {
            this.videoId = videoId;
            this.descriptions = descriptions;
        }
    }

    public static class WordStatsResults {
        public final String videoId;
        public final JsonNode wordStats;

        public WordStatsResults(String videoId, Map<String, Long> wordStats) {
            this.videoId = videoId;
            ObjectMapper mapper = new ObjectMapper();
            this.wordStats = mapper.valueToTree(wordStats);
            System.out.println(wordStats);
        }
    }
}

