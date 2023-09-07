package com.commercetools.sync.cartdiscounts.helpers;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart_discount.*;
import com.commercetools.api.models.common.DefaultCurrencyUnits;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.common.MoneyBuilder;
import com.commercetools.api.models.type.*;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.commons.MockUtils;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.services.TypeService;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartDiscountReferenceResolverTest {

  private CartDiscountReferenceResolver referenceResolver;
  private TypeService typeService;

  /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    final CartDiscountSyncOptions syncOptions =
        CartDiscountSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    typeService = MockUtils.getMockTypeService();
    referenceResolver = new CartDiscountReferenceResolver(syncOptions, typeService);
  }

  @Test
  void resolveReferences_WithoutReferences_ShouldNotResolveReferences() {
    final CartDiscountDraft cartDiscountDraft =
        CartDiscountDraftBuilder.of()
            .cartPredicate("cartPredicate")
            .name(LocalizedString.ofEnglish("foo"))
            .requiresDiscountCode(true)
            .sortOrder("0.1")
            .target(CartDiscountTargetBuilder::shippingBuilder)
            .value(
                CartDiscountValueAbsoluteDraftBuilder.of()
                    .money(
                        MoneyBuilder.of()
                            .centAmount(10L)
                            .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                            .build())
                    .build())
            .key("cart-discount-key")
            .build();

    final CartDiscountDraft resolvedDraft =
        referenceResolver.resolveReferences(cartDiscountDraft).toCompletableFuture().join();

    assertThat(resolvedDraft).isEqualTo(cartDiscountDraft);
  }

  @Test
  void
      resolveCustomTypeReference_WithNullKeyOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
    final CustomFieldsDraft newCustomFieldsDraft = mock(CustomFieldsDraft.class);
    when(newCustomFieldsDraft.getType()).thenReturn(TypeResourceIdentifierBuilder.of().build());

    final CartDiscountDraftBuilder cartDiscountDraftBuilder =
        CartDiscountDraftBuilder.of()
            .cartPredicate("cartPredicate")
            .name(LocalizedString.ofEnglish("foo"))
            .requiresDiscountCode(true)
            .sortOrder("0.1")
            .target(CartDiscountTargetBuilder::shippingBuilder)
            .value(
                CartDiscountValueAbsoluteDraftBuilder.of()
                    .money(
                        MoneyBuilder.of()
                            .centAmount(10L)
                            .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                            .build())
                    .build())
            .key("cart-discount-key")
            .custom(newCustomFieldsDraft);

    assertThat(
            referenceResolver
                .resolveCustomTypeReference(cartDiscountDraftBuilder)
                .toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve custom type reference on CartDiscountDraft with "
                    + "key:'cart-discount-key'. Reason: %s",
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void
      resolveCustomTypeReference_WithEmptyKeyOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
    final CartDiscountDraftBuilder cartDiscountDraftBuilder =
        CartDiscountDraftBuilder.of()
            .cartPredicate("cartPredicate")
            .name(LocalizedString.ofEnglish("foo"))
            .requiresDiscountCode(true)
            .sortOrder("0.1")
            .target(CartDiscountTargetBuilder::shippingBuilder)
            .value(
                CartDiscountValueAbsoluteDraftBuilder.of()
                    .money(
                        MoneyBuilder.of()
                            .centAmount(10L)
                            .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                            .build())
                    .build())
            .key("cart-discount-key")
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key("").build())
                    .build());

    assertThat(
            referenceResolver
                .resolveCustomTypeReference(cartDiscountDraftBuilder)
                .toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve custom type reference on CartDiscountDraft with "
                    + "key:'cart-discount-key'. Reason: %s",
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
    final String customTypeKey = "customTypeKey";
    final TypeResourceIdentifier typeResourceIdentifier =
        TypeResourceIdentifierBuilder.of().key(customTypeKey).build();
    final CartDiscountDraftBuilder cartDiscountDraftBuilder =
        CartDiscountDraftBuilder.of()
            .cartPredicate("cartPredicate")
            .name(LocalizedString.ofEnglish("foo"))
            .requiresDiscountCode(true)
            .sortOrder("0.1")
            .target(CartDiscountTargetBuilder::shippingBuilder)
            .value(
                CartDiscountValueAbsoluteDraftBuilder.of()
                    .money(
                        MoneyBuilder.of()
                            .centAmount(10L)
                            .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                            .build())
                    .build())
            .key("cart-discount-key")
            .custom(CustomFieldsDraftBuilder.of().type(typeResourceIdentifier).build());

    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    // Test and assertion
    final String expectedExceptionMessage =
        String.format(
            CartDiscountReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE,
            cartDiscountDraftBuilder.getKey());
    final String expectedMessageWithCause =
        format(
            "%s Reason: %s",
            expectedExceptionMessage,
            String.format(CustomReferenceResolver.TYPE_DOES_NOT_EXIST, customTypeKey));
    assertThat(referenceResolver.resolveCustomTypeReference(cartDiscountDraftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(expectedMessageWithCause);
  }

  @Test
  void resolveReferences_WithAllValidReferences_ShouldResolveReferences() {
    final TypeResourceIdentifier typeResourceIdentifier =
        TypeResourceIdentifierBuilder.of().key("typeKey").build();
    final FieldContainer fieldContainer =
        FieldContainerBuilder.of().values(new HashMap<>()).build();
    final CartDiscountDraft cartDiscountDraft =
        CartDiscountDraftBuilder.of()
            .cartPredicate("cartPredicate")
            .name(LocalizedString.ofEnglish("foo"))
            .requiresDiscountCode(true)
            .sortOrder("0.1")
            .target(CartDiscountTargetBuilder::shippingBuilder)
            .value(CartDiscountValueRelativeDraftBuilder.of().permyriad(10L).build())
            .key("cart-discount-key")
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(typeResourceIdentifier)
                    .fields(fieldContainer)
                    .build())
            .build();

    final CartDiscountDraft resolvedDraft =
        referenceResolver.resolveReferences(cartDiscountDraft).toCompletableFuture().join();

    final CartDiscountDraft expectedDraft =
        CartDiscountDraftBuilder.of(cartDiscountDraft)
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().id("typeId").build())
                    .fields(fieldContainer)
                    .build())
            .build();

    assertThat(resolvedDraft).isEqualTo(expectedDraft);
  }
}
