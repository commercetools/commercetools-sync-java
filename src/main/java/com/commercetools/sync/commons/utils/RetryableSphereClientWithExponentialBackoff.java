package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.QueueSphereClientDecorator;
import io.sphere.sdk.client.RetrySphereClientDecorator;
import io.sphere.sdk.client.SphereAccessTokenSupplier;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.http.AsyncHttpClientAdapter;
import io.sphere.sdk.http.HttpClient;
import io.sphere.sdk.retry.RetryAction;
import io.sphere.sdk.retry.RetryContext;
import io.sphere.sdk.retry.RetryPredicate;
import io.sphere.sdk.retry.RetryRule;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class RetryableSphereClientWithExponentialBackoff {

    protected static SphereClient of(@Nonnull final SphereClientConfig clientConfig) {
        final SphereClientConfigOptions sphereClientConfigOptions =
                new SphereClientConfigOptions.Builder(clientConfig)
                        .build();
        return createClient(sphereClientConfigOptions);
    }

    protected static SphereClient of(@Nonnull final SphereClientConfig clientConfig,
                                  final long timeout,
                                  @Nonnull final TimeUnit timeUnit) {
        final SphereClientConfigOptions sphereClientConfigOptions =
                new SphereClientConfigOptions.Builder(clientConfig)
                        .withTimeout(timeout)
                        .withTimeUnit(timeUnit)
                        .build();
        return createBlockingSphereClient(sphereClientConfigOptions);
    }

    /**
     +     * Creates a {@link SphereClient}.
     +     *
     +     * @param clientConfigOptions the client configuration for the client.
     +     * @return the instantiated {@link SphereClient}.
     +     */
    protected static SphereClient createClient(@Nonnull final SphereClientConfigOptions clientConfigOptions) {
        final SphereClient underlyingClient = createSphereClient(clientConfigOptions.getClientConfig());
        return decorateSphereClient(underlyingClient, clientConfigOptions.getMaxRetries(),
            context -> calculateDurationWithExponentialRandomBackoff(context.getAttempt(),
            clientConfigOptions.getInitialDelay(), clientConfigOptions.getTimeout()),
            clientConfigOptions.getMaxParallelRequests());
    }

    /**
     * Creates a {@link BlockingSphereClient} with a custom {@code timeout} with a custom {@link TimeUnit}.
     *
     * @param clientConfigOptions the client configuration for the client with custom Timeout and TimeUnit.
     * @return the instantiated {@link BlockingSphereClient}.
     */
    protected static SphereClient createBlockingSphereClient(
            @Nonnull final SphereClientConfigOptions clientConfigOptions) {
        final SphereClient underlyingClient = createSphereClient(clientConfigOptions.getClientConfig());
        final SphereClient decoratedClient = decorateSphereClient(underlyingClient, clientConfigOptions.getMaxRetries(),
            context -> calculateDurationWithExponentialRandomBackoff(context.getAttempt(),
            clientConfigOptions.getInitialDelay(), clientConfigOptions.getTimeout()),
            clientConfigOptions.getMaxParallelRequests());

        return BlockingSphereClient.of(
                decoratedClient, clientConfigOptions.getTimeout(), clientConfigOptions.getTimeUnit());
    }


    protected static SphereClient createSphereClient(@Nonnull final SphereClientConfig clientConfig) {
        final HttpClient httpClient = getHttpClient();
        final SphereAccessTokenSupplier tokenSupplier =
                SphereAccessTokenSupplier.ofAutoRefresh(clientConfig, httpClient, false);
        return SphereClient.of(clientConfig, httpClient, tokenSupplier);
    }

    protected static SphereClient decorateSphereClient(
            @Nonnull final SphereClient underlyingClient,
            final long maxRetryAttempt,
            @Nonnull final Function<RetryContext, Duration> durationFunction,
            final int maxParallelRequests) {

        final SphereClient retryClient = withRetry(underlyingClient, maxRetryAttempt, durationFunction);
        return withLimitedParallelRequests(retryClient, maxParallelRequests);
    }

    /**
     * Gets an asynchronous {@link HttpClient} to be used by the {@link BlockingSphereClient}.
     *
     * @return {@link HttpClient}
     */
    private static HttpClient getHttpClient() {
        final AsyncHttpClient asyncHttpClient =
                new DefaultAsyncHttpClient(
                        new DefaultAsyncHttpClientConfig.Builder().build());
        return AsyncHttpClientAdapter.of(asyncHttpClient);
    }

    private static SphereClient withRetry(
            @Nonnull final SphereClient delegate,
            long maxRetryAttempt,
            @Nonnull final Function<RetryContext, Duration> durationFunction) {
        final RetryAction scheduledRetry = RetryAction.ofScheduledRetry(maxRetryAttempt, durationFunction);
        final RetryPredicate http5xxMatcher =
                RetryPredicate.ofMatchingStatusCodes(errCode -> errCode > 499 && errCode < 600);
        final List<RetryRule> retryRules = Collections.singletonList(RetryRule.of(http5xxMatcher, scheduledRetry));
        return RetrySphereClientDecorator.of(delegate, retryRules);
    }

    /**
     * Computes a exponential backoff time delay in milliseconds to be used in retries, the delay grows with failed
     * retry attempts count with a randomness interval.
     * (see: <a href=https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/>)
     * (see: <a href="http://dthain.blogspot.com/2009/02/exponential-backoff-in-distributed.html"/>)
     *
     * @param retryAttempt the number of attempts already tried by the client.
     * @return a duration in milliseconds, that grows with the number of failed attempts.
     */
    protected static Duration calculateDurationWithExponentialRandomBackoff(final long retryAttempt,
                                                                            long initialRetryDelay, long timeout) {
        final double exponentialFactor = Math.pow(2, retryAttempt - 1);
        final double jitter = 1 + Math.random();
        final long delay = (long)Math.min(initialRetryDelay * exponentialFactor * jitter, timeout);
        return Duration.ofMillis(delay);
    }

    private static SphereClient withLimitedParallelRequests(
            final SphereClient delegate, final int maxParallelRequests) {
        return QueueSphereClientDecorator.of(delegate, maxParallelRequests);
    }

    private RetryableSphereClientWithExponentialBackoff() {
    }

}
