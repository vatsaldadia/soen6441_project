package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import java.util.List;
import services.SentimentAnalyzer;



public class SentimentAnalysisActor extends AbstractActor {

	public static Props props() {
		return Props.create(SentimentAnalysisActor.class);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(initSentimentAnalyzerService.class, message -> {
				String sentiment = SentimentAnalyzer.analyzeSentiment(
					message.descriptions
				);
				System.out.println(sentiment);
				getSender()
					.tell(
						new SentimentAnalysisResults(message.query, sentiment),
						getSelf()
					);
			})
			.build();
	}

	public static class initSentimentAnalyzerService {

		public String query;
		public List<String> descriptions;

		public initSentimentAnalyzerService(
			String query,
			List<String> descriptions
		) {
			this.query = query;
			this.descriptions = descriptions;
		}
	}

	public static class SentimentAnalysisResults {

		public final String searchQuery;
		// public final String videoId;
		// public final double sentimentAnalysisLevel;
		public final String sentiment;

		public SentimentAnalysisResults(String searchQuery, String sentiment) {
			this.searchQuery = searchQuery;
			// this.videoId = videoId;
			// this.sentimentAnalysisLevel = sentimentAnalysisLevel;
			this.sentiment = sentiment;
		}
	}
}
