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
import java.util.stream.IntStream;

/**
 * To create a Sphere Client with retry logic which computes a exponential backoff time delay in milliseconds.
 * And handle all the configurations for the creation of client.
 */
public final class RetryableSphereClientWithExponentialBackoff {
    protected static final long DEFAULT_TIMEOUT = 30000;
    protected static final long INITIAL_RETRY_DELAY = 200;
    protected static final TimeUnit DEFAULT_TIMEOUT_TIME_UNIT = TimeUnit.MILLISECONDS;
    protected static final int MAX_RETRIES = 5;
    protected static final int MAX_PARALLEL_REQUESTS = 20;
    private static final int[] DEFAULT_RETRY_ERROR_STATUS_CODES = {500, 502, 503, 504};

    private SphereClientConfig clientConfig;
    private long timeout;
    private long initialDelay;
    private TimeUnit timeUnit;
    private int maxRetries;
    private int maxParallelRequests;
    private int[] retryErrorStatusCodes;

    private RetryableSphereClientWithExponentialBackoff(@Nonnull final SphereClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.timeout = DEFAULT_TIMEOUT;
        this.timeUnit = DEFAULT_TIMEOUT_TIME_UNIT;
        this.initialDelay = INITIAL_RETRY_DELAY;
        this.maxRetries = MAX_RETRIES;
        this.maxParallelRequests = MAX_PARALLEL_REQUESTS;
        this.retryErrorStatusCodes = DEFAULT_RETRY_ERROR_STATUS_CODES;
    }

    /**
     * Creates a new instance of {@link RetryableSphereClientWithExponentialBackoff} given a {@link SphereClientConfig}
     * responsible for creation of a SphereClient.
     *
     * @param clientConfig the client configuration for the client.
     * @return the instantiated {@link RetryableSphereClientWithExponentialBackoff}.
     */
    public static RetryableSphereClientWithExponentialBackoff of(
            @Nonnull final SphereClientConfig clientConfig) {
        return new RetryableSphereClientWithExponentialBackoff(clientConfig);
    }

    /**
     * Sets the timeout value.
     * @param timeout - build with timeout value.
     * @return {@link RetryableSphereClientWithExponentialBackoff} with given timeout value.
     */
    public RetryableSphereClientWithExponentialBackoff withTimeout(final long timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Sets the initialDelay value.
     * @param initialDelay - build with initialDelay value.
     * @return {@link RetryableSphereClientWithExponentialBackoff} with given initialDelay value.
     */
    public RetryableSphereClientWithExponentialBackoff withInitialDelay(final long initialDelay) {
        if (initialDelay < timeout) {
            this.initialDelay = initialDelay;
        }
        return this;
    }

    /**
     * Sets the timeUnit.
     * @param timeUnit - build with timeUnit value.
     * @return {@link RetryableSphereClientWithExponentialBackoff} with given timeUnit value.
     */
    public RetryableSphereClientWithExponentialBackoff withTimeUnit(@Nonnull final TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }

    /**
     * Sets the Max Retry value.
     * @param maxRetries - build with maxRetries value.
     * @return {@link RetryableSphereClientWithExponentialBackoff} with given maxRetries value.
     */
    public RetryableSphereClientWithExponentialBackoff withMaxRetries(final int maxRetries) {
        if (maxRetries > 1) {
            this.maxRetries = maxRetries;
        }
        return this;
    }

    /**
     * Sets the Max Parallel Requests value.
     * @param maxParallelRequests - build with maxParallelRequests value.
     * @return {@link RetryableSphereClientWithExponentialBackoff} with given maxParallelRequests value.
     */
    public RetryableSphereClientWithExponentialBackoff withMaxParallelRequests(final int maxParallelRequests) {
        this.maxParallelRequests = maxParallelRequests;
        return this;
    }

    /**
     * Sets the Retry Error Status Codes.
     * @param retryErrorStatusCodes - build with retryErrorStatusCodes.
     * @return {@link RetryableSphereClientWithExponentialBackoff} with given retryErrorStatusCodes.
     */
    public RetryableSphereClientWithExponentialBackoff withRetryErrorStatusCodes(
            final int[] retryErrorStatusCodes) {
        this.retryErrorStatusCodes = retryErrorStatusCodes.clone();
        return this;
    }

    /**
     * creates a SphereClient using the class configuration values.
     * @return {@link SphereClient}
     */
    public SphereClient build() {
        return createClient();
    }

    /**
     * Creates a {@link SphereClient}.
     * @return the instantiated {@link SphereClient}.
     */
    protected SphereClient createClient() {
        final SphereClient underlyingClient = createSphereClient(clientConfig);
        return decorateSphereClient(underlyingClient, maxRetries,
            context -> calculateDurationWithExponentialRandomBackoff(context.getAttempt(),
            initialDelay, timeout), maxParallelRequests);
    }

    protected SphereClient createSphereClient(@Nonnull final SphereClientConfig clientConfig) {
        final HttpClient httpClient = getHttpClient();
        final SphereAccessTokenSupplier tokenSupplier =
                SphereAccessTokenSupplier.ofAutoRefresh(clientConfig, httpClient, false);
        return SphereClient.of(clientConfig, httpClient, tokenSupplier);
    }

    protected SphereClient decorateSphereClient(
            @Nonnull final SphereClient underlyingClient,
            final long maxRetryAttempt,
            @Nonnull final Function<RetryContext, Duration> durationFunction,
            final int maxParallelRequests) {
        final SphereClient retryClient = withRetry(underlyingClient, maxRetryAttempt, durationFunction);
        return withLimitedParallelRequests(retryClient, maxParallelRequests);
    }

    /**
     * Gets an asynchronous {@link HttpClient} to be used by the {@link BlockingSphereClient}.
     * @return {@link HttpClient}
     */
    public HttpClient getHttpClient() {
        final AsyncHttpClient asyncHttpClient =
                new DefaultAsyncHttpClient(
                        new DefaultAsyncHttpClientConfig.Builder().build());
        return AsyncHttpClientAdapter.of(asyncHttpClient);
    }

    private SphereClient withRetry(
            @Nonnull final SphereClient delegate,
            long maxRetryAttempt,
            @Nonnull final Function<RetryContext, Duration> durationFunction) {
        final RetryAction scheduledRetry = RetryAction.ofScheduledRetry(maxRetryAttempt, durationFunction);
        final RetryPredicate http5xxMatcher = RetryPredicate.ofMatchingStatusCodes(
            errCode -> IntStream.of(retryErrorStatusCodes).anyMatch(i -> i == errCode));
        final List<RetryRule> retryRules = Collections.singletonList(RetryRule.of(http5xxMatcher, scheduledRetry));
        return RetrySphereClientDecorator.of(delegate, retryRules);
    }

    /**
     * Computes a exponential backoff time delay in milliseconds to be used in retries, the delay grows with failed
     * retry attempts count with a randomness interval.
     * (see: <a href="https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter"/>)
     * (see: <a href="http://dthain.blogspot.com/2009/02/exponential-backoff-in-distributed.html"/>)
     *
     * @param retryAttempt the number of attempts already tried by the client.
     * @param initialRetryDelay the initial Retry delay.
     * @param timeout the timeout in milliseconds.
     * @return a duration in milliseconds, that grows with the number of failed attempts.
     */
    public Duration calculateDurationWithExponentialRandomBackoff(final long retryAttempt,
                                                                  final long initialRetryDelay,
                                                                  final long timeout) {
        final double exponentialFactor = Math.pow(2, retryAttempt - 1);
        final double jitter = 1 + Math.random();
        final long delay = (long)Math.min(initialRetryDelay * exponentialFactor * jitter, timeout);
        return Duration.ofMillis(delay);
    }

    private SphereClient withLimitedParallelRequests(final SphereClient delegate, final int maxParallelRequests) {
        return QueueSphereClientDecorator.of(delegate, maxParallelRequests);
    }

}
