package com.commercetools.sync.sdk2.services.impl;

import com.commercetools.api.client.ByProjectKeyTypesGet;
import com.commercetools.api.client.ByProjectKeyTypesKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyTypesPost;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.services.TypeService;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// todo: reuse duplicated code between TypeService and CustomerService
public final class TypeServiceImpl
    extends BaseService<
        BaseSyncOptions,
        Type,
        TypeDraft,
        ByProjectKeyTypesGet,
        ByProjectKeyTypesKeyByKeyGet,
        Type,
        ByProjectKeyTypesPost>
    implements TypeService {

  public TypeServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> typeKeys) {
    return super.cacheKeysToIds(typeKeys, GraphQlQueryResource.TYPES);
  }

  @Nonnull
  private CompletionStage<Optional<Type>> fetchTypeByKey(@Nullable final String key) {
    return super.fetchResource(key, syncOptions.getCtpClient().types().withKey(key).get());
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedTypeId(@Nonnull String key) {
    ByProjectKeyTypesGet query =
        syncOptions
            .getCtpClient()
            .types()
            .get()
            .withWhere("key in :keys")
            .withPredicateVar("keys", Collections.singletonList(key));

    return super.fetchCachedResourceId(key, (resource) -> resource.getKey(), query);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<Type>> fetchMatchingTypesByKeys(@Nonnull final Set<String> keys) {
    return fetchMatchingResources(
        keys,
        type -> type.getKey(),
        (keysNotCached) ->
            syncOptions
                .getCtpClient()
                .types()
                .get()
                .withWhere("key in :keys")
                .withPredicateVar("keys", keysNotCached));
  }
}
