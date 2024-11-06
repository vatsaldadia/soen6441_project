package services;


import java.util.List;

public class ReadabilityCalculator {

    public static double calculateGradeAvg(List<Double> grades) {
        return grades.stream().
                mapToDouble(Double::doubleValue).
                average().
                orElse(0.0);
    }

    public static double calculateScoreAvg(List<Double> scores) {
        return scores.stream().
                mapToDouble(Double::doubleValue).
                average().
                orElse(0.0);
    }

    public static double calculateFleschKincaidGradeLevel(String description) {
        int totalWords = countWords(description);
        int totalSentences = countSentences(description);
        int totalSyllables = countSyllables(description);

        return 0.39 * ((double) totalWords / totalSentences) + 11.8 * ((double) totalSyllables / totalWords) - 15.59;
    }

    public static double calculateFleschReadingScore(String description) {
        int totalWords = countWords(description);
        int totalSentences = countSentences(description);
        int totalSyllables = countSyllables(description);

        return 206.835 - 1.015 * ((double) totalWords / totalSentences) - 84.6 * ((double) totalSyllables / totalWords);
    }

    static int countWords(String description) {
        String[] words = description.split("\\s+");
        return words.length;
    }

    static int countSentences(String description) {
        String[] sentences = description.split("[.!?]");
        return sentences.length;
    }

    static int countSyllables(String word) {
        String[] words = word.split("\\s+");
        int syllableCount = 0;
        for (String w : words) {
            syllableCount += countSyllablesInWord(w);
        }
        return syllableCount;
    }

    static int countSyllablesInWord(String word) {
        word = word.toLowerCase();
        int count = 0;
        boolean isPrevVowel = false;
        String vowels = "aeiouy";
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (vowels.indexOf(c) != -1) {
                if (!isPrevVowel) {
                    count++;
                    isPrevVowel = true;
                }
            } else {
                isPrevVowel = false;
            }
        }
        if (word.endsWith("e")) {
            count--;
        }
        if (count == 0) {
            count = 1;
        }
        return count;
    }
}