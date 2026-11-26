package rockstar.client.network;


import rockstar.client.*;
import java.io.IOException;
import javax.annotation.Nonnull;
import rockstar.client.network.CRESTClientHttpResponse;
import rockstar.client.network.RawResponseHandler;

@FunctionalInterface
public interface ResponseHandler<R> {
    public static ResponseHandler<CRESTClientHttpResponse> internalMethod06946() {
        return typedValue035 -> typedValue035;
    }

    public static RawResponseHandler internalMethod06947() {
        return new RawResponseHandler();
    }

    public R handle(@Nonnull CRESTClientHttpResponse localValue1) throws IOException;
}

