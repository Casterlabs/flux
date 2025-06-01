package co.casterlabs.flux.server.authenticator;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.flux.packets.types.TubeID;
import co.casterlabs.flux.packets.types.UserID;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;
import lombok.AllArgsConstructor;
import lombok.experimental.StandardException;

public interface Authenticator {

    public Authentication authenticate(String token) throws AuthenticationException;

    public static @Nullable Authenticator get(URI uri) {
        return switch (uri.getScheme()) {
            case "jwt" -> new JWTAuthenticator(uri);
            case "dummy" -> new DummyAuthenticator(uri);
            case "http", "https" -> new HTTPAuthenticator(uri);
            default -> throw new IllegalArgumentException("Unknown authenticator scheme: " + uri);
        };
    }

    @StandardException
    public static class AuthenticationException extends Exception {
        private static final long serialVersionUID = 7908136495875714095L;
    }

    @AllArgsConstructor
    public class Authentication {
        private final UserID id;
        private final String[] sendRegexes;
        private final String[] recvRegexes;

        private final Map<TubeID, Boolean> canSendCache = new HashMap<>();
        private final Map<TubeID, Boolean> canRecvCache = new HashMap<>();

        public UserID id() {
            return this.id;
        }

        public boolean canSend(TubeID id) {
            Profile profile = Profiler.AUTHENTICATION_CHECK_SEND.start();
            try {
                if (this.canSendCache.containsKey(id)) {
                    return this.canSendCache.get(id);
                }

                for (String aud : this.sendRegexes) {
                    if (id.matches(aud)) {
                        this.canSendCache.put(id, true);
                        return true;
                    }
                }

                this.canSendCache.put(id, false);
                return false;
            } finally {
                profile.end();
            }
        }

        public boolean canReceive(TubeID id) {
            Profile profile = Profiler.AUTHENTICATION_CHECK_RECV.start();
            try {
                if (this.canRecvCache.containsKey(id)) {
                    return this.canRecvCache.get(id);
                }

                for (String aud : this.recvRegexes) {
                    if (id.matches(aud)) {
                        this.canRecvCache.put(id, true);
                        return true;
                    }
                }

                this.canRecvCache.put(id, false);
                return false;
            } finally {
                profile.end();
            }
        }

    }

}
