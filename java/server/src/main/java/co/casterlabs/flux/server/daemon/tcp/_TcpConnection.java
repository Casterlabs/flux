package co.casterlabs.flux.server.daemon.tcp;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.EndOfStreamException;
import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketError.Reason;
import co.casterlabs.flux.packets.PacketPublish;
import co.casterlabs.flux.packets.protocols.BinaryWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.packets.protocols.bytes.ByteWireProtocol;
import co.casterlabs.flux.server.Client;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.authenticator.Authenticator.AuthenticationException;

class _TcpConnection implements AutoCloseable, Client.Handle {
    private static final BinaryWireProtocol PROTOCOL = ByteWireProtocol.INSTANCE;

    private final _SocketConnection socket;
    private boolean closed = false;
    private Client client;

    _TcpConnection(_SocketConnection socket) {
        this.socket = socket;
    }

    @Override
    public void handleOutgoing(Packet packet) {
        try {
            PROTOCOL.serialize(packet, this.socket.out);
        } catch (WireProtocolException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        try (this) {
            // SPEC: first packet MUST be a string publish packet with a tube name of "__flux:authenticate"
            {
                Packet authPacket = PROTOCOL.parse(this.socket.in);
                
                if (authPacket instanceof PacketPublish pub) {
					if (pub.tube.toString().equals(Flux.META_TUBE_AUTH)) {
						this.client = new Client(pub.message.asString(), this, true);
					}
				}
                
                if (this.client == null) {
                    // Client didn't adhere to the spec, first packet should've been the auth packet.
                    this.handleOutgoing(new PacketError(Reason.AUTHENTICATION_FAILED));
                    return;
                }
            }
            
            while (!this.closed) {
                Packet packet = PROTOCOL.parse(this.socket.in);
                this.client.processIncoming(packet);
            }
        } catch (AuthenticationException e) {
            this.handleOutgoing(new PacketError(Reason.AUTHENTICATION_FAILED));
        } catch (EndOfStreamException e) {
            // Already handled by the try-with-resources
        } catch (Throwable t) {
			if (Flux.DEBUG) {
				t.printStackTrace();
			}
        }
    }

    @Override
    public void close() throws IOException {
        this.closed = true;

        if (this.client != null) {
            this.client.close();
        }

        this.socket.close();
    }

}
