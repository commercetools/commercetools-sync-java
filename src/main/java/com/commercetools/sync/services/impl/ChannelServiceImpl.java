package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
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
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;


public final class ChannelServiceImpl extends BaseService<ChannelDraft, Channel, BaseSyncOptions, ChannelQuery,
    ChannelQueryModel, ChannelExpansionModel<Channel>> implements ChannelService {

    private final Set<ChannelRole> channelRoles;

    public ChannelServiceImpl(
        @Nonnull final BaseSyncOptions syncOptions,
        @Nonnull final Set<ChannelRole> channelRoles) {

        super(syncOptions);
        this.channelRoles = channelRoles;
    }

    public ChannelServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
        super(syncOptions);
        this.channelRoles = Collections.emptySet();
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedChannelId(@Nonnull final String key) {

        return fetchCachedResourceId(
            key,
            () -> ChannelQueryBuilder.of()
                                     .plusPredicates(queryModel -> queryModel.key().is(key))
                                     .build());

    }

    @Nonnull
    @Override
    public CompletionStage<Channel> createChannel(@Nonnull final String key) {



        final ChannelDraft draft = ChannelDraftBuilder.of(key)
                                                      .roles(channelRoles)
                                                      .build();

        createResource(draft, ChannelDraft::getKey, ChannelCreateCommand::of);

        return syncOptions.getCtpClient().execute(ChannelCreateCommand.of(draft));
    }

    @Nonnull
    @Override
    public CompletionStage<Channel> createAndCacheChannel(@Nonnull final String key) {
        return createChannel(key).thenApply(channel -> {
            cacheChannel(channel);
            return channel;
        });
    }

    @Override
    public void cacheChannel(@Nonnull final Channel channel) {
        keyToIdCache.put(channel.getKey(), channel.getId());
    }
}
