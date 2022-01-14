package com.commercetools.sync.products.helpers.productreferenceresolver;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getBuilderWithRandomProductType;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomObjectService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductTypeService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockStateService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockTaxCategoryService;
import static com.commercetools.sync.products.helpers.ProductReferenceResolver.FAILED_TO_RESOLVE_REFERENCE;
import static com.commercetools.sync.products.helpers.ProductReferenceResolver.STATE_DOES_NOT_EXIST;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.StateService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.states.State;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateReferenceResolverTest {
  private static final String CHANNEL_KEY = "channel-key_1";
  private static final String CHANNEL_ID = "1";
  private static final String PRODUCT_TYPE_ID = "productTypeId";
  private static final String TAX_CATEGORY_ID = "taxCategoryId";
  private static final String STATE_ID = "stateId";
  private static final String PRODUCT_ID = "productId";
  private static final String CUSTOM_OBJECT_ID = "customObjectId";
  private static final String CUSTOMER_ID = "customerId";

  private StateService stateService;
  private ProductReferenceResolver referenceResolver;

  /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    stateService = getMockStateService(STATE_ID);
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    referenceResolver =
        new ProductReferenceResolver(
            syncOptions,
            getMockProductTypeService(PRODUCT_TYPE_ID),
            mock(CategoryService.class),
            getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID),
            stateService,
            getMockProductService(PRODUCT_ID),
            getMockCustomObjectService(CUSTOM_OBJECT_ID),
            getMockCustomerService(CUSTOMER_ID));
  }

  @Test
  void resolveStateReference_WithKeys_ShouldResolveReference() {
    final ProductDraftBuilder productBuilder =
        getBuilderWithRandomProductType().state(ResourceIdentifier.ofKey("stateKey"));

    final ProductDraftBuilder resolvedDraft =
        referenceResolver.resolveStateReference(productBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getState()).isNotNull();
    assertThat(resolvedDraft.getState().getId()).isEqualTo(STATE_ID);
  }

  @Test
  void resolveStateReference_WithNullState_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder = getBuilderWithRandomProductType().key("dummyKey");

    assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
        .isCompletedWithValueMatching(resolvedDraft -> Objects.isNull(resolvedDraft.getState()));
  }

  @Test
  void resolveStateReference_WithNonExistentState_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder =
        getBuilderWithRandomProductType()
            .state(ResourceIdentifier.ofKey("nonExistentKey"))
            .key("dummyKey");

    when(stateService.fetchCachedStateId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final String expectedMessageWithCause =
        format(
            FAILED_TO_RESOLVE_REFERENCE,
            State.resourceTypeId(),
            "dummyKey",
            format(STATE_DOES_NOT_EXIST, "nonExistentKey"));

    referenceResolver
        .resolveStateReference(productBuilder)
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
  void resolveStateReference_WithNullKeyOnStateReference_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder =
        getBuilderWithRandomProductType().state(ResourceIdentifier.ofKey(null)).key("dummyKey");

    assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                "Failed to resolve 'state' resource identifier on ProductDraft with "
                    + "key:'%s'. Reason: %s",
                productBuilder.getKey(), BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveStateReference_WithEmptyKeyOnStateReference_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder =
        getBuilderWithRandomProductType().state(ResourceIdentifier.ofKey("")).key("dummyKey");

    assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                "Failed to resolve 'state' resource identifier on ProductDraft with "
                    + "key:'%s'. Reason: %s",
                productBuilder.getKey(), BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveStateReference_WithExceptionOnFetch_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder =
        getBuilderWithRandomProductType()
            .state(ResourceIdentifier.ofKey("stateKey"))
            .key("dummyKey");

    final CompletableFuture<Optional<String>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
    when(stateService.fetchCachedStateId(anyString())).thenReturn(futureThrowingSphereException);

    assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(SphereException.class)
        .withMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveStateReference_WithIdOnStateReference_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder =
        getBuilderWithRandomProductType()
            .state(ResourceIdentifier.ofId("existing-id"))
            .key("dummyKey");

    assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
        .isCompleted()
        .isCompletedWithValueMatching(
            resolvedDraft -> Objects.equals(resolvedDraft.getState(), productBuilder.getState()));
  }
}
