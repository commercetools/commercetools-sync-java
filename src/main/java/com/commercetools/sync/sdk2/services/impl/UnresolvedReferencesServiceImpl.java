package com.commercetools.sync.sdk2.services.impl;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

import com.commercetools.api.client.ByProjectKeyCustomObjectsByContainerGet;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.models.WaitingToBeResolved;
import com.commercetools.sync.sdk2.commons.utils.ChunkUtils;
import com.commercetools.sync.sdk2.services.UnresolvedReferencesService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UnresolvedReferencesServiceImpl<WaitingToBeResolvedT extends WaitingToBeResolved>
    implements UnresolvedReferencesService<WaitingToBeResolvedT> {

  private final BaseSyncOptions syncOptions;

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
  public CompletionStage<Set<WaitingToBeResolvedT>> fetch(
      @Nonnull final Set<String> keys,
      @Nonnull final String containerKey,
      @Nonnull final Class<WaitingToBeResolvedT> clazz) {

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

    final List<ByProjectKeyCustomObjectsByContainerGet> chunkedRequests =
        chunkedKeys.stream()
            .map(
                _keys ->
                    syncOptions
                        .getCtpClient()
                        .customObjects()
                        .withContainer(containerKey)
                        .get()
                        .withWhere("key in :keys")
                        .withPredicateVar("keys", _keys))
            .collect(toList());

    return ChunkUtils.executeChunks(chunkedRequests)
        .thenApply(
            apiHttpResponses ->
                apiHttpResponses.stream()
                    .map(apiHttpResponse -> apiHttpResponse.getBody().getResults())
                    .flatMap(List::stream)
                    .collect(toList()))
        .thenApply(
            customObjects ->
                customObjects.stream()
                    .map(customObject -> OBJECT_MAPPER.convertValue(customObject.getValue(), clazz))
                    .collect(toSet()));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<WaitingToBeResolvedT>> save(
      @Nonnull final WaitingToBeResolvedT draft,
      @Nonnull final String containerKey,
      @Nonnull final Class<WaitingToBeResolvedT> clazz) {
    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container(containerKey)
            .key(hash(draft.getKey()))
            .value(draft)
            .build();

    return syncOptions
        .getCtpClient()
        .customObjects()
        .post(customObjectDraft)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .handle(
            (resource, exception) -> {
              if (exception == null) {
                return Optional.of(OBJECT_MAPPER.convertValue(resource.getValue(), clazz));
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
  public CompletionStage<Optional<WaitingToBeResolvedT>> delete(
      @Nonnull final String key,
      @Nonnull final String containerKey,
      @Nonnull final Class<WaitingToBeResolvedT> clazz) {

    return syncOptions
        .getCtpClient()
        .customObjects()
        .withContainerAndKey(containerKey, hash(key))
        .delete()
        .execute()
        .handle(
            (resource, exception) -> {
              if (exception == null) {
                return Optional.of(
                    OBJECT_MAPPER.convertValue(resource.getBody().getValue(), clazz));
              } else {
                syncOptions.applyErrorCallback(
                    new SyncException(format(DELETE_FAILED, hash(key), key), exception));
                return Optional.empty();
              }
            });
  }
}
