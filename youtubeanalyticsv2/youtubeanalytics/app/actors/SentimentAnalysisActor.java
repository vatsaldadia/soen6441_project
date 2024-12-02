package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import messages.Messages.TerminateActor;

import java.util.List;
import services.SentimentAnalyzer;


/**
 * Actor for SentimentAnalyser
 * @author Mohnish Mirchandani
 */
public class SentimentAnalysisActor extends AbstractActor {


	/**
	 * Props for SentimentAnalysisActor
	 * @return Props
	 * @author Mohnish Mirchandani
	 */
	public static Props props() {
		return Props.create(SentimentAnalysisActor.class);
	}

	/**
	 * Create receive for SentimentAnalysisActor
	 * @return Receive
	 * @author Mohnish Mirchandani
	 */

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(initSentimentAnalyzerService.class, message -> {
				try{

				String sentiment = SentimentAnalyzer.analyzeSentiment(
					message.descriptions
					);
					// System.out.println(sentiment);
					getSender()
					.tell(
						new SentimentAnalysisResults(message.query, sentiment),
						getSelf()
						);
					} catch (Exception e){
						throw new RuntimeException("Sentiment Analysis Failed");
					}
			})
			.match(TerminateActor.class, message -> {
                    System.out.println("Terminating SearchActor");
                    getContext().stop(getSelf());
                })
			.build();
	}

	/**
	 * Class for initializing SentimentAnalyzerService
	 * @author Mohnish Mirchandani
	 */

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

	/**
	 * Class for SentimentAnalysisResults
	 * @author Mohnish Mirchandani
	 */

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
