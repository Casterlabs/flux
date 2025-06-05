package co.casterlabs.flux.client.realtime.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.reading.StreamByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.commons.io.bytes.writing.StreamByteWriter;
import co.casterlabs.commons.io.streams.MTUOutputStream;

class _SocketConnection implements Closeable {
    private final Socket socket;

    final ByteReader in;
    final ByteWriter out;

    _SocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new StreamByteReader(
            this.socket.getInputStream()
        );
        this.out = new StreamByteWriter(
            new MTUOutputStream(
                this.socket.getOutputStream(),
                MTUOutputStream.guessMtu(socket)
            )
        );
    }

    @Override
    public void close() throws IOException {
        this.socket.close();
    }

}
