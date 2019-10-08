package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.UnresolvedReferencesService;
import com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.products.ProductDraft;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteWaitingToBeResolvedCustomObjects;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

class UnresolvedReferencesServiceImplIT {

    private UnresolvedReferencesService unresolvedReferencesService;

    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;


    @AfterEach
    void tearDown() {
        deleteWaitingToBeResolvedCustomObjects(CTP_TARGET_CLIENT);
    }

    @BeforeEach
    void setupTest() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (errorMessage, exception) -> {
                    errorCallBackMessages
                            .add(errorMessage);
                    errorCallBackExceptions
                            .add(exception);
                })
            .warningCallback(warningMessage ->
                    warningCallBackMessages
                            .add(warningMessage))
            .build();

        unresolvedReferencesService = new UnresolvedReferencesServiceImpl(productSyncOptions);
    }

    @Test
    void saveFetchAndDelete_WithoutExceptions_shouldWorkCorrectly() {
        // preparation
        final ProductDraft productDraft =
            SphereJsonUtils.readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, ProductDraft.class);

        final WaitingToBeResolved productDraftWithUnresolvedRefs =
                new WaitingToBeResolved(productDraft, asSet("foo", "bar"));

        // test
        final Optional<WaitingToBeResolved> result = unresolvedReferencesService
            .save(productDraftWithUnresolvedRefs)
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(result).hasValueSatisfying(waitingToBeResolved ->
            assertThat(waitingToBeResolved.getProductDraft()).isEqualTo(productDraft));

        // test
        final Set<WaitingToBeResolved> waitingDrafts = unresolvedReferencesService
            .fetch(singleton(productDraft.getKey()))
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(waitingDrafts).containsExactly(productDraftWithUnresolvedRefs);

        // test
        final Optional<WaitingToBeResolved> deletionResult = unresolvedReferencesService
            .delete(productDraft.getKey())
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(deletionResult).hasValueSatisfying(waitingToBeResolved ->
            assertThat(waitingToBeResolved.getProductDraft()).isEqualTo(productDraft));

        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }
}
