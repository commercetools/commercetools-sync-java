package com.commercetools.sync.products.helpers.productreferenceresolver;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategory;
import static com.commercetools.sync.commons.MockUtils.getMockCategoryService;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductTypeService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockStateService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockTaxCategoryService;
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
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .allowUuidKeys(true)
                                                                               .build();

        final int nCategories = 10;
        final List<Category> categories = IntStream.range(0, nCategories)
                                                   .mapToObj(i -> i + "")
                                                   .map(key -> getMockCategory(key, key))
                                                   .collect(Collectors.toList());

        final CategoryService mockCategoryService = getMockCategoryService(categories);
        final List<Reference<Category>> categoryReferences = categories.stream()
                                                                       .map(Category::toReference)
                                                                       .collect(Collectors.toList());
        final Map<String, String> categoryOrderHintValues = categories.stream()
                                                                       .collect(Collectors.toMap(Category::getKey,
                                                                           Category::getId));

        final CategoryOrderHints categoryOrderHints = CategoryOrderHints.of(categoryOrderHintValues);

        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .categories(categoryReferences).categoryOrderHints(categoryOrderHints);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveCategoryReferences(productBuilder)
                                                                          .toCompletableFuture().join();

        assertThat(resolvedDraft.getCategories()).isNotNull();
        assertThat(resolvedDraft.getCategories()).hasSize(nCategories);
        assertThat(resolvedDraft.getCategories()).containsOnlyElementsOf(categoryReferences);

        assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
        assertThat(resolvedDraft.getCategoryOrderHints().getAsMap()).hasSize(categoryOrderHintValues.size());
    }

    @Test
    public void resolveCategoryReferences_WithCategoryKeysAndNoCategoryOrderHints_ShouldResolveReferences() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .allowUuidKeys(true)
                                                                               .build();

        final int nCategories = 10;
        final List<Category> categories = IntStream.range(0, nCategories)
                                                   .mapToObj(i -> i + "")
                                                   .map(key -> getMockCategory(key, key))
                                                   .collect(Collectors.toList());

        final CategoryService mockCategoryService = getMockCategoryService(categories);
        final List<Reference<Category>> categoryReferences = categories.stream()
                                                                       .map(Category::toReference)
                                                                       .collect(Collectors.toList());

        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .categories(categoryReferences);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveCategoryReferences(productBuilder)
                                                                          .toCompletableFuture().join();

        assertThat(resolvedDraft.getCategories()).isNotNull();
        assertThat(resolvedDraft.getCategories()).hasSize(nCategories);
        assertThat(resolvedDraft.getCategories()).containsOnlyElementsOf(categoryReferences);

        assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
        assertThat(resolvedDraft.getCategoryOrderHints().getAsMap()).isEmpty();
    }

    @Test
    public void resolveCategoryReferences_WithCategoryKeysAndSomeCategoryOrderHints_ShouldResolveReferences() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .allowUuidKeys(true)
                                                                               .build();

        final int nCategories = 10;
        final List<Category> categories = IntStream.range(0, nCategories)
                                                   .mapToObj(i -> i + "")
                                                   .map(key -> getMockCategory(key, key))
                                                   .collect(Collectors.toList());

        final CategoryService mockCategoryService = getMockCategoryService(categories);
        final List<Reference<Category>> categoryReferences = categories.stream()
                                                                       .map(Category::toReference)
                                                                       .collect(Collectors.toList());
        final Map<String, String> categoryOrderHintValues = categories.stream().limit(3)
                                                                      .collect(Collectors.toMap(Category::getKey,
                                                                          (category -> "0.00" + category.getId())));

        final CategoryOrderHints categoryOrderHints = CategoryOrderHints.of(categoryOrderHintValues);

        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .categories(categoryReferences).categoryOrderHints(categoryOrderHints);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveCategoryReferences(productBuilder)
                                                                          .toCompletableFuture().join();

        assertThat(resolvedDraft.getCategories()).isNotNull();
        assertThat(resolvedDraft.getCategories()).hasSize(nCategories);
        assertThat(resolvedDraft.getCategories()).containsOnlyElementsOf(categoryReferences);

        assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
        assertThat(resolvedDraft.getCategoryOrderHints().getAsMap()).hasSize(3);
    }

    @Test
    public void resolveCategoryReferences_WithKeysAsUuidSetAndAllowed_ShouldResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .allowUuidKeys(true)
                                                                               .build();

        final int nCategories = 10;
        final List<Category> categories = IntStream.range(0, nCategories)
                                                   .mapToObj(i -> UUID.randomUUID().toString())
                                                   .map(key -> getMockCategory(key, key))
                                                   .collect(Collectors.toList());

        final CategoryService mockCategoryService = getMockCategoryService(categories);
        final List<Reference<Category>> categoryReferences = categories.stream()
                                                                       .map(Category::toReference)
                                                                       .collect(Collectors.toList());
        final Map<String, String> categoryOrderHintValues = categories.stream()
                                                                      .collect(Collectors.toMap(Category::getKey,
                                                                          Resource::getId));

        final CategoryOrderHints categoryOrderHints = CategoryOrderHints.of(categoryOrderHintValues);

        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .categories(categoryReferences).categoryOrderHints(categoryOrderHints);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveCategoryReferences(productBuilder)
                                                                          .toCompletableFuture().join();

        assertThat(resolvedDraft.getCategories()).isNotNull();
        assertThat(resolvedDraft.getCategories()).hasSize(nCategories);
        assertThat(resolvedDraft.getCategories()).containsOnlyElementsOf(categoryReferences);

        assertThat(resolvedDraft.getCategoryOrderHints()).isNotNull();
        assertThat(resolvedDraft.getCategoryOrderHints().getAsMap()).hasSize(categoryOrderHintValues.size());
    }

    @Test
    public void resolveCategoryReferences_WithKeysAsUuidSetAndNotAllowed_ShouldResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .allowUuidKeys(false)
                                                                               .build();

        final int nCategories = 10;
        final List<Category> categories = IntStream.range(0, nCategories)
                                                   .mapToObj(i -> UUID.randomUUID().toString())
                                                   .map(key -> getMockCategory(key, key))
                                                   .collect(Collectors.toList());

        final CategoryService mockCategoryService = getMockCategoryService(categories);
        final List<Reference<Category>> categoryReferences = categories.stream()
                                                                       .map(Category::toReference)
                                                                       .collect(Collectors.toList());
        final Map<String, String> categoryOrderHintValues = categories.stream()
                                                                      .collect(Collectors.toMap(Category::getKey,
                                                                          Resource::getId));

        final CategoryOrderHints categoryOrderHints = CategoryOrderHints.of(categoryOrderHintValues);

        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .categories(categoryReferences).categoryOrderHints(categoryOrderHints);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'category' on ProductDraft"
                + " with key:'" + productBuilder.getKey() + "'. Reason: Found a UUID"
                + " in the id field. Expecting a key without a UUID value. If you want to"
                + " allow UUID values for reference keys, please use the "
                + "allowUuidKeys(true) option in the sync options.");
    }

    @Test
    public void resolveCategoryReferences_WithNullCategoryReferences_ShouldNotResolveReferences() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();


        final CategoryService mockCategoryService = getMockCategoryService(Collections.emptyList());
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid();

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft -> resolvedDraft.getCategories().isEmpty());
    }

    @Ignore("TODO: SHOULD FAIL ON RESOLUTION IF NOT FOUND ON FETCH GITHUB ISSUE#219")
    @Test
    public void resolveCategoryReferences_WithANonExistentCategoryReference_ShouldNotResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();

        final CategoryService mockCategoryService = getMockCategoryService(Collections.emptyList());
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .categories(Collections.singleton(Category.referenceOfId("non-existent-category")));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'category' on ProductDraft"
                + " with key:'" + productBuilder.getKey() + "'. Reason: Could not fetch Category with key.");
    }

    @Test
    public void resolveCategoryReferences_WithNullIdOnCategoryReference_ShouldNotResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();

        final CategoryService mockCategoryService = getMockCategoryService(Collections.emptyList());
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .categories(Collections.singleton(Category.referenceOfId(null)));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'category' on ProductDraft"
                + " with key:'" + productBuilder.getKey() + "'. Reason: Reference 'id' field value is blank "
                + "(null/empty).");
    }

    @Test
    public void resolveCategoryReferences_WithEmptyIdOnCategoryReference_ShouldNotResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();

        final CategoryService mockCategoryService = getMockCategoryService(Collections.emptyList());
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .categories(Collections.singleton(Category.referenceOfId("")));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'category' on ProductDraft"
                + " with key:'" + productBuilder.getKey() + "'. Reason: Reference 'id' field value is blank "
                + "(null/empty).");
    }

    @Test
    public void resolveCategoryReferences_WithExceptionCategoryFetch_ShouldNotResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final Category category = getMockCategory("categoryKey", "categoryKey");
        final List<Category> categoryList = Collections.singletonList(category);
        final List<Reference<Category>> references = categoryList.stream().map(Category::toReference)
                                                                 .collect(Collectors.toList());

        final CategoryService mockCategoryService = getMockCategoryService(categoryList);
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid().categories(references);

        final CompletableFuture<Set<Category>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(mockCategoryService.fetchMatchingCategoriesByKeys(anySet())).thenReturn(futureThrowingSphereException);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mockCategoryService, getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        assertThat(productReferenceResolver.resolveCategoryReferences(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Nonnull
    private static ProductDraftBuilder getBuilderWithProductTypeRefId(@Nonnull final String refId) {
        return ProductDraftBuilder.of(ProductType.referenceOfId(refId),
            LocalizedString.ofEnglish("testName"),
            LocalizedString.ofEnglish("testSlug"),
            (ProductVariantDraft)null);
    }

    @Nonnull
    private static ProductDraftBuilder getBuilderWithRandomProductTypeUuid() {
        return getBuilderWithProductTypeRefId(UUID.randomUUID().toString());
    }

}
