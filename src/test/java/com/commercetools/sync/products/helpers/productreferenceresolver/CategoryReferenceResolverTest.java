package com.commercetools.sync.products.helpers.productreferenceresolver;

import static com.commercetools.sync.commons.MockUtils.mockCategoryService;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.CategoryOrderHintsBuilder;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.sync.categories.CategorySyncMockUtils;
import com.commercetools.sync.commons.MockUtils;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.inventories.InventorySyncMockUtils;
import com.commercetools.sync.products.ProductSyncMockUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.CustomerGroupService;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CategoryReferenceResolverTest {
  private static final String CHANNEL_KEY = "channel-key_1";
  private static final String CHANNEL_ID = "1";
  private static final String PRODUCT_TYPE_ID = "productTypeId";
  private static final String TAX_CATEGORY_ID = "taxCategoryId";
  private static final String STATE_ID = "stateId";
  private static final String PRODUCT_ID = "productId";
  private static final String CUSTOM_OBJECT_ID = "customObjectId";
  private static final String CUSTOMER_ID = "customerId";

  @Test
  void resolveCategoryReferences_WithCategoryKeysAndCategoryOrderHints_ShouldResolveReferences() {
    // preparation
    final int nCategories = 10;
    final List<Category> categories =
        IntStream.range(0, nCategories)
            .mapToObj(i -> i + "")
            .map(
                key ->
                    CategorySyncMockUtils.getMockCategory(
                        key, key, "name" + key, "slug" + key, Locale.ENGLISH))
            .collect(Collectors.toList());

    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(new HashSet<>(categories), null);

    final List<CategoryResourceIdentifier> categoryResourceIdentifiers =
        categories.stream()
            .map(category -> CategoryResourceIdentifierBuilder.of().key(category.getKey()).build())
            .collect(toList());

    final Map<String, String> categoryOrderHintValues =
        categories.stream()
            .collect(Collectors.toMap(Category::getKey, (category -> "0.00" + category.getId())));

    final ProductDraftBuilder productBuilder =
        ProductSyncMockUtils.getBuilderWithRandomProductType()
            .categories(categoryResourceIdentifiers)
            .categoryOrderHints(
                CategoryOrderHintsBuilder.of().values(categoryOrderHintValues).build());

    final ProductReferenceResolver productReferenceResolver =
        createProductReferenceResolver(mockCategoryService);

    // test
    final ProductDraftBuilder resolvedDraft =
        productReferenceResolver
            .resolveCategoryReferences(productBuilder)
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(resolvedDraft.getCategories())
        .hasSameElementsAs(
            categoryResourceIdentifiers.stream()
                .map(
                    categoryResourceIdentifier ->
                        CategoryResourceIdentifierBuilder.of()
                            .id(categoryResourceIdentifier.getKey())
                            .build())
                .collect(Collectors.toList()));
    assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
    assertThat(resolvedDraft.getCategoryOrderHints().values())
        .hasSize(categoryOrderHintValues.size());
  }

  @Test
  void resolveCategoryReferences_WithCategoryKeysAndNoCategoryOrderHints_ShouldResolveReferences() {
    // preparation
    final int nCategories = 10;
    final List<Category> categories =
        IntStream.range(0, nCategories)
            .mapToObj(i -> i + "")
            .map(
                key ->
                    CategorySyncMockUtils.getMockCategory(
                        key, key, "name" + key, "slug" + key, Locale.ENGLISH))
            .collect(Collectors.toList());
    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(new HashSet<>(categories), null);

    final List<CategoryResourceIdentifier> categoryResourceIdentifiers =
        categories.stream()
            .map(category -> CategoryResourceIdentifierBuilder.of().key(category.getKey()).build())
            .collect(toList());

    final ProductDraftBuilder productBuilder =
        ProductSyncMockUtils.getBuilderWithRandomProductType()
            .categories(categoryResourceIdentifiers);
    final ProductReferenceResolver productReferenceResolver =
        createProductReferenceResolver(mockCategoryService);

    // test
    final ProductDraftBuilder resolvedDraft =
        productReferenceResolver
            .resolveCategoryReferences(productBuilder)
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(resolvedDraft.getCategories())
        .hasSameElementsAs(
            categoryResourceIdentifiers.stream()
                .map(
                    categoryResourceIdentifier ->
                        CategoryResourceIdentifierBuilder.of()
                            .id(categoryResourceIdentifier.getKey())
                            .build())
                .collect(Collectors.toList()));
    assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
    assertThat(resolvedDraft.getCategoryOrderHints().values()).isEmpty();
  }

  @Test
  void
      resolveCategoryReferences_WithCategoryKeysAndSomeCategoryOrderHints_ShouldResolveReferences() {
    // preparation
    final int nCategories = 10;
    final List<Category> categories =
        IntStream.range(0, nCategories)
            .mapToObj(i -> i + "")
            .map(
                key ->
                    CategorySyncMockUtils.getMockCategory(
                        key, key, "name" + key, "slug" + key, Locale.ENGLISH))
            .collect(Collectors.toList());
    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(new HashSet<>(categories), null);

    final List<CategoryResourceIdentifier> categoryResourceIdentifiers =
        categories.stream()
            .map(category -> CategoryResourceIdentifierBuilder.of().key(category.getKey()).build())
            .collect(toList());

    final Map<String, String> categoryOrderHintValues =
        categories.stream()
            .limit(3)
            .collect(Collectors.toMap(Category::getKey, (category -> "0.00" + category.getId())));
    final CategoryOrderHints categoryOrderHints =
        CategoryOrderHintsBuilder.of().values(categoryOrderHintValues).build();
    final ProductDraftBuilder productBuilder =
        ProductSyncMockUtils.getBuilderWithRandomProductType()
            .categories(categoryResourceIdentifiers)
            .categoryOrderHints(categoryOrderHints);
    final ProductReferenceResolver productReferenceResolver =
        createProductReferenceResolver(mockCategoryService);

    // test
    final ProductDraftBuilder resolvedDraft =
        productReferenceResolver
            .resolveCategoryReferences(productBuilder)
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(resolvedDraft.getCategories())
        .hasSameElementsAs(
            categoryResourceIdentifiers.stream()
                .map(
                    categoryResourceIdentifier ->
                        CategoryResourceIdentifierBuilder.of()
                            .id(categoryResourceIdentifier.getKey())
                            .build())
                .collect(Collectors.toList()));
    assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
    assertThat(resolvedDraft.getCategoryOrderHints().values()).hasSize(3);
  }

  @Test
  void resolveCategoryReferences_WithEmptyCategoryReferences_ShouldNotResolveReferences() {
    // preparation
    final CategoryService mockCategoryService = MockUtils.mockCategoryService(emptySet(), null);
    final ProductDraftBuilder productBuilder =
        ProductSyncMockUtils.getBuilderWithRandomProductType();

    productBuilder.categories(emptyList());

    final ProductReferenceResolver productReferenceResolver =
        createProductReferenceResolver(mockCategoryService);

    // test and assertion
    assertThat(
            productReferenceResolver
                .resolveCategoryReferences(productBuilder)
                .toCompletableFuture())
        .matches(f -> !f.isCompletedExceptionally() || f.isCancelled())
        .isCompletedWithValueMatching(resolvedDraft -> resolvedDraft.getCategories().isEmpty());
  }

  @Test
  void resolveCategoryReferences_WithNonExistentCategoryReferences_ShouldNotResolveReferences() {
    // preparation
    final CategoryService mockCategoryService = MockUtils.mockCategoryService(emptySet(), null);

    final List<CategoryResourceIdentifier> categories = new ArrayList<>();
    categories.add(CategoryResourceIdentifierBuilder.of().key("non-existent-category-1").build());
    categories.add(CategoryResourceIdentifierBuilder.of().key("non-existent-category-2").build());

    final ProductDraftBuilder productBuilder =
        ProductSyncMockUtils.getBuilderWithRandomProductType()
            .key("dummyKey")
            .categories(categories);

    final ProductReferenceResolver productReferenceResolver =
        createProductReferenceResolver(mockCategoryService);

    // test and assertion
    final String expectedMessageWithCause =
        String.format(
            ProductReferenceResolver.FAILED_TO_RESOLVE_REFERENCE,
            Category.referenceTypeId().getJsonName(),
            "dummyKey",
            String.format(
                ProductReferenceResolver.CATEGORIES_DO_NOT_EXIST,
                "non-existent-category-1, non-existent-category-2"));

    productReferenceResolver
        .resolveCategoryReferences(productBuilder)
        .exceptionally(
            exception -> {
              assertThat(exception).hasCauseExactlyInstanceOf(ReferenceResolutionException.class);
              assertThat(exception.getCause().getMessage()).isEqualTo(expectedMessageWithCause);
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void resolveCategoryReferences_WithNullKeyOnCategoryReference_ShouldNotResolveReference() {
    // preparation
    final CategoryService mockCategoryService = MockUtils.mockCategoryService(emptySet(), null);
    final ProductDraftBuilder productBuilder =
        ProductSyncMockUtils.getBuilderWithRandomProductType()
            .categories(singletonList(CategoryResourceIdentifierBuilder.of().key(null).build()));

    final ProductReferenceResolver productReferenceResolver =
        createProductReferenceResolver(mockCategoryService);

    // test and assertion
    assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve 'category' resource identifier on ProductDraft with "
                    + "key:'%s'. Reason: %s",
                productBuilder.getKey(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCategoryReferences_WithEmptyKeyOnCategoryReference_ShouldNotResolveReference() {
    // preparation
    final CategoryService mockCategoryService = MockUtils.mockCategoryService(emptySet(), null);
    final ProductDraftBuilder productBuilder =
        ProductSyncMockUtils.getBuilderWithRandomProductType()
            .categories(singletonList(CategoryResourceIdentifierBuilder.of().key("").build()));

    final ProductReferenceResolver productReferenceResolver =
        createProductReferenceResolver(mockCategoryService);

    // test and assertion
    assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve 'category' resource identifier on ProductDraft with "
                    + "key:'%s'. Reason: %s",
                productBuilder.getKey(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCategoryReferences_WithCategoryReferencesWithId_ShouldResolveReference() {
    // preparation
    final CategoryService mockCategoryService = MockUtils.mockCategoryService(emptySet(), null);
    final ProductDraftBuilder productBuilder =
        ProductSyncMockUtils.getBuilderWithRandomProductType()
            .categories(
                singletonList(
                    CategoryResourceIdentifierBuilder.of().id("existing-category-id").build()));

    final ProductReferenceResolver productReferenceResolver =
        createProductReferenceResolver(mockCategoryService);

    final ProductDraftBuilder resolvedDraft =
        productReferenceResolver
            .resolveCategoryReferences(productBuilder)
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(resolvedDraft.getCategories())
        .isEqualTo(
            singletonList(
                CategoryResourceIdentifierBuilder.of().id("existing-category-id").build()));
  }

  @Test
  void resolveCategoryReferences_WithExceptionCategoryFetch_ShouldNotResolveReference() {
    final Category category =
        CategorySyncMockUtils.getMockCategory(
            "categoryKey", "categoryKey", "name", "slug", Locale.ENGLISH);
    final List<Category> categories = Collections.singletonList(category);

    final List<CategoryResourceIdentifier> categoryResourceIdentifiers =
        categories.stream()
            .map(cat -> CategoryResourceIdentifierBuilder.of().key(cat.getKey()).build())
            .collect(toList());

    final CategoryService mockCategoryService =
        MockUtils.mockCategoryService(new HashSet<>(categories), null);
    final ProductDraftBuilder productBuilder =
        ProductSyncMockUtils.getBuilderWithRandomProductType()
            .categories(categoryResourceIdentifiers);

    final CompletableFuture<Set<Category>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(new RuntimeException("CTP error on fetch"));
    when(mockCategoryService.fetchMatchingCategoriesByKeys(anySet()))
        .thenReturn(futureThrowingSphereException);

    final ProductReferenceResolver productReferenceResolver =
        createProductReferenceResolver(mockCategoryService);

    // test and assertion
    assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(RuntimeException.class)
        .withMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveCategoryReferences_WithIdOnCategoryReference_ShouldNotResolveReference() {
    // preparation
    final CategoryService mockCategoryService = MockUtils.mockCategoryService(emptySet(), null);
    final ProductDraftBuilder productBuilder =
        ProductSyncMockUtils.getBuilderWithRandomProductType()
            .categories(
                Arrays.asList(
                    CategoryResourceIdentifierBuilder.of().id("existing-id").build(), null));

    final ProductReferenceResolver productReferenceResolver =
        createProductReferenceResolver(mockCategoryService);

    // test and assertion
    assertThat(
            productReferenceResolver
                .resolveCategoryReferences(productBuilder)
                .toCompletableFuture())
        .matches(f -> !f.isCompletedExceptionally() || f.isCancelled())
        .isCompletedWithValueMatching(
            resolvedDraft ->
                Objects.equals(resolvedDraft.getCategories(), productBuilder.getCategories()));
  }

  @Nonnull
  private ProductReferenceResolver createProductReferenceResolver(
      @Nonnull final CategoryService categoryService) {

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    return new ProductReferenceResolver(
        productSyncOptions,
        ProductSyncMockUtils.getMockProductTypeService(PRODUCT_TYPE_ID),
        categoryService,
        MockUtils.getMockTypeService(),
        InventorySyncMockUtils.getMockChannelService(
            InventorySyncMockUtils.getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
        Mockito.mock(CustomerGroupService.class),
        ProductSyncMockUtils.getMockTaxCategoryService(TAX_CATEGORY_ID),
        ProductSyncMockUtils.getMockStateService(STATE_ID),
        ProductSyncMockUtils.getMockProductService(PRODUCT_ID),
        ProductSyncMockUtils.getMockCustomObjectService(CUSTOM_OBJECT_ID),
        ProductSyncMockUtils.getMockCustomerService(CUSTOMER_ID));
  }
}
