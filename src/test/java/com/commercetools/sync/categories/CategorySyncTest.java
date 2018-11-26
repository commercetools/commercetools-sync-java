package com.commercetools.sync.categories;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategory;
import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.MockUtils.mockCategoryService;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CategorySyncTest {
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
     * Initializes instances of  {@link CategorySyncOptions} and {@link CategorySync} which will be used by some
     * of the unit test methods in this test class.
     */
    @Before
    public void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);
        categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient)
                                                        .errorCallback((errorMessage, exception) -> {
                                                            errorCallBackMessages.add(errorMessage);
                                                            errorCallBackExceptions.add(exception);
                                                        })
                                                        .build();
    }

    @Test
    public void sync_WithEmptyListOfDrafts_ShouldNotProcessAnyCategories() {
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), emptySet());
        final CategorySync mockCategorySync =
            new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(emptyList())
                                                                      .toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(0, 0, 0, 0);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    public void sync_WithANullDraft_ShouldBeCountedAsFailed() {
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), emptySet());
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
    public void sync_WithADraftWithNoSetKey_ShouldFailSync() {
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), emptySet());
        final CategorySync mockCategorySync =
            new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);
        final List<CategoryDraft> categoryDrafts = singletonList(
            getMockCategoryDraft(Locale.ENGLISH, "noKeyDraft", "no-key-id-draft",null));

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                                                                      .toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("CategoryDraft with name: "
            + "LocalizedString(en -> noKeyDraft) doesn't have a key.");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    @Test
    public void sync_WithNoExistingCategory_ShouldCreateCategory() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), singleton(mockCategory));
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
    public void sync_WithExistingCategory_ShouldUpdateCategory() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService =
            mockCategoryService(singleton(mockCategory), emptySet(), mockCategory);
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
    public void sync_WithIdenticalExistingCategory_ShouldNotUpdateCategory() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryDraft identicalCategoryDraft = CategoryDraftBuilder.of(mockCategory).build();
        final CategoryService mockCategoryService =
            mockCategoryService(singleton(mockCategory), emptySet(), mockCategory);
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
    public void sync_WithExistingCategoryButWithNullParentReference_ShouldFailSync() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService =
            mockCategoryService(singleton(mockCategory), emptySet(), mockCategory);
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            mockCategoryService);
        final List<CategoryDraft> categoryDrafts = singletonList(
            getMockCategoryDraft(Locale.ENGLISH, "name", "key", null,"customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve parent reference on CategoryDraft"
            + " with key:'key'. Reason: %s",
            ReferenceResolutionException.class.getCanonicalName(), BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithExistingCategoryButWithNoCustomType_ShouldSync() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService =
            mockCategoryService(singleton(mockCategory), emptySet(), mockCategory);
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
    public void sync_WithExistingCategoryButWithEmptyParentReference_ShouldFailSync() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService =
            mockCategoryService(singleton(mockCategory), emptySet(), mockCategory);
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            mockCategoryService);
        final List<CategoryDraft> categoryDrafts = singletonList(
            getMockCategoryDraft(Locale.ENGLISH, "name", "key", "",
            "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
                + " key:'key'. Reason: %s: Failed to resolve parent reference on CategoryDraft with key:'key'. "
                + "Reason: %s", ReferenceResolutionException.class.getCanonicalName(),
            BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithExistingCategoryButWithEmptyCustomTypeReference_ShouldFailSync() {
        final Category mockCategory = getMockCategory(UUID.randomUUID().toString(), "key");
        final CategoryService mockCategoryService =
            mockCategoryService(singleton(mockCategory), emptySet(), mockCategory);
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            mockCategoryService);
        final List<CategoryDraft> categoryDrafts = singletonList(
            getMockCategoryDraft(Locale.ENGLISH, "name", "key", "parentKey",
                "", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve custom type reference on CategoryDraft with key:'key'. Reason:"
                + " %s", ReferenceResolutionException.class.getCanonicalName(), BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void requiresChangeParentUpdateAction_WithTwoDifferentParents_ShouldReturnTrue() {
        final String parentId = "parentId";
        final Category category = mock(Category.class);
        when(category.getParent()).thenReturn(Category.referenceOfId(parentId));

        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "name"), LocalizedString.of(Locale.ENGLISH, "slug"))
            .parent(Category.referenceOfId("differentParent").toResourceIdentifier())
            .build();
        final boolean doesRequire = CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
        assertThat(doesRequire).isTrue();
    }

    @Test
    public void requiresChangeParentUpdateAction_WithTwoIdenticalParents_ShouldReturnFalse() {
        final String parentId = "parentId";
        final Category category = mock(Category.class);
        when(category.getParent()).thenReturn(Category.referenceOfId(parentId));

        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "name"), LocalizedString.of(Locale.ENGLISH, "slug"))
            .parent(Category.referenceOfId(parentId).toResourceIdentifier())
            .build();
        final boolean doesRequire = CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
        assertThat(doesRequire).isFalse();
    }

    @Test
    public void requiresChangeParentUpdateAction_WithNonExistingParents_ShouldReturnFalse() {
        final Category category = mock(Category.class);
        when(category.getParent()).thenReturn(null);

        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "name"), LocalizedString.of(Locale.ENGLISH, "slug"))
            .build();
        final boolean doesRequire = CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
        assertThat(doesRequire).isFalse();
    }

    @Test
    public void sync_WithBatchSizeSet_ShouldCallSyncOnEachBatch() {
        final int batchSize = 1;
        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback(
                (errorMessage, exception) -> {
                    errorCallBackMessages.add(errorMessage);
                    errorCallBackExceptions.add(exception);
                })
            .batchSize(batchSize)
            .build();
        final int numberOfCategoryDrafts  = 160;
        final List<Category> mockedCreatedCategories =
            IntStream.range(0, numberOfCategoryDrafts )
                     .mapToObj(i -> getMockCategory(UUID.randomUUID().toString(), "key" + i))
                     .collect(Collectors.toList());
        final List<CategoryDraft> categoryDrafts =
            mockedCreatedCategories.stream()
                                   .map(category -> CategoryDraftBuilder.of(category).build())
                                   .collect(Collectors.toList());
        final CategoryService mockCategoryService =
            mockCategoryService(emptySet(), new HashSet<>(mockedCreatedCategories));
        final CategorySyncMock categorySync = new CategorySyncMock(categorySyncOptions, getMockTypeService(),
            mockCategoryService);
        final CategorySyncMock syncSpy = spy(categorySync);

        syncSpy.sync(categoryDrafts).toCompletableFuture().join();

        int expectedNumberOfCalls = (int) (Math.ceil(numberOfCategoryDrafts / batchSize) + 1);
        verify(syncSpy, times(expectedNumberOfCalls)).syncBatches(any(), any());
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithDefaultBatchSize_ShouldCallSyncOnEachBatch() {
        final int numberOfCategoryDrafts  = 160;
        final List<Category> mockedCreatedCategories =
            IntStream.range(0, numberOfCategoryDrafts )
                     .mapToObj(i -> getMockCategory(UUID.randomUUID().toString(), "key" + i))
                     .collect(Collectors.toList());
        final List<CategoryDraft> categoryDrafts =
            mockedCreatedCategories.stream()
                                   .map(category -> CategoryDraftBuilder.of(category).build())
                                   .collect(Collectors.toList());
        final CategoryService mockCategoryService =
            mockCategoryService(emptySet(), new HashSet<>(mockedCreatedCategories));
        final CategorySyncMock categorySync = new CategorySyncMock(categorySyncOptions, getMockTypeService(),
            mockCategoryService);
        final CategorySyncMock syncSpy = spy(categorySync);

        syncSpy.sync(categoryDrafts).toCompletableFuture().join();

        final int expectedNumberOfCalls =
            (int) (Math.ceil(numberOfCategoryDrafts / (double) CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT) + 1);
        verify(syncSpy, times(expectedNumberOfCalls)).syncBatches(any(), any());
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithFailOnCachingKeysToIds_ShouldTriggerErrorCallbackAndReturnProperStats() {
        // preparation
        final SphereClient mockClient = mock(SphereClient.class);
        when(mockClient.execute(any(CategoryQuery.class)))
                .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder
                .of(mockClient)
                .errorCallback((errorMessage, exception) -> {
                    errorCallBackMessages.add(errorMessage);
                    errorCallBackExceptions.add(exception);
                })
                .build();

        final CategoryService categoryServiceSpy = spy(new CategoryServiceImpl(syncOptions));

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
                .hasOnlyOneElementSatisfying(message ->
                        assertThat(message).contains("Failed to build a cache of keys to ids.")
                );

        assertThat(errorCallBackExceptions)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(throwable -> {
                    assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                    assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
                });
    }

    @Test
    public void sync_WithFailOnFetchingCategories_ShouldTriggerErrorCallbackAndReturnProperStats() {
        // preparation
        final SphereClient mockClient = mock(SphereClient.class);

        final String categoryKey = "key";
        final Category mockCategory = getMockCategory("foo", categoryKey);
        final PagedQueryResult<Category> pagedQueryResult = mock(PagedQueryResult.class);
        when(pagedQueryResult.getResults()).thenReturn(singletonList(mockCategory));

        // successful caching but exception on fetch.
        when(mockClient.execute(any(CategoryQuery.class)))
                .thenReturn(CompletableFuture.completedFuture(pagedQueryResult))
                .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        when(mockClient.execute(any(CategoryCreateCommand.class)))
                .thenReturn(CompletableFuture.completedFuture(mockCategory));

        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder
                .of(mockClient)
                .errorCallback((errorMessage, exception) -> {
                    errorCallBackMessages.add(errorMessage);
                    errorCallBackExceptions.add(exception);
                })
                .build();

        final CategoryService categoryServiceSpy = spy(new CategoryServiceImpl(syncOptions));

        final CategorySync mockCategorySync = new CategorySync(syncOptions, getMockTypeService(), categoryServiceSpy);

        final CategoryDraft categoryDraft =
                getMockCategoryDraft(Locale.ENGLISH, "name", categoryKey, "parentKey", "customTypeId", new HashMap<>());

        // test
        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(singletonList(categoryDraft))
                .toCompletableFuture().join();

        // assertions
        assertThat(syncStatistics).hasValues(1, 0, 0, 1);

        assertThat(errorCallBackMessages)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(message ->
                        assertThat(message).contains("Failed to fetch existing categories")
                );

        assertThat(errorCallBackExceptions)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(throwable -> {
                    assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                    assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
                });
    }
}
