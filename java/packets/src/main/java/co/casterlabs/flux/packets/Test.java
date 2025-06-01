package co.casterlabs.flux.packets;

import co.casterlabs.flux.packets.PacketError.Reason;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rakurai.json.validation.JsonValidationException;

public class Test {

    public static void main(String[] args) throws JsonValidationException, JsonParseException {
        JsonElement e = Rson.DEFAULT.toJson(new PacketError(Reason.AUTHENTICATION_FAILED));
        System.out.println(e);
        System.out.println(Rson.DEFAULT.fromJson(e, PacketError.class));
    }

}
