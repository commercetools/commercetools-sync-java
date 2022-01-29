package com.commercetools.sync.products.helpers;

import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerGroup;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerGroupService;
import static com.commercetools.sync.products.helpers.PriceReferenceResolver.CUSTOMER_GROUP_DOES_NOT_EXIST;
import static com.commercetools.sync.products.helpers.PriceReferenceResolver.FAILED_TO_RESOLVE_REFERENCE;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.models.DefaultCurrencyUnits;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.utils.MoneyImpl;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceCustomerGroupReferenceResolverTest {
  private static final String CUSTOMER_GROUP_KEY = "customer-group-key_1";
  private static final String CUSTOMER_GROUP_ID = "1";

  private CustomerGroupService customerGroupService;
  private PriceReferenceResolver referenceResolver;

  /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    customerGroupService =
        getMockCustomerGroupService(getMockCustomerGroup(CUSTOMER_GROUP_ID, CUSTOMER_GROUP_KEY));
    ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    referenceResolver =
        new PriceReferenceResolver(
            syncOptions, mock(TypeService.class), mock(ChannelService.class), customerGroupService);
  }

  @Test
  void resolveCustomerGroupReference_WithKeys_ShouldResolveReference() {
    final ResourceIdentifier<CustomerGroup> customerGroupResourceIdentifier =
        ResourceIdentifier.ofKey("anyKey");
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(customerGroupResourceIdentifier);

    final PriceDraftBuilder resolvedDraft =
        referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getCustomerGroup()).isNotNull();
    assertThat(resolvedDraft.getCustomerGroup().getId()).isEqualTo(CUSTOMER_GROUP_ID);
  }

  @Test
  void resolveCustomerGroupReference_WithNullCustomerGroup_ShouldNotResolveReference() {
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR));

    assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
        .isCompletedWithValueMatching(resolvedDraft -> isNull(resolvedDraft.getCustomerGroup()));
  }

  @Test
  void resolveCustomerGroupReference_WithNonExistentCustomerGroup_ShouldNotResolveReference() {
    final ResourceIdentifier<CustomerGroup> customerGroupResourceIdentifier =
        ResourceIdentifier.ofKey("nonExistentKey");
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(customerGroupResourceIdentifier);

    when(customerGroupService.fetchCachedCustomerGroupId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                FAILED_TO_RESOLVE_REFERENCE,
                CustomerGroup.referenceTypeId(),
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                format(CUSTOMER_GROUP_DOES_NOT_EXIST, "nonExistentKey")));
  }

  @Test
  void
      resolveCustomerGroupReference_WithNullKeyOnCustomerGroupResourceIdentifier_ShouldNotResolveReference() {
    final ResourceIdentifier<CustomerGroup> customerGroupResourceIdentifier =
        ResourceIdentifier.ofKey(null);

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(customerGroupResourceIdentifier);

    assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                "Failed to resolve 'customer-group' reference on PriceDraft with country:'%s' and"
                    + " value: '%s'. Reason: %s",
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void
      resolveCustomerGroupReference_WithEmptyKeyOnCustomerGroupResourceIdentifier_ShouldNotResolveReference() {
    final ResourceIdentifier<CustomerGroup> customerGroupResourceIdentifier =
        ResourceIdentifier.ofKey("");
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(customerGroupResourceIdentifier);

    assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                "Failed to resolve 'customer-group' reference on PriceDraft with country:'%s' and"
                    + " value: '%s'. Reason: %s",
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomerGroupReference_WithExceptionOnFetch_ShouldNotResolveReference() {
    final ResourceIdentifier<CustomerGroup> customerGroupResourceIdentifier =
        ResourceIdentifier.ofKey("CustomerGroupKey");
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(customerGroupResourceIdentifier);

    final CompletableFuture<Optional<String>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
    when(customerGroupService.fetchCachedCustomerGroupId(anyString()))
        .thenReturn(futureThrowingSphereException);

    assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(SphereException.class)
        .withMessageContaining("CTP error on fetch");
  }
}
