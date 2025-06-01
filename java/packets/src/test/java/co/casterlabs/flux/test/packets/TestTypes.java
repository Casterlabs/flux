package co.casterlabs.flux.test.packets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.packets.types.TubeID;
import co.casterlabs.flux.packets.types.UserID;

public class TestTypes {

    @Test
    public void tube() throws WireProtocolException {
        TubeID a = new TubeID("example");
        TubeID b = new TubeID("example");
        assertEquals(a, b, "equals");
    }

    @Test
    public void user() throws WireProtocolException {
        UserID a = new UserID("example");
        UserID b = new UserID("example");
        assertEquals(a, b, "equals");
    }

}
