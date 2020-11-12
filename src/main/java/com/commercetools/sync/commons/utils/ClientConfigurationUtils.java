package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public final class ClientConfigurationUtils {

    /**
     * Creates a {@link SphereClient} with a default {@code timeout} value of 30 seconds.
     *
     * @param clientConfig the client configuration for the client.
     * @return the instantiated {@link SphereClient}.
     */
    public static SphereClient createClient(@Nonnull final SphereClientConfig clientConfig) {
        return RetryableSphereClientWithExponentialBackoff.of(clientConfig);
    }

    /**
     * Creates a {@link BlockingSphereClient} with a custom {@code timeout} with a custom {@link TimeUnit}.
     *
     * @param clientConfig the client configuration for the client.
     * @param timeout the timeout value for the client requests.
     * @param timeUnit the timeout time unit.
     * @return the instantiated {@link BlockingSphereClient}.
     */
    public static SphereClient createClient(@Nonnull final SphereClientConfig clientConfig,
                                                         final long timeout,
                                                         @Nonnull final TimeUnit timeUnit) {
        return RetryableSphereClientWithExponentialBackoff.of(clientConfig, timeout, timeUnit);
    }

    private ClientConfigurationUtils() {
    }
}
