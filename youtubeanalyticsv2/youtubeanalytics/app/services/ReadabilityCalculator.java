package services;

import akka.actor.AbstractActor;
import akka.actor.Props;
import messages.Messages.TerminateActor;
import services.ReadabilityCalculator.ReadabilityResults;

import java.util.List;

/**
 * This actor calculates the readability of a given text.
 * @author Vatsal Dadia
 */

public class ReadabilityCalculator extends AbstractActor{

    /**
     * Props for creating the actor.
     * @return Props for creating the actor.
     * @author Vatsal Dadia
     */

    public static Props props(){
        return Props.create(ReadabilityCalculator.class);
    }

    /**
     * Receives a message to initialize the readability calculator service.
     * @return Receive message to initialize the readability calculator service.
     * @author Vatsal Dadia
     */

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(initReadabilityCalculatorService.class, message -> {

                    double gradeLevel = calculateFleschKincaidGradeLevel(message.description);
                    double readingScore = calculateFleschReadingScore(message.description);
                    System.out.println("Readibility init");
                    getSender().tell(new ReadabilityResults(message.videoId, gradeLevel, readingScore), getSelf());
                })
                .match(TerminateActor.class, message -> {
                    System.out.println("Terminating ReadabilityActor");
                    getContext().stop(getSelf());
                })
                .build();
    }

    /**
     * Message to initialize the readability calculator service.
     * @author Vatsal Dadia
     */
    public static class initReadabilityCalculatorService {
        public final String videoId;
        public final String description;
        public initReadabilityCalculatorService(String videoId, String message){
            this.videoId = videoId;
            this.description = message;
        }
    }


    /**
     * Message to send results of the readability calculation.
     * @author Vatsal Dadia
     */
    public static class ReadabilityResults {
        public final String videoId;
        public final double gradeLevel;
        public final double readingScore;
        public ReadabilityResults(String videoId, double gradeLevel, double readingScore){
            this.videoId = videoId;
            this.gradeLevel = gradeLevel;
            this.readingScore = readingScore;
            System.out.println("Readibility results");
            System.out.println("Grade Level: " + gradeLevel);
            System.out.println("Reading Score: " + readingScore);
        }
    }

    /**
     * Calculates the Flesch-Kincaid grade level for a given description.
     *
     * @param description The text to analyze.
     * @return The Flesch-Kincaid grade level score.
     * @author Vatsal Dadia
     */
    public static double calculateFleschKincaidGradeLevel(String description) {
        if (description == null || description.isEmpty()) {
            return 0.0;
        }
        int totalWords = countWords(description);
        int totalSentences = countSentences(description);
        int totalSyllables = countSyllables(description);

        System.out.println("Total words: " + totalWords);
        return (
                0.39 * ((double) totalWords / totalSentences) +
                        11.8 * ((double) totalSyllables / totalWords) -
                        15.59
        );
    }

    /**
     * Calculates the Flesch reading score for a given description.
     *
     * @param description The text to analyze.
     * @return The Flesch reading score.
     * @author Vatsal Dadia
     */
    public static double calculateFleschReadingScore(String description) {
        if (description == null || description.isEmpty()) {
            return 0.0;
        }
        int totalWords = countWords(description);
        int totalSentences = countSentences(description);
        int totalSyllables = countSyllables(description);

        return (
                206.835 -
                        1.015 * ((double) totalWords / totalSentences) -
                        84.6 * ((double) totalSyllables / totalWords)
        );
    }

    /**
     * Counts the number of words in a given description.
     *
     * @param description The text to analyze.
     * @return The number of words in the description.
     * @author Vatsal Dadia
     */
    static int countWords(String description) {
        String[] words = description.split("\\s+");
        return words.length;
    }

    /**
     * Counts the number of sentences in a given description.
     *
     * @param description The text to analyze.
     * @return The number of sentences in the description.
     * @author Vatsal Dadia
     */
    static int countSentences(String description) {
        String[] sentences = description.split("[.!?]");
        return sentences.length;
    }

    /**
     * Counts the number of syllables in a given word.
     *
     * @param word The word to analyze.
     * @return The number of syllables in the word.
     * @author Vatsal Dadia
     */
    static int countSyllables(String word) {
        String[] words = word.split("\\s+");
        int syllableCount = 0;
        for (String w : words) {
            syllableCount += countSyllablesInWord(w);
        }
        return syllableCount;
    }

    /**
     * Counts the number of syllables in a single word.
     *
     * @param word The word to analyze.
     * @return The number of syllables in the word.
     * @author Vatsal Dadia
     */
    static int countSyllablesInWord(String word) {
        word = word.toLowerCase();
        int count = 0;
        boolean isPrevVowel = false;
        String vowels = "aeiouy";
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (vowels.indexOf(c) != -1) {
                if (!isPrevVowel) {
                    count++;
                    isPrevVowel = true;
                }
            } else {
                isPrevVowel = false;
            }
        }
        if (word.endsWith("e")) {
            count--;
        }
        if (count == 0) {
            count = 1;
        }
        return count;
    }

}
