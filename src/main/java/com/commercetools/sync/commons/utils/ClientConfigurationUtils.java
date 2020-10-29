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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.sphere.sdk.http.HttpStatusCode.BAD_GATEWAY_502;
import static io.sphere.sdk.http.HttpStatusCode.GATEWAY_TIMEOUT_504;
import static io.sphere.sdk.http.HttpStatusCode.SERVICE_UNAVAILABLE_503;

public final class ClientConfigurationUtils {
    private static HttpClient httpClient;
    protected static final long DEFAULT_TIMEOUT = 30000;
    protected static final long MAX_TIMEOUT = 50000;
    protected static final TimeUnit DEFAULT_TIMEOUT_TIME_UNIT = TimeUnit.MILLISECONDS;
    protected static final int MAX_RETRIES = 5;
    private static final int MAX_PARALLEL_REQUESTS = 20;

    private static final Map<SphereClientConfig, SphereClient> delegatesCache = new HashMap<>();

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
        if (!delegatesCache.containsKey(clientConfig)) {
            final SphereClient underlyingClient = createSphereClient(clientConfig);
            final SphereClient decoratedClient = decorateSphereClient(underlyingClient,
                MAX_RETRIES,
                context -> calculateDurationWithExponentialRandomBackoff(context.getAttempt()));

            delegatesCache.put(clientConfig, decoratedClient);
        }
        return BlockingSphereClient.of(delegatesCache.get(clientConfig), timeout, timeUnit);
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
     * Client is created during first invocation and then cached.
     *
     * @return {@link HttpClient}
     */
    private static synchronized HttpClient getHttpClient() {
        if (httpClient == null) {
            final AsyncHttpClient asyncHttpClient =
                new DefaultAsyncHttpClient(
                    new DefaultAsyncHttpClientConfig.Builder()
                                                    .setHandshakeTimeout((int) DEFAULT_TIMEOUT)
                                                    .build());
            httpClient = AsyncHttpClientAdapter.of(asyncHttpClient);
        }
        return httpClient;
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
     * Computes a exponential backoff time delay in seconds, that grows with failed retry attempts count
     * with a random interval between {@code DEFAULT_TIMEOUT} and {@code MAX_TIMEOUT}.
     *
     * @param retryAttempt the number of attempts already tried by the client.
     * @return a computed variable delay in seconds, that grows with the number of failed attempts.
     */
    protected static Duration calculateDurationWithExponentialRandomBackoff(final Long retryAttempt) {
        final long timeoutInSeconds = TimeUnit.SECONDS.convert(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_TIME_UNIT);
        final long maxTimeoutInSeconds = TimeUnit.SECONDS.convert(MAX_TIMEOUT, DEFAULT_TIMEOUT_TIME_UNIT);
        final long randomNumberInRange = getRandomNumberInRange(timeoutInSeconds, maxTimeoutInSeconds);

        final long failedRetryAttempt = retryAttempt - 1; // first call is not a retry.
        final long timeoutMultipliedByFailedAttempts = timeoutInSeconds * failedRetryAttempt;

        return Duration.ofSeconds(timeoutMultipliedByFailedAttempts + randomNumberInRange);
    }

    private static long getRandomNumberInRange(final long min, final long max) {
        return new Random().longs(min + 1, max + 1).limit(1).findFirst().orElse(min);
    }

    private static SphereClient withLimitedParallelRequests(final SphereClient delegate) {
        return QueueSphereClientDecorator.of(delegate, MAX_PARALLEL_REQUESTS);
    }

    private ClientConfigurationUtils() {
    }
}
