package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.ChannelService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.channels.queries.ChannelQueryBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.QueryExecutionUtils;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


public class ChannelServiceImpl implements ChannelService {

    private final SphereClient ctpClient;

    /**
     * Cache of Channels' [key -> id].
     */
    private final Map<String, String> cache = new HashMap<>();

    public ChannelServiceImpl(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedChannelIdByKeyAndRoles(@Nonnull final String key,
                                                                               @Nonnull final List<ChannelRole> roles) {
        if (cache.isEmpty()) {
            return cacheAndFetch(key, roles);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(cache.get(key)));
    }

    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key,
                                                            @Nonnull final List<ChannelRole> roles) {
        final ChannelQuery query = ChannelQueryBuilder.of()
                                                      .plusPredicates(channelQueryModel ->
                                                          channelQueryModel.roles().containsAny(roles))
                                                      .build();
        return QueryExecutionUtils.queryAll(ctpClient, query)
                                  .thenApply(channels -> {
                                      channels.forEach(channel ->
                                          cache.put(channel.getKey(), channel.getId()));
                                      return Optional.ofNullable(cache.get(key));
                                  });
    }

    @Nonnull
    @Override
    public CompletionStage<Channel> createChannel(@Nonnull final String key, @Nonnull final Set<ChannelRole> roles) {
        final ChannelDraft draft = ChannelDraftBuilder.of(key)
                                                      .roles(roles)
                                                      .build();
        return ctpClient.execute(ChannelCreateCommand.of(draft));
    }

    @Nonnull
    @Override
    public CompletionStage<Channel> createAndCacheChannel(@Nonnull final String key,
                                                          @Nonnull final Set<ChannelRole> roles) {
        return createChannel(key, roles)
            .thenApply(channel -> {
                cacheChannel(channel);
                return channel;
            });
    }

    @Override
    public void cacheChannel(@Nonnull final Channel channel) {
        cache.put(channel.getKey(), channel.getId());
    }
}
