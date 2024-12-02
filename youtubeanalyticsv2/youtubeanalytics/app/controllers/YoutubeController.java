package controllers;

import actors.HelperActor;
import actors.SearchActor;
import actors.SentimentAnalysisActor;
import actors.SupervisorActor;
import actors.UserActor;
import actors.*;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.stream.Materializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import play.cache.AsyncCacheApi;
import play.libs.streams.ActorFlow;
import play.libs.ws.WSClient;
import play.mvc.*;
import services.ReadabilityCalculator;

/**
 * This controller contains an action to handle HTTP requests to the application's home page.
 * @author Vatsal Dadia
 */
public class YoutubeController extends Controller {

	private final ActorSystem actorSystem;
	private final Materializer materializer;
	private final WSClient ws;
	private AsyncCacheApi cache;
	private final ActorRef readabilityCalculatorActor;
	private final ActorRef sentimentAnalysisActor;
	private final ActorRef supervisorActor;
	
	private final ActorRef wordStatsActor;
	//	private final ActorRef helperActor;

	private Map<String, ActorRef> searchActors;

	/**
	 * Constructor for YoutubeController.
	 *
	 * @param ws The WSClient for making HTTP requests.
	 * @param system The ActorSystem to create actors in.
	 * @param materializer The Materializer for stream handling.
	 * @author Vatsal Dadia
	 */
	@Inject
	public YoutubeController(
		WSClient ws,
		ActorSystem system,
		Materializer materializer
		//		AsyncCacheApi cache
	) {
		this.ws = ws;
		this.actorSystem = system;
		this.materializer = materializer;
		this.searchActors = new HashMap<>();
		//		this.cache = cache;
		this.readabilityCalculatorActor = system.actorOf(
			ReadabilityCalculator.props()
		);
		this.sentimentAnalysisActor = system.actorOf(
			SentimentAnalysisActor.props()
		);
		this.supervisorActor = system.actorOf(
            SupervisorActor.props(this.actorSystem, ws),
            "supervisorActor"
        );
		this.wordStatsActor = system.actorOf(
				WordStatsActor.props()
		);
		//		this.helperActor = system.actorOf(HelperActor.props(system, ws));
		//		system.actorOf(Props.create(TestActor.class));
	}

	/**
	 * An action that renders an HTML page with a welcome message.
	 * @author Vatsal Dadia
	 */
	public Result index() {
		return ok(views.html.search.render());
	}

	/**
	 * An action that retrieves word statistics for a given query.
	 *
	 * @param query The search query.
	 * @return A Result rendering the word statistics page.
	 * @author Rolwyn Raju
	 */
	public Result getWordStats(String query) {
		JsonNode wordStats = WordStatsActor.wordStatsMap.get(query);
		return ok(views.html.wordstats.render(wordStats, query));
	}

	/**
	 * Creates a WebSocket connection.
	 * @return A WebSocket connection.
	 * @author Vatsal Dadia
	 */
	public WebSocket ws() {
		return WebSocket.Json.accept(request ->
			ActorFlow.actorRef(
				wsout -> UserActor.props(wsout, this),
				this.actorSystem,
				this.materializer
			)
		);
	}

	/**
	 * Retrieves or creates a SearchActor for a given query.
	 *
	 * @param query The search query.
	 * @return The ActorRef for the SearchActor.
	 * @author Vatsal Dadia
	 */
	public ActorRef getSearchActor(String query) {
		if (!searchActors.containsKey(query)) {
			searchActors.put(
				query,
				this.actorSystem.actorOf(
						SearchActor.props(
							ws,
							query,
							cache,
							readabilityCalculatorActor,
							sentimentAnalysisActor, wordStatsActor
						)
					)
			);
		}
		return searchActors.get(query);
	}
}
