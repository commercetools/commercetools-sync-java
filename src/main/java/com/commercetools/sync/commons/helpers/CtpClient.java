package com.commercetools.sync.commons.helpers;


import com.commercetools.sync.commons.utils.ClientConfigurationUtils;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientConfig;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper that contains an instance of a {@link SphereClientConfig} and a {@link BlockingSphereClient}.
 */
public class CtpClient {
    private SphereClientConfig clientConfig;
    private BlockingSphereClient client;

    public CtpClient(@Nonnull final SphereClientConfig clientConfig,
                     final long timeout, @Nonnull final TimeUnit timeoutTimeUnit) {
        this.clientConfig = clientConfig;
        this.client = ClientConfigurationUtils.createClient(clientConfig, timeout, timeoutTimeUnit);
    }

    public CtpClient(@Nonnull final SphereClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.client = ClientConfigurationUtils.createClient(clientConfig);
    }

    public SphereClientConfig getClientConfig() {
        return clientConfig;
    }

    public BlockingSphereClient getClient() {
        return client;
    }
}
