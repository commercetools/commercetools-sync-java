package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.services.ChannelService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.expansion.ChannelExpansionModel;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.channels.queries.ChannelQueryBuilder;
import io.sphere.sdk.channels.queries.ChannelQueryModel;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;


public final class ChannelServiceImpl extends BaseServiceWithKey<ChannelDraft, Channel, BaseSyncOptions, ChannelQuery,
    ChannelQueryModel, ChannelExpansionModel<Channel>> implements ChannelService {

    private final Set<ChannelRole> channelRoles;


    public ChannelServiceImpl(
        @Nonnull final BaseSyncOptions syncOptions,
        @Nonnull final Set<ChannelRole> channelRoles) {
        super(syncOptions);
        this.channelRoles = channelRoles;
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> channelKeys) {

        return cacheKeysToIds(
            channelKeys,
            keysNotCached -> new ResourceKeyIdGraphQlRequest(keysNotCached, GraphQlQueryResources.CHANNELS));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedChannelId(@Nonnull final String key) {

        return fetchCachedResourceId(
            key,
            () -> ChannelQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().is(key))
                .build());

    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Channel>> createChannel(@Nonnull final String key) {

        final ChannelDraft draft = ChannelDraftBuilder
            .of(key)
            .roles(channelRoles)
            .build();

        return createResource(draft, ChannelCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Channel>> createAndCacheChannel(@Nonnull final String key) {

        return createChannel(key)
            .thenApply(channelOptional -> {
                channelOptional.ifPresent(channel -> keyToIdCache.put(key, channel.getId()));
                return channelOptional;
            });
    }
}
