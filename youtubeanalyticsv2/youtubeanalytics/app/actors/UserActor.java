package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.YoutubeController;
import play.libs.Json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserActor handles WebSocket messages and manages search history and responses.
 * @author Vatsal Dadia
 */
public class UserActor extends AbstractActor {

	private final ActorRef wsout;
//	private final WSClient ws;
//	private final ActorRef helperActor;
	private final YoutubeController youtubeController;
	private final List<String> searchHistory;
	private Map<String, ObjectNode> searchResponses;

	/**
	 * Creates Props for an actor of this type.
	 *
	 * @param wsout The WebSocket output actor.
	 * @param youtubeController The YouTube controller.
	 * @return A Props for creating this actor.
	 */
	public static Props props(ActorRef wsout, YoutubeController youtubeController) {
		return Props.create(UserActor.class, wsout, youtubeController);
	}

	/**
	 * Constructor for UserActor.
	 *
	 * @param wsout The WebSocket output actor.
	 * @param youtubeController The YouTube controller.
	 */
	public UserActor(ActorRef wsout, YoutubeController youtubeController) {
		this.wsout = wsout;
//		this.ws = ws;
		this.youtubeController = youtubeController;
        this.searchResponses = new HashMap<>();
        this.searchHistory = new ArrayList<>();
	}

	// public static class ServerReasponse {
	// 	public JsonNode data;
	// 	public ServerReasponse(JsonNode data) {
	// 		this.data = data;
	// 	}
	// }

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(JsonNode.class, this::handleWebSocketMessage)
//			.match(Messages.SearchUpdate.class, this::handleSearchUpdate)
			.match(SearchActor.SearchResponse.class, message -> {
//				System.out.println("UserActor got search response");
				searchResponses.put(message.query, message.response);
				handleSearchUpdate();
			})
			.build();
	}

	/**
	 * Handles incoming WebSocket messages.
	 *
	 * @param json The incoming JSON message.
	 */
	private void handleWebSocketMessage(JsonNode json) {
		if (json.get("action").asText().equals("search")) {
			String query = json.get("query").asText();
			if (searchHistory.contains(query)) {
				searchHistory.remove(query);
			}
			searchHistory.add(0, query);
			ActorRef searchActor = youtubeController.getSearchActor(query);
			searchActor.tell(new SearchActor.RegisterMsg(query), getSelf());
		}
	}

	/**
	 * Handles search updates by sending the latest search responses to the WebSocket output.
	 */
	private void handleSearchUpdate() {
		ArrayNode responses = JsonNodeFactory.instance.arrayNode();
		searchHistory.stream()
				.limit(5)
				.forEach(query -> {
					responses.add(searchResponses.get(query));
				});
		ObjectNode result = Json.newObject();
		result.set("responses", responses);
		wsout.tell(result, self());
	}
}
