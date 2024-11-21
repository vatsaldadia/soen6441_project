package controllers;

import actors.SearchActor;
import actors.UserActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import com.google.inject.Inject;
import play.libs.streams.ActorFlow;
import play.libs.ws.WSClient;
import play.mvc.*;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class YoutubeController extends Controller {

	private final ActorSystem actorSystem;
	private final Materializer materializer;
	private final WSClient ws;
	private final ActorRef searchActor;

	@Inject
	public YoutubeController(
		WSClient ws,
		ActorSystem system,
		Materializer materializer
	) {
		this.ws = ws;
		this.actorSystem = system;
		this.materializer = materializer;
		this.searchActor = system.actorOf(SearchActor.props(ws));
	}

	/**
	 * An action that renders an HTML page with a welcome message.
	 * The configuration in the <code>routes</code> file means that
	 * this method will be called when the application receives a
	 * <code>GET</code> request with a path of <code>/</code>.
	 */
	public Result index() {
		return ok(views.html.index.render());
	}

	public WebSocket ws() {
		return WebSocket.Json.accept(request ->
			ActorFlow.actorRef(
				out -> UserActor.props(out, searchActor),
				this.actorSystem,
				this.materializer
			)
		);
	}
}
