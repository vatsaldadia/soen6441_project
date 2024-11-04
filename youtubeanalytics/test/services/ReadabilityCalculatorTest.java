package services;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReadabilityCalculatorTest {

    @Test
    public void testCalculateFleschKincaidGradeLevel() {
        String description = "This is a simple sentence. It is used for testing.";
        double expectedGradeLevel = 2.89; // Example expected value
        double actualGradeLevel = Math.round(ReadabilityCalculator.calculateFleschKincaidGradeLevel(description) * 100.0) / 100.0;
        assertEquals(expectedGradeLevel, actualGradeLevel, 0.02);
    }

    @Test
    public void testCalculateFleschReadingScore() {
        String description = "This is a simple sentence. It is used for testing.";
        double expectedReadingScore = 83.3; // Example expected value
        double actualReadingScore = Math.round(ReadabilityCalculator.calculateFleschReadingScore(description) * 100.0) / 100.0;
        assertEquals(expectedReadingScore, actualReadingScore, 0.02);
    }

    @Test
    public void testCountWords() {
        String description = "This is a simple sentence.";
        int expectedWordCount = 5;
        int actualWordCount = ReadabilityCalculator.countWords(description);
        assertEquals(expectedWordCount, actualWordCount);
    }

    @Test
    public void testCountSentences() {
        String description = "This is a simple sentence. It is used for testing.";
        int expectedSentenceCount = 2;
        int actualSentenceCount = ReadabilityCalculator.countSentences(description);
        assertEquals(expectedSentenceCount, actualSentenceCount);
    }

    @Test
    public void testCountSyllables() {
        String description = "This is a simple sentence.";
        int expectedSyllableCount = 7; // Example expected value
        int actualSyllableCount = ReadabilityCalculator.countSyllables(description);
        assertEquals(expectedSyllableCount, actualSyllableCount);
    }

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
    }
}
