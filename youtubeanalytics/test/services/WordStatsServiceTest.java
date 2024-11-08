package services;

import static org.junit.Assert.*;
import org.junit.Test;
import services.WordStatsService;
import models.YoutubeVideo;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WordStatsServiceTest {

    private final WordStatsService wordStatsService = new WordStatsService();

    @Test
    public void testCalculateWordStats() {
        YoutubeVideo video1 = new YoutubeVideo("id1", "title1", "Test this description.", "url1", "channel1", "date1", null);
        YoutubeVideo video2 = new YoutubeVideo("id2", "title2", "Another description test.", "url2", "channel2", "date2", 10L);
        List<YoutubeVideo> videos = Arrays.asList(video1, video2);

        Map<String, Long> wordCounts = wordStatsService.calculateWordStats(
                videos.stream().map(YoutubeVideo::getDescription).collect(Collectors.toList())
        );


        assertEquals(Long.valueOf(2), wordCounts.get("test"));
        assertEquals(Long.valueOf(2), wordCounts.get("description"));
        assertEquals(Long.valueOf(1), wordCounts.get("another"));
    }

    @Test
    public void testCalculateWordStatsWithStopwords() {
        YoutubeVideo video = new YoutubeVideo("id1", "title1", "This is a test.", "url1", "channel1", "date1", null);
        List<YoutubeVideo> videos = Arrays.asList(video);

        Map<String, Long> wordCounts = wordStatsService.calculateWordStats(
                videos.stream().map(YoutubeVideo::getDescription).collect(Collectors.toList())
        );

        assertNull(wordCounts.get("is"));
        assertEquals(Long.valueOf(1), wordCounts.get("test"));
    }

    @Test
    public void testGetWordStatsEmptyDescriptions() {
        // Creating a list with a YoutubeVideo having an empty description
        YoutubeVideo video = new YoutubeVideo("id1", "title1", "", "url1", "channel1", "date1", null);
        List<YoutubeVideo> videos = Arrays.asList(video);

        // Calling getWordStats method
        Map<String, Long> wordCounts = WordStatsService.getWordStats(videos);

        // Verifying the word count map is empty since the description is empty
        assertTrue(wordCounts.isEmpty());
    }
}
