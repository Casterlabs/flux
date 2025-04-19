package co.casterlabs.flux.server.daemon.healthcheck;

import co.casterlabs.flux.server.util.RakuraiTaskExecutor;
import co.casterlabs.rhs.HttpServer;
import co.casterlabs.rhs.HttpServerBuilder;
import co.casterlabs.rhs.protocol.http.HttpProtocol;

public class HealthcheckDaemon {

    public static void init() throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("FLUX_HEALTHCHECK_PORT", "7082"));
        if (port <= 0) return;

        HttpServer server = new HttpServerBuilder()
            .withPort(port)
            .withServerHeader("Casterlabs/Flux")
            .withTaskExecutor(RakuraiTaskExecutor.INSTANCE)
            .with(new HttpProtocol(), new _HealthcheckHandler())
            .build();

        server.start();

        System.out.println("Healthcheck Daemon started on port " + port);
    }

}
