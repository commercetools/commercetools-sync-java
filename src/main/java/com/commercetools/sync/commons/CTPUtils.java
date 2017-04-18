package com.commercetools.sync.commons;

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
import java.util.concurrent.TimeUnit;

public class CTPUtils {
    private static BlockingSphereClient ctpClient;

    /**
     *
     * TODO: think about wrapping it to {@link io.sphere.sdk.client.QueueSphereClientDecorator}
     *
     * @return {@link BlockingSphereClient}
     */
    public static BlockingSphereClient createClient(@Nonnull final SphereClientConfig clientConfig,
                                                    final long timeout,
                                                    @Nonnull final TimeUnit timeUnit) {
        if (ctpClient == null) {
            final HttpClient httpClient = newHttpClient();
            final SphereAccessTokenSupplier tokenSupplier = SphereAccessTokenSupplier.ofAutoRefresh(clientConfig, httpClient, false);
            final SphereClient underlying = SphereClient.of(clientConfig, httpClient, tokenSupplier);
            ctpClient = BlockingSphereClient.of(underlying, timeout, timeUnit);
        }
        return ctpClient;
    }

    /**
     * Creates an asynchronous {@link HttpClient} to be used by the {@link BlockingSphereClient}.
     *
     * @return {@link HttpClient}
     */
    private static HttpClient newHttpClient() {
        final AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build());
        return AsyncHttpClientAdapter.of(asyncHttpClient);
    }
}
