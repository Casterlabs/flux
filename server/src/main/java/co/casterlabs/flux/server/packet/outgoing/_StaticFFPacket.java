package co.casterlabs.flux.server.packet.outgoing;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.annotating.JsonExclude;
import co.casterlabs.rakurai.json.annotating.JsonSerializationMethod;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonString;

@JsonClass(exposeAll = true)
abstract class _StaticFFPacket implements FromFluxPacket {
    private final @JsonExclude int size = FromFluxPacket.super.size(); // cache.

    @JsonSerializationMethod("type")
    private JsonElement $serialize_type() {
        return new JsonString(this.type().name());
    }

    @JsonSerializationMethod("tube")
    private JsonElement $serialize_tube() {
        return new JsonString(this.tube().toString());
    }

    @JsonSerializationMethod("from")
    private JsonElement $serialize_from() {
        return new JsonString(this.from().toString());
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void serialize(ByteWriter writer) throws IOException {
        FromFluxPacket.super.serialize(writer);
    }

}
