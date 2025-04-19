package co.casterlabs.flux.server;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import co.casterlabs.flux.server.authenticator.Authenticator.Authentication;
import co.casterlabs.flux.server.authenticator.Authenticator.AuthenticationException;
import co.casterlabs.flux.server.packet.incoming.TFPacketPublish;
import co.casterlabs.flux.server.packet.incoming.TFPacketSubscribe;
import co.casterlabs.flux.server.packet.incoming.TFPacketUnsubscribe;
import co.casterlabs.flux.server.packet.incoming.ToFluxPacket;
import co.casterlabs.flux.server.packet.outgoing.FFPacketError;
import co.casterlabs.flux.server.packet.outgoing.FFPacketMessage;
import co.casterlabs.flux.server.packet.outgoing.FromFluxPacket;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.util.LockableResource;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;

public class Client implements Closeable {
    public final Authentication auth;

    private Handle handle;
    private volatile boolean isClosed = false;
    private LockableResource<Map<TubeID, Tube>> subscriptions = new LockableResource<>(new HashMap<>());

    public Client(String token, Handle handle, boolean autoRegisterMeta) throws AuthenticationException {
        this.auth = Flux.authenticator.authenticate(token);
        this.handle = handle;

        if (autoRegisterMeta) {
            this.subscriptions.acquireUnsafe().put(Flux.metaBroadcast.id, Flux.metaBroadcast);
            Flux.metaBroadcast.registerReceiver(this);
        }
    }

    public void processIncoming(ToFluxPacket rawPacket) {
        if (Flux.DEBUG) {
            System.out.printf("[Client (%s)] %s\n", this.auth.id(), rawPacket);
        }

        Profile profile = Profiler.CLIENT_INCOMING.start();
        try {
            switch (rawPacket.type()) {
                case KEEP_ALIVE:
                    return; // The transport takes care of the connection life-cycle.

                case PUBLISH: {
                    TFPacketPublish packet = (TFPacketPublish) rawPacket;
                    if (this.isClosed) return;

                    if (!this.auth.canSend(packet.tube)) {
                        this.handleOutgoing(
                            new FFPacketError(
                                FFPacketError.Reason.PUBLISH_FAILED_PERMISSIONS,
                                packet.tube
                            )
                        );
                        return;
                    }

                    FromFluxPacket out = new FFPacketMessage(packet.message, this.auth.id(), packet.tube);

                    Tube tube = Tube.get(packet.tube, false);
                    if (tube == null) {
                        this.handleOutgoing(
                            new FFPacketError(
                                FFPacketError.Reason.PUBLISH_FAILED_NO_RECIPIENT,
                                packet.tube
                            )
                        );
                    } else if (tube.isMeta && tube != Flux.metaBroadcast) {
                        this.handleOutgoing(
                            new FFPacketError(
                                FFPacketError.Reason.PUBLISH_FAILED_META_TUBE,
                                packet.tube
                            )
                        );
                    } else {
                        tube.send(out);

                        if (tube != Flux.metaWildcard) {
                            Flux.metaWildcard.send(out);
                        }
                    }
                    return;
                }

                case UNSUBSCRIBE: {
                    TFPacketUnsubscribe packet = (TFPacketUnsubscribe) rawPacket;

                    Map<TubeID, Tube> subscriptions = this.subscriptions.acquire();
                    try {
                        if (this.isClosed) return;

                        Tube tube = subscriptions.remove(packet.tube);
                        if (tube == null) return;
                        tube.unregisterReceiver(this);
                        return;
                    } finally {
                        this.subscriptions.release();
                    }
                }

                case SUBSCRIBE: {
                    TFPacketSubscribe packet = (TFPacketSubscribe) rawPacket;

                    Map<TubeID, Tube> subscriptions = this.subscriptions.acquire();
                    try {
                        if (this.isClosed) return;

                        Tube tube = subscriptions.get(packet.tube);
                        if (tube != null) return;

                        if (!this.auth.canReceive(packet.tube)) {
                            this.handleOutgoing(
                                new FFPacketError(
                                    FFPacketError.Reason.SUBSCRIPTION_FAILED_PERMISSIONS,
                                    packet.tube
                                )
                            );
                            return;
                        }

                        tube = Tube.get(packet.tube, true);
                        subscriptions.put(packet.tube, tube);

                        tube.registerReceiver(this);
                        return;
                    } finally {
                        this.subscriptions.release();
                    }
                }
            }
        } finally {
            profile.end();
        }
    }

    @Override
    public void close() {
        Map<TubeID, Tube> subscriptions = this.subscriptions.acquire();
        try {
            this.isClosed = true;
            for (Tube tube : subscriptions.values()) {
                tube.unregisterReceiver(this);
            }
            subscriptions.clear();
        } finally {
            this.subscriptions.release();
        }
    }

    void handleOutgoing(FromFluxPacket packet) {
        if (this.isClosed) return;
        if (this.auth.id().equals(packet.from())) return;
        try {
            this.handle.handleOutgoing(packet);
        } catch (Throwable e) {}
    }

    @FunctionalInterface
    public static interface Handle {
        public static final Handle NOOP = (p) -> {
        };

        public void handleOutgoing(FromFluxPacket packet) throws Throwable;
    }

}
