package co.casterlabs.flux.server.types;

import java.nio.charset.StandardCharsets;

interface _StringType extends CharSequence {

    public String backing();

    /**
     * @return a byte array with a max length of u16
     */
    default byte[] bytes() {
        return this.backing().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    default int length() {
        return this.backing().length();
    }

    @Override
    default char charAt(int index) {
        return this.backing().charAt(index);
    }

    @Override
    default CharSequence subSequence(int start, int end) {
        return this.backing().subSequence(start, end);
    }

    default boolean startsWith(String prefix) {
        return this.backing().startsWith(prefix);
    }

    default boolean matches(String regex) {
        return this.backing().matches(regex);
    }

}
