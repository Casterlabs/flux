package co.casterlabs.flux.server.daemon.http;

import co.casterlabs.commons.io.bytes.reading.ArrayByteReader;
import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.Packet.PacketType;
import co.casterlabs.flux.packets.PacketAck;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketError.Reason;
import co.casterlabs.flux.packets.protocols.BinaryWireProtocol;
import co.casterlabs.flux.packets.protocols.StringWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.server.Client;
import co.casterlabs.flux.server.Client.Handle;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.authenticator.Authenticator.AuthenticationException;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;
import co.casterlabs.rhs.HttpStatus;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.HeaderValue;
import co.casterlabs.rhs.protocol.exceptions.DropConnectionException;
import co.casterlabs.rhs.protocol.exceptions.HttpException;
import co.casterlabs.rhs.protocol.http.HttpProtocol.HttpProtoHandler;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;

class _HTTPHandler implements HttpProtoHandler {
    static final HeaderValue ANONYMOUS_BEARER = new HeaderValue("Bearer anonymous");
    private static final String UNSUPPORTED_PROTO_MESSAGE = "Unsupported protocol (Content-Type), supported types:" + String.join("\n- ", WireProtocol.MIMES.keySet());

    @Override
    public HttpResponse handle(HttpSession session) throws HttpException, DropConnectionException {
        switch (session.method()) {
            case OPTIONS:
                return cors(HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_CONTENT));

            case POST: {
                Profile profile = Profiler.DAEMON_HTTP_POST.start();

                WireProtocol protocol = getProtocol(session.headers().getSingle("Content-Type"));
                if (protocol == null) {
                    // It's safe to discard the profile without end()ing it.
                    return cors(HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, UNSUPPORTED_PROTO_MESSAGE));
                }

                try {
                    String token = session.headers()
                        .getSingleOrDefault("Authorization", ANONYMOUS_BEARER)
                        .raw()
                        .substring("Bearer ".length());

                    Packet packet;
                    if (protocol instanceof BinaryWireProtocol bin) {
                        packet = bin.parse(new ArrayByteReader(session.body().bytes()));
                    } else {
                        packet = ((StringWireProtocol) protocol).parse(session.body().string());
                    }

                    if (packet.type() != PacketType.PUBLISH) {
                        return protoResponse(
                            protocol,
                            StandardHttpStatus.BAD_REQUEST,
                            new PacketError(Reason.PACKET_INVALID)
                        );
                    }

                    try (Client client = new Client(token, Handle.NOOP, false)) {
                        client.processIncoming(packet);

                        return protoResponse(
                            protocol,
                            StandardHttpStatus.CREATED,
                            new PacketAck(client.auth.id())
                        );
                    }
                } catch (WireProtocolException e) {
                    if (Flux.DEBUG) {
                        e.printStackTrace();
                    }
                    return protoResponse(
                        protocol,
                        StandardHttpStatus.BAD_REQUEST,
                        new PacketError(Reason.PACKET_INVALID)
                    );
                } catch (AuthenticationException e) {
                    if (Flux.DEBUG) {
                        e.printStackTrace();
                    }
                    return protoResponse(
                        protocol,
                        StandardHttpStatus.UNAUTHORIZED,
                        new PacketError(Reason.AUTHENTICATION_FAILED)
                    );
                } catch (Throwable t) {
                    if (Flux.DEBUG) {
                        t.printStackTrace();
                    }
                    return protoResponse(
                        protocol,
                        StandardHttpStatus.INTERNAL_ERROR,
                        new PacketError(Reason.SERVER_INTERNAL_ERROR)
                    );
                } finally {
                    profile.end();
                }
            }

            case CONNECT:
            case TRACE:
                return null; // Not supported.

            default:
                return HttpResponse.newFixedLengthResponse(StandardHttpStatus.METHOD_NOT_ALLOWED, "Please use POST")
                    .header("Allow", "OPTIONS, POST");
        }
    }

    private static WireProtocol getProtocol(HeaderValue header) {
        if (header == null) {
            return null;
        }

        return WireProtocol.MIMES.get(header.withoutDirectives().toLowerCase());
    }

    private static HttpResponse protoResponse(WireProtocol protocol, HttpStatus status, Packet packet) {
        if (protocol instanceof BinaryWireProtocol bin) {
            return new HttpResponse(new _BinaryResponseContent(bin, packet), status)
                .header("Content-Type", protocol.mime());
        } else {
            try {
                String serialized = ((StringWireProtocol) protocol).serialize(packet);

                return HttpResponse.newFixedLengthResponse(status, serialized)
                    .header("Content-Type", protocol.mime());
            } catch (WireProtocolException e) {
                if (Flux.DEBUG) {
                    e.printStackTrace();
                }
                return null;
            }
        }
    }

    private static HttpResponse cors(HttpResponse response) {
        return response
            .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "POST, OPTIONS");
    }

}
