package co.casterlabs.flux.server.daemon.tcp;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import co.casterlabs.commons.io.bytes.EndOfStreamException;
import co.casterlabs.flux.server.Flux;

public class TcpDaemon {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(60);

    public static void init() throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("FLUX_TCP_PORT", "7083"));
        if (port <= 0) return;

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(port));
            System.out.println("TCP Daemon started on port " + port);

            while (true) {
                Socket sock = serverSocket.accept();

                Thread.ofPlatform()
                    .name("TCP Read Thread - " + sock.getRemoteSocketAddress())
                    .start(() -> {
                        try (sock) {
                            sock.setTcpNoDelay(true);
                            sock.setSoTimeout((int) TIMEOUT);

                            try (_TcpConnection conn = new _TcpConnection(new _SocketConnection(sock))) {
                                conn.run();
                            }
                        } catch (EndOfStreamException ignored) {
                            return;
                        } catch (Throwable t) {
                            if (Flux.DEBUG) {
                                t.printStackTrace();
                            }
                        }
                    }).start();
            }
        }
    }

}
