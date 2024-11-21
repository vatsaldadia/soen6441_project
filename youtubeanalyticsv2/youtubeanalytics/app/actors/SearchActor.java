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
import play.libs.ws.WSClient;

public class SearchActor extends AbstractActor {

	private final WSClient ws;
	private static final String YOUTUBE_API_KEY = "YOUR_API_KEY";
	private static final String YOUTUBE_URL =
		"https://www.googleapis.com/youtube/v3";

	public SearchActor(WSClient ws) {
		this.ws = ws;
	}

	public static Props props(WSClient ws) {
		return Props.create(SearchActor.class, ws);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(Messages.SearchRequest.class, this::handleSearch)
			.build();
	}

	private void handleSearch(Messages.SearchRequest search) {
		// Notify that search is starting
		getSender()
			.tell(
				new Messages.SearchUpdate(
					"searching",
					Json.newObject().put("message", "Starting search...")
				),
				getSelf()
			);

		ws
			.url(YOUTUBE_URL + "/search")
			.addQueryParameter("part", "snippet")
			.addQueryParameter("maxResults", "10")
			.addQueryParameter("q", search.getQuery())
			.addQueryParameter("type", "video")
			.addQueryParameter("key", YOUTUBE_API_KEY)
			.get()
			.thenAccept(response -> {
				if (response.getStatus() == 200) {
					JsonNode rawData = response.asJson();
					JsonNode processedData = processSearchResults(rawData);
					getSender()
						.tell(
							new Messages.SearchUpdate(
								"completed",
								processedData
							),
							getSelf()
						);
				} else {
					getSender()
						.tell(
							new Messages.SearchUpdate(
								"error",
								Json.newObject().put("error", "Search failed")
							),
							getSelf()
						);
				}
			});
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
