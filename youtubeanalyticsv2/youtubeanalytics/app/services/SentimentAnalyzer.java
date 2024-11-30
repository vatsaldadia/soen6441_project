package services;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for analyzing sentiment in video descriptions.
 *
 * @author Mohnish Mirchandani
 */
public class SentimentAnalyzer {

	private static final List<String> HAPPY_WORDS = Arrays.asList(
		"happy",
		"joy",
		"excellent",
		"great",
		"amazing",
		"wonderful",
		"fantastic",
		"good",
		"love",
		"awesome",
		"excited",
		"fun",
		"beautiful",
		"smile",
		"laugh",
		"best",
		"perfect",
		"brilliant",
		"excellent",
		"😊",
		"😃",
		"😄",
		"🙂",
		":-)",
		":)",
		"=)"
	);

	private static final List<String> SAD_WORDS = Arrays.asList(
		"sad",
		"bad",
		"terrible",
		"awful",
		"horrible",
		"worst",
		"hate",
		"disappointed",
		"unfortunate",
		"sorry",
		"fail",
		"poor",
		"crying",
		"unhappy",
		"miserable",
		"😢",
		"😭",
		"😞",
		"😥",
		":-(",
		":(",
		"=("
	);

	/**
	 * Analyzes the sentiment of a list of descriptions.
	 *
	 * @param descriptions List of text descriptions.
	 * @return The overall sentiment as a string emoticon.
	 * @author Mohnish Mirchandani
	 */
	public static String analyzeSentiment(List<String> descriptions) {
		double[] sentiments = descriptions
			.stream()
			.mapToDouble(SentimentAnalyzer::analyzeDescription)
			.filter(score -> score != 0.0)
			.toArray();

		double happyAverage = Arrays.stream(sentiments)
			.filter(score -> score > 0)
			.average()
			.orElse(0.0);

		double sadAverage = Arrays.stream(sentiments)
			.filter(score -> score < 0)
			.average()
			.orElse(0.0);

		sadAverage = Math.abs(sadAverage);

		// System.out.println(happyAverage);
		// System.out.println(sadAverage);

		if (happyAverage > 70) {
			return ":-)";
		} else if (sadAverage > 70) {
			return ":-(";
		} else {
			return ":-|";
		}
	}

	/**
	 * Analyzes the sentiment of a single description.
	 *
	 * @param description The text description to analyze.
	 * @return The sentiment score as a double value. Positive for happy, negative for sad, and zero for neutral sentiment.
	 */
	public static double analyzeDescription(String description) {
		String[] words = description.toLowerCase().split("\\s+");

		long totalSentimentWords = Arrays.stream(words)
			.filter(
				word -> HAPPY_WORDS.contains(word) || SAD_WORDS.contains(word)
			)
			.count();

		totalSentimentWords = Math.max((long) 1, totalSentimentWords);

		long happyCount = Arrays.stream(words)
			.filter(HAPPY_WORDS::contains)
			.count();

		long sadCount = Arrays.stream(words)
			.filter(SAD_WORDS::contains)
			.count();

		double happyPercentage = (happyCount * 100) / totalSentimentWords;
		double sadPercentage = (sadCount * 100) / totalSentimentWords;

		if (happyCount > sadCount) {
			return happyPercentage;
		} else if (sadCount > happyCount) {
			return -1 * sadPercentage;
		} else {
			return 0.0;
		}
	}
}
