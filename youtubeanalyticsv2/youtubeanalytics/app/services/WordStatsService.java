package services;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for calculating word statistics from video descriptions.
 * This class provides methods to process video descriptions and compute word frequencies.
 *
 * @author Rolwyn Raju
 */
public class WordStatsService {

    // List of stopwords
    private static final List<String> STOPWORDS = Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "he",
            "in", "is", "it", "its", "of", "on", "that", "the", "to", "was", "were",
            "will", "with", "i", "you", "she", "we", "they", "my", "your", "this",
            "these", "those", "or", "but", "if", "because", "just"
    );

    /**
     * Processes the video descriptions and returns a word frequency map.
     *
     * @param descriptions List of video descriptions.
     * @return Sorted map of word frequencies.
     * @author Rolwyn Raju
     */
    public static Map<String, Long> calculateWordStats(List<String> descriptions) {
        // Concatenate all descriptions into a single string
        String allDescriptions = descriptions.stream().collect(Collectors.joining(" "));

        // Split the string into words, normalize to lowercase, and count word frequencies
        Map<String, Long> wordCount = Arrays.stream(allDescriptions.split("\\W+"))
                .map(String::toLowerCase)
                .filter(word -> !word.isEmpty()) // Remove empty words
                .filter(word -> word.length() > 1) // Remove single-character words
                .filter(word -> !STOPWORDS.contains(word)) // Remove stopwords
                .filter(word -> !word.matches("\\d+")) // Remove numbers
                .collect(Collectors.groupingBy(word -> word, Collectors.counting()));

        // Sort the map by frequency in descending order
        return wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Computes word statistics from a list of YoutubeVideo objects.
     *
     * @param descriptions is a List of String descriptions.
     * @return Sorted map of word frequencies.
     * @author Rolwyn Raju
     */
    public static Map<String, Long> computeWordStats(List<String> descriptions) {
        // Splitting the string into words, then to lowercase, and counting word frequencies
        Map<String, Long> sortedWordCount = calculateWordStats(descriptions);

        // Passing the sorted word stats map and search query to the view
        return sortedWordCount;
    }
}