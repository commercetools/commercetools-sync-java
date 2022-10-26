package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import javax.annotation.Nonnull;

protected final class CompactClientConfigurationUtils {

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

  private CompactClientConfigurationUtils() {}
}
