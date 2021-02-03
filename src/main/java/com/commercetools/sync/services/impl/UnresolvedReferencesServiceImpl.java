package com.commercetools.sync.services.impl;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.services.UnresolvedReferencesService;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UnresolvedReferencesServiceImpl<T extends WaitingToBeResolved>
    implements UnresolvedReferencesService<T> {

  private final BaseSyncOptions syncOptions;

  private static final String SAVE_FAILED =
      "Failed to save CustomObject with key: '%s' (hash of product key: '%s').";
  private static final String DELETE_FAILED =
      "Failed to delete CustomObject with key: '%s' (hash of product key: '%s').";

  public static final String CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY =
      "commercetools-sync-java.UnresolvedReferencesService.productDrafts";
  public static final String CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY =
      "commercetools-sync-java.UnresolvedReferencesService.categoryDrafts";
  public static final String CUSTOM_OBJECT_TRANSITION_CONTAINER_KEY =
      "commercetools-sync-java.UnresolvedTransitionsService.stateDrafts";

  public UnresolvedReferencesServiceImpl(@Nonnull final BaseSyncOptions baseSyncOptions) {
    this.syncOptions = baseSyncOptions;
  }

  @Nonnull
  private String hash(@Nullable final String customObjectKey) {
    return sha1Hex(customObjectKey);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<T>> fetch(
      @Nonnull final Set<String> keys,
      @Nonnull final String containerKey,
      @Nonnull final Class<T> clazz) {

    if (keys.isEmpty()) {
      return CompletableFuture.completedFuture(Collections.emptySet());
    }

    /*
     * A key is a 41 characters long hashed string. (i.e: c25a67e262265fbc36edb33ed0375263586be278) We
     * chunk them in 250 keys we will have around a query around 11.000 characters. Above this size it
     * could return - Error 413 (Request Entity Too Large)
     */
    final int CHUNK_SIZE = 250;
    final Set<String> hashedKeys = keys.stream().map(this::hash).collect(Collectors.toSet());
    final List<List<String>> chunkedKeys = ChunkUtils.chunk(hashedKeys, CHUNK_SIZE);

    final List<CustomObjectQuery<T>> chunkedRequests =
        chunkedKeys.stream()
            .map(
                _keys ->
                    CustomObjectQuery.of(clazz)
                        .byContainer(containerKey)
                        .plusPredicates(p -> p.key().isIn(_keys)))
            .collect(toList());

    return ChunkUtils.executeChunks(syncOptions.getCtpClient(), chunkedRequests)
        .thenApply(ChunkUtils::flattenPagedQueryResults)
        .thenApply(
            customObjects -> customObjects.stream().map(CustomObject::getValue).collect(toSet()));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<T>> save(
      @Nonnull final T draft, @Nonnull final String containerKey, @Nonnull final Class<T> clazz) {
    final CustomObjectDraft<T> customObjectDraft =
        CustomObjectDraft.ofUnversionedUpsert(containerKey, hash(draft.getKey()), draft, clazz);

    return syncOptions
        .getCtpClient()
        .execute(CustomObjectUpsertCommand.of(customObjectDraft))
        .handle(
            (resource, exception) -> {
              if (exception == null) {
                return Optional.of((T) resource.getValue());
              } else {
                syncOptions.applyErrorCallback(
                    new SyncException(
                        format(SAVE_FAILED, customObjectDraft.getKey(), draft.getKey()),
                        exception));
                return Optional.empty();
              }
            });
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<T>> delete(
      @Nonnull final String key,
      @Nonnull final String containerKey,
      @Nonnull final Class<T> clazz) {

    return syncOptions
        .getCtpClient()
        .execute(CustomObjectDeleteCommand.of(containerKey, hash(key), clazz))
        .handle(
            (resource, exception) -> {
              if (exception == null) {
                return Optional.of(resource.getValue());
              } else {
                syncOptions.applyErrorCallback(
                    new SyncException(format(DELETE_FAILED, hash(key), key), exception));
                return Optional.empty();
              }
            });
  }
}
