package com.commercetools.sync.internals.helpers;

import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientDecorator;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.client.SphereRequestDecorator;
import io.sphere.sdk.http.HttpHeaders;
import javax.annotation.Nonnull;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.utils.SyncSolutionInfo.LIB_NAME;
import static com.commercetools.sync.commons.utils.SyncSolutionInfo.LIB_VERSION;

public final class CustomHeaderSphereClientDecorator extends SphereClientDecorator {

    private SphereRequestDecorator delegatedSphereRequestDecorator;

    private CustomHeaderSphereClientDecorator(@Nonnull final SphereClient sphereClient) {
        super(sphereClient);
    }

    @Nonnull
    public static SphereClient of(@Nonnull final SphereClient sphereClient) {
        return new CustomHeaderSphereClientDecorator(sphereClient);
    }

    @Override
    public <T> CompletionStage<T> execute(@Nonnull final SphereRequest<T> sphereRequest) {
        delegatedSphereRequestDecorator = new CustomHeaderSphereRequestDecorator<>(sphereRequest);
        return super.execute(delegatedSphereRequestDecorator);
    }

    protected HttpRequestIntent getHttpRequestIntent() {
        return delegatedSphereRequestDecorator.httpRequestIntent();
    }

    private static final class CustomHeaderSphereRequestDecorator<T> extends SphereRequestDecorator<T> {
        CustomHeaderSphereRequestDecorator(@Nonnull final SphereRequest<T> delegate) {
            super(delegate);
        }

        @Override
        public HttpRequestIntent httpRequestIntent() {
            final HttpHeaders headers =
                    HttpHeaders.of(HttpHeaders.USER_AGENT, LIB_NAME + "( ver : " + LIB_VERSION + ")");
            return super.httpRequestIntent().withHeaders(headers);
        }
    }
}
