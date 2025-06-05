package co.casterlabs.flux.client.realtime.tcp;

import java.io.IOException;

import javax.net.SocketFactory;

import co.casterlabs.flux.client.realtime.FluxRealtimeClient;
import co.casterlabs.flux.client.realtime.FluxRealtimeClientListener;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import lombok.experimental.Accessors;
import lombok.experimental.NonFinal;

@With
@Value
@NonNull
@NoArgsConstructor
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FluxTcpClientBuilder {
    private @NonFinal String hostAddress;
    private @NonFinal int hostPort = -1;
    private @NonFinal String token;

    private @NonFinal SocketFactory socketFactory = SocketFactory.getDefault();
    private @NonFinal FluxRealtimeClientListener realtimeListener;

    public FluxRealtimeClient build() throws IOException {
        if (this.realtimeListener == null) {
            throw new IllegalArgumentException("You must specify a realtime listener");
        }
        if (this.hostAddress == null) {
            throw new IllegalArgumentException("You must specify an address");
        }
        if (this.hostPort == -1) {
            throw new IllegalArgumentException("You must specify a port");
        }
        if (this.token == null) {
            throw new IllegalArgumentException("You must specify a token");
        }

        return new _FluxTcpClient(this);
    }

}
