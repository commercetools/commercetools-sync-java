package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereAccessTokenSupplier;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.http.AsyncHttpClientAdapter;
import io.sphere.sdk.http.HttpClient;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ClientConfigurationUtils {
    private static HttpClient httpClient;
    private static final long DEFAULT_TIMEOUT = 30;
    private static final TimeUnit DEFAULT_TIMEOUT_TIME_UNIT = TimeUnit.SECONDS;
    private static Map<SphereClientConfig, SphereClient> delegatesCache = new HashMap<>();

    /**
     * Creates a {@link BlockingSphereClient} with a custom {@code timeout} with a custom {@link TimeUnit}.
     *
     * @return the instanted {@link BlockingSphereClient}.
     */
    public static synchronized BlockingSphereClient createClient(@Nonnull final SphereClientConfig clientConfig,
                                                                 final long timeout,
                                                                 @Nonnull final TimeUnit timeUnit) {
        if (!delegatesCache.containsKey(clientConfig)) {
            final HttpClient httpClient = getHttpClient();
            final SphereAccessTokenSupplier tokenSupplier =
                SphereAccessTokenSupplier.ofAutoRefresh(clientConfig, httpClient, false);
            final SphereClient underlying = SphereClient.of(clientConfig, httpClient, tokenSupplier);
            delegatesCache.put(clientConfig, underlying);
        }
        return BlockingSphereClient.of(delegatesCache.get(clientConfig), timeout, timeUnit);
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
     * Gets an asynchronous {@link HttpClient} to be used by the {@link BlockingSphereClient}.
     * Client is created during first invocation and then cached.
     *
     * @return {@link HttpClient}
     */
    private static synchronized HttpClient getHttpClient() {
        if (httpClient == null) {
            final AsyncHttpClient asyncHttpClient =
                new DefaultAsyncHttpClient(
                    new DefaultAsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build());
            httpClient = AsyncHttpClientAdapter.of(asyncHttpClient);
        }
        return httpClient;
    }
}
