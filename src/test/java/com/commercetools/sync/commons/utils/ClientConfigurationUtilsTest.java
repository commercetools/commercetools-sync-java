package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.retry.RetryContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ClientConfigurationUtilsTest {
    private static final long TIMEOUT = 20000;
    private static final TimeUnit TIMEOUT_TIME_UNIT = TimeUnit.MILLISECONDS;

    @Test
    void createClient_WithConfig_ReturnsSphereClient() {
        final SphereClientConfig clientConfig =
                SphereClientConfig.of("project-key", "client-id", "client-secret");
        final SphereClient sphereClient = ClientConfigurationUtils.createClient(clientConfig);

        assertThat(sphereClient instanceof SphereClient).isTrue();

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

    @Test
    void createClient_Simulate_Stuck_Sdk() {
        final Function<RetryContext, Duration> durationFunction = retryContext -> {
            throw new IllegalArgumentException();
        };
        final CompletionStage<Duration> durationCompletableFuture = CompletableFuture.supplyAsync(
            () -> durationFunction.apply(null));
        final Function<RetryContext, CompletionStage<Duration>> f = retryContext -> durationCompletableFuture;
        final Executor executor = Executors.newSingleThreadExecutor();
        final CompletableFuture<Duration> result = new CompletableFuture<>();
        try {
            final CompletionStage<Duration> initialCompletionStage = f.apply(null);
            initialCompletionStage.whenCompleteAsync((res, firstError) -> {
                final boolean isErrorCase = firstError != null;
                if (isErrorCase) {
                    // Error Case Exception should be thrown
                } else {
                    result.complete(res);
                }
            }, executor);
        } catch (final Throwable exception) { //necessary if f.apply() throws directly an exception
            result.completeExceptionally(exception);
        }
    }
}
