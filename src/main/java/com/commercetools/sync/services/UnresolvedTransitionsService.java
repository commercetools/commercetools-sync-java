package com.commercetools.sync.services;

import com.commercetools.sync.commons.models.WaitingToBeResolvedTransitions;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public interface UnresolvedTransitionsService {

  /**
   * Given state draft keys, this method fetches the persisted drafts waiting to be resolved. If
   * there is no matching draft, an empty {@link Set} will be returned in the returned future.
   *
   * @param keys the keys of the persisted state drafts, waiting to be resolved, to fetch.
   * @return {@link CompletionStage}&lt;{@link Set}&gt; in which the result of its completion
   *     contains a {@link Set} that contains the matching drafts if any exist, otherwise empty.
   */
  @Nonnull
  CompletionStage<Set<WaitingToBeResolvedTransitions>> fetch(@Nonnull final Set<String> keys);

  /**
   * Persists a state draft that is not ready to be resolved yet.
   *
   * @param draft the draft that should be persisted.
   * @return a {@link CompletionStage} containing an optional with the created resource if
   *     successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<WaitingToBeResolvedTransitions>> save(
      @Nonnull final WaitingToBeResolvedTransitions draft);

  /**
   * Given a state draft key, this methods deletes the matching state draft from persistence.
   *
   * @param key the key of the state draft to delete from persistence.
   * @return a {@link CompletionStage} containing an optional with the deleted resource if
   *     successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<WaitingToBeResolvedTransitions>> delete(@Nonnull final String key);
}
