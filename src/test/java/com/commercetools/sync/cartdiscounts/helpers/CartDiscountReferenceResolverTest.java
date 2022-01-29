package com.commercetools.sync.cartdiscounts.helpers;

import static com.commercetools.sync.cartdiscounts.helpers.CartDiscountReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.RelativeCartDiscountValue;
import io.sphere.sdk.cartdiscounts.ShippingCostTarget;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.DefaultCurrencyUnits;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.MoneyImpl;
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
        CartDiscountSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    typeService = getMockTypeService();
    referenceResolver = new CartDiscountReferenceResolver(syncOptions, typeService);
  }

  @Test
  void resolveReferences_WithoutReferences_ShouldNotResolveReferences() {
    final CartDiscountDraft cartDiscountDraft =
        CartDiscountDraftBuilder.of(
                "cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                CartDiscountValue.ofAbsolute(MoneyImpl.of(10, DefaultCurrencyUnits.EUR)))
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
    when(newCustomFieldsDraft.getType()).thenReturn(ResourceIdentifier.ofId(null));

    final CartDiscountDraftBuilder cartDiscountDraftBuilder =
        CartDiscountDraftBuilder.of(
                "cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                CartDiscountValue.ofAbsolute(MoneyImpl.of(10, DefaultCurrencyUnits.EUR)))
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
            format(
                "Failed to resolve custom type reference on CartDiscountDraft with "
                    + "key:'cart-discount-key'. Reason: %s",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void
      resolveCustomTypeReference_WithEmptyKeyOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
    final CartDiscountDraftBuilder cartDiscountDraftBuilder =
        CartDiscountDraftBuilder.of(
                "cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                CartDiscountValue.ofAbsolute(MoneyImpl.of(10, DefaultCurrencyUnits.EUR)))
            .key("cart-discount-key")
            .custom(CustomFieldsDraft.ofTypeKeyAndJson("", emptyMap()));

    assertThat(
            referenceResolver
                .resolveCustomTypeReference(cartDiscountDraftBuilder)
                .toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                "Failed to resolve custom type reference on CartDiscountDraft with "
                    + "key:'cart-discount-key'. Reason: %s",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
    final String customTypeKey = "customTypeKey";
    final CartDiscountDraftBuilder cartDiscountDraftBuilder =
        CartDiscountDraftBuilder.of(
                "cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                CartDiscountValue.ofAbsolute(MoneyImpl.of(10, DefaultCurrencyUnits.EUR)))
            .key("cart-discount-key")
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, emptyMap()));

    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    // Test and assertion
    final String expectedExceptionMessage =
        format(FAILED_TO_RESOLVE_CUSTOM_TYPE, cartDiscountDraftBuilder.getKey());
    final String expectedMessageWithCause =
        format(
            "%s Reason: %s", expectedExceptionMessage, format(TYPE_DOES_NOT_EXIST, customTypeKey));
    assertThat(referenceResolver.resolveCustomTypeReference(cartDiscountDraftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(expectedMessageWithCause);
  }

  @Test
  void resolveReferences_WithAllValidReferences_ShouldResolveReferences() {
    final CartDiscountDraft cartDiscountDraft =
        CartDiscountDraftBuilder.of(
                "cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                RelativeCartDiscountValue.of(10))
            .key("cart-discount-key")
            .custom(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", new HashMap<>()))
            .build();

    final CartDiscountDraft resolvedDraft =
        referenceResolver.resolveReferences(cartDiscountDraft).toCompletableFuture().join();

    final CartDiscountDraft expectedDraft =
        CartDiscountDraftBuilder.of(cartDiscountDraft)
            .custom(CustomFieldsDraft.ofTypeIdAndJson("typeId", new HashMap<>()))
            .build();

    assertThat(resolvedDraft).isEqualTo(expectedDraft);
  }
}
