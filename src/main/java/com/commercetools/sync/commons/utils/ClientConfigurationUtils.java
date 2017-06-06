package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.RetrySphereClientDecorator;
import io.sphere.sdk.client.ServerErrorException;
import io.sphere.sdk.client.SphereAccessTokenSupplier;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.http.AsyncHttpClientAdapter;
import io.sphere.sdk.http.HttpClient;
import io.sphere.sdk.retry.RetryContext;
import io.sphere.sdk.retry.RetryRule;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.sphere.sdk.retry.RetryAction.ofScheduledRetry;
import static io.sphere.sdk.retry.RetryPredicate.ofMatchingErrors;
import static java.util.Collections.singletonList;

public class ClientConfigurationUtils {
    private static BlockingSphereClient ctpClient;
    private static final long DEFAULT_TIMEOUT = 30;
    private static final TimeUnit DEFAULT_TIMEOUT_TIME_UNIT = TimeUnit.SECONDS;
    private static final int RETRY_ON_5XX_ATTEMPTS_LIMIT = 27;

    /**
     * Creates a {@link BlockingSphereClient} with a custom {@code timeout} with a custom {@link TimeUnit}.
     *
     * @return the instanted {@link BlockingSphereClient}.
     */
    public static synchronized BlockingSphereClient createClient(@Nonnull final SphereClientConfig clientConfig,
                                                                 final long timeout,
                                                                 @Nonnull final TimeUnit timeUnit) {
        if (ctpClient == null) {
            final HttpClient httpClient = newHttpClient();
            final SphereAccessTokenSupplier tokenSupplier =
                SphereAccessTokenSupplier.ofAutoRefresh(clientConfig, httpClient, false);
            final SphereClient underlying = SphereClient.of(clientConfig, httpClient, tokenSupplier);
            ctpClient = BlockingSphereClient.of(underlying, timeout, timeUnit);
        }
        return ctpClient;
    }

    /**
     * Return a {@link SphereClient} instance which will retry its request after it receives a server error from CTP.
     * A given {@code sphereClient} is wrapped by {@link RetrySphereClientDecorator} with proper rules initialized, and
     * then returned.
     *
     * @param sphereClient a delegate that will execute requests
     * @return {@link SphereClient} that will retry its requests on 5xx errors
     */
    @Nonnull
    public static SphereClient getRetryOn5xxClient(@Nonnull final SphereClient sphereClient) {
        return RetrySphereClientDecorator.of(sphereClient, singletonList(RetryRule.of(
            ofMatchingErrors(ServerErrorException.class),
            ofScheduledRetry(RETRY_ON_5XX_ATTEMPTS_LIMIT, ClientConfigurationUtils::retryOn5xxDurationCalculator))));
    }

    /**
     * Given a {@link RetryContext} it returns duration for a current attempt. The duration is calculated from a
     * following equation: <pre>duration = 2 ^ (((retryAttemptNumber - 1) / 3) + 1) [seconds]</pre>
     * It results in a sequence of durations: <pre>2s, 2s, 2s, 4s, 4s, 4s, 8s, 8s, 8s, ...</pre> for a subsequent retry
     * attempts starting from the 1st one.
     *
     * @param retryContext contains information about the latest failed attempt
     * @return a duration for a given attempt
     */
    @Nonnull
    static Duration retryOn5xxDurationCalculator(@Nonnull final RetryContext retryContext) {
        final double exponent = ((retryContext.getAttempt() - 1L) / 3L) + 1.0d;
        return Duration.ofSeconds((long) Math.pow(2.0d, exponent));
    }

    /**
     * Creates a {@link BlockingSphereClient} with a default {@code timeout} value of 30 seconds.
     *
     * @return the instanted {@link BlockingSphereClient}.
     */
    public static BlockingSphereClient createClient(@Nonnull final SphereClientConfig clientConfig) {
        return createClient(clientConfig, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_TIME_UNIT);
    }

    /**
     * Creates an asynchronous {@link HttpClient} to be used by the {@link BlockingSphereClient}.
     *
     * @return {@link HttpClient}
     */
    private static HttpClient newHttpClient() {
        final AsyncHttpClient asyncHttpClient =
            new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build());
        return AsyncHttpClientAdapter.of(asyncHttpClient);
    }
}
