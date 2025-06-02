package co.casterlabs.flux.client.realtime.websocket;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import co.casterlabs.commons.websocket.WebSocketClient;
import co.casterlabs.flux.client.realtime.FluxRealtimeClient;
import co.casterlabs.flux.client.realtime.FluxRealtimeClientListener;
import co.casterlabs.flux.packets.protocols.WireProtocol;
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
public class FluxWebsocketClientBuilder {
    private @NonFinal long timeout = TimeUnit.SECONDS.toMillis(30);
    private @NonFinal WireProtocol.Type protocol = WireProtocol.Type.BYTES;

    private @NonFinal Function<WireProtocol, WebSocketClient> websocketFactory;
    private @NonFinal FluxRealtimeClientListener realtimeListener;

    public FluxWebsocketClientBuilder withUrl(@NonNull String url, @NonNull String token) {
        URI uri = URI.create(
            String.format(
                "%s?authorization=%s",
                url,
                URLEncoder.encode(token, StandardCharsets.UTF_8).replace("+", "%20")
            )
        );

        return this.withWebsocketFactory(
            (protocol) -> new WebSocketClient(
                uri,
                Arrays.asList(protocol.type().name())
            )
        );
    }

    public FluxRealtimeClient build() throws IOException {
        if (this.realtimeListener == null) {
            throw new IllegalArgumentException("You must specify a realtime listener");
        }
        if (this.websocketFactory == null) {
            throw new IllegalArgumentException("You must specify a websocket factory OR use withUrl()");
        }

        return new _FluxWebsocketClient(this);
    }

}
