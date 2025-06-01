package co.casterlabs.flux.server;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.PacketMemberJoin;
import co.casterlabs.flux.packets.PacketMemberLeave;
import co.casterlabs.flux.packets.PacketMembers;
import co.casterlabs.flux.packets.PacketMessage;
import co.casterlabs.flux.packets.types.Message;
import co.casterlabs.flux.packets.types.TubeID;
import co.casterlabs.flux.packets.types.UserID;
import co.casterlabs.flux.server.util.LockableResource;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Tube {
    private static final LockableResource<Map<TubeID, Tube>> allTubes = new LockableResource<>(new HashMap<>());

    private LockableResource<Client[]> clients = new LockableResource<>(new Client[0]);
    public final TubeID id;
    public final boolean isMeta;

    void registerReceiver(Client client) {
        Profile profile = Profiler.TUBE_REGISTER.start();
        Client[] clients = this.clients.acquire();
        try {
            Client[] newClientsArr = new Client[clients.length + 1];
            System.arraycopy(clients, 0, newClientsArr, 0, clients.length);
            newClientsArr[clients.length] = client;
            this.clients.set(newClientsArr);

            if (!this.isMeta) {
                UserID[] members = new UserID[newClientsArr.length];
                for (int i = 0; i < newClientsArr.length; i++) {
                    members[i] = newClientsArr[i].auth.id();
                }

                this.send(new PacketMembers(this.id, members));
                this.send(new PacketMemberJoin(this.id, client.auth.id()));
            }
        } finally {
            this.clients.release();
            profile.end();
        }
    }

    void unregisterReceiver(Client client) {
        Profile profile = Profiler.TUBE_UNREGISTER.start();
        Map<TubeID, Tube> tubes = allTubes.acquire(); // MUST lock tubesLock first, then the local tube lock. Otherwise, deadlock.
        Client[] clients = this.clients.acquire();
        try {
            Client[] newClientsArr = new Client[clients.length - 1];
            int idx = 0;
            for (Client c : clients) {
                if (c != client) {
                    newClientsArr[idx] = c;
                    idx++;
                }
            }
            this.clients.set(newClientsArr);

            if (!this.isMeta) {
                if (newClientsArr.length == 0) {
                    tubes.remove(this.id);
                    Flux.metaTubeDestroy.send(
                        new PacketMessage(
                            Flux.metaTubeDestroy.id,
                            Flux.META_PUBLISHER,
                            new Message<>(this.id.toString())
                        )
                    );
                } else {
                    UserID[] members = new UserID[newClientsArr.length];
                    for (int i = 0; i < newClientsArr.length; i++) {
                        members[i] = newClientsArr[i].auth.id();
                    }

                    this.send(new PacketMembers(this.id, members));
                    this.send(new PacketMemberLeave(this.id, client.auth.id()));
                }
            }
        } finally {
            this.clients.release();
            allTubes.release();
            profile.end();
        }
    }

    public void send(Packet packet) {
        if (Flux.DEBUG) {
            System.out.printf("[Tube (%s)] %s\n", this.id, packet);
        }

        Profile profile = (this.isMeta ? Profiler.META_TUBE_PUBLISH : Profiler.TUBE_PUBLISH).start();
        try {
            Client[] clients = this.clients.acquireUnsafe(); // We can safely treat this as a copy. Since changes don't mutate the array.
            if (clients.length == 0) return;
            for (Client client : clients) {
                client.handleOutgoing(packet);
            }
        } finally {
            profile.end();
        }
    }

    public static @Nullable Tube get(TubeID id, boolean createIfNeeded) {
        Profile profile = Profiler.TUBE_GET.start();
        Map<TubeID, Tube> tubes = allTubes.acquire();
        try {
            Tube tube = tubes.get(id);
            if (tube == null && createIfNeeded) {
                tube = new Tube(id, id.startsWith(Flux.META_SEQ));
                tubes.put(id, tube);

                if (!tube.isMeta) {
                    Flux.metaTubeCreate.send(
                        new PacketMessage(
                            Flux.metaTubeDestroy.id,
                            Flux.META_PUBLISHER,
                            new Message<>(id.toString())
                        )
                    );
                }
            }
            return tube;
        } finally {
            allTubes.release();
            profile.end();
        }
    }

}
