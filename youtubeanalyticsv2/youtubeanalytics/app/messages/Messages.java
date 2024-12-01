package messages;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;


/**
 * Message passing class
 * @author Mohnish Mirchandani
 */
public class Messages {

	/**
	 * Class for terminating actors
	 * @author Mohnish Mirchandani
	 */
	public static class TerminateActor {
    }
    
}
