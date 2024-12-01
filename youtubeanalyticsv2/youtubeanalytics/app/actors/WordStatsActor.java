package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.Messages;
import services.WordStatsService;

import static services.WordStatsService.calculateWordStats;



/**
 * Actor class for processing word statistics from video descriptions.
 * This actor receives messages to calculate word statistics and returns the results.
 *
 * @author Rolwyn Raju
 */
public class WordStatsActor extends AbstractActor {

    public static Map<String, JsonNode> wordStatsMap = new HashMap<>();


    /**
     * Creates the receive behavior for this actor.
     *
     * @return the receive behavior.
     * @author Rolwyn Raju
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InitWordStatsService.class, message -> {
                    Map<String, Long> wordStats = calculateWordStats(message.descriptions);
                    getSender().tell(new WordStatsResults(message.videoId, wordStats), getSelf());
                })
                .match(Messages.TerminateActor.class, message -> {
                    System.out.println("Terminating WordStatsActor");
                    getContext().stop(getSelf());
                })
                .build();
    }


    /**
     * Creates a Props instance for this actor.
     *
     * @return a Props instance.
     * @author Rolwyn Raju
     */
    public static Props props(){
        return Props.create(WordStatsActor.class);
    }


    /**
     * Message class to initialize the word statistics service.
     */
    public static class InitWordStatsService {
        public final String videoId;
        public final List<String> descriptions;


        /**
         * Constructs an InitWordStatsService message.
         *
         * @param videoId the ID of the video.
         * @param descriptions the list of video descriptions.
         */
        public InitWordStatsService(String videoId, List<String> descriptions) {
            this.videoId = videoId;
            this.descriptions = descriptions;
        }
    }


    /**
     * Message class for returning word statistics results.
     */
    public static class WordStatsResults {
        public final String videoId;
        public final JsonNode wordStats;


        /**
         * Constructs a WordStatsResults message.
         *
         * @param videoId the ID of the video.
         * @param wordStats the map of word statistics.
         * @author Rolwyn Raju
         */
        public WordStatsResults(String videoId, Map<String, Long> wordStats) {
            this.videoId = videoId;
            ObjectMapper mapper = new ObjectMapper();
            this.wordStats = mapper.valueToTree(wordStats);
            System.out.println(wordStats);
        }
    }
}

