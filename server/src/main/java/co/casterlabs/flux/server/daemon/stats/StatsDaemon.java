package co.casterlabs.flux.server.daemon.stats;

import java.util.HashMap;
import java.util.Map;

import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.ProfilerAverage;
import co.casterlabs.flux.server.util.RakuraiTaskExecutor;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rhs.HttpServer;
import co.casterlabs.rhs.HttpServerBuilder;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.exceptions.DropConnectionException;
import co.casterlabs.rhs.protocol.exceptions.HttpException;
import co.casterlabs.rhs.protocol.http.HttpProtocol;
import co.casterlabs.rhs.protocol.http.HttpProtocol.HttpProtoHandler;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;

public class StatsDaemon implements HttpProtoHandler {

    public static void init() throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("FLUX_STATS_PORT", "7081"));
        if (port <= 0) return;

        HttpServer server = new HttpServerBuilder()
            .withPort(port)
            .withServerHeader("Casterlabs/Flux")
            .withTaskExecutor(RakuraiTaskExecutor.INSTANCE)
            .with(new HttpProtocol(), new StatsDaemon())
            .build();

        server.start();

        System.out.println("Stats HTTP Daemon started on port " + port);
    }

    @Override
    public HttpResponse handle(HttpSession session) throws HttpException, DropConnectionException {
        switch (session.method()) {
            case HEAD:
            case GET: {
                Map<String, ProfilerAverage> profiles = new HashMap<>();
                for (Profiler profiler : Profiler.values()) {
                    profiles.put(profiler.name(), profiler.average());
                }

                return HttpResponse.newFixedLengthResponse(
                    StandardHttpStatus.OK,
                    new JsonObject()
                        .put("profiles", Rson.DEFAULT.toJson(profiles))
                        .put("expirationInterval", Profiler.EXPIRE_AFTER)
                        .toString(true)
                )
                    .mime("application/json; charset=utf-8");
            }

            default:
                return HttpResponse.newFixedLengthResponse(StandardHttpStatus.METHOD_NOT_ALLOWED, "Please use GET")
                    .header("Allow", "GET, HEAD");
        }
    }

}
