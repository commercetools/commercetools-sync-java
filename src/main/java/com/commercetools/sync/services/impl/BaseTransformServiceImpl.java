package com.commercetools.sync.services.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.models.WithKey;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

public abstract class BaseTransformServiceImpl {
  /*
   * An id is a 36 characters long string. (i.e: 53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3) We
   * chunk them in 300 ids, we will have a query around 11.000 characters. Above this size it
   * could return - Error 413 (Request Entity Too Large)
   */
  public static final int CHUNK_SIZE = 300;
  public static final String KEY_IS_NOT_SET_PLACE_HOLDER = "KEY_IS_NOT_SET";
  protected final ReferenceIdToKeyCache referenceIdToKeyCache;

  private final SphereClient ctpClient;

  protected BaseTransformServiceImpl(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    this.ctpClient = ctpClient;
    this.referenceIdToKeyCache = referenceIdToKeyCache;
  }

  protected SphereClient getCtpClient() {
    return ctpClient;
  }

  protected CompletableFuture<Void> fetchAndFillReferenceIdToKeyCache(
      @Nonnull final Set<String> ids, @Nonnull final GraphQlQueryResources requestType) {

    final Set<String> nonCachedReferenceIds = getNonCachedReferenceIds(ids);
    if (nonCachedReferenceIds.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    final List<List<String>> chunkedIds = ChunkUtils.chunk(nonCachedReferenceIds, CHUNK_SIZE);

    List<ResourceIdsGraphQlRequest> collectedRequests =
        createResourceIdsGraphQlRequests(chunkedIds, requestType);

    return ChunkUtils.executeChunks(getCtpClient(), collectedRequests)
        .thenApply(ChunkUtils::flattenGraphQLBaseResults)
        .thenCompose(
            results -> {
              cacheResourceReferenceKeys(results);
              return CompletableFuture.completedFuture(null);
            });
  }

  @Nonnull
  protected List<ResourceIdsGraphQlRequest> createResourceIdsGraphQlRequests(
      @Nonnull final List<List<String>> chunkedIds,
      @Nonnull final GraphQlQueryResources resourceType) {
    return chunkedIds.stream()
        .map(chunk -> new ResourceIdsGraphQlRequest(new HashSet<>(chunk), resourceType))
        .collect(toList());
  }

  @Nonnull
  protected Set<String> getNonCachedReferenceIds(@Nonnull final Set<String> referenceIds) {
    return referenceIds.stream()
        .filter(
            id ->
                (!referenceIdToKeyCache.containsKey(id)
                    || KEY_IS_NOT_SET_PLACE_HOLDER.equals(referenceIdToKeyCache.get(id))))
        .collect(toSet());
  }

  protected void cacheResourceReferenceKeys(final Set<ResourceKeyId> results) {
    Optional.ofNullable(results)
        .orElseGet(Collections::emptySet)
        .forEach(
            resourceKeyId ->
                fillReferenceIdToKeyCache(resourceKeyId.getId(), resourceKeyId.getKey()));
  }

  protected <U extends Resource<U> & WithKey> void cacheResourceReferenceKeys(
      final List<U> results) {
    Optional.ofNullable(results)
        .orElseGet(Collections::emptyList)
        .forEach(resource -> fillReferenceIdToKeyCache(resource.getId(), resource.getKey()));
  }

  private void fillReferenceIdToKeyCache(String id, String key) {
    final String keyValue = StringUtils.isBlank(key) ? KEY_IS_NOT_SET_PLACE_HOLDER : key;
    referenceIdToKeyCache.add(id, keyValue);
  }
}
