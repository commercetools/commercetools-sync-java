package com.commercetools.sync.inventories;

import io.sphere.sdk.channels.Channel;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

/**
 * Helper class that holds {@code id} and {@code keys} of given {@code Channels}.
 */
class ChannelsMap {

    public final Map<String, String> channelKeyToChannelId;
    public final Map<String, String> channelIdToChannelKey;

    private ChannelsMap(@Nonnull final Map<String, String> channelKeyToChannelId ,
                        @Nonnull final Map<String, String> channelIdToChannelKey) {
        this.channelKeyToChannelId = new HashMap<>(channelKeyToChannelId);
        this.channelIdToChannelKey = new HashMap<>(channelIdToChannelKey);
    }

    /**
     * Returns corresponding {@code id} for given {@code channelKey} if such tuple exists in a cache.
     *
     * @param channelKey channel's {@code key}
     * @return {@link Optional} that contains corresponding {@code id} for given {@code channelKey} or empty
     *      {@link Optional} if such tuple doesn't exists in a cache
     */
    public Optional<String> getChannelId(@Nonnull final String channelKey) {
        if (channelKeyToChannelId.containsKey(channelKey)) {
            return Optional.of(channelKeyToChannelId.get(channelKey));
        }
        return Optional.empty();
    }

    /**
     * Returns corresponding {@code key} for given {@code channelId} if such tuple exists in a cache.
     *
     * @param channelId channel's {@code id}
     * @return {@link Optional} that contains corresponding {@code key} for given {@code channelId} or empty
     *      {@link Optional} if such tuple doesn't exists in a cache
     */
    public Optional<String> getChannelKey(@Nonnull final String channelId) {
        if (channelIdToChannelKey.containsKey(channelId)) {
            return Optional.of(channelIdToChannelKey.get(channelId));
        }
        return Optional.empty();
    }

    static class Builder {

        public final Map<String, String> channelKeyToChannelId;
        public final Map<String, String> channelIdToChannelKey;

        private Builder(@Nonnull final Map<String, String> channelKeyToChannelId,
                        @Nonnull final Map<String, String> channelIdToChannelKey) {
            this.channelKeyToChannelId = channelKeyToChannelId;
            this.channelIdToChannelKey = channelIdToChannelKey;
        }

        public static Builder of(@Nonnull final List<Channel> channels) {
            final Map<String, String> keyToId = channels
                .stream()
                .collect(toMap(Channel::getKey, Channel::getId));
            final Map<String, String> idToKey = channels
                .stream()
                .collect(toMap(Channel::getId, Channel::getKey));
            return new Builder(keyToId, idToKey);
        }

        public Builder add(@Nonnull final Channel channel) {
            channelKeyToChannelId.put(channel.getKey(), channel.getId());
            channelIdToChannelKey.put(channel.getId(), channel.getKey());
            return this;
        }

        public ChannelsMap build() {
            return new ChannelsMap(channelKeyToChannelId, channelIdToChannelKey);
        }
    }
}
