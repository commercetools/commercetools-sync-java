package com.commercetools.sync.cartdiscounts.helpers;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;

public final class CartDiscountReferenceResolver
    extends CustomReferenceResolver<CartDiscountDraft, CartDiscountDraftBuilder, CartDiscountSyncOptions> {

    static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
        + "CartDiscountDraft with key:'%s'.";

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

    /**
     * Given a {@link CartDiscountDraft} this method attempts to resolve the custom type reference to
     * return a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * reference or, in case an error occurs during reference resolution, a {@link ReferenceResolutionException}.
     *
     * @param draft the CartDiscountDraft to resolve its references.
     * @return a {@link CompletionStage} that contains as a result a new CartDiscountDraft instance with resolved
     *          custom type reference or, in case an error occurs during reference resolution,
     *          a {@link ReferenceResolutionException}.
     */
    @Override
    @Nonnull
    public CompletionStage<CartDiscountDraft> resolveReferences(@Nonnull final CartDiscountDraft draft) {
        return resolveCustomTypeReference(CartDiscountDraftBuilder.of(draft))
            .thenApply(CartDiscountDraftBuilder::build);
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
