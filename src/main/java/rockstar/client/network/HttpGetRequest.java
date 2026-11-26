package rockstar.client.network;


import rockstar.client.*;
import java.net.MalformedURLException;
import java.net.URL;
import rockstar.client.network.CRESTClientHttpRequest;

public class HttpGetRequest
extends CRESTClientHttpRequest {
    public HttpGetRequest(String string) throws MalformedURLException {
        super("GET", string);
    }

    public HttpGetRequest(URL uRL) {
        super("GET", uRL);
    }
}

