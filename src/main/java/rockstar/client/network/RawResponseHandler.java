package rockstar.client.network;


import rockstar.client.*;
import java.io.IOException;
import javax.annotation.Nonnull;

public class RawResponseHandler implements ResponseHandler<CRESTClientHttpResponse> {
   @Override
   public CRESTClientHttpResponse handle(@Nonnull CRESTClientHttpResponse localValue1) throws IOException {
      if (localValue1.internalMethod00588() >= 300) {
         throw new HttpRequestException(localValue1);
      } else {
         return localValue1;
      }
   }
}
