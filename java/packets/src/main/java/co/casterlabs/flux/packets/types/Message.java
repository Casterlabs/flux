package co.casterlabs.flux.packets.types;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.annotating.JsonSerializer;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.element.JsonString;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.NonNull;

@JsonClass(serializer = MessageSerializer.class)
public record Message<T>(T value) {

    public Message(@NonNull T value) {
        if (!(value instanceof String) && !(value instanceof byte[])) {
            throw new IllegalArgumentException("Invalid message type: " + value.getClass());
        }

        this.value = value;
    }

    public boolean isString() {
        return this.value instanceof String;
    }

    public String asString() {
        return (String) this.value;
    }

    public boolean isBinary() {
        return this.value instanceof byte[];
    }

    public byte[] asBinary() {
        return (byte[]) this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Message other) {
            if (other.isBinary() != this.isBinary()) {
                return false;
            }

            if (other.isBinary()) {
                return Arrays.equals(this.asBinary(), other.asBinary());
            } else {
                return other.asString().equals(this.asString());
            }
        }
        return false;
    }

    /**
     * @return either the raw binary as-is, or a UTF-8 encoded string
     */
    public byte[] toBytes() {
        if (this.isBinary()) {
            return this.asBinary();
        } else {
            return this.asString().getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * @return a string representation of this class, not a decoded value.
     */
    @Override
    public String toString() {
        if (this.isBinary()) {
            return "Message(<binary>, " + this.asBinary().length + " bytes)";
        }

        return "Message(<string>, '" + this.asString() + "')";
    }

}

class MessageSerializer implements JsonSerializer<Message<?>> {
    @Override
    public @Nullable Message<?> deserialize(@NonNull JsonElement value, @NonNull Class<?> type, @NonNull Rson rson) throws JsonParseException {
        if (value.isJsonString()) {
            return new Message<>(value.getAsString());
        }

        JsonObject object = (JsonObject) value;
        if (object.getBoolean("binary")) {
            String b64 = object.getString("data");
            return new Message<>(Base64.getDecoder().decode(b64));
        } else {
            return new Message<>(object.getString("data"));
        }
    }

    @Override
    public JsonElement serialize(@NonNull Object value, @NonNull Rson rson) {
        Message<?> message = (Message<?>) value;

        if (message.isBinary()) {
            return new JsonObject()
                .put("binary", true)
                .put("data", Base64.getEncoder().encodeToString(message.asBinary()));
        } else {
            return new JsonString(message.asString());
        }
    }
}
