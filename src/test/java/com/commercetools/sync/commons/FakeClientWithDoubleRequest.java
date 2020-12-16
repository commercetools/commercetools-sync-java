package com.commercetools.sync.commons;

import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.utils.CompletableFutureUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionStage;

public class FakeClientWithDoubleRequest<T, U> implements SphereClient {

    private boolean isExecuted = false;
    private T firstResult;
    private U nextResult;
    private Throwable mockException;
    private int occurrence = 0;
    public FakeClientWithDoubleRequest(@Nonnull final T firstResult, @Nonnull final U nextResult) {
        this.firstResult = firstResult;
        this.nextResult = nextResult;
    }

    @Override
    public <T> CompletionStage execute(final SphereRequest<T> sphereRequest) {
        isExecuted = true;
        ++occurrence;

        if (occurrence==1 && firstResult != null) {
            return CompletableFutureUtils.successful(firstResult);
        }
        if (occurrence==2 && nextResult != null) {
            return CompletableFutureUtils.successful(nextResult);
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

    public int getOccurrence() {
        return occurrence;
    }
}