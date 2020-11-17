package com.commercetools.sync.categories;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.CustomFieldsDraft;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategory;
import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.MockUtils.mockCategoryService;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.categories.CategorySyncMockUtils.CATEGORY_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.categories.CategorySyncMockUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.commons.MockUtils.getMockCustomFieldsDraft;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategorySyncTest {
    private CategorySyncOptions categorySyncOptions;
    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    // protected method access helper
    private static class CategorySyncMock extends CategorySync {

        CategorySyncMock(@Nonnull final CategorySyncOptions syncOptions,
                         @Nonnull final TypeService typeService,
                         @Nonnull final CategoryService categoryService) {
            super(syncOptions, typeService, categoryService);
        }

        @Override
        protected CompletionStage<CategorySyncStatistics> syncBatches(
                @Nonnull final List<List<CategoryDraft>> batches,
                @Nonnull final CompletionStage<CategorySyncStatistics> result) {
            return super.syncBatches(batches, result);
        }
    }

    /**
     * Initializes instances of  {@link CategorySyncOptions} and {@link CategorySync} which will be used by some of the
     * unit test methods in this test class.
     */
    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);
        categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                }).build();
    }

    @Test
    void sync_WithEmptyListOfDrafts_ShouldNotProcessAnyCategories() {
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), null);
        final CategorySync mockCategorySync =
                new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(emptyList())
                .toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(0, 0, 0, 0);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    void sync_WithANullDraft_ShouldBeCountedAsFailed() {
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), null);
        final CategorySync mockCategorySync =
                new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(singletonList(null))
                .toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("CategoryDraft is null.");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    @Test
    void sync_WithADraftWithNoSetKey_ShouldFailSync() {
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), null);
        final CategorySync mockCategorySync =
                new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);
        final List<CategoryDraft> categoryDrafts = singletonList(
                getMockCategoryDraft(Locale.ENGLISH, "noKeyDraft", "no-key-id-draft", null));

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                .toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("CategoryDraft with name: "
            + "LocalizedString(en -> noKeyDraft) doesn't have a key. Please make sure all category drafts have keys."
        );
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    @Test
    void sync_WithNoExistingCategory_ShouldCreateCategory() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), mockCategory);
        final CategorySync mockCategorySync =
                new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);
        final List<CategoryDraft> categoryDrafts = singletonList(
                getMockCategoryDraft(Locale.ENGLISH, "name", "newKey", "parentKey", "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                .toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    void sync_WithExistingCategory_ShouldUpdateCategory() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService =
                mockCategoryService(singleton(mockCategory), null, mockCategory);
        final CategorySync mockCategorySync =
                new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);
        final List<CategoryDraft> categoryDrafts = singletonList(
                getMockCategoryDraft(Locale.ENGLISH, "name", "key", "parentKey", "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                .toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    void sync_WithIdenticalExistingCategory_ShouldNotUpdateCategory() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryDraft identicalCategoryDraft = CategoryDraftBuilder.of(mockCategory).build();
        final CategoryService mockCategoryService =
                mockCategoryService(singleton(mockCategory), null, mockCategory);
        final CategorySync mockCategorySync =
                new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);
        final List<CategoryDraft> categoryDrafts = singletonList(identicalCategoryDraft);

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                .toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 0);
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    void sync_WithExistingCategoryButWithNullParentReference_ShouldFailSync() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService =
                mockCategoryService(singleton(mockCategory), null, mockCategory);
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
                mockCategoryService);
        final List<CategoryDraft> categoryDrafts = singletonList(
                getMockCategoryDraft(Locale.ENGLISH, "name", "key", null, "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to process the CategoryDraft with"
                + " key:'key'. Reason: %s: Failed to resolve parent reference on CategoryDraft"
                + " with key:'key'. Reason: %s",
            ReferenceResolutionException.class.getCanonicalName(), BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    void sync_WithExistingCategoryButWithNoCustomType_ShouldSync() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService =
                mockCategoryService(singleton(mockCategory), null, mockCategory);
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
                mockCategoryService);

        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        when(categoryDraft.getName()).thenReturn(LocalizedString.of(Locale.ENGLISH, "name"));
        when(categoryDraft.getKey()).thenReturn("key");
        when(categoryDraft.getCustom()).thenReturn(null);

        final List<CategoryDraft> categoryDrafts = singletonList(categoryDraft);

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    void sync_WithExistingCategoryButWithEmptyParentReference_ShouldFailSync() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService =
                mockCategoryService(singleton(mockCategory), null, mockCategory);
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
                mockCategoryService);
        final List<CategoryDraft> categoryDrafts = singletonList(
                getMockCategoryDraft(Locale.ENGLISH, "name", "key", "",
                        "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to process the CategoryDraft with"
                + " key:'key'. Reason: %s: Failed to resolve parent reference on CategoryDraft with key:'key'. "
                + "Reason: %s", ReferenceResolutionException.class.getCanonicalName(),
            BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    void sync_WithExistingCategoryButWithEmptyCustomTypeReference_ShouldFailSync() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService =
                mockCategoryService(singleton(mockCategory), null, mockCategory);
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
                mockCategoryService);
        final List<CategoryDraft> categoryDrafts = singletonList(
                getMockCategoryDraft(Locale.ENGLISH, "name", "key", "parentKey",
                        "", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to process the CategoryDraft with"
                        + " key:'key'. Reason: %s: Failed to resolve custom type reference on CategoryDraft "
                        + "with key:'key'. Reason: %s", ReferenceResolutionException.class.getCanonicalName(),
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    void requiresChangeParentUpdateAction_WithTwoDifferentParents_ShouldReturnTrue() {
        final String parentId = "parentId";
        final Category category = mock(Category.class);
        when(category.getParent()).thenReturn(Category.referenceOfId(parentId));

        final CategoryDraft categoryDraft = CategoryDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "name"), LocalizedString.of(Locale.ENGLISH, "slug"))
                .parent(ResourceIdentifier.ofId("differentParent"))
                .build();
        final boolean doesRequire = CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
        assertThat(doesRequire).isTrue();
    }

    @Test
    void requiresChangeParentUpdateAction_WithTwoIdenticalParents_ShouldReturnFalse() {
        final String parentId = "parentId";
        final String parentKey = "parentkey";
        final Category category = mock(Category.class);
        when(category.getParent()).thenReturn(Category.referenceOfId(parentId));

        final CategoryDraft categoryDraft = CategoryDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "name"), LocalizedString.of(Locale.ENGLISH, "slug"))
                .parent(ResourceIdentifier.ofIdOrKey(parentId, parentKey))
                .build();

        // checking with ids (on draft reference id will be resolved, but in test it's given)
        final boolean doesRequire = CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
        assertThat(doesRequire).isFalse();
    }

    @Test
    void requiresChangeParentUpdateAction_WithNonExistingParents_ShouldReturnFalse() {
        final Category category = mock(Category.class);
        when(category.getParent()).thenReturn(null);

        final CategoryDraft categoryDraft = CategoryDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "name"), LocalizedString.of(Locale.ENGLISH, "slug"))
                .build();
        final boolean doesRequire = CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
        assertThat(doesRequire).isFalse();
    }

    @Test
    void sync_WithBatchSizeSet_ShouldCallSyncOnEachBatch() {
        // preparation
        final int batchSize = 1;
        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                })
                .batchSize(batchSize)
                .build();
        final int numberOfCategoryDrafts = 160;
        final List<Category> mockedCreatedCategories =
                IntStream.range(0, numberOfCategoryDrafts)
                        .mapToObj(i -> getMockCategory(UUID.randomUUID().toString(), "key" + i))
                        .collect(Collectors.toList());
        final List<CategoryDraft> categoryDrafts =
                mockedCreatedCategories.stream()
                        .map(category -> CategoryDraftBuilder.of(category).build())
                        .collect(Collectors.toList());

        final Category createdCategory = mock(Category.class);
        when(createdCategory.getKey()).thenReturn("foo");
        when(createdCategory.getId()).thenReturn(UUID.randomUUID().toString());

        final CategoryService mockCategoryService = mockCategoryService(emptySet(), createdCategory);

        final CategorySyncMock categorySync = new CategorySyncMock(categorySyncOptions, getMockTypeService(),
                mockCategoryService);
        final CategorySyncMock syncSpy = spy(categorySync);

        // test
        syncSpy.sync(categoryDrafts).toCompletableFuture().join();

        // assertion
        int expectedNumberOfCalls = (int) (Math.ceil(numberOfCategoryDrafts / batchSize) + 1);
        verify(syncSpy, times(expectedNumberOfCalls)).syncBatches(any(), any());
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    void sync_WithDefaultBatchSize_ShouldCallSyncOnEachBatch() {
        // preparation
        final int numberOfCategoryDrafts = 160;
        final List<Category> mockedCreatedCategories =
                IntStream.range(0, numberOfCategoryDrafts)
                        .mapToObj(i -> getMockCategory(UUID.randomUUID().toString(), "key" + i))
                        .collect(Collectors.toList());
        final List<CategoryDraft> categoryDrafts =
                mockedCreatedCategories.stream()
                        .map(category -> CategoryDraftBuilder.of(category).build())
                        .collect(Collectors.toList());

        final Category createdCategory = mock(Category.class);
        when(createdCategory.getKey()).thenReturn("foo");
        when(createdCategory.getId()).thenReturn(UUID.randomUUID().toString());

        final CategoryService mockCategoryService = mockCategoryService(emptySet(), createdCategory);

        final CategorySyncMock categorySync = new CategorySyncMock(categorySyncOptions, getMockTypeService(),
                mockCategoryService);

        final CategorySyncMock syncSpy = spy(categorySync);

        // test
        syncSpy.sync(categoryDrafts).toCompletableFuture().join();

        // assertion
        final int expectedNumberOfCalls =
                (int) (Math.ceil(numberOfCategoryDrafts / (double) CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT) + 1);
        verify(syncSpy, times(expectedNumberOfCalls)).syncBatches(any(), any());
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    void sync_WithFailOnCachingKeysToIds_ShouldTriggerErrorCallbackAndReturnProperStats() {
        // preparation
        final SphereClient mockClient = mock(SphereClient.class);
        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder
                .of(mockClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                })
                .build();

        final CategoryService categoryServiceSpy = spy(new CategoryServiceImpl(syncOptions));
        when(categoryServiceSpy.cacheKeysToIds(anySet()))
                .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final CategorySync mockCategorySync = new CategorySync(syncOptions, getMockTypeService(), categoryServiceSpy);

        CategoryDraft categoryDraft =
                getMockCategoryDraft(Locale.ENGLISH, "name", "newKey", "parentKey", "customTypeId", new HashMap<>());

        // test
        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(singletonList(categoryDraft))
                .toCompletableFuture().join();

        // assertions
        assertThat(syncStatistics).hasValues(1, 0, 0, 1);

        assertThat(errorCallBackMessages)
                .hasSize(1)
                .singleElement().satisfies(message ->
                        assertThat(message).contains("Failed to build a cache of keys to ids."));

        assertThat(errorCallBackExceptions)
                .hasSize(1)
                .singleElement().satisfies(throwable -> {
                    assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                    assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
                });
    }

    @Test
    void sync_WithFailOnFetchingCategories_ShouldTriggerErrorCallbackAndReturnProperStats() {
        // preparation
        final SphereClient mockClient = mock(SphereClient.class);

        final String categoryKey = "key";
        final Category mockCategory = getMockCategory("foo", categoryKey);

        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder
                .of(mockClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                })
                .build();

        final CategoryService categoryServiceSpy = spy(new CategoryServiceImpl(syncOptions));
        final CategorySync mockCategorySync = new CategorySync(syncOptions, getMockTypeService(), categoryServiceSpy);

        final CategoryDraft categoryDraft =
                getMockCategoryDraft(Locale.ENGLISH, "name", categoryKey, "parentKey", "customTypeId", new HashMap<>());

        final Map<String, String> keyToIds = new HashMap<>();
        when(categoryServiceSpy.fetchMatchingCategoriesByKeys(anySet()))
                .thenReturn(supplyAsync(() -> { throw new CompletionException(new SphereException()); }));

        keyToIds.put(categoryDraft.getKey(), UUID.randomUUID().toString());
        when(categoryServiceSpy.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));

        // test
        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(singletonList(categoryDraft))
                .toCompletableFuture().join();

        // assertions
        assertThat(syncStatistics).hasValues(1, 0, 0, 1);

        assertThat(errorCallBackMessages)
                .hasSize(1)
                .singleElement().satisfies(message ->
                        assertThat(message).contains("Failed to fetch existing categories"));

        assertThat(errorCallBackExceptions)
                .hasSize(1)
                .singleElement().satisfies(throwable -> {
                    assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                    assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
                });
    }

    @Test
    void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
        // preparation
        final CategoryDraft categoryDraft =
                getMockCategoryDraft(Locale.ENGLISH, "name", "foo", "parentKey", "customTypeId", new HashMap<>());

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
                .of(mock(SphereClient.class))
                .build();

        final Category createdCategory = mock(Category.class);
        when(createdCategory.getKey()).thenReturn(categoryDraft.getKey());

        final CategoryService categoryService = mockCategoryService(emptySet(), createdCategory);

        final CategorySyncOptions spyCategorySyncOptions = spy(categorySyncOptions);

        // test
        new CategorySync(spyCategorySyncOptions, getMockTypeService(), categoryService)
                .sync(singletonList(categoryDraft)).toCompletableFuture().join();

        // assertion
        verify(spyCategorySyncOptions).applyBeforeCreateCallback(any());
        verify(spyCategorySyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
    }

    @Test
    void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
        // preparation
        final CategoryDraft categoryDraft =
                getMockCategoryDraft(Locale.ENGLISH, "name", "1", "parentKey", "customTypeId", new HashMap<>());

        final Category mockedExistingCategory =
                readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, Category.class);

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
                .of(mock(SphereClient.class))
                .build();

        final CategoryService categoryService = mockCategoryService(singleton(mockedExistingCategory),
                mockedExistingCategory, mockedExistingCategory);

        when(categoryService.cacheKeysToIds(anySet()))
                .thenReturn(completedFuture(singletonMap("1", UUID.randomUUID().toString())));

        final CategorySyncOptions spyCategorySyncOptions = spy(categorySyncOptions);

        // test
        new CategorySync(spyCategorySyncOptions, getMockTypeService(), categoryService)
                .sync(singletonList(categoryDraft)).toCompletableFuture().join();

        // assertion
        verify(spyCategorySyncOptions).applyBeforeUpdateCallback(any(), any(), any());
        verify(spyCategorySyncOptions, never()).applyBeforeCreateCallback(any());
    }

    @Test
    void syncDrafts_WithConcurrentModificationException_ShouldRetryToUpdateNewCategoryWithSuccess() {
        // Preparation
        final String oldCategoryKey = "oldCategoryKey";
        final LocalizedString newCategoryName = LocalizedString.of(Locale.ENGLISH, "Modern Furniture");
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();

        final CategoryDraft categoryDraft = CategoryDraftBuilder
                .of(newCategoryName, LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
                .key(oldCategoryKey)
                .custom(CustomFieldsDraft.ofTypeKeyAndJson(
                        OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsDraft().getFields()))
                .build();

        final CategorySyncOptions categorySyncOptions =
                CategorySyncOptionsBuilder
                    .of(mock(SphereClient.class))
                    .errorCallback((exception, oldResource, newResource, updateActions) -> {
                        errorMessages.add(exception.getMessage());
                        exceptions.add(exception.getCause());
                    })
                    .warningCallback((exception, oldResource, newResource)
                        -> warningCallBackMessages.add(exception.getMessage()))
                    .build();

        final CategoryService categoryService = buildMockCategoryServiceWithSuccessfulUpdateOnRetry(categoryDraft);
        final TypeService typeService = mock(TypeService.class);
        when(typeService.fetchCachedTypeId(any()))
                .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));

        final CategorySync categorySync =
                new CategorySync(categorySyncOptions, typeService, categoryService);

        // Test
        final CategorySyncStatistics statistics = categorySync.sync(Collections.singletonList(categoryDraft))
                .toCompletableFuture()
                .join();

        // Assertion
        assertThat(statistics).hasValues(1, 0, 1, 0);
        assertThat(exceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    void syncDrafts_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
        final String oldCategoryKey = "oldCategoryKey";
        final LocalizedString newCategoryName = LocalizedString.of(Locale.ENGLISH, "Modern Furniture");
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();

        final CategorySyncOptions categorySyncOptions =
                CategorySyncOptionsBuilder
                    .of(mock(SphereClient.class))
                    .errorCallback((exception, oldResource, newResource, updateActions) -> {
                        errorMessages.add(exception.getMessage());
                        exceptions.add(exception.getCause());
                    })
                    .warningCallback((exception, oldResource, newResource)
                        -> warningCallBackMessages.add(exception.getMessage()))
                    .build();

        final CategoryDraft categoryDraft = CategoryDraftBuilder
                .of(newCategoryName, LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
                .key(oldCategoryKey)
                .custom(CustomFieldsDraft.ofTypeKeyAndJson(
                        OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsDraft().getFields()))
                .build();

        final CategoryService categoryService = buildMockCategoryServiceWithFailedFetchOnRetry(categoryDraft);
        final TypeService typeService = mock(TypeService.class);
        when(typeService.fetchCachedTypeId(any()))
                .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));

        final CategorySync categorySync =
                new CategorySync(categorySyncOptions, typeService, categoryService);

        final CategorySyncStatistics syncStatistics = categorySync.sync(Collections.singletonList(categoryDraft))
                .toCompletableFuture()
                .join();

        AssertionsForStatistics.assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions.get(0)).isNotNull();
        assertThat(exceptions.get(0)).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorMessages.get(0)).contains(
                format("Failed to update Category with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                        + "after concurrency modification.", categoryDraft.getKey()));
    }

    @Test
    void syncDrafts_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
        final String oldCategoryKey = "oldCategoryKey";
        final LocalizedString newCategoryName = LocalizedString.of(Locale.ENGLISH, "Modern Furniture");
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();

        final CategorySyncOptions categorySyncOptions =
                CategorySyncOptionsBuilder
                        .of(mock(SphereClient.class))
                        .errorCallback((exception, oldResource, newResource, updateActions) -> {
                            errorMessages.add(exception.getMessage());
                            exceptions.add(exception.getCause());
                        })
                        .warningCallback((exception, oldResource, newResource)
                            -> warningCallBackMessages.add(exception.getMessage()))
                        .build();

        final CategoryDraft categoryDraft = CategoryDraftBuilder
                .of(newCategoryName, LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
                .key(oldCategoryKey)
                .custom(CustomFieldsDraft.ofTypeKeyAndJson(
                        OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsDraft().getFields()))
                .build();

        final CategoryService categoryService = buildMockCategoryServiceWithNotFoundFetchOnRetry(categoryDraft);
        final TypeService typeService = mock(TypeService.class);
        when(typeService.fetchCachedTypeId(any()))
                .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));

        final CategorySync categorySync =
                new CategorySync(categorySyncOptions, typeService, categoryService);

        final CategorySyncStatistics syncStatistics = categorySync.sync(Collections.singletonList(categoryDraft))
                .toCompletableFuture()
                .join();

        AssertionsForStatistics.assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions).hasSize(1);
        assertThat(errorMessages.get(0)).contains(
                format("Failed to update Category with key: '%s'. Reason: Not found when attempting to fetch while"
                        + " retrying after concurrency modification.", categoryDraft.getKey()));
    }

    @Nonnull
    private CategoryService buildMockCategoryServiceWithSuccessfulUpdateOnRetry(
            @Nonnull final CategoryDraft categoryDraft) {

        final CategoryService mockCategoryService = mock(CategoryService.class);

        final Category mockCategory = mock(Category.class);
        when(mockCategory.getKey()).thenReturn("oldCategoryKey");

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(categoryDraft.getKey(), UUID.randomUUID().toString());

        when(mockCategoryService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockCategoryService.fetchMatchingCategoriesByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockCategory)));
        when(mockCategoryService.fetchCategory(any())).thenReturn(completedFuture(Optional.of(mockCategory)));
        when(mockCategoryService.updateCategory(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockCategory));
        return mockCategoryService;
    }

    @Nonnull
    private CategoryService buildMockCategoryServiceWithFailedFetchOnRetry(
            @Nonnull final CategoryDraft categoryDraft) {

        final CategoryService mockCategoryService = mock(CategoryService.class);

        final Category mockCategory = mock(Category.class);
        when(mockCategory.getKey()).thenReturn("oldCategoryKey");

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(categoryDraft.getKey(), UUID.randomUUID().toString());

        when(mockCategoryService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockCategoryService.fetchMatchingCategoriesByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockCategory)));
        when(mockCategoryService.fetchCategory(any()))
                .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()));
        when(mockCategoryService.updateCategory(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockCategory));
        return mockCategoryService;
    }

    @Nonnull
    private CategoryService buildMockCategoryServiceWithNotFoundFetchOnRetry(
            @Nonnull final CategoryDraft categoryDraft) {

        final CategoryService mockCategoryService = mock(CategoryService.class);

        final Category mockCategory = mock(Category.class);
        when(mockCategory.getKey()).thenReturn("oldCategoryKey");

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(categoryDraft.getKey(), UUID.randomUUID().toString());

        when(mockCategoryService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockCategoryService.fetchMatchingCategoriesByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockCategory)));
        when(mockCategoryService.fetchCategory(any()))
                .thenReturn(completedFuture(Optional.empty()));
        when(mockCategoryService.updateCategory(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockCategory));
        return mockCategoryService;
    }
}
