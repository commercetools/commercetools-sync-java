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
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategory;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.MockUtils.mockCategoryService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getBuilderWithProductTypeRefKey;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductTypeService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockStateService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockTaxCategoryService;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryReferenceResolverTest {
    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = "1";
    private static final String PRODUCT_TYPE_ID = "productTypeId";
    private static final String TAX_CATEGORY_ID = "taxCategoryId";
    private static final String STATE_ID = "stateId";
    private static final String PRODUCT_ID = "productId";

    @Test
    public void resolveCategoryReferences_WithCategoryKeysAndCategoryOrderHints_ShouldResolveReferences() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final int nCategories = 10;
        final List<Category> categories = IntStream.range(0, nCategories)
                                                   .mapToObj(i -> i + "")
                                                   .map(key -> getMockCategory(UUID.randomUUID().toString(), key))
                                                   .collect(Collectors.toList());
        final CategoryService mockCategoryService = mockCategoryService(new HashSet<>(categories), emptySet());
        final Set<ResourceIdentifier<Category>> categoryReferences = categories
            .stream()
            .map(category -> ResourceIdentifier.<Category>ofKey(category.getKey()))
            .collect(Collectors.toSet());
        final Map<String, String> categoryOrderHintValues = categories
            .stream()
            .collect(Collectors.toMap(Category::getKey, (category -> "0.00" + category.getKey())));

        final CategoryOrderHints categoryOrderHints = CategoryOrderHints.of(categoryOrderHintValues);
        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefKey("anyProductTypeKey")
            .categories(categoryReferences).categoryOrderHints(categoryOrderHints);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test
        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveCategoryReferences(productBuilder)
                                                                          .toCompletableFuture().join();

        // assertion
        assertThat(resolvedDraft.getCategories()).hasSize(nCategories);
        assertThat(resolvedDraft.getCategories()).containsOnlyElementsOf(categoryReferences);
        assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
        assertThat(resolvedDraft.getCategoryOrderHints().getAsMap()).hasSize(categoryOrderHintValues.size());
    }

    @Test
    public void resolveCategoryReferences_WithCategoryKeysAndNoCategoryOrderHints_ShouldResolveReferences() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final int nCategories = 10;
        final List<Category> categories = IntStream.range(0, nCategories)
                                                   .mapToObj(i -> i + "")
                                                   .map(key -> getMockCategory(key, key))
                                                   .collect(Collectors.toList());

        final CategoryService mockCategoryService = mockCategoryService(new HashSet<>(categories), emptySet());

        final Set<ResourceIdentifier<Category>> categoryReferences = categories
            .stream()
            .map(category -> ResourceIdentifier.<Category>ofKey(category.getKey()))
            .collect(Collectors.toSet());

        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefKey("anyProductTypeKey")
            .categories(categoryReferences);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test
        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveCategoryReferences(productBuilder)
                                                                          .toCompletableFuture().join();

        // assertion
        assertThat(resolvedDraft.getCategories()).hasSize(nCategories);
        assertThat(resolvedDraft.getCategories()).containsOnlyElementsOf(categoryReferences);
        assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
        assertThat(resolvedDraft.getCategoryOrderHints().getAsMap()).isEmpty();
    }

    @Test
    public void resolveCategoryReferences_WithCategoryKeysAndSomeCategoryOrderHints_ShouldResolveReferences() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final int nCategories = 10;
        final List<Category> categories = IntStream.range(0, nCategories)
                                                   .mapToObj(i -> i + "")
                                                   .map(key -> getMockCategory(key, key))
                                                   .collect(Collectors.toList());
        final CategoryService mockCategoryService = mockCategoryService(new HashSet<>(categories), emptySet());

        final Set<ResourceIdentifier<Category>> categoryReferences = categories
            .stream()
            .map(category -> ResourceIdentifier.<Category>ofKey(category.getKey()))
            .collect(Collectors.toSet());

        final Map<String, String> categoryOrderHintValues = categories
            .stream()
            .limit(3)
            .collect(Collectors.toMap(Category::getKey, (category -> "0.00" + category.getKey())));

        final CategoryOrderHints categoryOrderHints = CategoryOrderHints.of(categoryOrderHintValues);
        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefKey("anyProductTypeKey")
            .categories(categoryReferences).categoryOrderHints(categoryOrderHints);

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
        assertThat(resolvedDraft.getCategories()).hasSize(nCategories);
        assertThat(resolvedDraft.getCategories()).containsOnlyElementsOf(categoryReferences);
        assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
        assertThat(resolvedDraft.getCategoryOrderHints().getAsMap()).hasSize(3);
    }

    @Test
    public void resolveCategoryReferences_WithNullCategoryReferences_ShouldNotResolveReferences() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), emptySet());
        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefKey("anyProductTypeKey");

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

    @Ignore("TODO: SHOULD FAIL ON RESOLUTION IF NOT FOUND ON FETCH GITHUB ISSUE#219")
    @Test
    public void resolveCategoryReferences_WithANonExistentCategoryReference_ShouldNotResolveReference() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), emptySet());
        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefKey("anyProductTypeKey")
            .categories(singleton(Category.referenceOfId("non-existent-category")));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test and assertion
        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'category' on ProductDraft"
                + " with key:'" + productBuilder.getKey() + "'. Reason: Could not fetch Category with key.");
    }

    @Test
    public void resolveCategoryReferences_WithNullKeyOnCategoryReference_ShouldNotResolveReference() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), emptySet());
        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefKey("anyProductTypeKey")
            .categories(singleton(ResourceIdentifier.ofKey(null)));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test and assertion
        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve reference 'category' on ProductDraft with "
                + "key:'%s'. Reason: %s", productBuilder.getKey(), BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveCategoryReferences_WithEmptyIdOnCategoryReference_ShouldNotResolveReference() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final CategoryService mockCategoryService = mockCategoryService(emptySet(), emptySet());
        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefKey("anyProductTypeKey")
            .categories(singleton(ResourceIdentifier.ofKey("")));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test and assertion
        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve reference 'category' on ProductDraft with "
                + "key:'%s'. Reason: %s", productBuilder.getKey(), BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveCategoryReferences_WithExceptionCategoryFetch_ShouldNotResolveReference() {
        // preparation
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final Category category = getMockCategory("categoryKey", "categoryKey");
        final List<Category> categories = Collections.singletonList(category);
        final Set<ResourceIdentifier<Category>> categoryReferences = categories
            .stream()
            .map(cat -> ResourceIdentifier.<Category>ofKey(cat.getKey()))
            .collect(Collectors.toSet());

        final CategoryService mockCategoryService = mockCategoryService(new HashSet<>(categories), emptySet());
        final ProductDraftBuilder productBuilder =
            getBuilderWithProductTypeRefKey("anyProductTypeKey").categories(categoryReferences);

        final CompletableFuture<Set<Category>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(mockCategoryService.fetchMatchingCategoriesByKeys(anySet())).thenReturn(futureThrowingSphereException);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        // test and assertion
        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }
}
