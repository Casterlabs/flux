package co.casterlabs.flux.server.packet.incoming;

public interface ToFluxPacket {

    public Type type();

    static enum Type {
        KEEP_ALIVE,
        PUBLISH,
        SUBSCRIBE,
        UNSUBSCRIBE,
    }

}
