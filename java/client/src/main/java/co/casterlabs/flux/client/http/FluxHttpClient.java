package co.casterlabs.flux.client.http;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.StreamByteReader;
import co.casterlabs.commons.io.bytes.writing.ArrayByteWriter;
import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketPublish;
import co.casterlabs.flux.packets.protocols.BinaryWireProtocol;
import co.casterlabs.flux.packets.protocols.StringWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.packets.types.Message;
import co.casterlabs.flux.packets.types.TubeID;
import lombok.NonNull;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FluxHttpClient {
    private final WireProtocol protocol;
    private final OkHttpClient client;
    private final String url;
    private final String token;

    public FluxHttpClient(String url, String token) {
        this(url, token, new OkHttpClient());
    }

    public FluxHttpClient(@NonNull String url, @NonNull String token, @NonNull OkHttpClient client) {
        this(url, token, client, WireProtocol.Type.BYTES);
    }

    public FluxHttpClient(@NonNull String url, @NonNull String token, @NonNull OkHttpClient client, @NonNull WireProtocol.Type protocol) {
        this.url = url;
        this.token = token;
        this.client = client;
        this.protocol = WireProtocol.TYPES.get(protocol);
    }

    /**
     * @throws IOException     if an I/O error occurs.
     * @throws PublishingError if an error is returned by Flux.
     */
    public void post(@NonNull String tube, @NonNull String message) throws IOException, PublishingError {
        try {
            this.post(
                new PacketPublish(
                    new TubeID(tube),
                    new Message<>(message)
                )
            );
        } catch (WireProtocolException e) {
            throw new IOException(e);
        }
    }

    /**
     * @throws IOException     if an I/O error occurs.
     * @throws PublishingError if an error is returned by Flux.
     */
    public void post(@NonNull String tube, @NonNull byte[] message) throws IOException, PublishingError {
        try {
            this.post(
                new PacketPublish(
                    new TubeID(tube),
                    new Message<>(message)
                )
            );
        } catch (WireProtocolException e) {
            throw new IOException(e);
        }
    }

    private void post(PacketPublish packet) throws IOException, PublishingError, WireProtocolException {
        RequestBody body;

        if (this.protocol instanceof BinaryWireProtocol bin) {
            ArrayByteWriter serializedPacket = new ArrayByteWriter();
            bin.serialize(packet, serializedPacket);

            body = RequestBody.create(
                serializedPacket.buffer(),
                MediaType.parse(this.protocol.mime())
            );
        } else {
            StringWireProtocol str = (StringWireProtocol) this.protocol;

            body = RequestBody.create(
                str.serialize(packet),
                MediaType.parse(this.protocol.mime())
            );
        }

        Request request = new Request.Builder()
            .url(this.url)
            .header("Authorization", this.token)
            .post(body)
            .build();

        Call call = this.client.newCall(request);

        try (Response res = call.execute()) {
            if (!this.protocol.mime().equalsIgnoreCase(res.header("Content-Type"))) {
                throw new IOException(res.code() + ": " + res.body().string());
            }

            Packet result;
            if (this.protocol instanceof BinaryWireProtocol bin) {
                result = bin.parse(new StreamByteReader(res.body().byteStream()));
            } else {
                StringWireProtocol str = (StringWireProtocol) this.protocol;

                result = str.parse(res.body().string());
            }

            if (result instanceof PacketError err) {
                throw new PublishingError(err.reason);
            }
        }
    }

}
