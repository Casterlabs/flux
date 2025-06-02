package co.casterlabs.flux.client.http;

import co.casterlabs.flux.packets.PacketError.Reason;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PublishingError extends Exception {
    private static final long serialVersionUID = -9071409205078920455L;

    public final Reason reason;

}
