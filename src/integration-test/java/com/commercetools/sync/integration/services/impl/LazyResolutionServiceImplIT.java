package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.LazyResolutionService;
import com.commercetools.sync.services.impl.LazyResolutionServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.DeleteCommand;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.products.ProductDraft;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LazyResolutionServiceImplIT {

    private LazyResolutionService lazyResolutionService;

    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;


    @BeforeAll
    static void setup() {
    }


    @AfterAll
    static void tearDown() {
        deleteCustomObjects(CTP_TARGET_CLIENT);
    }

    private static void deleteCustomObjects(final SphereClient sphereClient) {
        final CustomObjectQuery<WaitingToBeResolved> customObjectQuery = CustomObjectQuery
            .of(WaitingToBeResolved.class).byContainer("[commercetools-sync-java]"
                + "-products-with-unresolved-references");
        List<CustomObject<WaitingToBeResolved>> existingCOs = sphereClient
                .execute(customObjectQuery).toCompletableFuture().join().getResults();

        existingCOs.forEach(obj -> {
            DeleteCommand<CustomObject<WaitingToBeResolved>> deleteCommand = CustomObjectDeleteCommand
                    .of(obj, WaitingToBeResolved.class);
            sphereClient.execute(deleteCommand).toCompletableFuture().join();
        });
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

        lazyResolutionService = new LazyResolutionServiceImpl(productSyncOptions);
    }


    @Test
    void createOrUpdateCustomObject_createNewCustomObject() {
        // preparation
        final ProductDraft productDraft =
            SphereJsonUtils.readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, ProductDraft.class);

        final WaitingToBeResolved productDraftWithUnresolvedRefs =
                new WaitingToBeResolved(productDraft, asSet("foo", "bar"));

        // test
        final Optional<CustomObject<WaitingToBeResolved>> result =
            lazyResolutionService.save(productDraftWithUnresolvedRefs)
                                 .toCompletableFuture()
                                 .join();

        final Set<CustomObject<WaitingToBeResolved>> customObjectOptional =
            lazyResolutionService.fetch(asSet(productDraft.getKey())).toCompletableFuture().join();

        final Optional<CustomObject<WaitingToBeResolved>> join = lazyResolutionService
            .delete(productDraft.getKey()).toCompletableFuture().join();

        // assertions
        assertTrue(result.isPresent());
        CustomObject<WaitingToBeResolved> createdCustomObject = result.get();
        assertThat(createdCustomObject.getKey()).isEqualTo("test-co-key");
        assertThat(createdCustomObject.getContainer()).isEqualTo("[commercetools-sync-java]"
            + "-products-with-unresolved-references");
    }

    @Test
    void fetchCustomObject_shouldReturnCorrectCustomObject() {
/*        // preparation
        final ProductDraft productDraft =
            SphereJsonUtils.readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, ProductDraft.class);

        final NonResolvedReferencesCustomObject valueObject =
            new NonResolvedReferencesCustomObject("test-product-key", productDraft,
                        null);

        final CustomObjectDraft<NonResolvedReferencesCustomObject> customObjectDraft =
                CustomObjectDraft.ofUnversionedUpsert(containerKey, "test-co-key", valueObject,
                        NonResolvedReferencesCustomObject.class);
        customObjectService
                .createOrUpdateCustomObject(customObjectDraft).toCompletableFuture().join();

        // test
        Optional<CustomObject<NonResolvedReferencesCustomObject>> result = customObjectService
                .fetchCustomObject("test-co-key").toCompletableFuture().join();

        // assertions
        assertTrue(result.isPresent());
        CustomObject<NonResolvedReferencesCustomObject> savedCustomObject = result.get();
        assertThat(savedCustomObject.getContainer()).isEqualTo(containerKey);*/
    }


    @Test
    void deleteCustomObject_shouldReturnCorrectCustomObject() {
/*        // preparation
        final ProductDraft productDraft =
            SphereJsonUtils.readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, ProductDraft.class);

        final NonResolvedReferencesCustomObject valueObject =
                new NonResolvedReferencesCustomObject("test-product-key", productDraft,
                        null);

        CustomObjectDraft<NonResolvedReferencesCustomObject> customObjectDraft =
                CustomObjectDraft.ofUnversionedUpsert(containerKey, "test-co-key", valueObject,
                        NonResolvedReferencesCustomObject.class);
        Optional<CustomObject<NonResolvedReferencesCustomObject>> optionalCustomObject = customObjectService
                .createOrUpdateCustomObject(customObjectDraft).toCompletableFuture().join();

        // test
        Optional<CustomObject<NonResolvedReferencesCustomObject>> result = customObjectService
                .deleteCustomObject(optionalCustomObject.get()).toCompletableFuture().join();

        // assertions
        assertTrue(result.isPresent());
        CustomObject<NonResolvedReferencesCustomObject> deletedCustomObject = result.get();
        assertThat(deletedCustomObject.getContainer()).isEqualTo(containerKey);

        Optional<CustomObject<NonResolvedReferencesCustomObject>> nonExistingObj = customObjectService
                .fetchCustomObject("test-co-key").toCompletableFuture().join();
        assertFalse(nonExistingObj.isPresent());*/
    }

}
