package co.casterlabs.flux.client.realtime;

public interface FluxTubeListener {

    public default void onMembers(String[] memberIds) {}

    public default void onMemberJoin(String memberId) {}

    public default void onMemberLeave(String memberId) {}

    public default void onStringMessage(String from, String message) {}

    public default void onBinaryMessage(String from, byte[] message) {}

}
