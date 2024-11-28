package controllers;

import actors.HelperActor;
import actors.SearchActor;
import actors.SentimentAnalysisActor;
import actors.SupervisorActor;
import actors.UserActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.stream.Materializer;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import play.cache.AsyncCacheApi;
import play.libs.streams.ActorFlow;
import play.libs.ws.WSClient;
import play.mvc.*;
import services.ReadabilityCalculator;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class YoutubeController extends Controller {

	private final ActorSystem actorSystem;
	private final Materializer materializer;
	private final WSClient ws;
	private AsyncCacheApi cache;
	private final ActorRef readabilityCalculatorActor;
	private final ActorRef sentimentAnalysisActor;
	private final ActorRef supervisorActor;
	
	//	private final ActorRef helperActor;

	private Map<String, ActorRef> searchActors;

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
		//		this.helperActor = system.actorOf(HelperActor.props(system, ws));
		//		system.actorOf(Props.create(TestActor.class));
	}

	/**
	 * An action that renders an HTML page with a welcome message.
	 * The configuration in the <code>routes</code> file means that
	 * this method will be called when the application receives a
	 * <code>GET</code> request with a path of <code>/</code>.
	 */
	public Result index() {
		return ok(views.html.search.render());
	}

	public WebSocket ws() {
		return WebSocket.Json.accept(request ->
			ActorFlow.actorRef(
				wsout -> UserActor.props(wsout, this),
				this.actorSystem,
				this.materializer
			)
		);
	}

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
							sentimentAnalysisActor
						)
					)
			);
		}
		return searchActors.get(query);
	}
}
