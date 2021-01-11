package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.retry.RetryableSphereClientBuilder;
import io.sphere.sdk.http.AsyncHttpClientAdapter;
import io.sphere.sdk.http.HttpClient;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

public final class ClientConfigurationUtils {

  /**
   * Creates a {@link SphereClient} with retry logic which computes a exponential backoff time delay
   * in milliseconds.
   *
   * @param clientConfig the client configuration for the client.
   * @return the instantiated {@link SphereClient}.
   */
  public static SphereClient createClient(@Nonnull final SphereClientConfig clientConfig) {
    return RetryableSphereClientBuilder.of(clientConfig, getHttpClient()).build();
  }

  /**
   * Creates a {@link BlockingSphereClient} with a custom {@code timeout} with a custom {@link
   * TimeUnit}.
   *
   * @param clientConfig the client configuration for the client.
   * @param timeout the timeout value for the client requests.
   * @param timeUnit the timeout time unit.
   * @return the instantiated {@link BlockingSphereClient}.
   */
  public static SphereClient createClient(
      @Nonnull final SphereClientConfig clientConfig,
      final long timeout,
      @Nonnull final TimeUnit timeUnit) {
    return BlockingSphereClient.of(createClient(clientConfig), timeout, timeUnit);
  }

  /**
   * Gets an asynchronous {@link HttpClient} of `asynchttpclient` library, to be used by as an
   * underlying http client for the {@link SphereClient}.
   *
   * @return an asynchronous {@link HttpClient}
   */
  private static HttpClient getHttpClient() {
    final AsyncHttpClient asyncHttpClient =
        new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().build());
    return AsyncHttpClientAdapter.of(asyncHttpClient);
  }

  private ClientConfigurationUtils() {}
}
