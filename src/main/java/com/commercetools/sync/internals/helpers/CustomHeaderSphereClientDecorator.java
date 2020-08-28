package com.commercetools.sync.internals.helpers;

import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientDecorator;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.client.SphereRequestDecorator;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.utils.SyncSolutionInfo.LIB_NAME;
import static com.commercetools.sync.commons.utils.SyncSolutionInfo.LIB_VERSION;

public final class CustomHeaderSphereClientDecorator extends SphereClientDecorator {


    private CustomHeaderSphereClientDecorator(@Nonnull final SphereClient sphereClient) {
        super(sphereClient);
    }

    @Nonnull
    public static SphereClient of(@Nonnull final SphereClient sphereClient) {
        return new CustomHeaderSphereClientDecorator(sphereClient);
    }

    @Override
    public <T> CompletionStage<T> execute(@Nonnull final SphereRequest<T> sphereRequest) {
        return super.execute(new CustomHeaderSphereRequestDecorator<>(sphereRequest));
    }

    private static final class CustomHeaderSphereRequestDecorator<T> extends SphereRequestDecorator<T> {
        CustomHeaderSphereRequestDecorator(@Nonnull final SphereRequest<T> delegate) {
            super(delegate);
        }

        @Override
        public HttpRequestIntent httpRequestIntent() {
            return super.httpRequestIntent().plusHeader(LIB_NAME, LIB_VERSION);
        }
    }
}
