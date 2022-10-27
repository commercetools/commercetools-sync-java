package com.commercetools.sync.commons.utils;

import com.commercetools.api.client.ApiInternalLoggerFactory;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.compat.CompatSphereClient;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.error.ApiClientException;
import io.vrap.rmf.base.client.http.ErrorMiddleware;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.slf4j.event.Level;

public final class ClientV2ConfigurationUtils {

  /**
   * Creates a {@link SphereClient} with compact layer provided in SDK-v2, which enable different
   * middlewares to suport features like retry logic, NotFoundException handling and conversion of
   * API error to Exception.
   *
   * @param clientConfig the client configuration for the client.
   * @return the instantiated {@link SphereClient}.
   */
  protected static SphereClient createClient(@Nonnull final SphereClientConfig clientConfig) {
    ProjectApiRoot apiRoot =
        ApiRootBuilder.of()
            .defaultClient(
                ClientCredentials.of()
                    .withClientSecret(clientConfig.getClientSecret())
                    .withClientId(clientConfig.getClientId())
                    .build(),
                clientConfig.getAuthUrl() + "/oauth/token",
                clientConfig.getApiUrl())
            .withInternalLoggerFactory(
                ApiInternalLoggerFactory::get,
                Level.INFO,
                Level.INFO,
                Level.ERROR,
                Collections.singletonMap(ApiClientException.class, Level.INFO))
            .withErrorMiddleware(ErrorMiddleware.ExceptionMode.UNWRAP_COMPLETION_EXCEPTION)
            .addNotFoundExceptionMiddleware(Collections.singleton(ApiHttpMethod.GET))
            .withRetryMiddleware(
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

  private ClientV2ConfigurationUtils() {}
}
