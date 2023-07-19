package com.commercetools.sync.sdk2.products.helpers.productreferenceresolver;

import static com.commercetools.sync.sdk2.commons.ExceptionUtils.createBadGatewayException;
import static com.commercetools.sync.sdk2.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.sdk2.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.sdk2.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.sdk2.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static com.commercetools.sync.sdk2.products.helpers.ProductReferenceResolver.FAILED_TO_RESOLVE_REFERENCE;
import static com.commercetools.sync.sdk2.products.helpers.ProductReferenceResolver.STATE_DOES_NOT_EXIST;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.sdk2.services.CategoryService;
import com.commercetools.sync.sdk2.services.CustomerGroupService;
import com.commercetools.sync.sdk2.services.StateService;
import io.vrap.rmf.base.client.error.BadGatewayException;
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
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
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
        getBuilderWithRandomProductType()
            .state(StateResourceIdentifierBuilder.of().key("stateKey").build());

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
            .state(StateResourceIdentifierBuilder.of().key("nonExistentKey").build())
            .key("dummyKey");

    when(stateService.fetchCachedStateId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final String expectedMessageWithCause =
        format(
            FAILED_TO_RESOLVE_REFERENCE,
            StateResourceIdentifier.STATE,
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
        getBuilderWithRandomProductType()
            .state(StateResourceIdentifierBuilder.of().key(null).build())
            .key("dummyKey");

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
        getBuilderWithRandomProductType()
            .state(StateResourceIdentifierBuilder.of().key("").build())
            .key("dummyKey");

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
            .state(StateResourceIdentifierBuilder.of().key("stateKey").build())
            .key("dummyKey");

    final CompletableFuture<Optional<String>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(createBadGatewayException());
    when(stateService.fetchCachedStateId(anyString())).thenReturn(futureThrowingSphereException);

    assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class)
        .withMessageContaining("test");
  }

  @Test
  void resolveStateReference_WithIdOnStateReference_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder =
        getBuilderWithRandomProductType()
            .state(StateResourceIdentifierBuilder.of().id("existing-id").build())
            .key("dummyKey");

    assertThat(referenceResolver.resolveStateReference(productBuilder).toCompletableFuture())
        .isCompleted()
        .isCompletedWithValueMatching(
            resolvedDraft -> Objects.equals(resolvedDraft.getState(), productBuilder.getState()));
  }
}
