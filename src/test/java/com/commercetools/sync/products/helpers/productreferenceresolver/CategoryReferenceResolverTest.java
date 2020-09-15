package com.commercetools.sync.products.helpers.productreferenceresolver;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.CustomerGroupService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.ProductDraftBuilder;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategory;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.MockUtils.mockCategoryService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getBuilderWithRandomProductType;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductTypeService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockStateService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockTaxCategoryService;
import static com.commercetools.sync.products.helpers.ProductReferenceResolver.CATEGORIES_DO_NOT_EXIST;
import static com.commercetools.sync.products.helpers.ProductReferenceResolver.FAILED_TO_RESOLVE_REFERENCE;
import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryReferenceResolverTest {
    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = "1";
    private static final String PRODUCT_TYPE_ID = "productTypeId";
    private static final String TAX_CATEGORY_ID = "taxCategoryId";
    private static final String STATE_ID = "stateId";
    private static final String PRODUCT_ID = "productId";

    @Test
    void resolveCategoryReferences_WithCategoryKeysAndCategoryOrderHints_ShouldResolveReferences() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final int nCategories = 10;
        final List<Category> categories = IntStream.range(0, nCategories)
                                                   .mapToObj(i -> i + "")
                                                   .map(key -> getMockCategory(key, key))
                                                   .collect(Collectors.toList());

        final CategoryService mockCategoryService = mockCategoryService(new HashSet<>(categories), null);

        final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers =
            categories.stream()
                      .map(category -> ResourceIdentifier.<Category>ofKey(category.getKey()))
                      .collect(toSet());

        final Map<String, String> categoryOrderHintValues =
            categories.stream()
                      .collect(Collectors.toMap(Category::getKey, (category -> "0.00" + category.getId())));

        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .categories(categoryResourceIdentifiers)
            .categoryOrderHints(CategoryOrderHints.of(categoryOrderHintValues));


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test
        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveCategoryReferences(productBuilder)
                                                                          .toCompletableFuture().join();

        // assertion
        assertThat(resolvedDraft.getCategories())
            .hasSameElementsAs(categoryResourceIdentifiers
                .stream()
                .map(categoryResourceIdentifier -> Category.referenceOfId(categoryResourceIdentifier.getKey()))
                .collect(Collectors.toSet()));
        assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
        assertThat(resolvedDraft.getCategoryOrderHints().getAsMap()).hasSize(categoryOrderHintValues.size());
    }

    @Test
    void resolveCategoryReferences_WithCategoryKeysAndNoCategoryOrderHints_ShouldResolveReferences() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final int nCategories = 10;
        final List<Category> categories = IntStream.range(0, nCategories)
                                                   .mapToObj(i -> i + "")
                                                   .map(key -> getMockCategory(key, key))
                                                   .collect(Collectors.toList());
        final CategoryService mockCategoryService = mockCategoryService(new HashSet<>(categories), null);

        final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers =
            categories.stream()
                      .map(category -> ResourceIdentifier.<Category>ofKey(category.getKey()))
                      .collect(toSet());

        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .categories(categoryResourceIdentifiers);
        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test
        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveCategoryReferences(productBuilder)
                                                                          .toCompletableFuture().join();

        // assertion
        assertThat(resolvedDraft.getCategories())
            .hasSameElementsAs(categoryResourceIdentifiers.stream().map(categoryResourceIdentifier ->
                Category.referenceOfId(categoryResourceIdentifier.getKey())).collect(toSet()));
        assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
        assertThat(resolvedDraft.getCategoryOrderHints().getAsMap()).isEmpty();
    }

    @Test
    void resolveCategoryReferences_WithCategoryKeysAndSomeCategoryOrderHints_ShouldResolveReferences() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final int nCategories = 10;
        final List<Category> categories = IntStream.range(0, nCategories)
                                                   .mapToObj(i -> i + "")
                                                   .map(key -> getMockCategory(key, key))
                                                   .collect(Collectors.toList());
        final CategoryService mockCategoryService = mockCategoryService(new HashSet<>(categories), null);

        final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers =
            categories.stream()
                      .map(category -> ResourceIdentifier.<Category>ofKey(category.getKey()))
                      .collect(toSet());

        final Map<String, String> categoryOrderHintValues = categories.stream().limit(3)
                                                                      .collect(Collectors.toMap(Category::getKey,
                                                                          (category -> "0.00" + category.getId())));
        final CategoryOrderHints categoryOrderHints = CategoryOrderHints.of(categoryOrderHintValues);
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .categories(categoryResourceIdentifiers)
            .categoryOrderHints(categoryOrderHints);
        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test
        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveCategoryReferences(productBuilder)
                                                                          .toCompletableFuture().join();

        // assertion
        assertThat(resolvedDraft.getCategories())
            .hasSameElementsAs(categoryResourceIdentifiers.stream().map(categoryResourceIdentifier ->
                Category.referenceOfId(categoryResourceIdentifier.getKey())).collect(toSet()));
        assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
        assertThat(resolvedDraft.getCategoryOrderHints().getAsMap()).hasSize(3);
    }

    @Test
    void resolveCategoryReferences_WithNullCategoryReferences_ShouldNotResolveReferences() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), null);
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType();

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test and assertion
        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft -> resolvedDraft.getCategories().isEmpty());
    }

    @Test
    void resolveCategoryReferences_WithNonExistentCategoryReferences_ShouldNotResolveReferences() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), null);

        final Set<ResourceIdentifier<Category>> categories = new HashSet<>();
        categories.add(ResourceIdentifier.ofKey("non-existent-category-1"));
        categories.add(ResourceIdentifier.ofKey("non-existent-category-2"));

        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .key("dummyKey")
            .categories(categories);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test and assertion
        final String expectedMessageWithCause = format(FAILED_TO_RESOLVE_REFERENCE, Category.resourceTypeId(),
            "dummyKey", format(CATEGORIES_DO_NOT_EXIST, "non-existent-category-1, non-existent-category-2"));

        productReferenceResolver
            .resolveCategoryReferences(productBuilder)
            .exceptionally(exception -> {
                assertThat(exception).hasCauseExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception.getCause().getMessage())
                    .isEqualTo(expectedMessageWithCause);
                return null;
            })
            .toCompletableFuture()
            .join();
    }

    @Test
    void resolveCategoryReferences_WithNullKeyOnCategoryReference_ShouldNotResolveReference() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), null);
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .categories(singleton(ResourceIdentifier.ofKey(null)));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test and assertion
        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve 'category' resource identifier on ProductDraft with "
                + "key:'%s'. Reason: %s", productBuilder.getKey(), BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    void resolveCategoryReferences_WithEmptyKeyOnCategoryReference_ShouldNotResolveReference() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), null);
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .categories(singleton(ResourceIdentifier.ofKey("")));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test and assertion
        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve 'category' resource identifier on ProductDraft with "
                + "key:'%s'. Reason: %s", productBuilder.getKey(), BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    void resolveCategoryReferences_WithCategoryReferencesWithId_ShouldResolveReference() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), null);
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .categories(singleton(ResourceIdentifier.ofId("existing-category-id")));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveCategoryReferences(productBuilder)
                                                                          .toCompletableFuture().join();

        // assertion
        assertThat(resolvedDraft.getCategories())
                .isEqualTo(singleton(ResourceIdentifier.ofId("existing-category-id")));
    }

    @Test
    void resolveCategoryReferences_WithExceptionCategoryFetch_ShouldNotResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final Category category = getMockCategory("categoryKey", "categoryKey");
        final List<Category> categories = Collections.singletonList(category);

        final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers =
            categories.stream()
                      .map(cat -> ResourceIdentifier.<Category>ofKey(cat.getKey()))
                      .collect(toSet());

        final CategoryService mockCategoryService = mockCategoryService(new HashSet<>(categories), null);
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .categories(categoryResourceIdentifiers);

        final CompletableFuture<Set<Category>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(mockCategoryService.fetchMatchingCategoriesByKeys(anySet())).thenReturn(futureThrowingSphereException);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test and assertion
        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    void resolveCategoryReferences_WithIdOnCategoryReference_ShouldNotResolveReference() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), null);
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .categories(asSet(ResourceIdentifier.ofId("existing-id"), null));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test and assertion
        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft -> Objects.equals(resolvedDraft.getCategories(),
                productBuilder.getCategories()));

    }
}
