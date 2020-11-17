package com.commercetools.sync.commons;

import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.utils.CompletableFutureUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionStage;

public class FakeClient<T> implements SphereClient {

    private boolean isExecuted = false;
    private T mockResult;
    private Throwable mockException;

    public FakeClient(@Nonnull final T mockResult) {
        this.mockResult = mockResult;
    }

    public FakeClient(@Nonnull final Throwable mockException) {
        this.mockException = mockException;
    }

    @Override
    public <T> CompletionStage<T> execute(final SphereRequest<T> sphereRequest) {
        isExecuted = true;
        if (mockResult != null) {
            return CompletableFutureUtils.successful((T)mockResult);
        }
        return CompletableFutureUtils.failed(mockException);
    }

    @Override
    public void close() {

    }

    @Override
    public SphereApiConfig getConfig() {
        return null;
    }

    public boolean isExecuted() {
        return isExecuted;
    }
}