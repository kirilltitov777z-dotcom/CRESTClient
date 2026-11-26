package rockstar.client.network;


import rockstar.client.*;
import rockstar.client.network.CRESTClientHttpResponse;
import rockstar.client.network.HttpRequestException;

public class HttpRetryException
extends HttpRequestException {
    public HttpRetryException(CRESTClientHttpResponse typedValue035) {
        super(typedValue035, "Maximum retry count exceeded");
    }

    public HttpRetryException(CRESTClientHttpResponse typedValue035, String string) {
        super(typedValue035, string);
    }
}

