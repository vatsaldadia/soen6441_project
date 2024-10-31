package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.libs.ws.*;
import play.mvc.*;

public class YoutubeController extends Controller {

    private final WSClient ws;
    private static final String YOUTUBE_API_KEY =
        "AIzaSyBn3hOC9y7PsDrQ62Xuj5M_P83ASq6GZRY";
    private static final String YOUTUBE_URL =
        "https://www.googleapis.com/youtube/v3/search";

    @Inject
    public YoutubeController(WSClient ws) {
        this.ws = ws;
    }

    public CompletionStage<Result> searchVideos(String query) {
        return ws
            .url(YOUTUBE_URL)
            .addQueryParameter("part", "snippet")
            .addQueryParameter("maxResults", "10")
            .addQueryParameter("q", query)
            .addQueryParameter("type", "video")
            .addQueryParameter("key", YOUTUBE_API_KEY)
            .get()
            .thenApply(response -> {
                if (response.getStatus() == 200) {
                    return ok(response.asJson());
                } else {
                    return internalServerError(
                        "YouTube API error: " + response.getBody()
                    );
                }
            });
    }
}
