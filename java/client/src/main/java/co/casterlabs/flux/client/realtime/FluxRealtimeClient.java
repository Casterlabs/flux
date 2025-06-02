package co.casterlabs.flux.client.realtime;

import java.io.Closeable;

import lombok.NonNull;

public interface FluxRealtimeClient extends Closeable {

    public FluxRealtimeClient publish(@NonNull String tube, @NonNull String message);

    public FluxRealtimeClient publish(@NonNull String tube, @NonNull byte[] message);

    public FluxRealtimeClient subscribe(@NonNull String tube, @NonNull FluxTubeListener listener);

    public FluxRealtimeClient unsubscribe(@NonNull String tube);

}
