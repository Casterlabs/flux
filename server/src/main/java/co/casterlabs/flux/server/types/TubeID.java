package co.casterlabs.flux.server.types;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.annotating.JsonSerializer;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonString;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.NonNull;

@JsonClass(serializer = TubeIDSerializer.class)
public record TubeID(String backing) implements _StringType {

    @Override
    public final String toString() {
        return this.backing;
    }

}

class TubeIDSerializer implements JsonSerializer<TubeID> {
    @Override
    public @Nullable TubeID deserialize(@NonNull JsonElement value, @NonNull Class<?> type, @NonNull Rson rson) throws JsonParseException {
        return new TubeID(value.getAsString());
    }

    @Override
    public JsonElement serialize(@NonNull Object value, @NonNull Rson rson) {
        return new JsonString(value.toString());
    }
}
