package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.ShoppingListService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.states.helpers.StateReferenceResolver;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

public final class ShoppigListReferenceResolver
    extends BaseReferenceResolver<ShoppingListDraft, ShoppingListSyncOptions> {

    private final ShoppingListService shoppingListService;
    private static final String FAILED_TO_RESOLVE_REFERENCE = "Failed to resolve 'transition' reference on "
            + "StateDraft with key:'%s'. Reason: %s";

    /**
     * Takes a {@link ShoppingListSyncOptions} instance, a {@link StateService} to instantiate a
     * {@link StateReferenceResolver} instance that could be used to resolve the category drafts in the
     * CTP project specified in the injected {@link ShoppingListSyncOptions} instance.
     *
     * @param shoppingListSyncOptions   the container of all the options of the sync process including the CTP project client
     *                                  and/or configuration and other sync-specific options.
     * @param shoppingListService       the service to fetch the states for reference resolution.
     */
    public ShoppigListReferenceResolver(@Nonnull final ShoppingListSyncOptions shoppingListSyncOptions,
                                        @Nonnull final ShoppingListService shoppingListService) {
        super(shoppingListSyncOptions);
        this.shoppingListService = shoppingListService;
    }

    /**
     * Given a {@link ShoppingListDraft} this method attempts to resolve the attribute definition references to return
     * a {@link CompletionStage} which contains a new instance of the draft with the resolved references.
     *
     * @param shoppingListDraft the shoppingListDraft to resolve its references.
     * @return a {@link CompletionStage} that contains as a result a new shoppingListDraft instance with resolved
     *         references or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<ShoppingListDraft> resolveReferences(@Nonnull final ShoppingListDraft shoppingListDraft) {
        return completedFuture(shoppingListDraft);
    }


}
