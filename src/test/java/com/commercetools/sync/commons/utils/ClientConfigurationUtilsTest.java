package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ClientConfigurationUtilsTest {
    private static final long TIMEOUT = 20000;
    private static final TimeUnit TIMEOUT_TIME_UNIT = TimeUnit.MILLISECONDS;

    @Test
    void createClient_WithConfig_ReturnsSphereClient() {
        final SphereClientConfig clientConfig =
                SphereClientConfig.of("project-key", "client-id", "client-secret");
        final SphereClient sphereClient = ClientConfigurationUtils.createClient(clientConfig);

        assertThat(sphereClient.getConfig().getProjectKey()).isEqualTo("project-key");
    }

    @Test
    void createClient_WithConfig_ReturnsBlockingSphereClient() {
        final SphereClientConfig clientConfig =
            SphereClientConfig.of("project-key", "client-id", "client-secret");
        final SphereClient sphereClient =
                ClientConfigurationUtils.createClient(clientConfig, TIMEOUT, TIMEOUT_TIME_UNIT);

        assertThat(sphereClient instanceof BlockingSphereClient).isTrue();

        assertThat(sphereClient.getConfig().getProjectKey()).isEqualTo("project-key");
    }
}
