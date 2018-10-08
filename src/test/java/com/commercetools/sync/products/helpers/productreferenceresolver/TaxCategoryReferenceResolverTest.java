package com.commercetools.sync.products.helpers.productreferenceresolver;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TaxCategoryService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.taxcategories.TaxCategory;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getBuilderWithRandomProductTypeUuid;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductTypeService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockStateService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockTaxCategoryService;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaxCategoryReferenceResolverTest {
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
    @Before
    public void setup() {
        taxCategoryService = getMockTaxCategoryService(TAX_CATEGORY_ID);
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new ProductReferenceResolver(syncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mock(CategoryService.class), getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)), mock(CustomerGroupService.class),
            taxCategoryService, getMockStateService(STATE_ID), getMockProductService(PRODUCT_ID));
    }
    @Test
    public void resolveTaxCategoryReference_WithKeys_ShouldResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .taxCategory(TaxCategory.referenceOfId("taxCategoryKey"));

        final ProductDraftBuilder resolvedDraft = referenceResolver.resolveTaxCategoryReference(productBuilder)
                                                                   .toCompletableFuture().join();

        assertThat(resolvedDraft.getTaxCategory()).isNotNull();
        assertThat(resolvedDraft.getTaxCategory().getId()).isEqualTo(TAX_CATEGORY_ID);
    }

    @Test
    public void resolveTaxCategoryReference_WithNullTaxCategory_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .key("dummyKey");

        assertThat(referenceResolver.resolveTaxCategoryReference(productBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft -> Objects.isNull(resolvedDraft.getTaxCategory()));
    }

    @Test
    public void resolveTaxCategoryReference_WithNonExistentTaxCategory_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .taxCategory(TaxCategory.referenceOfId("nonExistentKey"))
            .key("dummyKey");

        when(taxCategoryService.fetchCachedTaxCategoryId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThat(referenceResolver.resolveTaxCategoryReference(productBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft ->
                Objects.nonNull(resolvedDraft.getTaxCategory())
                    && Objects.equals(resolvedDraft.getTaxCategory().getId(), "nonExistentKey"));
    }

    @Test
    public void resolveTaxCategoryReference_WithNullIdOnTaxCategoryReference_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .taxCategory(Reference.of(TaxCategory.referenceTypeId(), (String)null))
            .key("dummyKey");

        assertThat(referenceResolver.resolveTaxCategoryReference(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve reference 'tax-category' on ProductDraft with "
                + "key:'%s'. Reason: %s", productBuilder.getKey(), BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveTaxCategoryReference_WithEmptyIdOnTaxCategoryReference_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .taxCategory(TaxCategory.referenceOfId(""))
            .key("dummyKey");

        assertThat(referenceResolver.resolveTaxCategoryReference(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve reference 'tax-category' on ProductDraft with "
                + "key:'%s'. Reason: %s", productBuilder.getKey(), BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveTaxCategoryReference_WithExceptionOnTaxCategoryFetch_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .taxCategory(TaxCategory.referenceOfId("taxCategoryKey"))
            .key("dummyKey");

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(taxCategoryService.fetchCachedTaxCategoryId(anyString())).thenReturn(futureThrowingSphereException);

        assertThat(referenceResolver.resolveTaxCategoryReference(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }
}
