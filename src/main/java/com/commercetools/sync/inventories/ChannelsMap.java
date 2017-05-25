package com.commercetools.sync.inventories;

import io.sphere.sdk.channels.Channel;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

/**
 * Holds {@code id} and {@code keys} of {@code Channels} passed to the {@link ChannelsMap.Builder}.
 */
class ChannelsMap {

    public final Map<String, String> channelKeyToChannelId;
    public final Map<String, String> channelIdToChannelKey;

    private ChannelsMap(@Nonnull final Map<String, String> channelKeyToChannelId ,
                        @Nonnull final Map<String, String> channelIdToChannelKey) {
        this.channelKeyToChannelId = new HashMap<>(channelKeyToChannelId);
        this.channelIdToChannelKey = new HashMap<>(channelIdToChannelKey);
    }

    public Optional<String> getChannelId(@Nonnull final String channelKey) {
        if (channelKeyToChannelId.containsKey(channelKey)) {
            return Optional.of(channelKeyToChannelId.get(channelKey));
        }
        return Optional.empty();
    }

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
