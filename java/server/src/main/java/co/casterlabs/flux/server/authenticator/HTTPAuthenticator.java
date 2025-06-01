package co.casterlabs.flux.server.authenticator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import co.casterlabs.flux.packets.types.UserID;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;
import co.casterlabs.flux.server.util.RsonBodyHandler;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;

class HTTPAuthenticator implements Authenticator {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Duration TIMEOUT = Duration.ofMinutes(1);

    private final URI uri;

    HTTPAuthenticator(URI uri) {
        this.uri = uri;
    }

    @Override
    public Authentication authenticate(String token) throws AuthenticationException {
        Profile profile = Profiler.AUTHENTICATION.start();
        try {
            HttpResponse<JsonObject> response = CLIENT.send(
                HttpRequest.newBuilder()
                    .uri(this.uri)
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + token)
                    .build(),
                RsonBodyHandler.of(JsonObject.class)
            );

            if (response.statusCode() != 200) {
                throw new AuthenticationException("Invalid token: " + response.statusCode());
            }

            JsonObject body = response.body();

            UserID id = body.containsKey("id") ? new UserID(body.getString("id")) : UserID.random();
            String[] sendRegexes = Rson.DEFAULT.fromJson(body.get("send"), String[].class);
            String[] recvRegexes = Rson.DEFAULT.fromJson(body.get("recv"), String[].class);

            return new Authentication(id, sendRegexes, recvRegexes);
        } catch (IOException | InterruptedException e) {
            throw new AuthenticationException("Invalid token: " + e.getMessage());
        } finally {
            profile.end();
        }
    }

}
