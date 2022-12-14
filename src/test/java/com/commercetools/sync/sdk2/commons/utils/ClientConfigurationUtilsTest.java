package com.commercetools.sync.sdk2.commons.utils;

import com.commercetools.api.client.ProjectApiRoot;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import io.vrap.rmf.base.client.oauth2.ClientCredentialsBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientConfigurationUtilsTest {

  @Test
  void createClient_WithConfig_ReturnsClient() {
    final ClientCredentials clientConfig =
        new ClientCredentialsBuilder().withClientId("client-id").withClientSecret("client-secret").withScopes("scopes").build();
    final ProjectApiRoot client = ClientConfigurationUtils.createClient("project-key", clientConfig, "", "");

    assertThat(client.getProjectKey()).isEqualTo("project-key");
  }
}
