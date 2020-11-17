package com.commercetools.sync.services.impl;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.FakeClient;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;


class CategoryServiceImplTest {

    private List<String> errorMessages;
    private List<Throwable> errorExceptions;
    private static final String oldCategoryKey = "oldCategoryKey";
    private CategoryServiceImpl service;
    private CategorySyncOptions categorySyncOptions;

    @BeforeEach
    void setUp() {
        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
    }

    @Test
    void fetchCategory_WithBadGateWayExceptionAlways_ShouldFail() {
        final FakeClient<Category> fakeProductClient = new FakeClient(new BadGatewayException());

        initMockService(fakeProductClient);

        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(service.fetchCategory(oldCategoryKey))
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BadGatewayException.class);
    }

    @Test
    void fetchMatchingCategoriesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {

        final FakeClient<Category> fakeCategoryClient = new FakeClient(new BadGatewayException());

        initMockService(fakeCategoryClient);

        final Set<String> keys =  new HashSet<>();
        keys.add(oldCategoryKey);

        // test and assert
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(service.fetchMatchingCategoriesByKeys(keys))
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BadGatewayException.class);
    }

    private void initMockService(@Nonnull final SphereClient mockSphereClient) {
        categorySyncOptions = CategorySyncOptionsBuilder
                .of(mockSphereClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    errorExceptions.add(exception.getCause());
                }).build();

        service = new CategoryServiceImpl(categorySyncOptions);
    }
}