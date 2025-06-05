package co.casterlabs.flux.test.packets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.PacketAck;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketError.Reason;
import co.casterlabs.flux.packets.PacketKeepAlive;
import co.casterlabs.flux.packets.PacketMemberJoin;
import co.casterlabs.flux.packets.PacketMemberLeave;
import co.casterlabs.flux.packets.PacketMembers;
import co.casterlabs.flux.packets.PacketMessage;
import co.casterlabs.flux.packets.PacketPublish;
import co.casterlabs.flux.packets.PacketSubscribe;
import co.casterlabs.flux.packets.PacketUnsubscribe;
import co.casterlabs.flux.packets.protocols.StringWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.packets.protocols.json.JsonWireProtocol;
import co.casterlabs.flux.packets.types.Message;
import co.casterlabs.flux.packets.types.TubeID;
import co.casterlabs.flux.packets.types.UserID;

public class TestJsonWire {
    private static final StringWireProtocol PROTOCOL = JsonWireProtocol.INSTANCE;

    @Test
    public void ack() throws WireProtocolException {
        UserID id = UserID.random();
        test(new PacketAck(id));
    }

    @Test
    public void error() throws WireProtocolException {
        for (Reason r : Reason.values()) {
            test(new PacketError(r));
        }
    }

    @Test
    public void keepAlive() throws WireProtocolException {
        test(PacketKeepAlive.INSTANCE);
    }

    @Test
    public void memberJoin() throws WireProtocolException {
        UserID member = UserID.random();
        TubeID tube = new TubeID("example");

        test(new PacketMemberJoin(tube, member));
    }

    @Test
    public void memberLeave() throws WireProtocolException {
        UserID member = UserID.random();
        TubeID tube = new TubeID("example");

        test(new PacketMemberLeave(tube, member));
    }

    @Test
    public void members() throws WireProtocolException {
        UserID[] members = {
                UserID.random(),
                UserID.random(),
                UserID.random()
        };
        TubeID tube = new TubeID("example");

        test(new PacketMembers(tube, members));
    }

    @Test
    public void message_string() throws WireProtocolException {
        UserID from = UserID.random();
        TubeID tube = new TubeID("example");
        Message<String> message = new Message<>("abcdefghijklmnopqrstuvwxyz");

        test(new PacketMessage(tube, from, message));
    }

    @Test
    public void message_binary() throws WireProtocolException {
        UserID from = UserID.random();
        TubeID tube = new TubeID("example");

        byte[] bytes = new byte[128];
        ThreadLocalRandom.current().nextBytes(bytes);

        Message<byte[]> message = new Message<>(bytes);

        test(new PacketMessage(tube, from, message));
    }

    @Test
    public void publish_string() throws WireProtocolException {
        TubeID tube = new TubeID("example");
        Message<String> message = new Message<>("abcdefghijklmnopqrstuvwxyz");

        test(new PacketPublish(tube, message));
    }

    @Test
    public void publish_binary() throws WireProtocolException {
        TubeID tube = new TubeID("example");

        byte[] bytes = new byte[128];
        ThreadLocalRandom.current().nextBytes(bytes);

        Message<byte[]> message = new Message<>(bytes);

        test(new PacketPublish(tube, message));
    }

    @Test
    public void subscribe() throws WireProtocolException {
        TubeID tube = new TubeID("example");

        test(new PacketSubscribe(tube));
    }

    @Test
    public void unsubscribe() throws WireProtocolException {
        TubeID tube = new TubeID("example");

        test(new PacketUnsubscribe(tube));
    }

    private static void test(Packet source) throws WireProtocolException {
        String sourceSer = PROTOCOL.serialize(source);
        Packet copy = PROTOCOL.parse(sourceSer);

        assertEquals(sourceSer, PROTOCOL.serialize(copy), "sane");
        assertEquals(source, copy, "equals");
    }

}
