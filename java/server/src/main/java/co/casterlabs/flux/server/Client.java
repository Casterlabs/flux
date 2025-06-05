package co.casterlabs.flux.server;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketError.Reason;
import co.casterlabs.flux.packets.PacketMemberJoin;
import co.casterlabs.flux.packets.PacketMemberLeave;
import co.casterlabs.flux.packets.PacketMessage;
import co.casterlabs.flux.packets.PacketPublish;
import co.casterlabs.flux.packets.PacketSubscribe;
import co.casterlabs.flux.packets.PacketUnsubscribe;
import co.casterlabs.flux.packets.types.TubeID;
import co.casterlabs.flux.server.authenticator.Authenticator.Authentication;
import co.casterlabs.flux.server.authenticator.Authenticator.AuthenticationException;
import co.casterlabs.flux.server.util.LockableResource;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;

public class Client implements Closeable {
    private static final ThreadFactory THREAD_FACTORY = Thread.ofVirtual().name("Client - Write Thread #", 0).factory();

    private final ExecutorService outgoingQueue = Executors.newSingleThreadExecutor(THREAD_FACTORY);
    private final LockableResource<Map<TubeID, Tube>> subscriptions = new LockableResource<>(new HashMap<>());

    private volatile boolean isClosed = false;
    private final Handle handle;

    public final Authentication auth;

    public Client(String token, Handle handle, boolean autoRegisterMeta) throws AuthenticationException {
        this.auth = Flux.authenticator.authenticate(token);
        this.handle = handle;

        if (autoRegisterMeta) {
            this.subscriptions.acquireUnsafe().put(Flux.metaBroadcast.id, Flux.metaBroadcast);
            Flux.metaBroadcast.registerReceiver(this);
        }
    }

    public void processIncoming(Packet rawPacket) {
        if (Flux.DEBUG) {
            System.out.printf("[Client (%s)] %s\n", this.auth.id(), rawPacket);
        }

        Profile profile = Profiler.CLIENT_INCOMING.start();
        try {
            switch (rawPacket.type()) {
                case PUBLISH: {
                    PacketPublish packet = (PacketPublish) rawPacket;
                    if (this.isClosed) return;

                    if (!this.auth.canSend(packet.tube)) {
                        this.handleOutgoing(
                            new PacketError(
                                Reason.PUBLISH_FAILED_PERMISSIONS
                            )
                        );
                        return;
                    }

                    PacketMessage toTube = new PacketMessage(packet.tube, this.auth.id(), packet.message);

                    Tube tube = Tube.get(packet.tube, false);
                    if (tube == null) {
                        this.handleOutgoing(
                            new PacketError(
                                Reason.PUBLISH_FAILED_NO_RECIPIENT
                            )
                        );
                    } else if (tube.isMeta && tube != Flux.metaBroadcast) {
                        this.handleOutgoing(
                            new PacketError(
                                Reason.PUBLISH_FAILED_META_TUBE
                            )
                        );
                    } else {
                        tube.send(toTube);

                        if (tube != Flux.metaWildcard) {
                            Flux.metaWildcard.send(toTube);
                        }
                    }
                    return;
                }

                case UNSUBSCRIBE: {
                    PacketUnsubscribe packet = (PacketUnsubscribe) rawPacket;

                    Map<TubeID, Tube> subscriptions = this.subscriptions.acquire();
                    try {
                        if (this.isClosed) return;

                        Tube tube = subscriptions.remove(packet.tube);
                        if (tube == null) return;
                        tube.unregisterReceiver(this);
                    } finally {
                        this.subscriptions.release();
                    }
                    return;
                }

                case SUBSCRIBE: {
                    PacketSubscribe packet = (PacketSubscribe) rawPacket;

                    Map<TubeID, Tube> subscriptions = this.subscriptions.acquire();
                    try {
                        if (this.isClosed) return;

                        Tube tube = subscriptions.get(packet.tube);
                        if (tube != null) return;

                        if (!this.auth.canReceive(packet.tube)) {
                            this.handleOutgoing(
                                new PacketError(
                                    Reason.SUBSCRIPTION_FAILED_PERMISSIONS
                                )
                            );
                            return;
                        }

                        tube = Tube.get(packet.tube, true);
                        subscriptions.put(packet.tube, tube);

                        tube.registerReceiver(this);
                    } finally {
                        this.subscriptions.release();
                    }
                    return;
                }

                case KEEP_ALIVE:
                    return; // The transport takes care of the connection life-cycle.

                default:
                    this.handleOutgoing(new PacketError(Reason.PACKET_INVALID));
                    return;
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

        this.outgoingQueue.shutdownNow();
    }

    void handleOutgoing(Packet packet) {
        if (this.isClosed) return;

        if (packet instanceof PacketMemberJoin join) {
            if (this.auth.id().equals(join.member)) return;
        } else if (packet instanceof PacketMemberLeave leave) {
            if (this.auth.id().equals(leave.member)) return;
        } else if (packet instanceof PacketMessage message) {
            if (this.auth.id().equals(message.from)) return;
        }

        this.outgoingQueue.submit(() -> {
            try {
                this.handle.handleOutgoing(packet);
            } catch (Throwable e) {}
        });
    }

    @FunctionalInterface
    public static interface Handle {
        public static final Handle NOOP = (p) -> {
        };

        public void handleOutgoing(Packet packet);
    }

}
