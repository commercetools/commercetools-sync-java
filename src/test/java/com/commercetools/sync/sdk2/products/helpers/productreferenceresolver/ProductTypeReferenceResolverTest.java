package com.commercetools.sync.sdk2.products.helpers.productreferenceresolver;

import static com.commercetools.sync.sdk2.commons.ExceptionUtils.createConcurrentModificationException;
import static com.commercetools.sync.sdk2.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.sdk2.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.sdk2.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.sdk2.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static com.commercetools.sync.sdk2.products.helpers.ProductReferenceResolver.FAILED_TO_RESOLVE_REFERENCE;
import static com.commercetools.sync.sdk2.products.helpers.ProductReferenceResolver.PRODUCT_TYPE_DOES_NOT_EXIST;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.ConcurrentModificationException;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.sdk2.services.CategoryService;
import com.commercetools.sync.sdk2.services.CustomerGroupService;
import com.commercetools.sync.sdk2.services.ProductTypeService;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductTypeReferenceResolverTest {
  private static final String CHANNEL_KEY = "channel-key_1";
  private static final String CHANNEL_ID = "1";
  private static final String PRODUCT_TYPE_ID = "productTypeId";
  private static final String TAX_CATEGORY_ID = "taxCategoryId";
  private static final String STATE_ID = "stateId";
  private static final String PRODUCT_ID = "productId";
  private static final String CUSTOM_OBJECT_ID = "customObjectId";
  private static final String CUSTOMER_ID = "customerId";

  private ProductTypeService productTypeService;
  private ProductReferenceResolver referenceResolver;

  /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    productTypeService = getMockProductTypeService(PRODUCT_TYPE_ID);
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    referenceResolver =
        new ProductReferenceResolver(
            syncOptions,
            productTypeService,
            mock(CategoryService.class),
            getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            mock(CustomerGroupService.class),
            getMockTaxCategoryService(TAX_CATEGORY_ID),
            getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID),
            getMockCustomObjectService(CUSTOM_OBJECT_ID),
            getMockCustomerService(CUSTOMER_ID));
  }

  @Test
  void resolveProductTypeReference_WithKeys_ShouldResolveReference() {
    final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefKey("productTypeKey");

    final ProductDraftBuilder resolvedDraft =
        referenceResolver.resolveProductTypeReference(productBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getProductType()).isNotNull();
    assertThat(resolvedDraft.getProductType().getId()).isEqualTo(PRODUCT_TYPE_ID);
  }

  @Test
  void resolveProductTypeReference_WithNonExistentProductType_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder =
        getBuilderWithProductTypeRefKey("anyKey").key("dummyKey");

    when(productTypeService.fetchCachedProductTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final String expectedMessageWithCause =
        format(
            FAILED_TO_RESOLVE_REFERENCE,
            ProductTypeReference.PRODUCT_TYPE,
            "dummyKey",
            format(PRODUCT_TYPE_DOES_NOT_EXIST, "anyKey"));

    referenceResolver
        .resolveProductTypeReference(productBuilder)
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
  void resolveProductTypeReference_WithNullKeyOnProductTypeReference_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder =
        getBuilderWithProductTypeRefKey(null).key("dummyKey");

    assertThat(referenceResolver.resolveProductTypeReference(productBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                "Failed to resolve '%s' resource identifier on ProductDraft"
                    + " with key:'%s'. Reason: %s",
                ProductTypeReference.PRODUCT_TYPE,
                productBuilder.getKey(),
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveProductTypeReference_WithEmptyKeyOnProductTypeReference_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefKey("").key("dummyKey");

    assertThat(referenceResolver.resolveProductTypeReference(productBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                "Failed to resolve '%s' resource identifier on ProductDraft"
                    + " with key:'%s'. Reason: %s",
                ProductTypeReference.PRODUCT_TYPE,
                productBuilder.getKey(),
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveProductTypeReference_WithExceptionOnProductTypeFetch_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder =
        getBuilderWithProductTypeRefKey("anyKey").key("dummyKey");

    final CompletableFuture<Optional<String>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(
        createConcurrentModificationException("CTP error on fetch"));
    when(productTypeService.fetchCachedProductTypeId(anyString()))
        .thenReturn(futureThrowingSphereException);

    assertThat(referenceResolver.resolveProductTypeReference(productBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ConcurrentModificationException.class)
        .withMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveProductTypeReference_WithIdOnProductTypeReference_ShouldNotResolveReference() {
    final ProductDraftBuilder productBuilder =
        getBuilderWithRandomProductType()
            .productType(ProductTypeResourceIdentifierBuilder.of().id("existing-id").build())
            .key("dummyKey");

    assertThat(referenceResolver.resolveProductTypeReference(productBuilder).toCompletableFuture())
        .isCompleted()
        .isCompletedWithValueMatching(
            resolvedDraft ->
                Objects.equals(resolvedDraft.getProductType(), productBuilder.getProductType()));
  }
}