package com.commercetools.sync.services.impl;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;

import com.commercetools.api.client.ByProjectKeyTypesGet;
import com.commercetools.api.client.ByProjectKeyTypesKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyTypesPost;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypePagedQueryResponse;
import com.commercetools.api.models.type.TypeUpdateAction;
import com.commercetools.api.models.type.TypeUpdateBuilder;
import com.commercetools.api.predicates.query.type.TypeQueryBuilderDsl;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.models.GraphQlQueryResource;
import com.commercetools.sync.services.TypeService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
        TypePagedQueryResponse,
        ByProjectKeyTypesKeyByKeyGet,
        Type,
        TypeQueryBuilderDsl,
        ByProjectKeyTypesPost>
    implements TypeService {

  public TypeServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> typeKeys) {
    return super.cacheKeysToIdsUsingGraphQl(typeKeys, GraphQlQueryResource.TYPES);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Type>> fetchTypeByKey(@Nullable final String key) {
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
    return super.fetchMatchingResources(
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

  @Nonnull
  @Override
  public CompletionStage<Optional<Type>> createType(@Nonnull TypeDraft typeDraft) {
    return super.createResource(
        typeDraft,
        TypeDraft::getKey,
        typeResult -> typeResult.getId(),
        type -> type,
        () -> syncOptions.getCtpClient().types().post(typeDraft));
  }

  @Nonnull
  @Override
  public CompletionStage<Type> updateType(
      @Nonnull final Type type, @Nonnull final List<TypeUpdateAction> updateActions) {

    final List<List<TypeUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ApiHttpResponse<Type>> resultStage =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, type));

    for (final List<TypeUpdateAction> batch : actionBatches) {
      resultStage =
          resultStage
              .thenApply(ApiHttpResponse::getBody)
              .thenCompose(
                  updatedType ->
                      syncOptions
                          .getCtpClient()
                          .types()
                          .withId(updatedType.getId())
                          .post(
                              TypeUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedType.getVersion())
                                  .build())
                          .execute());
    }

    return resultStage.thenApply(ApiHttpResponse::getBody);
  }
}
