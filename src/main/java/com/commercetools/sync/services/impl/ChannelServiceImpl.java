package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.services.ChannelService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.channels.queries.ChannelQueryBuilder;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


public final class ChannelServiceImpl implements ChannelService {

    private final BaseSyncOptions syncOptions;
    private final Set<ChannelRole> channelRoles;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    private boolean isCached = false;
    private static final String CHANNEL_KEY_NOT_SET = "Channel with id: '%s' has no key set. Keys are required for "
        + "channel matching.";

    public ChannelServiceImpl(@Nonnull final BaseSyncOptions syncOptions,
                              @Nonnull final Set<ChannelRole> channelRoles) {
        this.syncOptions = syncOptions;
        this.channelRoles = channelRoles;
    }

    public ChannelServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
        this.syncOptions = syncOptions;
        this.channelRoles = Collections.emptySet();
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedChannelId(@Nonnull final String key) {
        if (!isCached) {
            return cacheAndFetch(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }

    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        ChannelQueryBuilder channelQueryBuilder = ChannelQueryBuilder.of();
        if (!channelRoles.isEmpty()) {
            channelQueryBuilder = channelQueryBuilder
                .plusPredicates(channelQueryModel -> channelQueryModel.roles().containsAny(channelRoles));
        }
        final ChannelQuery query = channelQueryBuilder.build();
        final Consumer<List<Channel>> channelPageConsumer = channelsPage ->
            channelsPage.forEach(channel -> {
                final String fetchedChannelKey = channel.getKey();
                final String id = channel.getId();
                if (isNotBlank(fetchedChannelKey)) {
                    keyToIdCache.put(fetchedChannelKey, id);
                } else {
                    syncOptions.applyWarningCallback(format(CHANNEL_KEY_NOT_SET, id));
                }
            });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), query, channelPageConsumer)
                            .thenAccept(result -> isCached = true)
                            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    @Override
    public CompletionStage<Channel> createChannel(@Nonnull final String key) {
        final ChannelDraft draft = ChannelDraftBuilder.of(key)
                                                      .roles(channelRoles)
                                                      .build();
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
