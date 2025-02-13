package co.casterlabs.flux.server;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import co.casterlabs.flux.server.authenticator.Authenticator;
import co.casterlabs.flux.server.packet.outgoing.FFPacketKeepAlive;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;

public class Flux {
    public static final Authenticator authenticator = Authenticator.get(URI.create(System.getenv("FLUX_AUTHENTICATOR")));

    public static final boolean DEBUG = "true".equalsIgnoreCase(System.getenv("FLUX_DEBUG"));

    public static final String META_SEQ = "__flux:";

    // @formatter:off
    public static final UserID ANONYMOUS_PUBLISHER = new UserID(META_SEQ + "anonymous");
    public static final UserID META_PUBLISHER      = new UserID(META_SEQ + "meta"     );
    public static final UserID CONTROL_PUBLISHER   = new UserID(META_SEQ + "control"  );

    public static final TubeID CONTROL_TUBEID      = new TubeID(META_SEQ + "control");
    
    public static final Tube metaTubeCreate  = Tube.get(new TubeID(META_SEQ + "tube:create" ), true);
    public static final Tube metaTubeDestroy = Tube.get(new TubeID(META_SEQ + "tube:destroy"), true);
    public static final Tube metaBroadcast   = Tube.get(new TubeID(META_SEQ + "broadcast"   ), true);
    public static final Tube metaWildcard    = Tube.get(new TubeID(META_SEQ + "*"           ), true);
    // @formatter:on

    static {
        Thread.ofPlatform()
            .daemon(true)
            .name("Flux - KeepAlive Thread")
            .start(() -> {
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(15);
                    } catch (InterruptedException ignore) {}
                    Profile profile = Profiler.FLUX_KEEPALIVE.start();
                    metaBroadcast.send(FFPacketKeepAlive.INSTANCE);
                    profile.end();
                }
            });
        Thread.ofPlatform()
            .daemon(true)
            .name("Flux - GC Thread")
            .start(() -> {
                while (true) {
                    try {
                        TimeUnit.MINUTES.sleep(1);
                    } catch (InterruptedException ignore) {}
                    System.gc();
                }
            });
    }

}
