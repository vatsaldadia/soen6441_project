package services;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import models.YoutubeVideo;
import org.junit.Test;

/**
 * Test class for WordStatsService.
 * This class contains unit tests for the WordStatsService methods.
 *
 * @author Rolwyn Raju
 */
public class WordStatsServiceTest {

    private final WordStatsService wordStatsService = new WordStatsService();

    /**
     * Tests the calculateWordStats method.
     * Verifies that the method correctly calculates word statistics from video descriptions.
     *
     * @author Rolwyn Raju
     */
    @Test
    public void testCalculateWordStats() {
        YoutubeVideo video1 = new YoutubeVideo(
            "id1",
            "title1",
            "Test this description.",
            "url1",
            "channel1",
            "date1",
            null
        );
        YoutubeVideo video2 = new YoutubeVideo(
            "id2",
            "title2",
            "Another description test.",
            "url2",
            "channel2",
            "date2",
            10L
        );
        List<YoutubeVideo> videos = Arrays.asList(video1, video2);

        Map<String, Long> wordCounts = wordStatsService.calculateWordStats(
            videos
                .stream()
                .map(YoutubeVideo::getDescription)
                .collect(Collectors.toList())
        );

        assertEquals(Long.valueOf(2), wordCounts.get("test"));
        assertEquals(Long.valueOf(2), wordCounts.get("description"));
        assertEquals(Long.valueOf(1), wordCounts.get("another"));
    }

    /**
     * Tests the calculateWordStats method with stopwords.
     * Verifies that the method correctly excludes stopwords from the word statistics.
     *
     * @author Rolwyn Raju
     */
    @Test
    public void testCalculateWordStatsWithStopwords() {
        YoutubeVideo video = new YoutubeVideo(
            "id1",
            "title1",
            "This is a test.",
            "url1",
            "channel1",
            "date1",
            null
        );
        List<YoutubeVideo> videos = Arrays.asList(video);

        Map<String, Long> wordCounts = wordStatsService.calculateWordStats(
            videos
                .stream()
                .map(YoutubeVideo::getDescription)
                .collect(Collectors.toList())
        );

        assertNull(wordCounts.get("is"));
        assertEquals(Long.valueOf(1), wordCounts.get("test"));
    }

    /**
     * Tests the getWordStats method with empty descriptions.
     * Verifies that the method returns an empty map when descriptions are empty.
     *
     * @author Rolwyn Raju
     */
    @Test
    public void testGetWordStatsEmptyDescriptions() {
        // Creating a list with a YoutubeVideo having an empty description
        YoutubeVideo video = new YoutubeVideo(
            "id1",
            "title1",
            "",
            "url1",
            "channel1",
            "date1",
            null
        );
        List<YoutubeVideo> videos = Arrays.asList(video);

        // Calling getWordStats method
        Map<String, Long> wordCounts = WordStatsService.computeWordStats(
            videos
        );

        // Verifying the word count map is empty since the description is empty
        assertTrue(wordCounts.isEmpty());
    }
}