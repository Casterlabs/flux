package co.casterlabs.flux.packets.types;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import lombok.NonNull;

class _StringType implements CharSequence {
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private final String stringVersion;
    private final byte[] byteVersion;

    private _StringType(String string, byte[] bytes) {
        if (bytes.length > 0xFFFF) {
            throw new IllegalArgumentException("Tube ID cannot be longer than 65535 bytes.");
        }

        this.stringVersion = string;
        this.byteVersion = bytes;
    }

    _StringType(@NonNull String backing) {
        this(backing, backing.getBytes(CHARSET));
    }

    _StringType(@NonNull byte[] backing) {
        this(new String(backing, CHARSET), backing);
    }

    /**
     * @return a byte array with a max length of u16, encoded as UTF-8
     */
    public final byte[] bytes() {
        return this.byteVersion;
    }

    @Override
    public final int length() {
        return this.stringVersion.length();
    }

    @Override
    public final char charAt(int index) {
        return this.stringVersion.charAt(index);
    }

    @Override
    public final CharSequence subSequence(int start, int end) {
        return this.stringVersion.subSequence(start, end);
    }

    public final boolean startsWith(String prefix) {
        return this.stringVersion.startsWith(prefix);
    }

    public final boolean matches(String regex) {
        return this.stringVersion.matches(regex);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return this.stringVersion.hashCode();
    }

    @Override
    public final String toString() {
        return this.stringVersion;
    }

}
