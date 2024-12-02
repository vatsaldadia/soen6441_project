package services;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class for WordStatsService.
 * This class contains unit tests for the WordStatsService methods.
 * @author Rolwyn Raju
 */
public class WordStatsServiceTest {


    /**
     * Tests the calculateWordStats method with a list of descriptions.
     * Verifies that the word statistics are calculated correctly.
     * @author Rolwyn Raju
     */
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


    /**
     * Tests the calculateWordStats method with a list of descriptions.
     * Verifies that the word statistics are calculated correctly.
     * @author Rolwyn Raju
     */
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


    /**
     * Tests the calculateWordStats method with an empty list of descriptions.
     * Verifies that the word statistics map is empty.
     * @author Rolwyn Raju
     */
    @Test
    public void testEmptyDescriptions() {
        List<String> descriptions = Collections.emptyList();

        Map<String, Long> wordStats = WordStatsService.calculateWordStats(descriptions);

        assertTrue(wordStats.isEmpty());
    }


    /**
     * Tests the calculateWordStats method with descriptions containing only stopwords.
     * Verifies that the word statistics map is empty.
     * @author Rolwyn Raju
     */
    @Test
    public void testDescriptionsWithOnlyStopwords() {
        List<String> descriptions = Arrays.asList(
                "a an and are as at be by for from has he in is it its of on that the to was were will with"
        );

        Map<String, Long> wordStats = WordStatsService.calculateWordStats(descriptions);

        assertTrue(wordStats.isEmpty());
    }


    /**
     * Tests the calculateWordStats method with descriptions containing numbers.
     * Verifies that the word statistics are calculated correctly and numbers are ignored.
     * @author Rolwyn Raju
     */
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


    /**
     * Tests the calculateWordStats method with null descriptions.
     * Verifies that a NullPointerException is thrown.
     * @author Rolwyn Raju
     */
    @Test
    public void testNullDescriptions() {
        assertThrows(NullPointerException.class, () -> {
            WordStatsService.calculateWordStats(null);
        });
    }


    /**
     * Tests the calculateWordStats method with descriptions containing special characters.
     * Verifies that the word statistics are calculated correctly and special characters are ignored.
     * @author Rolwyn Raju
     */
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


    /**
     * Tests the calculateWordStats method with descriptions containing mixed case words.
     * Verifies that the word statistics are calculated correctly and case is ignored.
     * @author Rolwyn Raju
     */
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