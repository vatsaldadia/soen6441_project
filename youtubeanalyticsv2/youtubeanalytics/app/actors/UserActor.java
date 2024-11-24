package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import messages.Messages;
import play.libs.Json;

public class UserActor extends AbstractActor {

	private final ActorRef out;
	private final ActorRef searchActor;

	public static Props props(ActorRef out, ActorRef searchActor) {
		return Props.create(UserActor.class, out, searchActor);
	}

	public UserActor(ActorRef out, ActorRef searchActor) {
		this.out = out;
		this.searchActor = searchActor;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(JsonNode.class, this::handleWebSocketMessage)
			.match(Messages.SearchUpdate.class, this::handleSearchUpdate)
			.build();
	}

	private void handleWebSocketMessage(JsonNode json) {
		if (
			json.has("action") && json.get("action").asText().equals("search")
		) {
			String query = json.get("query").asText();
			searchActor.tell(new Messages.SearchRequest(query), self());
		}
	}

	private void handleSearchUpdate(Messages.SearchUpdate update) {
		System.out.println(update.toJson());
		out.tell(update.toJson(), self());
	}
}
