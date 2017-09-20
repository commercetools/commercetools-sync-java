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
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.lang.String.format;


public final class ChannelServiceImpl implements ChannelService {

    private final BaseSyncOptions syncOptions;
    private final Set<ChannelRole> channelRoles;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    private boolean invalidCache = false;
    private static final String CHANNEL_KEY_NOT_SET = "Channel with id: '%s' has no key set. Keys are required for "
        + "channel matching.";

    public ChannelServiceImpl(@Nonnull final BaseSyncOptions syncOptions,
                              @Nonnull final Set<ChannelRole> channelRoles) {
        this.syncOptions = syncOptions;
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
            channelsPage.forEach(channel -> {
                final String fetchedChannelKey = channel.getKey();
                final String id = channel.getId();
                if (StringUtils.isNotBlank(fetchedChannelKey)) {
                    keyToIdCache.put(fetchedChannelKey, id);
                } else {
                    syncOptions.applyWarningCallback(format(CHANNEL_KEY_NOT_SET, id));
                }
            });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), query, channelPageConsumer)
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

    @Override
    public void invalidateCache() {
        invalidCache = true;
    }
}
