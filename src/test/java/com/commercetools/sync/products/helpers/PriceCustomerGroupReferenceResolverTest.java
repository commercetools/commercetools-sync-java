package com.commercetools.sync.products.helpers;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.DefaultCurrencyUnits;
import com.commercetools.api.models.common.MoneyBuilder;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifier;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.products.ProductSyncMockUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TypeService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PriceCustomerGroupReferenceResolverTest {
  private static final String CUSTOMER_GROUP_KEY = "customer-group-key_1";
  private static final String CUSTOMER_GROUP_ID = "1";

  private CustomerGroupService customerGroupService;
  private PriceReferenceResolver referenceResolver;

  /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    customerGroupService =
        ProductSyncMockUtils.getMockCustomerGroupService(
            ProductSyncMockUtils.getMockCustomerGroup(CUSTOMER_GROUP_ID, CUSTOMER_GROUP_KEY));
    ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    referenceResolver =
        new PriceReferenceResolver(
            syncOptions,
            Mockito.mock(TypeService.class),
            Mockito.mock(ChannelService.class),
            customerGroupService);
  }

  @Test
  void resolveCustomerGroupReference_WithKeys_ShouldResolveReference() {
    final CustomerGroupResourceIdentifier customerGroupResourceIdentifier =
        CustomerGroupResourceIdentifierBuilder.of().key("anyKey").build();
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .customerGroup(customerGroupResourceIdentifier);

    final PriceDraftBuilder resolvedDraft =
        referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getCustomerGroup()).isNotNull();
    assertThat(resolvedDraft.getCustomerGroup().getId()).isEqualTo(CUSTOMER_GROUP_ID);
  }

  @Test
  void resolveCustomerGroupReference_WithNullCustomerGroup_ShouldNotResolveReference() {
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build());

    assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
        .isCompletedWithValueMatching(resolvedDraft -> isNull(resolvedDraft.getCustomerGroup()));
  }

  @Test
  void resolveCustomerGroupReference_WithNonExistentCustomerGroup_ShouldNotResolveReference() {
    final CustomerGroupResourceIdentifier customerGroupResourceIdentifier =
        CustomerGroupResourceIdentifierBuilder.of().key("nonExistentKey").build();
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .customerGroup(customerGroupResourceIdentifier);

    when(customerGroupService.fetchCachedCustomerGroupId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                PriceReferenceResolver.FAILED_TO_RESOLVE_REFERENCE,
                CustomerGroup.referenceTypeId(),
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                String.format(
                    PriceReferenceResolver.CUSTOMER_GROUP_DOES_NOT_EXIST, "nonExistentKey")));
  }

  @Test
  void
      resolveCustomerGroupReference_WithNullKeyOnCustomerGroupResourceIdentifier_ShouldNotResolveReference() {
    final CustomerGroupResourceIdentifier customerGroupResourceIdentifier =
        CustomerGroupResourceIdentifierBuilder.of().key(null).build();

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .customerGroup(customerGroupResourceIdentifier);

    assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve 'customer-group' reference on PriceDraft with country:'%s' and"
                    + " value: '%s'. Reason: %s",
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void
      resolveCustomerGroupReference_WithEmptyKeyOnCustomerGroupResourceIdentifier_ShouldNotResolveReference() {
    final CustomerGroupResourceIdentifier customerGroupResourceIdentifier =
        CustomerGroupResourceIdentifierBuilder.of().key("").build();
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .customerGroup(customerGroupResourceIdentifier);

    assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve 'customer-group' reference on PriceDraft with country:'%s' and"
                    + " value: '%s'. Reason: %s",
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomerGroupReference_WithExceptionOnFetch_ShouldNotResolveReference() {
    final CustomerGroupResourceIdentifier customerGroupResourceIdentifier =
        CustomerGroupResourceIdentifierBuilder.of().key("CustomerGroupKey").build();
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .customerGroup(customerGroupResourceIdentifier);

    final CompletableFuture<Optional<String>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(
        new ReferenceResolutionException("CTP error on fetch"));
    when(customerGroupService.fetchCachedCustomerGroupId(anyString()))
        .thenReturn(futureThrowingSphereException);

    assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining("CTP error on fetch");
  }
}
