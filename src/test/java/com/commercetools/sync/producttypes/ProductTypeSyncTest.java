package com.commercetools.sync.producttypes;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeDescription;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.producttypes.ProductType.referenceOfId;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.list;
import static org.assertj.core.util.Sets.newLinkedHashSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductTypeSyncTest {

    @Test
    void sync_WithEmptyAttributeDefinitions_ShouldSyncCorrectly() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "foo",
            "name",
            "desc",
            emptyList()
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();


        final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);

        final ProductType existingProductType = mock(ProductType.class);
        when(existingProductType.getKey()).thenReturn(newProductTypeDraft.getKey());

        when(mockProductTypeService.fetchMatchingProductTypesByKeys(singleton(newProductTypeDraft.getKey())))
            .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
        when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
        when(mockProductTypeService.updateProductType(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(existingProductType));
        when(mockProductTypeService.cacheKeysToIds(anySet()))
            .thenReturn(CompletableFuture.completedFuture(emptyMap()));

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);
    }

    @Test
    void sync_WithNullAttributeDefinitions_ShouldSyncCorrectly() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "foo",
            "name",
            "desc",
            null
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<UpdateAction> actions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .beforeUpdateCallback((generatedActions, draft, productType) -> {
                actions.addAll(generatedActions);
                return generatedActions;
            })
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();


        final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);

        final ProductType existingProductType = mock(ProductType.class);
        when(existingProductType.getKey()).thenReturn(newProductTypeDraft.getKey());

        when(mockProductTypeService.fetchMatchingProductTypesByKeys(singleton(newProductTypeDraft.getKey())))
            .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
        when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
        when(mockProductTypeService.updateProductType(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(existingProductType));
        when(mockProductTypeService.cacheKeysToIds(anySet()))
            .thenReturn(CompletableFuture.completedFuture(emptyMap()));

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(actions).containsExactly(
            ChangeName.of(newProductTypeDraft.getName()),
            ChangeDescription.of(newProductTypeDraft.getDescription()));
        assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);
    }

    @Test
    void sync_WithNullInAttributeDefinitions_ShouldSyncCorrectly() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "foo",
            "name",
            "desc",
            singletonList(null)
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();


        final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);

        final ProductType existingProductType = mock(ProductType.class);
        when(existingProductType.getKey()).thenReturn(newProductTypeDraft.getKey());

        when(mockProductTypeService.fetchMatchingProductTypesByKeys(singleton(newProductTypeDraft.getKey())))
            .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
        when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
        when(mockProductTypeService.updateProductType(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(existingProductType));
        when(mockProductTypeService.cacheKeysToIds(anySet()))
            .thenReturn(CompletableFuture.completedFuture(emptyMap()));

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);
    }

    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "foo",
            "name",
            "desc",
            emptyList()
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();


        final ProductTypeService mockProductTypeService = mock(ProductTypeService.class);

        when(mockProductTypeService.fetchMatchingProductTypesByKeys(singleton(newProductTypeDraft.getKey())))
            .thenReturn(supplyAsync(() -> { throw new SphereException(); }));
        when(mockProductTypeService.cacheKeysToIds(anySet()))
            .thenReturn(CompletableFuture.completedFuture(emptyMap()));

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to fetch existing product types with keys: '[foo]'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(SyncException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(CompletionException.class);
                assertThat(throwable.getCause()).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
    }

    @Test
    void sync_WithErrorsOnSyncing_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "foo",
            "name",
            "desc",
            emptyList()
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();


        final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);

        final ProductType existingProductType = mock(ProductType.class);
        when(existingProductType.getKey()).thenReturn(null);

        when(mockProductTypeService.fetchMatchingProductTypesByKeys(singleton(newProductTypeDraft.getKey())))
            .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
        when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));


        when(mockProductTypeService.cacheKeysToIds(anySet()))
            .thenReturn(CompletableFuture.completedFuture(emptyMap()));

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);


        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement().satisfies(message ->
                assertThat(message).contains("Failed to process the productTypeDraft with key:'foo'."
                    + " Reason: java.lang.NullPointerException")
            );

        assertThat(exceptions)
            .hasSize(1)
            .singleElement().satisfies(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(SyncException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(CompletionException.class);
                assertThat(throwable.getCause()).hasCauseExactlyInstanceOf(NullPointerException.class);
            });

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
    }

    @Test
    void sync_WithErrorCachingKeysButNoKeysToCache_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "foo",
            "name",
            "desc",
            emptyList()
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final SphereClient sphereClient = mock(SphereClient.class);
        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(sphereClient)
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();

        final ProductTypeService mockProductTypeService = mock(ProductTypeService.class);
        when(mockProductTypeService.cacheKeysToIds(anySet()))
                .thenReturn(completedFuture(emptyMap()));
        when(mockProductTypeService.fetchMatchingProductTypesByKeys(anySet()))
                .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement().satisfies(message ->
                assertThat(message).isEqualTo("Failed to fetch existing product types with keys: '[foo]'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .singleElement().satisfies(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(SyncException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(CompletionException.class);
                assertThat(throwable.getCause()).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
    }


    @Test
    void sync_WithInvalidAttributeDefinitions_ShouldThrowError() {
        // preparation
        String nestedAttributeTypeId = "attributeId";
        NestedAttributeType nestedAttributeType = spy(NestedAttributeType.of(referenceOfId(nestedAttributeTypeId)));
        Reference reference = spy(Reference.class);
        when(reference.getId())
            .thenReturn(nestedAttributeTypeId)
            .thenReturn(null);

        when(nestedAttributeType.getTypeReference()).thenReturn(reference);
        final AttributeDefinitionDraft nestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(nestedAttributeType, "validNested", ofEnglish("koko"), true)
            .build();


        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "foo",
            "name",
            "desc",
            singletonList(nestedTypeAttrDefDraft)
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();


        final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);

        final ProductType existingProductType = mock(ProductType.class);
        when(existingProductType.getKey()).thenReturn(newProductTypeDraft.getKey());

        when(mockProductTypeService.fetchMatchingProductTypesByKeys(singleton(newProductTypeDraft.getKey())))
            .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
        when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
        when(mockProductTypeService.updateProductType(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(existingProductType));
        when(mockProductTypeService.cacheKeysToIds(anySet()))
            .thenReturn(CompletableFuture.completedFuture(emptyMap()));

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages.get(0))
            .contains("This exception is unexpectedly thrown since the draft batch has been"
                + "already validated for blank keys" );
        assertThat(errorMessages.get(1))
            .contains("Failed to process the productTypeDraft with key:'foo'" );
        assertThat(exceptions.size()).isEqualTo(2);


        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 2, 0);
    }

    @Test
    void sync_WithErrorUpdatingProductType_ShouldCallErrorCallback() {
        String draftKey = "key2";

        // preparation
        final ProductTypeDraft newProductTypeDraft2 = ProductTypeDraft.ofAttributeDefinitionDrafts(
            draftKey,
            "name",
            "desc",
            emptyList()
        );
        NestedAttributeType nestedTypeAttrDefDraft1 = NestedAttributeType
            .of(referenceOfId(draftKey));
        final AttributeDefinitionDraft nestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(nestedTypeAttrDefDraft1, "validNested", ofEnglish("koko"), true)
            .build();

        final ProductTypeDraft newProductTypeDraft1 = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "key1",
            "name",
            "desc",
            singletonList(nestedTypeAttrDefDraft)
        );
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();

        final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);
        final ProductType existingProductType = mock(ProductType.class);
        when(existingProductType.getKey()).thenReturn(newProductTypeDraft1.getKey());
        when(mockProductTypeService.fetchMatchingProductTypesByKeys(newLinkedHashSet(newProductTypeDraft2.getKey(),
            newProductTypeDraft1.getKey()))).thenReturn(CompletableFuture.completedFuture(Collections.emptySet()),
            CompletableFuture.completedFuture(singleton(existingProductType)));
        when(mockProductTypeService.fetchMatchingProductTypesByKeys(newLinkedHashSet(newProductTypeDraft1.getKey())))
            .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
        when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
        when(mockProductTypeService.updateProductType(any(), any()))
            .thenReturn(supplyAsync(() -> { throw new SphereException(); }));
        when(mockProductTypeService.cacheKeysToIds(anySet()))
            .thenReturn(CompletableFuture.completedFuture(emptyMap()));
        when(mockProductTypeService.createProductType(any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(existingProductType)));
        when(mockProductTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("key1")));

        // test
        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);
        productTypeSync.sync(list(newProductTypeDraft2, newProductTypeDraft1))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages.get(0))
            .contains("Failed to update product type with key: 'key1'. Reason: io.sphere.sdk.models.SphereException:");

    }

    @Test
    void sync_WithErrorCachingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final AttributeDefinitionDraft nestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(NestedAttributeType.of(referenceOfId("x")), "validNested", ofEnglish("koko"), true)
            .build();


        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "foo",
            "name",
            "desc",
            singletonList(nestedTypeAttrDefDraft)
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final SphereClient sphereClient = mock(SphereClient.class);
        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(sphereClient)
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();

        final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);
        when(mockProductTypeService.cacheKeysToIds(anySet()))
            .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to build a cache of keys to ids.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(SyncException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(CompletionException.class);
                assertThat(throwable.getCause()).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
    }

    @Test
    void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraftBuilder
            .of("newProductType", "productType", "a cool type", emptyList())
            .build();

        final SphereClient sphereClient = mock(SphereClient.class);
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(sphereClient)
            .build();

        final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);
        when(mockProductTypeService.cacheKeysToIds(anySet()))
                .thenReturn(supplyAsync(() -> { throw new SphereException(); }));
        when(mockProductTypeService.cacheKeysToIds(anySet())).thenReturn(completedFuture(emptyMap()));
        when(mockProductTypeService.fetchMatchingProductTypesByKeys(anySet())).thenReturn(completedFuture(emptySet()));

        final ProductType createdProductType = mock(ProductType.class);
        when(createdProductType.getKey()).thenReturn(newProductTypeDraft.getKey());
        when(createdProductType.getId()).thenReturn(UUID.randomUUID().toString());
        when(mockProductTypeService.createProductType(any()))
                .thenReturn(completedFuture(Optional.of(createdProductType)));

        final ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        // test
        new ProductTypeSync(spyProductTypeSyncOptions, mockProductTypeService)
            .sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

        // assertion
        verify(spyProductTypeSyncOptions).applyBeforeCreateCallback(newProductTypeDraft);
        verify(spyProductTypeSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
    }

    @Test
    void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraftBuilder
            .of("newProductType", "productType", "a cool type", emptyList())
            .build();

        final SphereClient sphereClient = mock(SphereClient.class);
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(sphereClient)
            .build();

        final ProductType mockedExistingProductType = mock(ProductType.class);
        when(mockedExistingProductType.getKey()).thenReturn(newProductTypeDraft.getKey());
        when(mockedExistingProductType.getId()).thenReturn(UUID.randomUUID().toString());

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(newProductTypeDraft.getKey(), UUID.randomUUID().toString());
        when(productTypeService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(productTypeService.fetchMatchingProductTypesByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockedExistingProductType)));
        when(productTypeService.updateProductType(any(), any())).thenReturn(completedFuture(mockedExistingProductType));

        final ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        // test
        new ProductTypeSync(spyProductTypeSyncOptions, productTypeService)
            .sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

        // assertion
        verify(spyProductTypeSyncOptions).applyBeforeUpdateCallback(any(), any(), any());
        verify(spyProductTypeSyncOptions, never()).applyBeforeCreateCallback(newProductTypeDraft);
    }

}
