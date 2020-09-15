package com.commercetools.sync.products.helpers.productreferenceresolver;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TaxCategoryService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.taxcategories.TaxCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getBuilderWithRandomProductType;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductTypeService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockStateService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockTaxCategoryService;
import static com.commercetools.sync.products.helpers.ProductReferenceResolver.FAILED_TO_RESOLVE_REFERENCE;
import static com.commercetools.sync.products.helpers.ProductReferenceResolver.TAX_CATEGORY_DOES_NOT_EXIST;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaxCategoryReferenceResolverTest {
    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = "1";
    private static final String PRODUCT_TYPE_ID = "productTypeId";
    private static final String TAX_CATEGORY_ID = "taxCategoryId";
    private static final String STATE_ID = "stateId";
    private static final String PRODUCT_ID = "productId";

    private ProductReferenceResolver referenceResolver;
    private TaxCategoryService taxCategoryService;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @BeforeEach
    void setup() {
        taxCategoryService = getMockTaxCategoryService(TAX_CATEGORY_ID);
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new ProductReferenceResolver(syncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mock(CategoryService.class), getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            taxCategoryService, getMockStateService(STATE_ID), getMockProductService(PRODUCT_ID));
    }

    @Test
    void resolveTaxCategoryReference_WithKeys_ShouldResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .taxCategory(ResourceIdentifier.ofKey("taxCategoryKey"));

        final ProductDraftBuilder resolvedDraft = referenceResolver.resolveTaxCategoryReference(productBuilder)
                                                                   .toCompletableFuture().join();

        assertThat(resolvedDraft.getTaxCategory()).isNotNull();
        assertThat(resolvedDraft.getTaxCategory().getId()).isEqualTo(TAX_CATEGORY_ID);
    }

    @Test
    void resolveTaxCategoryReference_WithNullTaxCategory_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .key("dummyKey");

        assertThat(referenceResolver.resolveTaxCategoryReference(productBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft -> Objects.isNull(resolvedDraft.getTaxCategory()));
    }

    @Test
    void resolveTaxCategoryReference_WithNonExistentTaxCategory_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .taxCategory(ResourceIdentifier.ofKey("nonExistentKey"))
            .key("dummyKey");

        when(taxCategoryService.fetchCachedTaxCategoryId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final String expectedMessageWithCause = format(FAILED_TO_RESOLVE_REFERENCE, TaxCategory.resourceTypeId(),
            "dummyKey", format(TAX_CATEGORY_DOES_NOT_EXIST, "nonExistentKey"));

        referenceResolver.resolveTaxCategoryReference(productBuilder)
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
    void resolveTaxCategoryReference_WithNullIdOnTaxCategoryReference_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .taxCategory(ResourceIdentifier.ofKey(null))
            .key("dummyKey");

        assertThat(referenceResolver.resolveTaxCategoryReference(productBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve 'tax-category' resource identifier on ProductDraft with "
                + "key:'%s'. Reason: %s", productBuilder.getKey(), BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    void resolveTaxCategoryReference_WithEmptyIdOnTaxCategoryReference_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .taxCategory(ResourceIdentifier.ofKey(""))
            .key("dummyKey");

        assertThat(referenceResolver.resolveTaxCategoryReference(productBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve 'tax-category' resource identifier on ProductDraft with "
                + "key:'%s'. Reason: %s", productBuilder.getKey(), BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    void resolveTaxCategoryReference_WithExceptionOnTaxCategoryFetch_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .taxCategory(ResourceIdentifier.ofKey("taxCategoryKey"))
            .key("dummyKey");

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(taxCategoryService.fetchCachedTaxCategoryId(anyString())).thenReturn(futureThrowingSphereException);

        assertThat(referenceResolver.resolveTaxCategoryReference(productBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    void resolveTaxCategoryReference_WithIdOnTaxCategoryReference_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType()
            .taxCategory(ResourceIdentifier.ofId("existing-id"))
            .key("dummyKey");

        assertThat(referenceResolver.resolveTaxCategoryReference(productBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft -> Objects.equals(resolvedDraft.getTaxCategory(),
                productBuilder.getTaxCategory()));
    }
}
