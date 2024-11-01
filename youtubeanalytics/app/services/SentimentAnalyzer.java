package services;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

	public static List<String> analyzeSentiment(List<String> descriptions) {
		List<String> sentimentList = descriptions
			.stream()
			.map(SentimentAnalyzer::analyzeDescription)
			.collect(Collectors.toList());

		return sentimentList;
	}

	public static String analyzeDescription(String description) {
		String[] words = description.toLowerCase().split("\\s+");

		long totalSentimentWords = Arrays.stream(words)
			.filter(
				word -> HAPPY_WORDS.contains(word) || SAD_WORDS.contains(word)
			)
			.count();

		if (totalSentimentWords == 0) {
			return ":-|";
		}

		long happyCount = Arrays.stream(words)
			.filter(HAPPY_WORDS::contains)
			.count();

		long sadCount = Arrays.stream(words)
			.filter(SAD_WORDS::contains)
			.count();

		double happyPercentage = (happyCount * 100) / totalSentimentWords;
		double sadPercentage = (sadCount * 100) / totalSentimentWords;

		return getSentimentEmoticon(happyPercentage, sadPercentage);
	}

	private static String getSentimentEmoticon(
		double happyPercentage,
		double sadPercentage
	) {
		if (happyPercentage > 0.7) {
			return ":-)";
		} else if (sadPercentage > 0.7) {
			return ":-(";
		} else {
			return ":-|";
		}
	}
}
