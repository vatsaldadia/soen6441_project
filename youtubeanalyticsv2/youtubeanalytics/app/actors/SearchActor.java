package actors;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.cache.AsyncCacheApi;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import scala.concurrent.duration.Duration;
import services.ReadabilityCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SearchActor extends AbstractActorWithTimers {

	private final List<ActorRef> userActorList;
	private final Map<String, ObjectNode> videoNodes;
	private final WSClient ws;
	private final String query;
//	private AsyncCacheApi cache;
	private ActorRef readabilityCalculatorActor;
	private static final String YOUTUBE_API_KEY =
		"AIzaSyBnujY6PQi1cXVkf02_epIdIVKT-ywT2vc";
	private static final String YOUTUBE_URL =
		"https://www.googleapis.com/youtube/v3";

	public SearchActor(WSClient ws, String query, AsyncCacheApi cache, ActorRef readabilityCalculatorActor) {
		this.ws = ws;
		this.query = query;
//		this.cache = cache;
		this.userActorList = new ArrayList<>();
		this.videoNodes = new HashMap<>();
		this.readabilityCalculatorActor = readabilityCalculatorActor;
	}

	public static Props props(WSClient ws, String query, AsyncCacheApi cache, ActorRef readabilityCalculatorActor) {
		return Props.create(SearchActor.class, ws, query, cache, readabilityCalculatorActor);
	}

//	public static SearchActor getInstance(String query) {
//		if (!instances.containsKey(query)) {
//			return null;
//		}
//		return instances.get(query);
//	}

	@Override
	public void preStart() {
		getTimers().startPeriodicTimer("Timer", new Tick(this.query), Duration.create(60, TimeUnit.SECONDS));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
//				.match(Tick.class, message -> {
//					if (message.getQuery().equals(this.query)) {
//						handleSearch();
//					}
//				})
				.match(RegisterMsg.class, message -> {
					if (message.getQuery().equals(this.query)) {
						userActorList.add(getSender());
						handleSearch();
					}
				})
				.match(ReadabilityCalculator.ReadabilityResults.class, message -> {
					ObjectNode videoNode = videoNodes.get(message.videoId);
					videoNode.put("fleschKincaidGradeLevel", String.format("%.2f", message.gradeLevel));
					videoNode.put("fleschReadingScore", String.format("%.2f", message.readingScore));
				})
				.build();
	}

//	public class Video {
//
//	}

	public final class Tick {
		private final String query;

		public Tick(String query) {
			this.query = query;
		}

		public String getQuery() {
			return query;
		}
	}

	public static final class RegisterMsg {
		private final String query;

		public RegisterMsg(String query) {
			this.query = query;
		}

		public String getQuery() {
			return query;
		}
	}

	public static class SearchResponse{
		final String query;
		final ObjectNode response;

		public SearchResponse(String query, ObjectNode response){
			this.query = query;
			this.response = response;
		}
	}

	private void handleSearch() {
		if (!userActorList.isEmpty()) {
			ws.url(YOUTUBE_URL + "/search")
				.addQueryParameter("part", "snippet")
				.addQueryParameter("maxResults", "1")
				.addQueryParameter("q", query)
				.addQueryParameter("type", "video")
				.addQueryParameter("order", "date")
				.addQueryParameter("key", YOUTUBE_API_KEY)
				.get()
				.thenCompose(youtubeResponse -> {
					JsonNode rawData = youtubeResponse.asJson();
//					System.out.println("API Response: " + self());
//					System.out.println("rawData: " + rawData);
					JsonNode items = rawData.get("items");
//					System.out.println(items);
					ObjectNode modifiedResponse = rawData.deepCopy();
					ArrayNode modifiedItems = JsonNodeFactory.instance.arrayNode();
					List<CompletableFuture<ObjectNode>> futures = new ArrayList<>();
					for (JsonNode item : items) {
//						System.out.println("item: " + item);
						ObjectNode videoNode = (ObjectNode) item;
						String videoId = videoNode.get("id").get("videoId").asText();
						videoNodes.put(videoId, videoNode);
						CompletionStage<ObjectNode> future = getVideo(videoId).thenApply(response -> {
							String description = response.asJson().get("items").get(0)
									.get("snippet").get("description").asText();

							readabilityCalculatorActor.tell(new ReadabilityCalculator
									.initReadabilityCalculatorService(videoId, description), getSelf());

							videoNode.put("description", description);
							return videoNode;
						});
						futures.add(future.toCompletableFuture());
					}
//					System.out.println("futures: " + futures);

					CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> {
						List<Double> grades = new ArrayList<>();
						List<Double> scores = new ArrayList<>();
						futures.stream()
								.map(CompletionStage::toCompletableFuture)
								.map(future -> future.getNow(null))
								.limit(10)
								.forEach(videoNode -> {
									System.out.println("videoNode: " + videoNode);
									double grade = Double.parseDouble(videoNode.get("fleschKincaidGradeLevel").asText());
									double score = Double.parseDouble(videoNode.get("fleschReadingScore").asText());
									grades.add(grade);
									scores.add(score);
									modifiedItems.add(videoNode);
								});
//						System.out.println("modifiedItems: " + modifiedItems);

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

						modifiedResponse.put("fleschKincaidGradeLevelAvg", String.format("%.2f", gradeAvg));
						modifiedResponse.put("fleschReadingScoreAvg", String.format("%.2f", scoreAvg));
						modifiedResponse.set("items", modifiedItems);
						modifiedResponse.put("query", query);

//						System.out.println("modifiedResponse: " + modifiedResponse);

						userActorList.forEach(userActor -> {
							userActor.tell(new SearchResponse(query, modifiedResponse), getSelf());
						});
//						self().tell(new SearchResponse(query, modifiedResponse), getSelf());
//						System.out.println("message sent");

						return null;
					});
                    return null;
                });
		}
	}

	public CompletionStage<WSResponse> getVideo(String video_id) {
//		return cache.getOrElseUpdate(
//				video_id,
//				() -> {
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

	private JsonNode processSearchResults(JsonNode rawData) {
		ArrayNode items = (ArrayNode) rawData.get("items");
		ArrayNode processedItems = Json.newArray();

		for (int i = 0; i < items.size(); i++) {
			JsonNode item = items.get(i);
			ObjectNode processedItem = (ObjectNode) item.deepCopy();
			// Add index to each item
			processedItem.put("index", i + 1);
			processedItems.add(processedItem);
		}

		ObjectNode result = Json.newObject();
		result.set("items", processedItems);
		return result;
	}
}
