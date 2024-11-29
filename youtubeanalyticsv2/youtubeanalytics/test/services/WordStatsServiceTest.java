package services;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class WordStatsServiceTest {

    @Test
    public void testCalculateWordStats() {
        List<String> descriptions = Arrays.asList(
                "This is a test description",
                "Another test description with more words",
                "Test description with some common words"
        );

        Map<String, Long> wordStats = WordStatsService.calculateWordStats(descriptions);

        assertEquals(1, wordStats.get("another"));
        assertEquals(3, wordStats.get("description"));
        assertEquals(3, wordStats.get("test"));
        assertTrue(wordStats.containsKey("words"));
    }

    @Test
    public void testComputeWordStats() {
        List<String> descriptions = Arrays.asList(
                "This is a test description",
                "Another test description with more words",
                "Test description with some common words"
        );

        Map<String, Long> wordStats = WordStatsService.calculateWordStats(descriptions);

        assertEquals(1, wordStats.get("another"));
        assertEquals(3, wordStats.get("description"));
        assertEquals(3, wordStats.get("test"));
        assertTrue(wordStats.containsKey("words"));
    }

    @Test
    public void testEmptyDescriptions() {
        List<String> descriptions = Collections.emptyList();

        Map<String, Long> wordStats = WordStatsService.calculateWordStats(descriptions);

        assertTrue(wordStats.isEmpty());
    }

    @Test
    public void testDescriptionsWithOnlyStopwords() {
        List<String> descriptions = Arrays.asList(
                "a an and are as at be by for from has he in is it its of on that the to was were will with"
        );

        Map<String, Long> wordStats = WordStatsService.calculateWordStats(descriptions);

        assertTrue(wordStats.isEmpty());
    }

    @Test
    public void testDescriptionsWithNumbers() {
        List<String> descriptions = Arrays.asList(
                "This is a test description with numbers 123 456 789"
        );

        Map<String, Long> wordStats = WordStatsService.calculateWordStats(descriptions);

        assertEquals(1, wordStats.get("test"));
        assertEquals(1, wordStats.get("description"));
        assertTrue(!wordStats.containsKey("123"));
    }

    @Test
    public void testNullDescriptions() {
        assertThrows(NullPointerException.class, () -> {
            WordStatsService.calculateWordStats(null);
        });
    }

    @Test
    public void testDescriptionsWithSpecialCharacters() {
        List<String> descriptions = Arrays.asList(
                "This is a test description with special characters !@#$%^&*()"
        );

        Map<String, Long> wordStats = WordStatsService.calculateWordStats(descriptions);

        assertEquals(1, wordStats.get("test"));
        assertEquals(1, wordStats.get("description"));
        assertTrue(!wordStats.containsKey("!@#$%^&*()"));
    }

    @Test
    public void testDescriptionsWithMixedCase() {
        List<String> descriptions = Arrays.asList(
                "This is a Test Description",
                "Another test description with more Words",
                "test DESCRIPTION with some common words"
        );

        Map<String, Long> wordStats = WordStatsService.calculateWordStats(descriptions);

        assertEquals(1, wordStats.get("another"));
        assertEquals(3, wordStats.get("description"));
        assertEquals(3, wordStats.get("test"));
        assertTrue(wordStats.containsKey("words"));
    }
}