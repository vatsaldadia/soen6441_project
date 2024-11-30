package services;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Test class for SentimentAnalyzer.
 * This class contains unit tests for the SentimentAnalyzer methods.
 *
 */
public class SentimentAnalyzerTest {

    /**
     * Tests the SentimentAnalyzer with various descriptions.
     * Verifies that the analyzeDescription and analyzeSentiment methods return the expected results.
     *
     */
    @Test
    public void testSentimentAnalyzer() {
        // Test 1: Happy words only
        String happyDescription = "This is a happy wonderful amazing video";
        double happyResult = SentimentAnalyzer.analyzeDescription(happyDescription);
        assertTrue("Should be positive for happy words", happyResult > 0);
        assertEquals(100.0, happyResult, 0.01);

        // Test 2: Sad words only
        String sadDescription = "This is sad and terrible content";
        double sadResult = SentimentAnalyzer.analyzeDescription(sadDescription);
        assertTrue("Should be negative for sad words", sadResult < 0);
        assertEquals(-100.0, sadResult, 0.01);

        // Test 3: Mixed sentiment
        String mixedDescription = "This is happy but also sad content";
        double mixedResult = SentimentAnalyzer.analyzeDescription(mixedDescription);
        assertEquals(0.0, mixedResult, 0.01);

        // Test 4: No sentiment words
        String neutralDescription = "This is just a normal video";
        double neutralResult = SentimentAnalyzer.analyzeDescription(neutralDescription);
        assertEquals(0.0, neutralResult, 0.01);

        // Test 5: Emojis
        String emojiDescription = "This video 😊 is great 😃";
        double emojiResult = SentimentAnalyzer.analyzeDescription(emojiDescription);
        assertTrue("Should be positive for happy emojis", emojiResult > 0);

        // Test 6: Case insensitivity
        String upperCaseDescription = "This is HAPPY and WONDERFUL";
        double caseResult = SentimentAnalyzer.analyzeDescription(upperCaseDescription);
        assertTrue("Should be positive regardless of case", caseResult > 0);

        // Test 7: Multiple spaces
        String spacedDescription = "happy    wonderful   amazing";
        double spacedResult = SentimentAnalyzer.analyzeDescription(spacedDescription);
        assertEquals(100.0, spacedResult, 0.01);

        // Test 8: analyzeSentiment with all happy descriptions
        List<String> happyDescriptions = Arrays.asList("happy wonderful video", "amazing fantastic content", "great excellent stuff");
        String happySentiment = SentimentAnalyzer.analyzeSentiment(happyDescriptions);
        assertEquals(":-)", happySentiment);

        // Test 9: analyzeSentiment with all sad descriptions
        List<String> sadDescriptions = Arrays.asList("sad terrible video", "horrible awful content", "worst disappointing stuff");
        String sadSentiment = SentimentAnalyzer.analyzeSentiment(sadDescriptions);
        assertEquals(":-(", sadSentiment);

        // Test 10: analyzeSentiment with mixed descriptions
        List<String> mixedDescriptions = Arrays.asList("happy wonderful video", "terrible awful content", "normal neutral stuff");
        String mixedSentiment = SentimentAnalyzer.analyzeSentiment(mixedDescriptions);
        assertEquals(":-)", mixedSentiment);

        // Test 11: Empty list
        List<String> emptyList = Collections.emptyList();
        String emptyResult = SentimentAnalyzer.analyzeSentiment(emptyList);
        assertEquals(":-|", emptyResult);

        // Test 12: No sentiment words in list
        List<String> neutralDescriptions = Arrays.asList("regular video", "normal content", "standard stuff");
        String neutralSentiment = SentimentAnalyzer.analyzeSentiment(neutralDescriptions);
        assertEquals(":-|", neutralSentiment);

        // Test 13: Below threshold sentiment
        List<String> belowThresholdDescriptions = Arrays.asList("happy video but also sad", "good and bad content", "normal stuff");
        String thresholdResult = SentimentAnalyzer.analyzeSentiment(belowThresholdDescriptions);
        assertEquals(":-|", thresholdResult);
    }

    
}