package com.commercetools.sync.sdk2.commons.utils;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import java.util.Arrays;
import javax.annotation.Nonnull;

public final class ClientConfigurationUtils {

  /**
   * Creates a {@link ProjectApiRoot}.
   *
   * @param projectKey commercetools project key
   * @param credentials api credentials with clientId, clientSecret and scopes
   * @param authUrl the auth url of the commercetools platform that project is bound
   * @param apiUrl the api url of the commercetools platform that project is bound
   * @return the instantiated {@link ProjectApiRoot}.
   */
  public static ProjectApiRoot createClient(
      @Nonnull final String projectKey,
      @Nonnull final ClientCredentials credentials,
      @Nonnull final String authUrl,
      @Nonnull final String apiUrl) {

    return ApiRootBuilder.of()
        .defaultClient(credentials, authUrl, apiUrl)
        .withRetryMiddleware(5, Arrays.asList(500, 502, 503, 504))
        .build(projectKey);
  }
}
