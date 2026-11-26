package rockstar.client.network;


import rockstar.client.*;
import java.net.MalformedURLException;
import java.net.URL;
import rockstar.client.network.CRESTClientHttpRequest;

public class HttpDeleteRequest
extends CRESTClientHttpRequest {
    public HttpDeleteRequest(String string) throws MalformedURLException {
        super("DELETE", string);
    }

    public HttpDeleteRequest(URL uRL) {
        super("DELETE", uRL);
    }
}

