package com.commercetools.sync.sdk2.services.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.type.Type;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.services.TypeService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// todo: reuse duplicated code between TypeService and CustomerService
public final class TypeServiceImpl extends BaseService<BaseSyncOptions> implements TypeService {

  public TypeServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> typeKeys) {
    return super.cacheKeysToIds(typeKeys, GraphQlQueryResource.TYPES);
  }

  @Nonnull
  public CompletionStage<Optional<Type>> fetchTypeByKey(@Nullable final String key) {

    if (isBlank(key)) {
      return CompletableFuture.completedFuture(null);
    }

    return syncOptions
        .getCtpClient()
        .types()
        .withKey(key)
        .get()
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .thenApply(
            type -> {
              keyToIdCache.put(type.getKey(), type.getId());
              return Optional.of(type);
            })
        .exceptionally(
            throwable -> {
              if (throwable instanceof NotFoundException) {
                return Optional.empty();
              }
              // todo: what is the best way to handle this ?
              syncOptions.applyErrorCallback(new SyncException(throwable));
              return Optional.empty();
            });
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedTypeId(@Nonnull String key) {
    if (isBlank(key)) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    final String id = keyToIdCache.getIfPresent(key);
    if (id != null) {
      return CompletableFuture.completedFuture(Optional.of(id));
    }

    return fetchTypeByKey(key).thenApply(type -> type.map(Type::getId));
  }
}
