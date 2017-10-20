package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.MockUtils.getMockCategoryService;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductTypeService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockStateService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockTaxCategoryService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductReferenceResolverTest {
    private ProductTypeService productTypeService;
    private CategoryService categoryService;
    private TypeService typeService;
    private ChannelService channelService;
    private TaxCategoryService taxCategoryService;
    private StateService stateService;

    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = "1";
    private static final String PRODUCT_TYPE_ID = "productTypeId";
    private static final String TAX_CATEGORY_ID = "taxCategoryId";
    private static final String STATE_ID = "stateId";
    private ProductSyncOptions syncOptions;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        productTypeService = getMockProductTypeService(PRODUCT_TYPE_ID);
        categoryService = getMockCategoryService();
        typeService = getMockTypeService();
        channelService = getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY));
        taxCategoryService = getMockTaxCategoryService(TAX_CATEGORY_ID);
        stateService = getMockStateService(STATE_ID);
        syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    }

    @Test
    public void resolveProductTypeReference_WithKeysAsUuidSetAndAllowed_ShouldResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .setAllowUuidKeys(true)
                                                                               .build();
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getProductType()).thenReturn(ProductType.referenceOfId(UUID.randomUUID().toString()));


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        final ProductDraft resolvedDraft = productReferenceResolver.resolveProductTypeReference(productDraft)
                                                                   .toCompletableFuture().join();


        assertThat(resolvedDraft.getProductType()).isNotNull();
        assertThat(resolvedDraft.getProductType().getId()).isEqualTo(PRODUCT_TYPE_ID);
    }

    @Test
    public void resolveProductTypeReference_WithKeys_ShouldResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getProductType()).thenReturn(ProductType.referenceOfId("productTypeKey"));


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        final ProductDraft resolvedDraft = productReferenceResolver.resolveProductTypeReference(productDraft)
                                                                   .toCompletableFuture().join();


        assertThat(resolvedDraft.getProductType()).isNotNull();
        assertThat(resolvedDraft.getProductType().getId()).isEqualTo(PRODUCT_TYPE_ID);
    }

    @Test
    public void resolveProductTypeReference_WithKeysAsUuidSetAndNotAllowed_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getProductType()).thenReturn(ProductType.referenceOfId(UUID.randomUUID().toString()));


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);


        assertThat(productReferenceResolver.resolveProductTypeReference(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve product type reference on ProductDraft"
                + " with key:'" + productDraft.getKey() + "'. Reason: Found a UUID"
                + " in the id field. Expecting a key without a UUID value. If you want to"
                + " allow UUID values for reference keys, please use the "
                + "setAllowUuidKeys(true) option in the sync options.");
    }

    @Test
    public void resolveProductTypeReference_WithNonExistentProductType_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getProductType()).thenReturn(ProductType.referenceOfId("anyKey"));
        when(productDraft.getKey()).thenReturn("dummyKey");

        when(productTypeService.fetchCachedProductTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveProductTypeReference(productDraft).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft ->
                Objects.nonNull(resolvedDraft.getProductType())
                    && Objects.equals(resolvedDraft.getProductType().getId(), "anyKey"));
    }

    @Test
    public void resolveProductTypeReference_WithNullIdOnProductTypeReference_ShouldNotResolveReference() {
        final String productTypeKey = null;
        final Reference<ProductType> productTypeReference = Reference.of(ProductType.referenceTypeId(), productTypeKey);

        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getProductType()).thenReturn(productTypeReference);
        when(productDraft.getKey()).thenReturn("dummyKey");

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveProductTypeReference(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve product type reference on ProductDraft"
                + " with key:'" + productDraft.getKey() + "'. Reason: Reference 'id' field"
                + " value is blank (null/empty).");
    }

    @Test
    public void resolveProductTypeReference_WithEmptyIdOnProductTypeReference_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getProductType()).thenReturn(ProductType.referenceOfId(""));
        when(productDraft.getKey()).thenReturn("dummyKey");

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveProductTypeReference(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve product type reference on ProductDraft"
                + " with key:'" + productDraft.getKey() + "'. Reason: Reference 'id' field"
                + " value is blank (null/empty).");
    }

    @Test
    public void resolveProductTypeReference_WithExceptionOnProductTypeFetch_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getProductType()).thenReturn(ProductType.referenceOfId(PRODUCT_TYPE_ID));
        when(productDraft.getKey()).thenReturn("dummyKey");

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(productTypeService.fetchCachedProductTypeId(anyString())).thenReturn(futureThrowingSphereException);

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveProductTypeReference(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    public void resolveTaxCategoryReference_WithKeysAsUuidSetAndAllowed_ShouldResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .setAllowUuidKeys(true)
                                                                               .build();
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getTaxCategory()).thenReturn(TaxCategory.referenceOfId(UUID.randomUUID().toString()));


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        final ProductDraft resolvedDraft = productReferenceResolver.resolveTaxCategoryReferences(productDraft)
                                                                   .toCompletableFuture().join();

        assertThat(resolvedDraft.getTaxCategory()).isNotNull();
        assertThat(resolvedDraft.getTaxCategory().getId()).isEqualTo(TAX_CATEGORY_ID);
    }

    @Test
    public void resolveTaxCategoryReference_WithKeys_ShouldResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getTaxCategory()).thenReturn(TaxCategory.referenceOfId("taxCategoryKey"));


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        final ProductDraft resolvedDraft = productReferenceResolver.resolveTaxCategoryReferences(productDraft)
                                                                   .toCompletableFuture().join();

        assertThat(resolvedDraft.getTaxCategory()).isNotNull();
        assertThat(resolvedDraft.getTaxCategory().getId()).isEqualTo(TAX_CATEGORY_ID);
    }

    @Test
    public void resolveTaxCategoryReference_WithKeysAsUuidSetAndNotAllowed_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getTaxCategory()).thenReturn(TaxCategory.referenceOfId(UUID.randomUUID().toString()));
        when(productDraft.getKey()).thenReturn("dummyKey");


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveTaxCategoryReferences(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'tax-category' on ProductDraft"
                + " with key:'" + productDraft.getKey() + "'. Reason: Found a UUID"
                + " in the id field. Expecting a key without a UUID value. If you want to"
                + " allow UUID values for reference keys, please use the "
                + "setAllowUuidKeys(true) option in the sync options.");
    }

    @Test
    public void resolveTaxCategoryReference_WithNullTaxCategory_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("dummyKey");


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveTaxCategoryReferences(productDraft).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft -> Objects.isNull(resolvedDraft.getTaxCategory()));
    }

    @Test
    public void resolveTaxCategoryReference_WithNonExistentTaxCategory_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getTaxCategory()).thenReturn(TaxCategory.referenceOfId("nonExistentKey"));
        when(productDraft.getKey()).thenReturn("dummyKey");

        when(taxCategoryService.fetchCachedTaxCategoryId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveTaxCategoryReferences(productDraft).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft ->
                Objects.nonNull(resolvedDraft.getTaxCategory())
                    && Objects.equals(resolvedDraft.getTaxCategory().getId(), "nonExistentKey"));
    }

    @Test
    public void resolveTaxCategoryReference_WithNullIdOnTaxCategoryReference_ShouldNotResolveReference() {
        final String taxCategoryKey = null;
        final Reference<TaxCategory> reference = Reference.of(TaxCategory.referenceTypeId(), taxCategoryKey);
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getTaxCategory()).thenReturn(reference);
        when(productDraft.getKey()).thenReturn("dummyKey");

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveTaxCategoryReferences(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'tax-category' on ProductDraft with "
                + "key:'" + productDraft.getKey() + "'. Reason: Reference 'id' field"
                + " value is blank (null/empty).");
    }

    @Test
    public void resolveTaxCategoryReference_WithEmptyIdOnTaxCategoryReference_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getTaxCategory()).thenReturn(TaxCategory.referenceOfId(""));
        when(productDraft.getKey()).thenReturn("dummyKey");

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveTaxCategoryReferences(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'tax-category' on ProductDraft with "
                + "key:'" + productDraft.getKey() + "'. Reason: Reference 'id' field"
                + " value is blank (null/empty).");
    }

    @Test
    public void resolveTaxCategoryReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getTaxCategory()).thenReturn(TaxCategory.referenceOfId("taxCategoryKey"));
        when(productDraft.getKey()).thenReturn("dummyKey");

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(taxCategoryService.fetchCachedTaxCategoryId(anyString())).thenReturn(futureThrowingSphereException);


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveTaxCategoryReferences(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }


    @Test
    public void resolveStateReference_WithKeysAsUuidSetAndAllowed_ShouldResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .setAllowUuidKeys(true)
                                                                               .build();
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getState()).thenReturn(State.referenceOfId(UUID.randomUUID().toString()));


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        final ProductDraft resolvedDraft = productReferenceResolver.resolveStateReferences(productDraft)
                                                                   .toCompletableFuture().join();

        assertThat(resolvedDraft.getState()).isNotNull();
        assertThat(resolvedDraft.getState().getId()).isEqualTo(STATE_ID);
    }

    @Test
    public void resolveStateReference_WithKeys_ShouldResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getState()).thenReturn(State.referenceOfId("stateKey"));


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        final ProductDraft resolvedDraft = productReferenceResolver.resolveStateReferences(productDraft)
                                                                   .toCompletableFuture().join();

        assertThat(resolvedDraft.getState()).isNotNull();
        assertThat(resolvedDraft.getState().getId()).isEqualTo(STATE_ID);
    }

    @Test
    public void resolveStateReference_WithKeysAsUuidSetAndNotAllowed_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getState()).thenReturn(State.referenceOfId(UUID.randomUUID().toString()));
        when(productDraft.getKey()).thenReturn("dummyKey");


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveStateReferences(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'state' on ProductDraft"
                + " with key:'" + productDraft.getKey() + "'. Reason: Found a UUID"
                + " in the id field. Expecting a key without a UUID value. If you want to"
                + " allow UUID values for reference keys, please use the "
                + "setAllowUuidKeys(true) option in the sync options.");
    }

    @Test
    public void resolveStateReference_WithNullState_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("dummyKey");


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveStateReferences(productDraft).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft -> Objects.isNull(resolvedDraft.getState()));
    }

    @Test
    public void resolveStateReference_WithNonExistentState_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getState()).thenReturn(State.referenceOfId("nonExistentKey"));
        when(productDraft.getKey()).thenReturn("dummyKey");

        when(stateService.fetchCachedStateId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveStateReferences(productDraft).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft ->
                Objects.nonNull(resolvedDraft.getState())
                    && Objects.equals(resolvedDraft.getState().getId(), "nonExistentKey"));
    }

    @Test
    public void resolveStateReference_WithNullIdOnStateReference_ShouldNotResolveReference() {
        final String stateKey = null;
        final Reference<State> reference = Reference.of(State.referenceTypeId(), stateKey);
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getState()).thenReturn(reference);
        when(productDraft.getKey()).thenReturn("dummyKey");

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveStateReferences(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'state' on ProductDraft with "
                + "key:'" + productDraft.getKey() + "'. Reason: Reference 'id' field"
                + " value is blank (null/empty).");
    }

    @Test
    public void resolveStateReference_WithEmptyIdOnStateReference_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getState()).thenReturn(State.referenceOfId(""));
        when(productDraft.getKey()).thenReturn("dummyKey");

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveStateReferences(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'state' on ProductDraft with "
                + "key:'" + productDraft.getKey() + "'. Reason: Reference 'id' field"
                + " value is blank (null/empty).");
    }

    @Test
    public void resolveStateReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReference() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getState()).thenReturn(State.referenceOfId("stateKey"));
        when(productDraft.getKey()).thenReturn("dummyKey");

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(stateService.fetchCachedStateId(anyString())).thenReturn(futureThrowingSphereException);


        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        assertThat(productReferenceResolver.resolveStateReferences(productDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }


    @Test
    public void resolveReferences_WithNoCategoryReferencesAndNoChannelReferences_ShouldResolveAvailableReferences() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getProductType()).thenReturn(ProductType.referenceOfId("productTypeKey"));
        when(productDraft.getState()).thenReturn(State.referenceOfId("stateKey"));
        when(productDraft.getTaxCategory()).thenReturn(TaxCategory.referenceOfId("taxCategoryKey"));
        when(productDraft.getKey()).thenReturn("dummyKey");

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(syncOptions,
            productTypeService, categoryService, typeService, channelService, taxCategoryService, stateService);

        final ProductDraft referencesResolvedDraft = productReferenceResolver.resolveReferences(productDraft)
                                                                             .toCompletableFuture().join();

        assertThat(referencesResolvedDraft).isNotNull();
        assertThat(referencesResolvedDraft.getProductType()).isNotNull();
        assertThat(referencesResolvedDraft.getProductType().getId()).isEqualTo(PRODUCT_TYPE_ID);

        assertThat(referencesResolvedDraft.getTaxCategory()).isNotNull();
        assertThat(referencesResolvedDraft.getTaxCategory().getId()).isEqualTo(TAX_CATEGORY_ID);

        assertThat(referencesResolvedDraft.getState()).isNotNull();
        assertThat(referencesResolvedDraft.getState().getId()).isEqualTo(STATE_ID);
    }

}
