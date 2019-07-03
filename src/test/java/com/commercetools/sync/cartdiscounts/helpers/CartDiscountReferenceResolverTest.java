package com.commercetools.sync.cartdiscounts.helpers;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CartDiscountReferenceResolverTest {

    private CartDiscountReferenceResolver referenceResolver;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @BeforeEach
    void setup() {
        final CartDiscountSyncOptions syncOptions = CartDiscountSyncOptionsBuilder
            .of(mock(SphereClient.class)).build();
        referenceResolver = new CartDiscountReferenceResolver(syncOptions, getMockTypeService());
    }

    @Test
    void resolveReferences_WithNoReferences_ShouldNotResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                CartDiscountValue.ofAbsolute(MoneyImpl.of(10, DefaultCurrencyUnits.EUR)))
            .key("cart-discount-key")
            .build();

        final CartDiscountDraft resolvedDraft = referenceResolver
            .resolveReferences(cartDiscountDraft)
            .toCompletableFuture()
            .join();

        assertThat(resolvedDraft).isEqualTo(cartDiscountDraft);
    }

    @Test
    void resolveCustomTypeReference_WithNullIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CustomFieldsDraft newCustomFieldsDraft = mock(CustomFieldsDraft.class);
        when(newCustomFieldsDraft.getType()).thenReturn(ResourceIdentifier.ofId(null));

        final CartDiscountDraftBuilder cartDiscountDraftBuilder = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                CartDiscountValue.ofAbsolute(MoneyImpl.of(10, DefaultCurrencyUnits.EUR)))
            .key("cart-discount-key")
            .custom(newCustomFieldsDraft);

        assertThat(referenceResolver.resolveCustomTypeReference(cartDiscountDraftBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve custom type reference on CartDiscountDraft with "
                + "key:'cart-discount-key'. Reason: %s", BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    void resolveCustomTypeReference_WithEmptyIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CartDiscountDraftBuilder cartDiscountDraftBuilder = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                CartDiscountValue.ofAbsolute(MoneyImpl.of(10, DefaultCurrencyUnits.EUR)))
            .key("cart-discount-key")
            .custom(CustomFieldsDraft.ofTypeIdAndObjects("", emptyMap()));

        assertThat(referenceResolver.resolveCustomTypeReference(cartDiscountDraftBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve custom type reference on CartDiscountDraft with "
                + "key:'cart-discount-key'. Reason: %s", BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    void resolveReferences_WithAllValidReferences_ShouldResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                RelativeCartDiscountValue.of(10))
            .key("cart-discount-key")
            .custom(CustomFieldsDraft.ofTypeIdAndJson("typeKey", new HashMap<>()))
            .build();

        final CartDiscountDraft resolvedDraft = referenceResolver
            .resolveReferences(cartDiscountDraft)
            .toCompletableFuture()
            .join();

        final CartDiscountDraft expectedDraft = CartDiscountDraftBuilder
            .of(cartDiscountDraft)
            .custom(CustomFieldsDraft.ofTypeIdAndJson("typeId", new HashMap<>()))
            .build();

        assertThat(resolvedDraft).isEqualTo(expectedDraft);
    }
}