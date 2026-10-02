package ar.edu.utn.dds.k3003.observability;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

public final class HttpClients {
  private HttpClients() {}
  public static JdkClientHttpRequestFactory requestFactory() {
    var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15)).build());
    factory.setReadTimeout(Duration.ofSeconds(180));
    return factory;
  }
}
