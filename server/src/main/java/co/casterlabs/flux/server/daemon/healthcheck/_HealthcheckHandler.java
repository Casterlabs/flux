package co.casterlabs.flux.server.daemon.healthcheck;

import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.exceptions.DropConnectionException;
import co.casterlabs.rhs.protocol.exceptions.HttpException;
import co.casterlabs.rhs.protocol.http.HttpProtocol.HttpProtoHandler;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;

class _HealthcheckHandler implements HttpProtoHandler {

    @Override
    public HttpResponse handle(HttpSession session) throws HttpException, DropConnectionException {
        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK);
    }

}
