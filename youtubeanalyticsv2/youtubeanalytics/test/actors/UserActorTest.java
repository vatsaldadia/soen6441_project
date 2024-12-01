package actors;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.YoutubeController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import play.libs.Json;

/**
 * Unit tests for the UserActor class.
 * @author Mohnish Mirchandani
 */
public class UserActorTest {

	private static ActorSystem system;

	@Mock
	private YoutubeController youtubeController;

	/**
	 * Sets up the ActorSystem and initializes mocks before each test.
	 */
	@Before
	public void setup() {
		system = ActorSystem.create();
		MockitoAnnotations.openMocks(this); // Initialize mocks
	}

	/**
	 * Shuts down the ActorSystem after each test.
	 */
	@After
	public void teardown() {
		TestKit.shutdownActorSystem(system);
		system = null;
	}

	/**
	 * Tests the search flow of the UserActor.
	 */
	@Test
	public void testSearchFlow() {
		new TestKit(system) {
			{
				// Create a probe to act as the WebSocket out actor
				TestKit wsOutProbe = new TestKit(system);

				// Create a probe to act as the search actor
				TestKit searchActorProbe = new TestKit(system);

				// Mock the YoutubeController to return our search actor probe
				when(youtubeController.getSearchActor(anyString())).thenReturn(
					searchActorProbe.getRef()
				);

				// Create the actor under test
				ActorRef userActor = system.actorOf(
					UserActor.props(wsOutProbe.getRef(), youtubeController)
				);

				// Create and send a search message
				ObjectNode searchMessage = Json.newObject();
				searchMessage.put("action", "search");
				searchMessage.put("query", "test query");
				userActor.tell(searchMessage, getRef());

				// Verify that search actor received registration message
				SearchActor.RegisterMsg registerMsg =
					searchActorProbe.expectMsgClass(
						SearchActor.RegisterMsg.class
					);
				assert (registerMsg.getQuery().equals("test query"));

				// Create and send a mock search response
				ObjectNode mockSearchResponse = Json.newObject();
				mockSearchResponse.put("someKey", "someValue");
				userActor.tell(
					new SearchActor.SearchResponse(
						"test query",
						mockSearchResponse
					),
					getRef()
				);

				// Verify that WebSocket out actor received the response
				ObjectNode response = wsOutProbe.expectMsgClass(
					ObjectNode.class
				);
				assert (response.has("responses"));
				assert (response.get("responses").isArray());
				assert (response
						.get("responses")
						.get(0)
						.get("someKey")
						.asText()
						.equals("someValue"));
			}
		};
	}

	/**
	 * Tests the UserActor's handling of multiple search queries.
	 */
	@Test
	public void testMultipleSearches() {
		new TestKit(system) {
			{
				TestKit wsOutProbe = new TestKit(system);
				TestKit searchActorProbe = new TestKit(system);

				when(youtubeController.getSearchActor(anyString())).thenReturn(
					searchActorProbe.getRef()
				);

				ActorRef userActor = system.actorOf(
					UserActor.props(wsOutProbe.getRef(), youtubeController)
				);

				// Send multiple searches
				String[] queries = { "query1", "query2", "query3", "query1" };
				for (String query : queries) {
					ObjectNode searchMessage = Json.newObject();
					searchMessage.put("action", "search");
					searchMessage.put("query", query);
					userActor.tell(searchMessage, getRef());

					// Verify registration message
					SearchActor.RegisterMsg registerMsg =
						searchActorProbe.expectMsgClass(
							SearchActor.RegisterMsg.class
						);
					assert (registerMsg.getQuery().equals(query));

					// Send back a response
					ObjectNode mockResponse = Json.newObject();
					mockResponse.put("query", query);
					userActor.tell(
						new SearchActor.SearchResponse(query, mockResponse),
						getRef()
					);

					// Verify WebSocket message
					ObjectNode wsResponse = wsOutProbe.expectMsgClass(
						ObjectNode.class
					);
					assert (wsResponse.has("responses"));
				}
				// // Verify that last response contains all searches (up to 5)
				// // ObjectNode finalResponse = wsOutProbe.lastMessage();
				// assert (finalResponse.get("responses").size() == 3);
			}
		};
	}

	/**
	 * Tests the UserActor's handling of invalid messages.
	 */
	@Test
	public void testInvalidMessage() {
		new TestKit(system) {
			{
				TestKit wsOutProbe = new TestKit(system);

				ActorRef userActor = system.actorOf(
					UserActor.props(wsOutProbe.getRef(), youtubeController)
				);

				// Send invalid message
				ObjectNode invalidMessage = Json.newObject();
				invalidMessage.put("action", "invalid");
				userActor.tell(invalidMessage, getRef());

				// Verify no response was sent to WebSocket
				wsOutProbe.expectNoMessage();
			}
		};
	}
}
