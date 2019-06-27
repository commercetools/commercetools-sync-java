package com.commercetools.sync.cartdiscounts.helpers;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.GiftLineItemCartDiscountValue;
import io.sphere.sdk.models.ResourceIdentifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class CartDiscountReferenceResolver
    extends CustomReferenceResolver<CartDiscountDraft, CartDiscountDraftBuilder, CartDiscountSyncOptions> {

    private static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
        + "CartDiscount with key:'%s'.";
    private static final String FAILED_TO_RESOLVE_GIFTLINEITEM = "Failed to resolve a GiftLineItem resourceIdentifier "
        + "on the CartDiscount with key:'%s'.";
    private static final String BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER = "The value of the 'key' field of the "
        + "resourceIdentifier of the '%s' field is blank (null/empty).";


    /**
     * Takes a {@link CartDiscountSyncOptions} instance, a  {@link TypeService} to instantiate a
     * {@link CartDiscountReferenceResolver} instance that could be used to resolve the cartDiscount drafts in the
     * CTP project specified in the injected {@link CartDiscountSyncOptions} instance.
     *
     * @param options     the container of all the options of the sync process including the CTP project client
     *                    and/or configuration and other sync-specific options.
     * @param typeService the service to fetch the custom types for reference resolution.
     */
    public CartDiscountReferenceResolver(
        @Nonnull final CartDiscountSyncOptions options,
        @Nonnull final TypeService typeService) {
        super(options, typeService);
    }

    @Override
    public CompletionStage<CartDiscountDraft> resolveReferences(@Nonnull final CartDiscountDraft draft) {
        return resolveCustomTypeReference(CartDiscountDraftBuilder.of(draft))
            .thenCompose(this::validateReferences)
            .thenApply(CartDiscountDraftBuilder::build);
    }

    @Nonnull
    private CompletionStage<CartDiscountDraftBuilder> validateReferences(
        @Nonnull final CartDiscountDraftBuilder cartDiscountDraftBuilder) {

        final CartDiscountValue cartDiscountValue = cartDiscountDraftBuilder.getValue();
        if (cartDiscountValue instanceof GiftLineItemCartDiscountValue) {
            try {
                validateReferences((GiftLineItemCartDiscountValue) cartDiscountValue);
            } catch (ReferenceResolutionException referenceResolutionException) {
                return exceptionallyCompletedFuture(
                    new ReferenceResolutionException(
                        format(FAILED_TO_RESOLVE_GIFTLINEITEM, cartDiscountDraftBuilder.getKey()),
                        referenceResolutionException));
            }
        }
        return CompletableFuture.completedFuture(cartDiscountDraftBuilder);
    }

    private void validateReferences(@Nonnull final GiftLineItemCartDiscountValue giftLineItemCartDiscountValue)
        throws ReferenceResolutionException {

        validateResourceIdentifier(giftLineItemCartDiscountValue.getProduct(), "product");
        validateResourceIdentifier(giftLineItemCartDiscountValue.getDistributionChannel(), "distributionChannel");
        validateResourceIdentifier(giftLineItemCartDiscountValue.getSupplyChannel(), "supplyChannel");
    }

    private <T> void validateResourceIdentifier(@Nullable final ResourceIdentifier<T> resourceIdentifier,
                                                @Nonnull final String fieldName)
        throws ReferenceResolutionException {

        if (resourceIdentifier != null && isBlank(resourceIdentifier.getKey())) {
            throw new ReferenceResolutionException(format(BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER, fieldName));
        }
    }

    @Override
    protected CompletionStage<CartDiscountDraftBuilder> resolveCustomTypeReference(
        @Nonnull final CartDiscountDraftBuilder draftBuilder) {

        return resolveCustomTypeReference(draftBuilder,
            CartDiscountDraftBuilder::getCustom,
            CartDiscountDraftBuilder::custom,
            format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getKey()));
    }
}
