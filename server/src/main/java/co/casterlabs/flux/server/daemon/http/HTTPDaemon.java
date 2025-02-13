package co.casterlabs.flux.server.daemon.http;

import co.casterlabs.flux.server.util.RakuraiTaskExecutor;
import co.casterlabs.rhs.HttpServer;
import co.casterlabs.rhs.HttpServerBuilder;
import co.casterlabs.rhs.protocol.http.HttpProtocol;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol;

public class HTTPDaemon {

    public static void init() throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("FLUX_HTTP_PORT", "7080"));
        if (port <= 0) return;

        HttpServer server = new HttpServerBuilder()
            .withPort(port)
            .withServerHeader("Casterlabs/Flux")
            .withTaskExecutor(RakuraiTaskExecutor.INSTANCE)
            .with(new HttpProtocol(), new _HTTPHandler())
            .with(new WebsocketProtocol(), new _WSHandler())
            .build();

        server.start();

        System.out.println("HTTP Daemon started on port " + port);
    }

}
