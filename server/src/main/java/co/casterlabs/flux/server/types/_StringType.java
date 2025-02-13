package co.casterlabs.flux.server.types;

interface _StringType extends CharSequence {

    public String backing();

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
