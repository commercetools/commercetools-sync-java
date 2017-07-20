package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.helpers.CtpQueryUtils;
import com.commercetools.sync.services.ChannelService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.channels.queries.ChannelQueryBuilder;
import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


public final class ChannelServiceImpl implements ChannelService {

    private final SphereClient ctpClient;
    private final Set<ChannelRole> channelRoles;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    private boolean invalidCache = false;

    public ChannelServiceImpl(@Nonnull final SphereClient ctpClient,
                              @Nonnull final Set<ChannelRole> channelRoles) {
        this.ctpClient = ctpClient;
        this.channelRoles = channelRoles;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedChannelId(@Nonnull final String key) {
        if (keyToIdCache.isEmpty() || invalidCache) {
            return cacheAndFetch(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }

    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        final ChannelQuery query =
            ChannelQueryBuilder.of()
                               .plusPredicates(channelQueryModel -> channelQueryModel.roles().containsAny(channelRoles))
                               .build();

        final Consumer<List<Channel>> channelPageConsumer = channelsPage ->
            channelsPage.forEach(channel -> keyToIdCache.put(channel.getKey(), channel.getId()));

        return CtpQueryUtils.queryAll(ctpClient, query, channelPageConsumer)
                            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    @Override
    public CompletionStage<Channel> createChannel(@Nonnull final String key) {
        final ChannelDraft draft = ChannelDraftBuilder.of(key)
                                                      .roles(channelRoles)
                                                      .build();
        return ctpClient.execute(ChannelCreateCommand.of(draft));
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

    @Override
    public void invalidateCache() {
        invalidCache = true;
    }
}
