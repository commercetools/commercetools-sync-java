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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.sphere.sdk.http.HttpStatusCode.BAD_GATEWAY_502;
import static io.sphere.sdk.http.HttpStatusCode.GATEWAY_TIMEOUT_504;
import static io.sphere.sdk.http.HttpStatusCode.SERVICE_UNAVAILABLE_503;

public final class ClientConfigurationUtils {

    protected static final long DEFAULT_TIMEOUT = 30000;
    protected static final long DEFAULT_WAIT_BASE_MILLISECONDS = 200;
    protected static final TimeUnit DEFAULT_TIMEOUT_TIME_UNIT = TimeUnit.MILLISECONDS;
    protected static final int MAX_RETRIES = 5;
    private static final int MAX_PARALLEL_REQUESTS = 20;

    /**
     * Creates a {@link BlockingSphereClient} with a default {@code timeout} value of 30 seconds.
     *
     * @param clientConfig the client configuration for the client.
     * @return the instantiated {@link BlockingSphereClient}.
     */
    public static SphereClient createClient(@Nonnull final SphereClientConfig clientConfig) {
        return createClient(clientConfig, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_TIME_UNIT);
    }

    /**
     * Creates a {@link BlockingSphereClient} with a custom {@code timeout} with a custom {@link TimeUnit}.
     *
     * @param clientConfig the client configuration for the client.
     * @param timeout the timeout value for the client requests.
     * @param timeUnit the timeout time unit.
     * @return the instantiated {@link BlockingSphereClient}.
     */
    public static synchronized SphereClient createClient(@Nonnull final SphereClientConfig clientConfig,
                                                         final long timeout,
                                                         @Nonnull final TimeUnit timeUnit) {

        final SphereClient underlyingClient = createSphereClient(clientConfig);
        final SphereClient decoratedClient = decorateSphereClient(underlyingClient, MAX_RETRIES,
            context -> calculateDurationWithExponentialRandomBackoff(context.getAttempt()));

        return BlockingSphereClient.of(decoratedClient, timeout, timeUnit);
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
        @Nonnull final Function<RetryContext, Duration> durationFunction) {

        final SphereClient retryClient = withRetry(underlyingClient, maxRetryAttempt, durationFunction);
        return withLimitedParallelRequests(retryClient);
    }

    /**
     * Gets an asynchronous {@link HttpClient} to be used by the {@link BlockingSphereClient}.
     *
     * @return {@link HttpClient}
     */
    private static synchronized HttpClient getHttpClient() {
        final AsyncHttpClient asyncHttpClient =
            new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                                                .setHandshakeTimeout((int) DEFAULT_TIMEOUT)
                                                .build());
        return AsyncHttpClientAdapter.of(asyncHttpClient);
    }

    private static SphereClient withRetry(
        @Nonnull final SphereClient delegate,
        long maxRetryAttempt,
        @Nonnull final Function<RetryContext, Duration> durationFunction) {

        final RetryAction scheduledRetry = RetryAction.ofScheduledRetry(maxRetryAttempt, durationFunction);
        final RetryPredicate http5xxMatcher = RetryPredicate
            .ofMatchingStatusCodes(BAD_GATEWAY_502, SERVICE_UNAVAILABLE_503, GATEWAY_TIMEOUT_504);
        final List<RetryRule> retryRules = Collections.singletonList(RetryRule.of(http5xxMatcher, scheduledRetry));
        return RetrySphereClientDecorator.of(delegate, retryRules);
    }

    /**
     * Computes a exponential backoff time delay in milliseconds to be used in retries, the delay grows with failed
     * retry attempts count with a randomness interval (a.k.a full jitter).
     * (see: <a href=https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/>)
     *
     * @param retryAttempt the number of attempts already tried by the client.
     * @return a duration in milliseconds, that grows with the number of failed attempts.
     */
    protected static Duration calculateDurationWithExponentialRandomBackoff(final long retryAttempt) {
        final long sleep = DEFAULT_WAIT_BASE_MILLISECONDS * ((long) Math.pow(2, retryAttempt - 1));
        final long sleepWithJitter = ThreadLocalRandom.current().nextLong(0, sleep);

        return Duration.ofMillis(sleepWithJitter);
    }

    private static SphereClient withLimitedParallelRequests(final SphereClient delegate) {
        return QueueSphereClientDecorator.of(delegate, MAX_PARALLEL_REQUESTS);
    }

    private ClientConfigurationUtils() {
    }
}
