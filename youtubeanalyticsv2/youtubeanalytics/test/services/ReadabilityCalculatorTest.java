package services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.Test;

/**
 * Test class for ReadabilityCalculator.
 * This class contains unit tests for the ReadabilityCalculator methods.
 *
 * @author Vatsal Dadia
 */
public class ReadabilityCalculatorTest {

    /**
     * Tests the calculateFleschKincaidGradeLevel method.
     * Verifies that the method correctly calculates the Flesch-Kincaid grade level.
     *
     * @author Vatsal Dadia
     */
    @Test
    public void testCalculateFleschKincaidGradeLevel() {
        String description = "This is a simple sentence. It is used for testing.";
        double expectedGradeLevel = 2.89; // Example expected value
        double actualGradeLevel = Math.round(
                ReadabilityCalculator.calculateFleschKincaidGradeLevel(description) * 100.0
        ) / 100.0;
        assertEquals(expectedGradeLevel, actualGradeLevel, 0.02);
    }

    /**
     * Tests the calculateFleschReadingScore method.
     * Verifies that the method correctly calculates the Flesch reading score.
     *
     * @author Vatsal Dadia
     */
    @Test
    public void testCalculateFleschReadingScore() {
        String description = "This is a simple sentence. It is used for testing.";
        double expectedReadingScore = 83.3; // Example expected value
        double actualReadingScore = Math.round(
                ReadabilityCalculator.calculateFleschReadingScore(description) * 100.0
        ) / 100.0;
        assertEquals(expectedReadingScore, actualReadingScore, 0.02);
    }

    /**
     * Tests the countWords method.
     * Verifies that the method correctly counts the number of words in a description.
     *
     * @author Vatsal Dadia
     */
    @Test
    public void testCountWords() {
        String description = "This is a simple sentence.";
        int expectedWordCount = 5;
        int actualWordCount = ReadabilityCalculator.countWords(description);
        assertEquals(expectedWordCount, actualWordCount);
    }

    /**
     * Tests the countWords method with an empty description.
     * Verifies that the method returns 1 for an empty description.
     *
     * @author Vatsal Dadia
     */
    @Test
    public void testCountWords_ZeroCheck() {
        String description = "";
        int expectedWordCount = 1;
        int actualWordCount = ReadabilityCalculator.countWords(description);
        assertEquals(expectedWordCount, actualWordCount);
    }

    /**
     * Tests the countSentences method.
     * Verifies that the method correctly counts the number of sentences in a description.
     *
     * @author Vatsal Dadia
     */
    @Test
    public void testCountSentences() {
        String description = "This is a simple sentence. It is used for testing.";
        int expectedSentenceCount = 2;
        int actualSentenceCount = ReadabilityCalculator.countSentences(description);
        assertEquals(expectedSentenceCount, actualSentenceCount);
    }

    /**
     * Tests the countSentences method with an empty description.
     * Verifies that the method returns 1 for an empty description.
     *
     * @author Vatsal Dadia
     */
    @Test
    public void testCountSentences_ZeroCheck() {
        String description = "";
        int expectedSentenceCount = 1;
        int actualSentenceCount = ReadabilityCalculator.countSentences(description);
        assertEquals(expectedSentenceCount, actualSentenceCount);
    }

    /**
     * Tests the countSyllables method.
     * Verifies that the method correctly counts the number of syllables in a description.
     *
     * @author Vatsal Dadia
     */
    @Test
    public void testCountSyllables() {
        String description = "This is a simple sentence.";
        int expectedSyllableCount = 7; // Example expected value
        int actualSyllableCount = ReadabilityCalculator.countSyllables(description);
        assertEquals(expectedSyllableCount, actualSyllableCount);
    }

    /**
     * Tests the countSyllablesInWord method.
     * Verifies that the method correctly counts the number of syllables in individual words.
     *
     * @author Vatsal Dadia
     */
    @Test
    public void testCountSyllablesInWord() {
        assertEquals(1, ReadabilityCalculator.countSyllablesInWord("This"));
        assertEquals(1, ReadabilityCalculator.countSyllablesInWord("is"));
        assertEquals(1, ReadabilityCalculator.countSyllablesInWord("a"));
        assertEquals(1, ReadabilityCalculator.countSyllablesInWord("simple"));
        assertEquals(2, ReadabilityCalculator.countSyllablesInWord("sentence"));
        assertEquals(1, ReadabilityCalculator.countSyllablesInWord("It"));
        assertEquals(1, ReadabilityCalculator.countSyllablesInWord("is"));
        assertEquals(2, ReadabilityCalculator.countSyllablesInWord("used"));
        assertEquals(1, ReadabilityCalculator.countSyllablesInWord("for"));
        assertEquals(2, ReadabilityCalculator.countSyllablesInWord("testing"));
        assertEquals(1, ReadabilityCalculator.countSyllablesInWord("ggc"));
        assertEquals(1, ReadabilityCalculator.countSyllablesInWord(""));
    }
}