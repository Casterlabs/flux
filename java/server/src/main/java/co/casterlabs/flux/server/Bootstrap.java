package co.casterlabs.flux.server;

import co.casterlabs.flux.server.daemon.healthcheck.HealthcheckDaemon;
import co.casterlabs.flux.server.daemon.http.HTTPDaemon;
import co.casterlabs.flux.server.daemon.stats.StatsDaemon;
import co.casterlabs.flux.server.daemon.tcp.TcpDaemon;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;

public class Bootstrap {

    public static void main(String[] args) throws Exception {
        FastLoggingFramework.setColorEnabled(false);
        HTTPDaemon.init();
        StatsDaemon.init();
        HealthcheckDaemon.init();
        TcpDaemon.init();
    }

}
