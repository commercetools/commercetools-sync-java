package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.TypeUpdateCommand;
import io.sphere.sdk.types.expansion.TypeExpansionModel;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.types.queries.TypeQueryBuilder;
import io.sphere.sdk.types.queries.TypeQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Implementation of TypeService interface.
 * TODO: USE graphQL to get only keys. GITHUB ISSUE#84
 */
public final class TypeServiceImpl extends BaseServiceWithKey<TypeDraft, Type, BaseSyncOptions, TypeQuery,
    TypeQueryModel, TypeExpansionModel<Type>> implements TypeService {

    public TypeServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedTypeId(@Nonnull final String key) {

        return fetchCachedResourceId(
            key,
            () -> TypeQueryBuilder.of()
                                  .plusPredicates(queryModel -> queryModel.key().is(key))
                                  .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Set<Type>> fetchMatchingTypesByKeys(@Nonnull final Set<String> keys) {

        return fetchMatchingResources(keys,
            () -> TypeQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(keys))
                .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Type>> fetchType(@Nullable final String key) {

        return fetchResource(key,
            () -> TypeQueryBuilder.of().plusPredicates(queryModel -> queryModel.key().is(key)).build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Type>> createType(@Nonnull final TypeDraft typeDraft) {
        return createResource(typeDraft, TypeCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<Type> updateType(
        @Nonnull final Type type,
        @Nonnull final List<UpdateAction<Type>> updateActions) {
        return updateResource(type, TypeUpdateCommand::of, updateActions);
    }
}
