package co.casterlabs.flux.server.authenticator;

import java.net.URI;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;

class JWTAuthenticator implements Authenticator {
    private static final String SEND_CLAIM = "flux_send";
    private static final String RECV_CLAIM = "flux_recv";

    private JWTVerifier verifier;

    JWTAuthenticator(URI uri) {
        String issuer = uri.getUserInfo();
        String secret = uri.getHost();

        Algorithm signingAlg = Algorithm.HMAC256(secret);

        this.verifier = JWT.require(signingAlg)
            .withIssuer(issuer)
            .build();
    }

    @Override
    public Authentication authenticate(String token) throws AuthenticationException {
        Profile profile = Profiler.AUTHENTICATION.start();
        try {
            DecodedJWT jwt = this.verifier.verify(token);

            UserID id;
            String sub = jwt.getClaim("sub").asString();
            if (sub == null || sub.startsWith(Flux.META_SEQ)) {
                id = UserID.random();
            } else {
                id = new UserID(sub);
            }

            String[] sendRegexes = claimToArray(jwt.getClaim(SEND_CLAIM));
            String[] recvRegexes = claimToArray(jwt.getClaim(RECV_CLAIM));
            return new Authentication(id, sendRegexes, recvRegexes);
        } catch (JWTVerificationException e) {
            throw new AuthenticationException("Invalid token: " + e.getMessage());
        } finally {
            profile.end();
        }
    }

    private static String[] claimToArray(Claim claim) {
        if (claim == null) {
            return new String[0];
        } else if (claim.asString() != null) {
            return new String[] {
                    claim.asString()
            };
        } else if (claim.asArray(String.class) != null) {
            return claim.asArray(String.class);
        } else {
            return new String[0];
        }
    }

}
