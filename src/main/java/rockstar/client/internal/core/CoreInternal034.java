package rockstar.client.internal.core;



import rockstar.client.network.*;
import rockstar.client.*;
import rockstar.client.network.CRESTClientHttpResponse;
import rockstar.client.network.HttpRequestException;

public class CoreInternal034
extends HttpRequestException {
    public CoreInternal034(CRESTClientHttpResponse typedValue035, String string) {
        super(typedValue035, "status: " + typedValue035.internalMethod00588() + " " + typedValue035.internalMethod06484() + ", message: " + string);
    }
}

