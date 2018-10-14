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

public final class HeaderSphereClientDecorator extends SphereClientDecorator {


    private HeaderSphereClientDecorator(@Nonnull final SphereClient delegate) {
        super(delegate);
    }

    @Nonnull
    public static SphereClient of(@Nonnull final SphereClient delegate) {
        return new HeaderSphereClientDecorator(delegate);
    }

    @Override
    public <T> CompletionStage<T> execute(@Nonnull final SphereRequest<T> sphereRequest) {
        return super.execute(new HeaderSphereRequestDecorator<>(sphereRequest));
    }

    private static final class HeaderSphereRequestDecorator<T> extends SphereRequestDecorator<T> {
        HeaderSphereRequestDecorator(@Nonnull final SphereRequest<T> delegate) {
            super(delegate);
        }

        @Override
        public HttpRequestIntent httpRequestIntent() {
            return super.httpRequestIntent().plusHeader(LIB_NAME, LIB_VERSION);
        }
    }
}
