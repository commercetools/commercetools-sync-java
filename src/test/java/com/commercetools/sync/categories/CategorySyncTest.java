package com.commercetools.sync.categories;

import static com.commercetools.sync.commons.MockUtils.mockCategoryService;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.utils.TestUtils.mockGraphQLResponse;
import static com.commercetools.sync.commons.utils.TestUtils.readObjectFromResource;
import static com.commercetools.sync.products.ProductSyncMockUtils.CATEGORY_KEY_1_RESOURCE_PATH;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.*;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.error.ErrorResponse;
import com.commercetools.api.models.error.ErrorResponseBuilder;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.categories.helpers.CategoryBatchValidator;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.MockUtils;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.commons.models.WaitingToBeResolvedCategories;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.UnresolvedReferencesService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.vrap.rmf.base.client.ApiHttpException;
import io.vrap.rmf.base.client.ApiHttpHeaders;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings("unchecked")
class CategorySyncTest {
  private CategorySyncOptions categorySyncOptions;
  private List<String> errorCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private UnresolvedReferencesService<WaitingToBeResolvedCategories>
      mockUnresolvedReferencesService;

  // protected method access helper
  private static class CategorySyncMock extends CategorySync {
    CategorySyncMock(
        @Nonnull final CategorySyncOptions syncOptions,
        @Nonnull final TypeService typeService,
        @Nonnull final CategoryService categoryService,
        @Nonnull
            final UnresolvedReferencesService<WaitingToBeResolvedCategories>
                unresolvedReferencesService) {
      super(syncOptions, typeService, categoryService, unresolvedReferencesService);
    }

    @Override
    protected CompletionStage<CategorySyncStatistics> syncBatches(
        @Nonnull final List<List<CategoryDraft>> batches,
        @Nonnull final CompletionStage<CategorySyncStatistics> result) {
      return super.syncBatches(batches, result);
    }
  }

  /**
   * Initializes instances of {@link CategorySyncOptions} and {@link CategorySync} which will be
   * used by some of the unit test methods in this test class.
   */
  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    mockUnresolvedReferencesService = mock(UnresolvedReferencesService.class);
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    categorySyncOptions =
        CategorySyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    when(mockUnresolvedReferencesService.save(any(), any(), any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    when(mockUnresolvedReferencesService.fetch(any(), any(), any()))
        .thenReturn(CompletableFuture.completedFuture(emptySet()));
    when(mockUnresolvedReferencesService.delete(any(), any(), any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
  }

  @Test
  void sync_WithEmptyListOfDrafts_ShouldNotProcessAnyCategories() {
    final CategoryService mockCategoryService = MockUtils.mockCategoryService(emptySet(), null);
    final CategorySync mockCategorySync =
        new CategorySync(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);

    final CategorySyncStatistics syncStatistics =
        mockCategorySync.sync(emptyList()).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(0, 0, 0, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
  }

  @Test
  void sync_WithANullDraft_ShouldBeCountedAsFailed() {
    final CategoryService mockCategoryService = MockUtils.mockCategoryService(emptySet(), null);
    final CategorySync mockCategorySync =
        new CategorySync(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);

    final CategorySyncStatistics syncStatistics =
        mockCategorySync.sync(singletonList(null)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0)).isEqualTo("CategoryDraft is null.");
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
  }

  @Test
  void sync_WithADraftWithNoSetKey_ShouldFailSync() {
    final CategoryService mockCategoryService = MockUtils.mockCategoryService(emptySet(), null);
    final CategorySync mockCategorySync =
        new CategorySync(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);
    final CategoryDraft categoryDraft =
        CategorySyncMockUtils.getMockCategoryDraft(
            Locale.ENGLISH, "noKeyDraft", "no-key-id-draft", null);
    final List<CategoryDraft> categoryDrafts = singletonList(categoryDraft);

    final CategorySyncStatistics syncStatistics =
        mockCategorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CategoryBatchValidator.CATEGORY_DRAFT_KEY_NOT_SET, categoryDraft.getName()));
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
  }

  @Test
  void sync_WithNoExistingCategory_ShouldCreateCategory() {
    final Category mockCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), "newKey", "name", "slug", Locale.ENGLISH);
    final Category mockParentCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), "parentKey", "parentName", "parentSlug", Locale.ENGLISH);
    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(singleton(mockParentCategory), mockCategory);
    when(mockCategoryService.fetchCachedCategoryId(Mockito.eq("parentKey")))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("parentId")));
    final CategorySync mockCategorySync =
        new CategorySync(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);
    final List<CategoryDraft> categoryDrafts =
        singletonList(
            CategorySyncMockUtils.getMockCategoryDraft(
                Locale.ENGLISH, "name", "newKey", "parentKey", "customTypeId", new HashMap<>()));

    final CategorySyncStatistics syncStatistics =
        mockCategorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(errorCallBackExceptions).hasSize(0);
  }

  @Test
  void sync_WithErrorOnFetchingUnresolvableCategory_ShouldNotCreateCategory() {
    final Category mockRootCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), "root", "name", "slug", Locale.ENGLISH);
    final Category mockParentCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), "parentKey", "parentName", "parentSlug", Locale.ENGLISH);
    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(Collections.singleton(mockRootCategory), mockParentCategory);

    when(mockCategoryService.fetchCachedCategoryId(Mockito.eq("root")))
        .thenReturn(completedFuture(Optional.of("rootID")));
    final CompletableFuture<Set<WaitingToBeResolvedCategories>> futureThrowingException =
        new CompletableFuture<>();
    futureThrowingException.completeExceptionally(new RuntimeException("CTP error on fetch"));
    when(mockUnresolvedReferencesService.fetch(any(), any(), any()))
        .thenReturn(futureThrowingException);
    final CategorySync mockCategorySync =
        new CategorySync(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);
    final List<CategoryDraft> categoryDrafts = new ArrayList<>();
    categoryDrafts.add(
        CategorySyncMockUtils.getMockCategoryDraft(
            Locale.ENGLISH, "name", "child", "parentKey", "customTypeId", new HashMap<>()));
    categoryDrafts.add(
        CategorySyncMockUtils.getMockCategoryDraft(
            Locale.ENGLISH, "name", "parentKey", "root", "customTypeId", new HashMap<>()));

    final CategorySyncStatistics syncStatistics =
        mockCategorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(2, 1, 0, 1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            "Failed to fetch CategoryDraft waiting to be resolved with parent keys: '[child]'.");
  }

  @Test
  void sync_WithExistingCategory_ShouldUpdateCategory() {
    final Category mockCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), "key", "name", "slug", Locale.ENGLISH);
    final Category mockParentCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), "parentKey", "parentName", "parentSlug", Locale.ENGLISH);
    HashSet<Category> existingCategories = new HashSet<>();
    existingCategories.add(mockCategory);
    existingCategories.add(mockParentCategory);
    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(existingCategories, null, mockCategory);
    when(mockCategoryService.fetchCachedCategoryId(Mockito.eq("parentKey")))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("parentId")));
    when(mockCategoryService.fetchCategory(Mockito.eq("key")))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(mockCategory)));

    final CategorySync mockCategorySync =
        new CategorySync(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);
    final List<CategoryDraft> categoryDrafts =
        singletonList(
            CategorySyncMockUtils.getMockCategoryDraft(
                Locale.ENGLISH, "name", "key", "parentKey", "customTypeId", new HashMap<>()));

    final CategorySyncStatistics syncStatistics =
        mockCategorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0);
    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(errorCallBackExceptions).hasSize(0);
  }

  @Test
  void sync_WithIdenticalExistingCategory_ShouldNotUpdateCategory() {
    final String categoryKey = "key";
    final Category mockCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), categoryKey, "name", "slug", Locale.ENGLISH);
    final CategoryDraft identicalCategoryDraft =
        CategorySyncMockUtils.getCategoryDraftFromCategory(mockCategory);
    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(singleton(mockCategory), null, mockCategory);
    when(mockCategoryService.fetchCategory(Mockito.eq(categoryKey)))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(mockCategory)));

    final CategorySync mockCategorySync =
        new CategorySync(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);
    final List<CategoryDraft> categoryDrafts = singletonList(identicalCategoryDraft);

    final CategorySyncStatistics syncStatistics =
        mockCategorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(errorCallBackExceptions).hasSize(0);
    assertThat(syncStatistics).hasValues(1, 0, 0, 0);
  }

  @Test
  void sync_WithExistingCategoryButWithNullParentReference_ShouldFailSync() {
    final Category mockCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), "key", "name", "slug", Locale.ENGLISH);
    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(singleton(mockCategory), null, mockCategory);
    final CategorySync categorySync =
        new CategorySync(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);
    final List<CategoryDraft> categoryDrafts =
        singletonList(
            CategorySyncMockUtils.getMockCategoryDraft(
                Locale.ENGLISH, "name", "key", null, "customTypeId", new HashMap<>()));

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .contains(BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER);
    assertThat(errorCallBackExceptions).hasSize(1);
  }

  @Test
  void sync_WithExistingCategoryButWithNoCustomType_ShouldSync() {
    final Category mockCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), "key", "name", "slug", Locale.ENGLISH);
    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(singleton(mockCategory), null, mockCategory);
    when(mockCategoryService.fetchCategory(Mockito.eq("key")))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(mockCategory)));

    final CategorySync categorySync =
        new CategorySync(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);

    final CategoryDraft categoryDraft = mock(CategoryDraft.class);
    when(categoryDraft.getName()).thenReturn(LocalizedString.of(Locale.ENGLISH, "new-name"));
    when(categoryDraft.getKey()).thenReturn("key");
    when(categoryDraft.getSlug()).thenReturn(LocalizedString.of(Locale.ENGLISH, "slug"));
    when(categoryDraft.getCustom()).thenReturn(null);

    final List<CategoryDraft> categoryDrafts = singletonList(categoryDraft);

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(errorCallBackExceptions).hasSize(0);
    assertThat(syncStatistics).hasValues(1, 0, 1, 0);
  }

  @Test
  void sync_WithExistingCategoryButWithEmptyParentReference_ShouldFailSync() {
    final Category mockCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), "key", "name", "slug", Locale.ENGLISH);
    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(singleton(mockCategory), null, mockCategory);
    when(mockCategoryService.fetchCategory(Mockito.eq("key")))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(mockCategory)));
    final CategorySync categorySync =
        new CategorySync(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);
    final List<CategoryDraft> categoryDrafts =
        singletonList(
            CategorySyncMockUtils.getMockCategoryDraft(
                Locale.ENGLISH, "name", "key", "", "customTypeId", new HashMap<>()));

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .contains(BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER);
    assertThat(errorCallBackExceptions).hasSize(1);
  }

  @Test
  void sync_WithExistingCategoryButWithEmptyCustomTypeReference_ShouldFailSync() {
    final Category mockCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), "key", "name", "slug", Locale.ENGLISH);
    final Category mockParentCategory =
        CategorySyncMockUtils.getMockCategory(
            UUID.randomUUID().toString(), "parentKey", "parentName", "parentSlug", Locale.ENGLISH);
    Set<Category> existingCategories = new HashSet<>();
    existingCategories.add(mockCategory);
    existingCategories.add(mockParentCategory);
    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(existingCategories, null, mockCategory);
    when(mockCategoryService.fetchCachedCategoryId(Mockito.eq("parentKey")))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("parentId")));
    final CategorySync categorySync =
        new CategorySync(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);
    final List<CategoryDraft> categoryDrafts =
        singletonList(
            CategorySyncMockUtils.getMockCategoryDraft(
                Locale.ENGLISH, "name", "key", "parentKey", "", new HashMap<>()));

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                "Failed to process the CategoryDraft with"
                    + " key: 'key'. Reason: %s: Failed to resolve custom type reference on CategoryDraft "
                    + "with key:'key'. Reason: %s",
                ReferenceResolutionException.class.getCanonicalName(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
    assertThat(errorCallBackExceptions.get(0).getCause())
        .isExactlyInstanceOf(ReferenceResolutionException.class);
  }

  @Test
  void requiresChangeParentUpdateAction_WithTwoDifferentParents_ShouldReturnTrue() {
    final String parentId = "parentId";
    final Category category = mock(Category.class);
    when(category.getParent()).thenReturn(CategoryReferenceBuilder.of().id(parentId).build());

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "name"))
            .slug(LocalizedString.of(Locale.ENGLISH, "slug"))
            .parent(CategoryResourceIdentifierBuilder.of().id("differentParent").build())
            .build();
    final boolean doesRequire =
        CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
    assertThat(doesRequire).isTrue();
  }

  @Test
  void requiresChangeParentUpdateAction_WithTwoIdenticalParents_ShouldReturnFalse() {
    final String parentId = "parentId";
    final String parentKey = "parentkey";
    final Category category = mock(Category.class);
    when(category.getParent()).thenReturn(CategoryReferenceBuilder.of().id(parentId).build());

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "name"))
            .slug(LocalizedString.of(Locale.ENGLISH, "slug"))
            .parent(CategoryResourceIdentifierBuilder.of().id(parentId).key(parentKey).build())
            .build();

    // checking with ids (on draft reference id will be resolved, but in test it's given)
    final boolean doesRequire =
        CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
    assertThat(doesRequire).isFalse();
  }

  @Test
  void requiresChangeParentUpdateAction_WithNonExistingParents_ShouldReturnFalse() {
    final Category category = mock(Category.class);
    when(category.getParent()).thenReturn(null);

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "name"))
            .slug(LocalizedString.of(Locale.ENGLISH, "slug"))
            .build();
    final boolean doesRequire =
        CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
    assertThat(doesRequire).isFalse();
  }

  @Test
  void sync_WithBatchSizeSet_ShouldCallSyncOnEachBatch() {
    // preparation
    final int batchSize = 1;
    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .batchSize(batchSize)
            .build();
    final int numberOfCategoryDrafts = 160;
    final List<Category> mockedCreatedCategories =
        IntStream.range(0, numberOfCategoryDrafts)
            .mapToObj(
                i ->
                    CategorySyncMockUtils.getMockCategory(
                        UUID.randomUUID().toString(), "key" + i, "name", "slug", Locale.ENGLISH))
            .collect(Collectors.toList());
    final List<CategoryDraft> categoryDrafts =
        mockedCreatedCategories.stream()
            .map(category -> CategorySyncMockUtils.getCategoryDraftFromCategory(category))
            .collect(Collectors.toList());

    final Category createdCategory = mock(Category.class);
    when(createdCategory.getKey()).thenReturn("foo");
    when(createdCategory.getId()).thenReturn(UUID.randomUUID().toString());

    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(emptySet(), createdCategory);

    final CategorySyncMock categorySync =
        new CategorySyncMock(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);
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
            .mapToObj(
                i ->
                    CategorySyncMockUtils.getMockCategory(
                        UUID.randomUUID().toString(), "key" + i, "name", "slug", Locale.ENGLISH))
            .collect(Collectors.toList());
    final List<CategoryDraft> categoryDrafts =
        mockedCreatedCategories.stream()
            .map(category -> CategorySyncMockUtils.getCategoryDraftFromCategory(category))
            .collect(Collectors.toList());

    final Category createdCategory = mock(Category.class);
    when(createdCategory.getKey()).thenReturn("foo");
    when(createdCategory.getId()).thenReturn(UUID.randomUUID().toString());

    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(emptySet(), createdCategory);

    final CategorySyncMock categorySync =
        new CategorySyncMock(
            categorySyncOptions,
            MockUtils.getMockTypeService(),
            mockCategoryService,
            mockUnresolvedReferencesService);

    final CategorySyncMock syncSpy = spy(categorySync);

    // test
    syncSpy.sync(categoryDrafts).toCompletableFuture().join();

    // assertion
    final int expectedNumberOfCalls =
        (int)
            (Math.ceil(
                    numberOfCategoryDrafts / (double) CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT)
                + 1);
    verify(syncSpy, times(expectedNumberOfCalls)).syncBatches(any(), any());
    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(errorCallBackExceptions).hasSize(0);
  }

  @Test
  void sync_WithFailOnCachingKeysToIds_ShouldTriggerErrorCallbackAndReturnProperStats() {
    // preparation
    final ProjectApiRoot mockClient = mock(ProjectApiRoot.class);

    when(mockClient.graphql()).thenReturn(mock());
    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock();
    when(mockClient.graphql().post(any(GraphQLRequest.class))).thenReturn(byProjectKeyGraphqlPost);
    when(byProjectKeyGraphqlPost.execute())
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new ApiHttpException(500, "", new ApiHttpHeaders());
                }));

    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(mockClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CategoryService categoryServiceSpy = Mockito.spy(new CategoryServiceImpl(syncOptions));

    final CategorySync mockCategorySync =
        new CategorySync(
            syncOptions,
            MockUtils.getMockTypeService(),
            categoryServiceSpy,
            mockUnresolvedReferencesService);

    CategoryDraft categoryDraft =
        CategorySyncMockUtils.getMockCategoryDraft(
            Locale.ENGLISH, "name", "newKey", "parentKey", "customTypeId", new HashMap<>());

    // test
    final CategorySyncStatistics syncStatistics =
        mockCategorySync.sync(singletonList(categoryDraft)).toCompletableFuture().join();

    // assertions
    assertThat(syncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to build a cache of keys to ids.");

    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            throwable ->
                throwable instanceof CompletionException
                    && throwable.getCause() instanceof RuntimeException);
  }

  @Test
  void sync_WithFailOnFetchingCategories_ShouldTriggerErrorCallbackAndReturnProperStats()
      throws Exception {
    // preparation
    final ProjectApiRoot mockClient = mock(ProjectApiRoot.class);

    final String categoryKey = "key";
    final String categoryJsonResponse =
        "{ \"categories\": {\"results\":[{\"id\":\"foo\",\"key\":\"key\"}" + "]}}";
    ;
    final ApiHttpResponse<GraphQLResponse> graphQLResponseApiHttpResponse =
        mockGraphQLResponse(categoryJsonResponse);

    when(mockClient.graphql()).thenReturn(mock());
    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock();
    when(mockClient.graphql().post(any(GraphQLRequest.class))).thenReturn(byProjectKeyGraphqlPost);
    when(byProjectKeyGraphqlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(graphQLResponseApiHttpResponse));

    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(mockClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CategoryService categoryServiceSpy = spy(new CategoryServiceImpl(syncOptions));
    final ErrorResponse errorResponse =
        ErrorResponseBuilder.of()
            .statusCode(400)
            .errors(Collections.emptyList())
            .message("test")
            .build();

    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json;
    try {
      json = ow.writeValueAsString(errorResponse);
    } catch (JsonProcessingException e) {
      // ignore the error
      json = null;
    }

    final String finalJson = json;
    doReturn(
            supplyAsync(
                () -> {
                  throw new BadRequestException(
                      500,
                      "",
                      null,
                      "",
                      new ApiHttpResponse<>(500, null, finalJson.getBytes(StandardCharsets.UTF_8)));
                }))
        .when(categoryServiceSpy)
        .fetchMatchingCategoriesByKeys(anySet());

    final CategorySync mockCategorySync =
        new CategorySync(
            syncOptions,
            MockUtils.getMockTypeService(),
            categoryServiceSpy,
            mockUnresolvedReferencesService);

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "name"))
            .slug(LocalizedString.of(Locale.ENGLISH, "slug"))
            .key(categoryKey)
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key("customType").build())
                    .build())
            .build();
    // test
    final CategorySyncStatistics syncStatistics =
        mockCategorySync.sync(singletonList(categoryDraft)).toCompletableFuture().join();

    // assertions
    assertThat(syncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to fetch existing categories");

    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .hasCauseExactlyInstanceOf(BadRequestException.class);
  }

  @Test
  void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
    // preparation
    final CategoryDraft categoryDraft =
        CategorySyncMockUtils.getMockCategoryDraft(
            Locale.ENGLISH, "name", "foo", "parentKey", "customTypeId", new HashMap<>());
    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final Category createdCategory = mock(Category.class);
    when(createdCategory.getKey()).thenReturn(categoryDraft.getKey());

    final CategoryService categoryService =
        MockUtils.mockCategoryService(emptySet(), createdCategory);
    when(categoryService.fetchCachedCategoryId(Mockito.eq("parentKey")))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("parentId")));

    Map<String, String> cache = new HashMap<>();
    cache.put("1", UUID.randomUUID().toString());
    cache.put("parentKey", UUID.randomUUID().toString());

    when(categoryService.cacheKeysToIds(anySet())).thenReturn(completedFuture(cache));
    final CategorySyncOptions spyCategorySyncOptions = spy(categorySyncOptions);

    // test
    new CategorySync(
            spyCategorySyncOptions,
            MockUtils.getMockTypeService(),
            categoryService,
            mockUnresolvedReferencesService)
        .sync(singletonList(categoryDraft))
        .toCompletableFuture()
        .join();

    // assertion
    verify(spyCategorySyncOptions).applyBeforeCreateCallback(any());
    verify(spyCategorySyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
    // preparation
    final CategoryDraft categoryDraft =
        CategorySyncMockUtils.getMockCategoryDraft(
            Locale.ENGLISH, "name", "categoryKey1", "parentKey", "customTypeId", new HashMap<>());

    final Category mockedExistingCategory =
        readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, Category.class);

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final CategoryService categoryService =
        MockUtils.mockCategoryService(
            singleton(mockedExistingCategory), mockedExistingCategory, mockedExistingCategory);

    Map<String, String> cache = new HashMap<>();
    cache.put("categoryKey1", mockedExistingCategory.getId());
    cache.put("parentKey", mockedExistingCategory.getParent().getId());
    when(categoryService.cacheKeysToIds(anySet())).thenReturn(completedFuture(cache));

    when(categoryService.fetchCachedCategoryId(Mockito.eq("parentKey")))
        .thenReturn(
            CompletableFuture.completedFuture(
                Optional.of(mockedExistingCategory.getParent().getId())));
    when(categoryService.fetchCategory(Mockito.eq("categoryKey1")))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(mockedExistingCategory)));

    final CategorySyncOptions spyCategorySyncOptions = spy(categorySyncOptions);

    // test
    new CategorySync(
            spyCategorySyncOptions,
            MockUtils.getMockTypeService(),
            categoryService,
            mockUnresolvedReferencesService)
        .sync(singletonList(categoryDraft))
        .toCompletableFuture()
        .join();

    // assertion
    verify(spyCategorySyncOptions).applyBeforeUpdateCallback(any(), any(), any());
    verify(spyCategorySyncOptions, never()).applyBeforeCreateCallback(any());
  }
}
