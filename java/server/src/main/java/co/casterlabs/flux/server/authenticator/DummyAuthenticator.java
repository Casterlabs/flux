package co.casterlabs.flux.server.authenticator;

import java.net.URI;

import co.casterlabs.flux.packets.types.UserID;

class DummyAuthenticator implements Authenticator {

    DummyAuthenticator(URI uri) {}

    @Override
    public Authentication authenticate(String token) throws AuthenticationException {
        return new Authentication(
            UserID.random(),
            new String[] {
                    ".*"
            },
            new String[] {
                    ".*"
            }
        );
    }

}
