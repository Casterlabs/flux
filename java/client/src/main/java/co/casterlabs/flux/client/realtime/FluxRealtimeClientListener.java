package co.casterlabs.flux.client.realtime;

import co.casterlabs.flux.packets.PacketError;

public interface FluxRealtimeClientListener {

    public default void onReady(FluxRealtimeClient client) {}

    public default void onClose() {}

    public default void onError(PacketError.Reason reason) {
        System.err.printf("Flux error: %s\n", reason);
    }

    public default void onException(Throwable t) {
        t.printStackTrace();
    }

}
