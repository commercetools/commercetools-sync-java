package com.commercetools.sync.products.helpers.productreferenceresolver;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.StateService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.states.State;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

public class StateReferenceResolverTest {
    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = "1";
    private static final String PRODUCT_TYPE_ID = "productTypeId";
    private static final String TAX_CATEGORY_ID = "taxCategoryId";
    private static final String STATE_ID = "stateId";
    private static final String PRODUCT_ID = "productId";

    private StateService stateService;
    private ProductReferenceResolver referenceResolver;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        stateService = getMockStateService(STATE_ID);
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new ProductReferenceResolver(syncOptions, getMockProductTypeService(PRODUCT_TYPE_ID),
            mock(CategoryService.class), getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), stateService,getMockProductService(PRODUCT_ID));
    }

    @Test
    public void resolveStateReference_WithKeysAsUuidSetAndAllowed_ShouldResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .allowUuidKeys(true)
                                                                               .build();
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .state(State.referenceOfId(UUID.randomUUID().toString()));

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID), mock(CategoryService.class), getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), stateService, getMockProductService(PRODUCT_ID));

        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveStateReference(productBuilder)
                                                                          .toCompletableFuture().join();

        assertThat(resolvedDraft.getState()).isNotNull();
        assertThat(resolvedDraft.getState().getId()).isEqualTo(STATE_ID);
    }

    @Test
    public void resolveStateReference_WithKeys_ShouldResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .state(State.referenceOfId("stateKey"));

        final ProductDraftBuilder resolvedDraft = referenceResolver.resolveStateReference(productBuilder)
                                                                   .toCompletableFuture().join();

        assertThat(resolvedDraft.getState()).isNotNull();
        assertThat(resolvedDraft.getState().getId()).isEqualTo(STATE_ID);
    }

    @Test
    public void resolveStateReference_WithKeysAsUuidSetAndNotAllowed_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .state(State.referenceOfId(UUID.randomUUID().toString()))
            .key("dummyKey");

        assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve reference 'state' on ProductDraft"
                + " with key:'" + productBuilder.getKey() + "'. Reason: Found a UUID"
                + " in the id field. Expecting a key without a UUID value. If you want to"
                + " allow UUID values for reference keys, please use the "
                + "allowUuidKeys(true) option in the sync options.");
    }

    @Test
    public void resolveStateReference_WithNullState_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .key("dummyKey");

        assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft -> Objects.isNull(resolvedDraft.getState()));
    }

    @Test
    public void resolveStateReference_WithNonExistentState_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .state(State.referenceOfId("nonExistentKey"))
            .key("dummyKey");

        when(stateService.fetchCachedStateId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft ->
                Objects.nonNull(resolvedDraft.getState())
                    && Objects.equals(resolvedDraft.getState().getId(), "nonExistentKey"));
    }

    @Test
    public void resolveStateReference_WithNullIdOnStateReference_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .state(Reference.of(State.referenceTypeId(), (String)null))
            .key("dummyKey");

        assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve reference 'state' on ProductDraft with "
                + "key:'%s'. Reason: %s", productBuilder.getKey(), BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveStateReference_WithEmptyIdOnStateReference_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .state(State.referenceOfId(""))
            .key("dummyKey");

        assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve reference 'state' on ProductDraft with "
                + "key:'%s'. Reason: %s", productBuilder.getKey(), BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveStateReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid()
            .state(State.referenceOfId("stateKey"))
            .key("dummyKey");

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(stateService.fetchCachedStateId(anyString())).thenReturn(futureThrowingSphereException);

        assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }
}
