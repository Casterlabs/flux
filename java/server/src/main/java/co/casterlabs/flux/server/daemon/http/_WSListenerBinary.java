package co.casterlabs.flux.server.daemon.http;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import co.casterlabs.commons.io.bytes.reading.ArrayByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.PacketAck;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketError.Reason;
import co.casterlabs.flux.packets.protocols.BinaryWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.server.Client;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.authenticator.Authenticator.AuthenticationException;
import co.casterlabs.rhs.protocol.websocket.Websocket;
import co.casterlabs.rhs.protocol.websocket.WebsocketListener;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class _WSListenerBinary implements WebsocketListener {
    private static final ThreadFactory THREAD_FACTORY = Thread.ofVirtual().name("HTTP Daemon - WebSocket - #", 0).factory();

    private final BinaryWireProtocol protocol;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(THREAD_FACTORY);

    private final FixedLengthByteWriter writer = new FixedLengthByteWriter();

    private Websocket websocket;
    private Client client;

    private void handle(Packet packet) throws IOException {
        this.executor.submit(() -> {
            try {
                long length = this.protocol.sizeOf(packet);

                if (this.writer.buffer.length < length) {
                    this.writer.buffer = new byte[(int) length];
                }
                this.writer.index = 0;

                this.protocol.serialize(packet, this.writer);

                byte[] serialized = new byte[this.writer.index];
                System.arraycopy(this.writer.buffer, 0, serialized, 0, this.writer.index);

                this.websocket.send(serialized);
            } catch (IOException | WireProtocolException ignored) {}
        });
    }

    @Override
    public void onOpen(Websocket websocket) throws IOException {
        this.websocket = websocket;
        try {
            this.client = new Client(
                websocket.session().uri().query.getSingleOrDefault("authorization", _HTTPHandler.ANONYMOUS_BEARER.raw()),
                this::handle,
                true
            );
            this.handle(new PacketAck(this.client.auth.id()));
        } catch (AuthenticationException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }
            this.handle(
                new PacketError(Reason.AUTHENTICATION_FAILED)
            );
            websocket.close();
        }
    }

    @Override
    public void onBinary(Websocket websocket, byte[] bytes) throws IOException {
        try {
            Packet packet = this.protocol.parse(new ArrayByteReader(bytes));
            this.client.processIncoming(packet);
        } catch (WireProtocolException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }
            this.handle(new PacketError(Reason.PACKET_INVALID));
        }
    }

    @Override
    public void onText(Websocket websocket, String message) throws IOException {
        this.handle(new PacketError(Reason.PACKET_INVALID));
    }

    @Override
    public void onClose(Websocket websocket) {
        this.client.close();
        this.executor.shutdownNow();
    }

    private static final class FixedLengthByteWriter extends ByteWriter {
        private byte[] buffer = new byte[1024]; // arbitrary initial size
        private int index = 0;

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            System.arraycopy(b, off, this.buffer, this.index, len);
            this.index += len;
        }

        @Override
        protected void write(int value) throws IOException {
            this.buffer[this.index++] = (byte) value;
        }

        @Override
        public void close() throws Exception {
            // NOOP
        }

    }

}
