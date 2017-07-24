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

import static io.sphere.sdk.http.HttpStatusCode.BAD_GATEWAY_502;
import static io.sphere.sdk.http.HttpStatusCode.GATEWAY_TIMEOUT_504;
import static io.sphere.sdk.http.HttpStatusCode.SERVICE_UNAVAILABLE_503;

public class ClientConfigurationUtils {
    private static HttpClient httpClient;
    private static final long DEFAULT_TIMEOUT = 30000;
    private static final TimeUnit DEFAULT_TIMEOUT_TIME_UNIT = TimeUnit.MILLISECONDS;
    private static Map<SphereClientConfig, SphereClient> delegatesCache = new HashMap<>();

    /**
     * Creates a {@link BlockingSphereClient} with a custom {@code timeout} with a custom {@link TimeUnit}.
     *
     * @return the instanted {@link BlockingSphereClient}.
     */
    public static synchronized SphereClient createClient(@Nonnull final SphereClientConfig clientConfig,
                                                         final long timeout,
                                                         @Nonnull final TimeUnit timeUnit) {
        if (!delegatesCache.containsKey(clientConfig)) {
            final HttpClient httpClient = getHttpClient();
            final SphereAccessTokenSupplier tokenSupplier =
                SphereAccessTokenSupplier.ofAutoRefresh(clientConfig, httpClient, false);
            final SphereClient underlying = SphereClient.of(clientConfig, httpClient, tokenSupplier);
            final SphereClient retryClient = withRetry(underlying);
            final SphereClient limitedParallelRequestsClient = withLimitedParallelRequests(retryClient);
            delegatesCache.put(clientConfig, limitedParallelRequestsClient);
        }
        return BlockingSphereClient.of(delegatesCache.get(clientConfig), timeout, timeUnit);
    }

    /**
     * Creates a {@link BlockingSphereClient} with a default {@code timeout} value of 30 seconds.
     *
     * @return the instanted {@link BlockingSphereClient}.
     */
    public static SphereClient createClient(@Nonnull final SphereClientConfig clientConfig) {
        return createClient(clientConfig, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_TIME_UNIT);
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
                    new DefaultAsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true)
                                                              .setHandshakeTimeout((int) DEFAULT_TIMEOUT)
                                                              .build());
            httpClient = AsyncHttpClientAdapter.of(asyncHttpClient);
        }
        return httpClient;
    }

    private static SphereClient withRetry(final SphereClient delegate) {
        final int maxAttempts = 5;
        final RetryAction scheduledRetry = RetryAction
            .ofScheduledRetry(maxAttempts, context -> calculateVariableDelay(context.getAttempt()));
        final RetryPredicate http5xxMatcher = RetryPredicate
            .ofMatchingStatusCodes(BAD_GATEWAY_502, SERVICE_UNAVAILABLE_503, GATEWAY_TIMEOUT_504);
        final List<RetryRule> retryRules = Collections.singletonList(RetryRule.of(http5xxMatcher, scheduledRetry));
        return RetrySphereClientDecorator.of(delegate, retryRules);
    }

    /**
     * Computes a variable delay in seconds (grows with attempts count with a random component).
     *
     * @param triedAttempts the number of attempts already tried by the client.
     * @return a computed variable delay in seconds, that grows with the number of attempts with a random component.
     */
    private static Duration calculateVariableDelay(final long triedAttempts) {
        long timeoutInSeconds = TimeUnit.SECONDS.convert(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        final long randomNumberInRange = getRandomNumberInRange(50, timeoutInSeconds);
        final long timeoutMultipliedByTriedAttempts = timeoutInSeconds * triedAttempts;
        return Duration.ofSeconds(timeoutMultipliedByTriedAttempts + randomNumberInRange);
    }

    private static long getRandomNumberInRange(final long min, final long max) {
        return new Random().longs(min, (max + 1)).limit(1).findFirst().getAsLong();
    }

    private static SphereClient withLimitedParallelRequests(final SphereClient delegate) {
        final int maxParallelRequests = 20;
        return QueueSphereClientDecorator.of(delegate, maxParallelRequests);
    }
}
