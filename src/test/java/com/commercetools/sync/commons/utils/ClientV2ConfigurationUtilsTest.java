package com.commercetools.sync.commons.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.defaultconfig.ServiceRegion;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ClientV2ConfigurationUtilsTest {
  private static final long TIMEOUT = 20000;
  private static final TimeUnit TIMEOUT_TIME_UNIT = TimeUnit.MILLISECONDS;

  @Test
  void createClient_WithProjectKey_WithClientCredentials_WithServiceConfig_ReturnsSphereClient() {
    final ClientCredentials clientCredentials =
        ClientCredentials.of().withClientId("clientId").withClientSecret("clientSecret").build();
    final ServiceRegion serviceRegion = ServiceRegion.GCP_EUROPE_WEST1;
    final String projectKey = "projectKey";
    final SphereClient sphereClient =
        ClientV2ConfigurationUtils.createClient(projectKey, clientCredentials, serviceRegion);

    assertThat(sphereClient.getConfig().getProjectKey()).isEqualTo("projectKey");
  }

  @Test
  void
      createClient_WithProjectKey_WithClientCredentials_WithServiceConfig_ReturnsBlockingSphereClient() {
    final ClientCredentials clientCredentials =
        ClientCredentials.of().withClientId("clientId").withClientSecret("clientSecret").build();
    final ServiceRegion serviceRegion = ServiceRegion.GCP_EUROPE_WEST1;
    final String projectKey = "projectKey";
    final SphereClient sphereClient =
        ClientV2ConfigurationUtils.createClient(
            projectKey, clientCredentials, serviceRegion, TIMEOUT, TIMEOUT_TIME_UNIT);

    assertThat(sphereClient instanceof BlockingSphereClient).isTrue();

    assertThat(sphereClient.getConfig().getProjectKey()).isEqualTo("projectKey");
  }
}
