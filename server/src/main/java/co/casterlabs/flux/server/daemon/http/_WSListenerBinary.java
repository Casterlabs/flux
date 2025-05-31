package co.casterlabs.flux.server.daemon.http;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import co.casterlabs.commons.io.bytes.reading.ArrayByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.server.Client;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.authenticator.Authenticator.AuthenticationException;
import co.casterlabs.flux.server.packet.incoming.ToFluxPacket;
import co.casterlabs.flux.server.packet.outgoing.FFPacketAck;
import co.casterlabs.flux.server.packet.outgoing.FFPacketError;
import co.casterlabs.flux.server.packet.outgoing.FromFluxPacket;
import co.casterlabs.flux.server.protocols.BinaryWireProtocol;
import co.casterlabs.flux.server.protocols.WireProtocol.WireProtocolException;
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

    private void handle(FromFluxPacket packet) throws IOException {
        this.executor.submit(() -> {
            try {
                int length = this.protocol.sizeOf(packet);

                if (this.writer.buffer.length < length) {
                    this.writer.buffer = new byte[length];
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
            this.handle(FFPacketAck.INSTANCE);
        } catch (AuthenticationException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }
            this.handle(
                new FFPacketError(
                    FFPacketError.Reason.AUTHENTICATION_FAILED,
                    Flux.CONTROL_TUBEID
                )
            );
            websocket.close();
        }
    }

    @Override
    public void onBinary(Websocket websocket, byte[] bytes) throws IOException {
        try {
            ToFluxPacket packet = this.protocol.parse(new ArrayByteReader(bytes));
            this.client.processIncoming(packet);
        } catch (WireProtocolException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }
            this.handle(new FFPacketError(FFPacketError.Reason.PACKET_INVALID, Flux.CONTROL_TUBEID));
        }
    }

    @Override
    public void onText(Websocket websocket, String message) throws IOException {
        this.handle(new FFPacketError(FFPacketError.Reason.PACKET_INVALID, Flux.CONTROL_TUBEID));
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
