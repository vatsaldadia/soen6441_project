package messages;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

public class Messages {

	// Search Messages
	public static class SearchRequest {

		private final String query;

		public SearchRequest(String query) {
			this.query = query;
		}

		public String getQuery() {
			return query;
		}
	}

	public static class SearchUpdate {

		private final String status;
		private final JsonNode data;

		public SearchUpdate(String status, JsonNode data) {
			this.status = status;
			this.data = data;
		}

		public JsonNode toJson() {
			ObjectNode result = Json.newObject();
			result.put("status", status);
			result.set("data", data);
			return result;
		}
	}
}
