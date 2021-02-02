package com.commercetools.sync.services;

import com.commercetools.sync.commons.models.WaitingToBeResolved;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public interface UnresolvedReferencesService<T extends WaitingToBeResolved> {

  /**
   * Given draft keys, this method fetches the persisted drafts waiting to be resolved. If there is
   * no matching draft, an empty {@link Set} will be returned in the returned future.
   *
   * @param keys the keys of the persisted drafts, waiting to be resolved, to fetch.
   * @param containerKey the key of the container, which contains the persisted draft
   * @param clazz Class of the object contained by custom object
   * @return {@link CompletionStage}&lt;{@link Set}&gt; in which the result of its completion
   *     contains a {@link Set} that contains the matching drafts if any exist, otherwise empty.
   */
  @Nonnull
  CompletionStage<Set<T>> fetch(
      @Nonnull final Set<String> keys,
      @Nonnull final String containerKey,
      @Nonnull Class<? extends WaitingToBeResolved> clazz);

  /**
   * Persists a draft that is not ready to be resolved yet.
   *
   * @param draft the draft that should be persisted.
   * @param containerKey the key of the container, which contains the persisted draft
   * @param clazz Class of the object contained by custom object
   * @return a {@link CompletionStage} containing an optional with the created resource if
   *     successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<T>> save(
      @Nonnull final T draft, @Nonnull final String containerKey, @Nonnull final Class clazz);

  /**
   * Given a draft key, this methods deletes the matching draft from persistence.
   *
   * @param key the key of the draft to delete from persistence.
   * @param containerKey the key of the container, which contains the persisted draft
   * @param clazz Class of the object contained by custom object
   * @return a {@link CompletionStage} containing an optional with the deleted resource if
   *     successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<T>> delete(
      @Nonnull final String key, @Nonnull final String containerKey, @Nonnull final Class<T> clazz);
}
