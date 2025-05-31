package co.casterlabs.flux.server.daemon.http;

import co.casterlabs.commons.io.bytes.reading.ArrayByteReader;
import co.casterlabs.flux.server.Client;
import co.casterlabs.flux.server.Client.Handle;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.authenticator.Authenticator.AuthenticationException;
import co.casterlabs.flux.server.packet.PacketType;
import co.casterlabs.flux.server.packet.incoming.ToFluxPacket;
import co.casterlabs.flux.server.packet.outgoing.FFPacketAck;
import co.casterlabs.flux.server.packet.outgoing.FFPacketError;
import co.casterlabs.flux.server.packet.outgoing.FromFluxPacket;
import co.casterlabs.flux.server.protocols.BinaryWireProtocol;
import co.casterlabs.flux.server.protocols.StringWireProtocol;
import co.casterlabs.flux.server.protocols.WireProtocol;
import co.casterlabs.flux.server.protocols.WireProtocol.WireProtocolException;
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
    private static final String UNSUPPORTED_PROTO_MESSAGE = "Unsupported protocol (a.k.a Content-Type), supported types:"
        + "\n- application/json; charset=utf-8";

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

                    ToFluxPacket packet;
                    if (protocol instanceof BinaryWireProtocol bin) {
                        packet = bin.parse(new ArrayByteReader(session.body().bytes()));
                    } else {
                        packet = ((StringWireProtocol) protocol).parse(session.body().string());
                    }

                    if (packet.type() != PacketType.PUBLISH) {
                        return protoResponse(
                            protocol,
                            StandardHttpStatus.BAD_REQUEST,
                            new FFPacketError(
                                FFPacketError.Reason.PACKET_INVALID,
                                Flux.CONTROL_TUBEID
                            )
                        );
                    }

                    try (Client client = new Client(token, Handle.NOOP, false)) {
                        client.processIncoming(packet);

                        return protoResponse(
                            protocol,
                            StandardHttpStatus.CREATED,
                            FFPacketAck.INSTANCE
                        );
                    }
                } catch (WireProtocolException e) {
                    if (Flux.DEBUG) {
                        e.printStackTrace();
                    }
                    return protoResponse(
                        protocol,
                        StandardHttpStatus.BAD_REQUEST,
                        new FFPacketError(
                            FFPacketError.Reason.PACKET_INVALID,
                            Flux.CONTROL_TUBEID
                        )
                    );
                } catch (AuthenticationException e) {
                    if (Flux.DEBUG) {
                        e.printStackTrace();
                    }
                    return protoResponse(
                        protocol,
                        StandardHttpStatus.UNAUTHORIZED,
                        new FFPacketError(
                            FFPacketError.Reason.AUTHENTICATION_FAILED,
                            Flux.CONTROL_TUBEID
                        )
                    );
                } catch (Throwable t) {
                    if (Flux.DEBUG) {
                        t.printStackTrace();
                    }
                    return protoResponse(
                        protocol,
                        StandardHttpStatus.INTERNAL_ERROR,
                        new FFPacketError(
                            FFPacketError.Reason.SERVER_INTERNAL_ERROR,
                            Flux.CONTROL_TUBEID
                        )
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

        String value = header.withoutDirectives().toLowerCase();

        switch (value) {
            case "application/json":
                if (header.directives().getSingleOrDefault("charset", "utf-8").equalsIgnoreCase("utf-8")) {
                    return WireProtocol.TYPES.get(WireProtocol.Type.JSON);
                } else {
                    return null;
                }

            case "application/octet-stream":
                return WireProtocol.TYPES.get(WireProtocol.Type.BINARY);

            default:
                return null;
        }
    }

    private static HttpResponse protoResponse(WireProtocol protocol, HttpStatus status, FromFluxPacket packet) {
        if (protocol instanceof BinaryWireProtocol bin) {
            return new HttpResponse(new _BinaryResponseContent(bin, packet), status)
                .header("Content-Type", "application/octet-stream");
        } else {
            try {
                String serialized = ((StringWireProtocol) protocol).serialize(packet);

                return HttpResponse.newFixedLengthResponse(status, serialized)
                    .header("Content-Type", "application/json; charset=utf-8");
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
