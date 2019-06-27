package com.commercetools.sync.cartdiscounts.helpers;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.GiftLineItemCartDiscountValue;
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
import java.util.UUID;

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
    void resolveReferences_WithGiftLineItemAllValidResourceIdentifiers_ShouldResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                GiftLineItemCartDiscountValue.of(
                    ResourceIdentifier.ofKey("productKey"),
                    1,
                    ResourceIdentifier.ofKey("supplyChannelKey"),
                    ResourceIdentifier.ofKey("distributionChannelKey")))
            .key("cart-discount-key")
            .build();

        final CartDiscountDraft resolvedDraft = referenceResolver
            .resolveReferences(cartDiscountDraft)
            .toCompletableFuture()
            .join();

        assertThat(resolvedDraft).isEqualTo(cartDiscountDraft);
    }

    @Test
    void resolveReferences_WithGiftLineItemWithProductIdResourceIdentifier_ShouldNotResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                GiftLineItemCartDiscountValue.of(
                    ResourceIdentifier.ofId(UUID.randomUUID().toString()),
                    1,
                    ResourceIdentifier.ofKey("supplyChannelKey"),
                    ResourceIdentifier.ofKey("distributionChannelKey")))
            .key("cart-discount-key")
            .build();

        assertThat(referenceResolver.resolveReferences(cartDiscountDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .satisfies(exception -> {
                assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception).hasMessage("Failed to resolve a GiftLineItem resourceIdentifier"
                    + " on the CartDiscount with key:'cart-discount-key'.");
                assertThat(exception.getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception.getCause()).hasMessage("The value of the 'key' field of the "
                    + "resourceIdentifier of the 'product' field is blank (null/empty).");
            });
    }

    @Test
    void resolveReferences_WithGiftLineItemWithProductAndDistChannelIdResourceIdentifiers_ShouldNotResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                GiftLineItemCartDiscountValue.of(
                    ResourceIdentifier.ofId(UUID.randomUUID().toString()),
                    1,
                    ResourceIdentifier.ofKey("supplyChannelKey"),
                    ResourceIdentifier.ofId(UUID.randomUUID().toString())))
            .key("cart-discount-key")
            .build();

        assertThat(referenceResolver.resolveReferences(cartDiscountDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .satisfies(exception -> {
                assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception).hasMessage("Failed to resolve a GiftLineItem resourceIdentifier"
                    + " on the CartDiscount with key:'cart-discount-key'.");
                assertThat(exception.getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception.getCause()).hasMessage("The value of the 'key' field of the "
                    + "resourceIdentifier of the 'product' field is blank (null/empty).");
            });
    }

    @Test
    void resolveReferences_WithGiftLineItemWithEmptyProductKeyResourceIdentifier_ShouldNotResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                GiftLineItemCartDiscountValue.of(
                    ResourceIdentifier.ofKey(""),
                    1,
                    ResourceIdentifier.ofKey("supplyChannelKey"),
                    ResourceIdentifier.ofKey("distributionChannelKey")))
            .key("cart-discount-key")
            .build();

        assertThat(referenceResolver.resolveReferences(cartDiscountDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .satisfies(exception -> {
                assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception).hasMessage("Failed to resolve a GiftLineItem resourceIdentifier"
                    + " on the CartDiscount with key:'cart-discount-key'.");
                assertThat(exception.getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception.getCause()).hasMessage("The value of the 'key' field of the "
                    + "resourceIdentifier of the 'product' field is blank (null/empty).");
            });
    }

    @Test
    void resolveReferences_WithGiftLineItemWithSupplyChannelIdResourceIdentifier_ShouldNotResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                GiftLineItemCartDiscountValue.of(
                    ResourceIdentifier.ofKey("productKey"),
                    1,
                    ResourceIdentifier.ofId(UUID.randomUUID().toString()),
                    ResourceIdentifier.ofKey("distributionChannelKey")))
            .key("cart-discount-key")
            .build();

        assertThat(referenceResolver.resolveReferences(cartDiscountDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .satisfies(exception -> {
                assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception).hasMessage("Failed to resolve a GiftLineItem resourceIdentifier"
                    + " on the CartDiscount with key:'cart-discount-key'.");
                assertThat(exception.getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception.getCause()).hasMessage("The value of the 'key' field of the "
                    + "resourceIdentifier of the 'supplyChannel' field is blank (null/empty).");
            });
    }

    @Test
    void resolveReferences_WithGiftLineItemWithEmptySupplyChannelKeyResourceIdentifier_ShouldNotResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                GiftLineItemCartDiscountValue.of(
                    ResourceIdentifier.ofKey("productKey"),
                    1,
                    ResourceIdentifier.ofKey(""),
                    ResourceIdentifier.ofKey("distributionChannelKey")))
            .key("cart-discount-key")
            .build();

        assertThat(referenceResolver.resolveReferences(cartDiscountDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .satisfies(exception -> {
                assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception).hasMessage("Failed to resolve a GiftLineItem resourceIdentifier"
                    + " on the CartDiscount with key:'cart-discount-key'.");
                assertThat(exception.getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception.getCause()).hasMessage("The value of the 'key' field of the "
                    + "resourceIdentifier of the 'supplyChannel' field is blank (null/empty).");
            });
    }

    @Test
    void resolveReferences_WithGiftLineItemWithNullSupplyChannelResourceIdentifier_ShouldResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                GiftLineItemCartDiscountValue.of(
                    ResourceIdentifier.ofKey("productKey"),
                    1,
                    null,
                    ResourceIdentifier.ofKey("distributionChannelKey")))
            .key("cart-discount-key")
            .build();

        final CartDiscountDraft resolvedDraft = referenceResolver
            .resolveReferences(cartDiscountDraft)
            .toCompletableFuture()
            .join();

        assertThat(resolvedDraft).isEqualTo(cartDiscountDraft);
    }

    @Test
    void resolveReferences_WithGiftLineItemWithDistChannelIdResourceIdentifier_ShouldNotResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                GiftLineItemCartDiscountValue.of(
                    ResourceIdentifier.ofKey("productKey"),
                    1,
                    null,
                    ResourceIdentifier.ofId(UUID.randomUUID().toString())))
            .key("cart-discount-key")
            .build();

        assertThat(referenceResolver.resolveReferences(cartDiscountDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .satisfies(exception -> {
                assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception).hasMessage("Failed to resolve a GiftLineItem resourceIdentifier"
                    + " on the CartDiscount with key:'cart-discount-key'.");
                assertThat(exception.getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception.getCause()).hasMessage("The value of the 'key' field of the "
                    + "resourceIdentifier of the 'distributionChannel' field is blank (null/empty).");
            });
    }

    @Test
    void resolveReferences_WithGiftLineItemWithEmptyDistChannelKeyResourceIdentifier_ShouldNotResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                GiftLineItemCartDiscountValue.of(
                    ResourceIdentifier.ofKey("productKey"),
                    1,
                    null,
                    ResourceIdentifier.ofKey("")))
            .key("cart-discount-key")
            .build();

        assertThat(referenceResolver.resolveReferences(cartDiscountDraft).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .satisfies(exception -> {
                assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception).hasMessage("Failed to resolve a GiftLineItem resourceIdentifier"
                    + " on the CartDiscount with key:'cart-discount-key'.");
                assertThat(exception.getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception.getCause()).hasMessage("The value of the 'key' field of the "
                    + "resourceIdentifier of the 'distributionChannel' field is blank (null/empty).");
            });
    }

    @Test
    void resolveReferences_WithGiftLineItemWithNullDistChannelResourceIdentifier_ShouldResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                GiftLineItemCartDiscountValue.of(
                    ResourceIdentifier.ofKey("productKey"),
                    1,
                    null,
                    null))
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
            .hasMessage(format("Failed to resolve custom type reference on CartDiscount with key:'cart-discount-key'. "
                    + "Reason: %s", BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
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
            .hasMessage(format("Failed to resolve custom type reference on CartDiscount with key:'cart-discount-key'. "
                + "Reason: %s", BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    void resolveReferences_WithAllValidReferences_ShouldResolveReferences() {
        final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder
            .of("cartPredicate",
                LocalizedString.ofEnglish("foo"),
                true,
                "0.1",
                ShippingCostTarget.of(),
                GiftLineItemCartDiscountValue.of(
                    ResourceIdentifier.ofKey("productKey"),
                    1,
                    ResourceIdentifier.ofKey("supplyChannelKey"),
                    ResourceIdentifier.ofKey("distributionChannelKey")))
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