package rockstar.client.network;


import rockstar.client.*;
import java.net.MalformedURLException;
import java.net.URL;
import rockstar.client.network.CRESTClientHttpRequest;

public class HttpHeadRequest
extends CRESTClientHttpRequest {
    public HttpHeadRequest(String string) throws MalformedURLException {
        super("HEAD", string);
    }

    public HttpHeadRequest(URL uRL) {
        super("HEAD", uRL);
    }
}

