package com.commercetools.sync.sdk2.services;

import com.commercetools.sync.sdk2.commons.models.WaitingToBeResolved;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public interface UnresolvedReferencesService<WaitingToBeResolvedT extends WaitingToBeResolved> {

  /**
   * Given draft keys, this method fetches the persisted drafts waiting to be resolved. If there is
   * no matching draft, an empty {@link java.util.Set} will be returned in the returned future.
   *
   * @param keys the keys of the persisted drafts, waiting to be resolved, to fetch.
   * @param containerKey the key of the container, which contains the persisted draft
   * @param clazz Class of the object contained by custom object
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Set}&gt; in which the
   *     result of its completion contains a {@link java.util.Set} that contains the matching drafts
   *     if any exist, otherwise empty.
   */
  @Nonnull
  CompletionStage<Set<WaitingToBeResolvedT>> fetch(
      @Nonnull final Set<String> keys,
      @Nonnull final String containerKey,
      @Nonnull Class<WaitingToBeResolvedT> clazz);

  /**
   * Persists a draft that is not ready to be resolved yet.
   *
   * @param draft the draft that should be persisted.
   * @param containerKey the key of the container, which contains the persisted draft
   * @param clazz Class of the object contained by custom object
   * @return a {@link java.util.concurrent.CompletionStage} containing an optional with the created
   *     resource if successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<WaitingToBeResolvedT>> save(
      @Nonnull final WaitingToBeResolvedT draft,
      @Nonnull final String containerKey,
      @Nonnull final Class<WaitingToBeResolvedT> clazz);

  /**
   * Given a draft key, this methods deletes the matching draft from persistence.
   *
   * @param key the key of the draft to delete from persistence.
   * @param containerKey the key of the container, which contains the persisted draft
   * @param clazz Class of the object contained by custom object
   * @return a {@link java.util.concurrent.CompletionStage} containing an optional with the deleted
   *     resource if successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<WaitingToBeResolvedT>> delete(
      @Nonnull final String key,
      @Nonnull final String containerKey,
      @Nonnull final Class<WaitingToBeResolvedT> clazz);
}
