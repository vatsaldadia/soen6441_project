package actors;

import actors.SearchActor.SearchResponse;
import actors.SentimentAnalysisActor;
import actors.WordStatsActor;
import akka.actor.AbstractActorWithTimers;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import messages.Messages.TerminateActor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import play.cache.AsyncCacheApi;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import scala.concurrent.duration.Duration;
import services.ReadabilityCalculator;

import static actors.WordStatsActor.wordStatsMap;

/**
 * Actor responsible for handling YouTube search queries and processing video data.
 * @author Mohnish Mirchandani, Vatsal Dadia
 */
public class SearchActor extends AbstractActorWithTimers {

	private final List<ActorRef> userActorList;
	private final Map<String, ObjectNode> videoNodes;
	private final WSClient ws;
	private final String query;
	private String searchSentiment;
	//	private AsyncCacheApi cache;
	private ActorRef readabilityCalculatorActor;
	private ActorRef sentimentAnalysisActor;
	private ActorRef wordStatsActor;
	private ActorRef channelProfileActor;
	private static final String YOUTUBE_API_KEY =
		"AIzaSyBIPw9IFVqkuCaXFx5usHyJHORujcIvex8";
	private static final String YOUTUBE_URL =
		"https://www.googleapis.com/youtube/v3";

	/**
	 * Constructor for SearchActor.
	 *
	 * @param ws WSClient for making HTTP requests.
	 * @param query The search query.
	 * @param cache AsyncCacheApi for caching responses.
	 * @param readabilityCalculatorActor ActorRef for readability calculations.
	 * @param sentimentAnalysisActor ActorRef for sentiment analysis.
	 * @param wordStatsActor ActorRef for word statistics.
	 * @author Mohnish Mirchandani, Vatsal Dadia
	 */
	public SearchActor(
		WSClient ws,
		String query,
		AsyncCacheApi cache,
		ActorRef readabilityCalculatorActor,
		ActorRef sentimentAnalysisActor,
		ActorRef wordStatsActor,
		ActorRef channelProfileActor
	) {
		this.ws = ws;
		this.query = query;
		//		this.cache = cache;
		this.userActorList = new ArrayList<>();
		this.videoNodes = new HashMap<>();
		this.readabilityCalculatorActor = readabilityCalculatorActor;
		this.sentimentAnalysisActor = sentimentAnalysisActor;
		this.wordStatsActor = wordStatsActor;
		this.searchSentiment = ":-|||||";
		this.channelProfileActor = channelProfileActor;
	}

	/**
	 * Creates Props for an actor of this type.
	 *
	 * @param ws WSClient for making HTTP requests.
	 * @param query The search query.
	 * @param cache AsyncCacheApi for caching responses.
	 * @param readabilityCalculatorActor ActorRef for readability calculations.
	 * @param sentimentAnalysisActor ActorRef for sentiment analysis.
	 * @param wordStatsActor ActorRef for word statistics.
	 * @return A Props for creating this actor.
	 * @author Mohnish Mirchandani, @author Vatsal Dadia
	 */
	public static Props props(
		WSClient ws,
		String query,
		AsyncCacheApi cache,
		ActorRef readabilityCalculatorActor,
		ActorRef sentimentAnalysisActor,
		ActorRef wordStatsActor,
		ActorRef channelProfileActor
	) {
		return Props.create(
			SearchActor.class,
			ws,
			query,
			cache,
			readabilityCalculatorActor,
			sentimentAnalysisActor,
			wordStatsActor,
				channelProfileActor
		);
	}

	//	public static SearchActor getInstance(String query) {
	//		if (!instances.containsKey(query)) {
	//			return null;
	//		}
	//		return instances.get(query);
	//	}

	@Override
	public void preStart() {
		getTimers()
			.startPeriodicTimer(
				"Timer",
				new Tick(this.query),
				Duration.create(1, TimeUnit.MINUTES)
			);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(Tick.class, message -> {
				if (message.getQuery().equals(this.query)) {
					handleSearch();
				}
			})
			.match(RegisterMsg.class, message -> {
				
				if (message.getQuery().equals(this.query)) {
					userActorList.add(getSender());
					handleSearch();
				}
			})
			//				.match(SearchResponse.class, message -> {
			//					userActorList.forEach(userActor -> {
			//							userActor.tell(message.response, getSelf());
			//						});
			//				})
			.match(ReadabilityCalculator.ReadabilityResults.class, message -> {
				ObjectNode videoNode = videoNodes.get(message.videoId);
				videoNode.put(
					"fleschKincaidGradeLevel",
					String.format("%.2f", message.gradeLevel)
				);
				videoNode.put(
					"fleschReadingScore",
					String.format("%.2f", message.readingScore)
				);
			})
			.match(
				SentimentAnalysisActor.SentimentAnalysisResults.class,
				message -> {
					this.searchSentiment = message.sentiment;
				}
			)
			 .match(TerminateActor.class, message -> {
                    System.out.println("Terminating SearchActor");
                    getContext().stop(getSelf());
                })
				.match(WordStatsActor.WordStatsResults.class, message -> {
					JsonNode wordStats = message.wordStats;
					wordStatsMap.put(message.videoId, wordStats);
				})
				.match(ChannelProfileRequest.class, message -> {
					channelProfileActor.tell(message.channelId, getSender());
				})
			.build();
	}

	//	public class Video {
	//
	//	}

	/**
	 * Message class for periodic timer ticks.
	 */
	public static final class Tick {

		private final String query;

		/**
		 * Constructor for Tick.
		 *
		 * @param query The search query.
		 */
		public Tick(String query) {
			this.query = query;
		}

		/**
		 * Gets the search query.
		 *
		 * @return The search query.
		 */
		public String getQuery() {
			return query;
		}
	}

	/**
	 * Message class for registering a user actor.
	 */
	public static final class RegisterMsg {

		private final String query;

		/**
		 * Constructor for RegisterMsg.
		 *
		 * @param query The search query.
		 */
		public RegisterMsg(String query) {
			this.query = query;
		}

		/**
		 * Gets the search query.
		 *
		 * @return The search query.
		 */
		public String getQuery() {
			return query;
		}
	}

	public static class SearchResponse {

		final String query;
		final ObjectNode response;

		/**
		 * Message class for search responses.
		 */
		public SearchResponse(String query, ObjectNode response) {
			this.query = query;
			this.response = response;
		}
	}

	public static final class ChannelProfileRequest {
		public final String channelId;

		public ChannelProfileRequest(String channelId) {
			this.channelId = channelId;
		}
	}
	
	/**
	 * Handles the search operation by making a request to the YouTube API and processing the results.
	 * @author Vatsal Dadia, Mohnish Mirchandani
	 */
	private void handleSearch() {
		if (!userActorList.isEmpty()) {
			ws
				.url(YOUTUBE_URL + "/search")
				.addQueryParameter("part", "snippet")
				.addQueryParameter("maxResults", "50")
				.addQueryParameter("q", query)
				.addQueryParameter("type", "video")
				.addQueryParameter("order", "date")
				.addQueryParameter("key", YOUTUBE_API_KEY)
				.get()
				.thenCompose(youtubeResponse -> {
					JsonNode rawData = youtubeResponse.asJson();
					System.out.println("Line0: API Response");
					JsonNode items = rawData.get("items");
					List<String> allDescriptions = new ArrayList<>();
					ObjectNode modifiedResponse = rawData.deepCopy();
					ArrayNode modifiedItems =
						JsonNodeFactory.instance.arrayNode();
					List<CompletableFuture<ObjectNode>> futures =
						new ArrayList<>();

					System.out.println("Line1. Get all videos and their descriptions");
					System.out.println(items);
					// 1. Get all videos and their descriptions
					for (JsonNode item : items) {
						ObjectNode videoNode = (ObjectNode) item;
						String videoId = videoNode
							.get("id")
							.get("videoId")
							.asText();
						videoNodes.put(videoId, videoNode);

						String channelId = videoNode.get("snippet").get("channelId").asText();
						videoNode.put("channelId", channelId);
						System.out.println("Captured Channel ID: " + channelId);

						CompletionStage<ObjectNode> future = getVideo(
							videoId
						).thenCompose(response -> {
							String description = response
							.asJson()
							.get("items")
							.get(0)
							.get("snippet")
							.get("description")
							.asText();
							// 2. For each video, ask readability actor
							return Patterns.ask(
								readabilityCalculatorActor,
								new ReadabilityCalculator.initReadabilityCalculatorService(
									videoId,
									description
								),
								java.time.Duration.ofSeconds(5)
							).thenApply(readabilityResult -> {
								System.out.println("readibility calculator");
								ReadabilityCalculator.ReadabilityResults results =
									(ReadabilityCalculator.ReadabilityResults) readabilityResult;
								System.out.println(results);
								videoNode.put("description", description);
								videoNode.put(
									"fleschKincaidGradeLevel",
									String.format("%.2f", results.gradeLevel)
								);
								videoNode.put(
									"fleschReadingScore",
									String.format("%.2f", results.readingScore)
								);
								videoNode.put("channelId", channelId);
								return videoNode;
							});
						});
						futures.add(future.toCompletableFuture());
					}
					System.out.println("Line3. After all videos are processed with readability");
					// 3. After all videos are processed with readability
					return CompletableFuture.allOf(
						futures.toArray(new CompletableFuture[0])
					).thenCompose(v -> {
						List<Double> grades = new ArrayList<>();
						List<Double> scores = new ArrayList<>();
						List<String> descriptions = new ArrayList<>();
						System.out.println("Line3.1");

						futures
							.stream()
							.map(CompletionStage::toCompletableFuture)
							.map(future -> future.getNow(null))
							.forEach(videoNode -> {
								double grade = Double.parseDouble(
									videoNode
										.get("fleschKincaidGradeLevel")
										.asText()
								);
								double score = Double.parseDouble(
									videoNode.get("fleschReadingScore").asText()
								);
								grades.add(grade);
								scores.add(score);
								descriptions.add(
									videoNode.get("description").asText()
								);
								modifiedItems.add(videoNode);
							});
						System.out.println("Line3.2");

						double gradeAvg = grades
							.stream()
							.mapToDouble(Double::doubleValue)
							.average()
							.orElse(0.0);
						double scoreAvg = scores
							.stream()
							.mapToDouble(Double::doubleValue)
							.average()
							.orElse(0.0);

						modifiedResponse.put(
							"fleschKincaidGradeLevelAvg",
							String.format("%.2f", gradeAvg)
						);
						modifiedResponse.put(
							"fleschReadingScoreAvg",
							String.format("%.2f", scoreAvg)
						);
						System.out.println("Line3.3");

						ArrayNode tempModifiedItems =
						JsonNodeFactory.instance.arrayNode();

						StreamSupport.stream(modifiedItems.spliterator(), false)
							.limit(10)
							.forEach(val -> {
								tempModifiedItems.add(val);
							});
						modifiedResponse.set("items", tempModifiedItems);
						modifiedResponse.put("query", query);

						System.out.println("Line4. Ask sentiment analysis actor");
						// Send the descriptions to the WordStatsService
						wordStatsActor.tell(new WordStatsActor.InitWordStatsService(query, descriptions), getSelf());

						// 4. Ask sentiment analysis actor
						return Patterns.ask(
							sentimentAnalysisActor,
							new SentimentAnalysisActor.initSentimentAnalyzerService(
								query,
								descriptions
							),
							java.time.Duration.ofSeconds(5)
						).thenApply(sentimentResult -> {
							SentimentAnalysisActor.SentimentAnalysisResults results =
								(SentimentAnalysisActor.SentimentAnalysisResults) sentimentResult;

							modifiedResponse.put(
								"sentiment",
								results.sentiment
							);

							System.out.println("Line5. Finally, send to user actors");
							// 5. Finally, send to user actors
							userActorList.forEach(userActor -> {
								userActor.tell(
									new SearchResponse(query, modifiedResponse),
									getSelf()
								);
							});

							return modifiedResponse;
						});
					});
				});
		}
		
	}

	/**
	 * Makes a request to the YouTube API to get video details.
	 *
	 * @param video_id The ID of the video.
	 * @return A CompletionStage containing the WSResponse.
	 * @author Vatsal Dadia
	 */
	public CompletionStage<WSResponse> getVideo(String video_id) {
		//		return cache.getOrElseUpdate(
		//				video_id,
		//				() -> {

		System.out.println("Requesting");
		return ws
			.url(YOUTUBE_URL + "/videos")
			.addQueryParameter("part", "snippet")
			.addQueryParameter("id", video_id)
			.addQueryParameter("key", YOUTUBE_API_KEY)
			.get();
		//				}
		//				3600
		//		);
	}

	// private JsonNode processSearchResults(JsonNode rawData) {
	// 	ArrayNode items = (ArrayNode) rawData.get("items");
	// 	ArrayNode processedItems = Json.newArray();

	// 	for (int i = 0; i < items.size(); i++) {
	// 		JsonNode item = items.get(i);
	// 		ObjectNode processedItem = (ObjectNode) item.deepCopy();
	// 		// Add index to each item
	// 		processedItem.put("index", i + 1);
	// 		processedItems.add(processedItem);
	// 	}

	// 	ObjectNode result = Json.newObject();
	// 	result.set("items", processedItems);
	// 	return result;
	// }
}
