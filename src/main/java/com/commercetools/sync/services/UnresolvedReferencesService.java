package com.commercetools.sync.services;


import com.commercetools.sync.commons.models.WaitingToBeResolved;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface UnresolvedReferencesService {

    /**
     * Given product draft keys, this method fetches the persisted drafts waiting to be resolved. If there is no
     * matching draft, an empty {@link Set} will be returned in the returned future.
     *
     * @param keys the keys of the persisted product drafts, waiting to be resolved, to fetch.
     * @return {@link CompletionStage}&lt;{@link Set}&gt; in which the result of its completion contains a
     *         {@link Set} that contains the matching drafts if any exist, otherwise empty.
     */
    @Nonnull
    CompletionStage<Set<WaitingToBeResolved>> fetch(@Nonnull final Set<String> keys);

    /**
     * Persists a product draft that is not ready to be resolved yet.
     *
     * @param draft the draft that should be persisted.
     * @return a {@link CompletionStage} containing an optional with the created resource if successful otherwise an
     *         empty optional.
     */
    @Nonnull
    CompletionStage<Optional<WaitingToBeResolved>> save(@Nonnull final WaitingToBeResolved draft);

    /**
     * Given a product draft key, this methods deletes the matching product draft from persistence.
     *
     * @param key the key of the product draft to delete from persistence.
     * @return a {@link CompletionStage} containing an optional with the deleted resource if successful otherwise an
     *     empty optional.
     */
    @Nonnull
    CompletionStage<Optional<WaitingToBeResolved>> delete(@Nonnull final String key);
}