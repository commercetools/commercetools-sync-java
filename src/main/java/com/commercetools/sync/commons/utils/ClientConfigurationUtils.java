package com.commercetools.sync.commons.utils;

import com.commercetools.api.client.ApiInternalLoggerFactory;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.compat.CompatSphereClient;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.http.AsyncHttpClientAdapter;
import io.sphere.sdk.http.HttpClient;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.error.ApiClientException;
import io.vrap.rmf.base.client.http.ErrorMiddleware;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.slf4j.event.Level;

public final class ClientConfigurationUtils {

  /**
   * Creates a {@link SphereClient} with retry logic which computes a exponential backoff time delay
   * in milliseconds.
   *
   * @param clientConfig the client configuration for the client.
   * @return the instantiated {@link SphereClient}.
   */
  public static SphereClient createClient(@Nonnull final SphereClientConfig clientConfig) {
    ProjectApiRoot apiRoot =
        ApiRootBuilder.of(new ForkJoinPool(8))
            .defaultClient(
                ClientCredentials.of()
                    .withClientSecret(clientConfig.getClientSecret())
                    .withClientId(clientConfig.getClientId())
                    .build(),
                clientConfig.getAuthUrl() + "/oauth/token",
                clientConfig.getApiUrl())
                .withOAuthExecutorService(new ForkJoinPool(8))
                .withInternalLoggerFactory(
                ApiInternalLoggerFactory::get,
                Level.INFO,
                Level.INFO,
                Level.ERROR,
                Collections.singletonMap(ApiClientException.class, Level.INFO))
            .withErrorMiddleware(ErrorMiddleware.ExceptionMode.UNWRAP_COMPLETION_EXCEPTION)
            .addNotFoundExceptionMiddleware(Collections.singleton(ApiHttpMethod.GET))
            .withRetryMiddleware(
                    new ForkJoinPool(8),
                5, 200, 60000, Arrays.asList(500, 502, 503, 504), null, options -> options)
            .build(clientConfig.getProjectKey());
    return CompatSphereClient.of(apiRoot);
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
