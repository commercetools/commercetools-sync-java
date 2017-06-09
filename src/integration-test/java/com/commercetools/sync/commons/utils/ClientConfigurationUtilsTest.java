package com.commercetools.sync.commons.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientConfigurationUtilsTest {

    @Test
    public void createClient_WithSameConfig_ReturnsSameClient() {
        assertThat(ClientConfigurationUtils.createClient(SphereClientUtils.CTP_SOURCE_CLIENT_CONFIG))
            .isEqualTo(ClientConfigurationUtils.createClient(SphereClientUtils.CTP_SOURCE_CLIENT_CONFIG));
    }

    @Test
    public void createClient_WithDifferentConfig_ReturnDifferentClient() {
        assertThat(ClientConfigurationUtils.createClient(SphereClientUtils.CTP_SOURCE_CLIENT_CONFIG))
            .isNotEqualTo(ClientConfigurationUtils.createClient(SphereClientUtils.CTP_TARGET_CLIENT_CONFIG));
    }
}
