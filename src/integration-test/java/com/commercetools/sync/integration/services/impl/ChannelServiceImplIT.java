package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.impl.ChannelServiceImpl;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.ChannelITUtils.deleteChannelsFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteInventoryEntriesFromTargetAndSource;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.populateTargetProject;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

class ChannelServiceImplIT {
    private ChannelService channelService;
    private static final String CHANNEL_KEY = "channel_key";

    /**
     * Deletes inventories and supply channels from source and target CTP projects.
     * Populates target CTP project with test data.
     */
    @BeforeEach
    void setup() {
        deleteInventoryEntriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        deleteChannelsFromTargetAndSource();
        populateTargetProject();
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                                     .build();
        channelService = new ChannelServiceImpl(inventorySyncOptions, singleton(ChannelRole.INVENTORY_SUPPLY));
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteInventoryEntriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        deleteChannelsFromTargetAndSource();
    }

    @Test
    void fetchCachedChannelId_WithNonExistingChannel_ShouldNotFetchAChannel() {
        Optional<String> channelId = channelService.fetchCachedChannelId(CHANNEL_KEY)
                                                   .toCompletableFuture()
                                                   .join();
        assertThat(channelId).isEmpty();
    }

    @Test
    void fetchCachedChannelId_WithExistingChannel_ShouldFetchChannelAndCache() {
        final ChannelDraft draft = ChannelDraftBuilder.of(CHANNEL_KEY)
                                                      .roles(singleton(ChannelRole.INVENTORY_SUPPLY))
                                                      .build();
        CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(draft)).toCompletableFuture().join();
        assertThat(channelService.fetchCachedChannelId(CHANNEL_KEY).toCompletableFuture().join()).isNotEmpty();
    }

    @Test
    void createChannel_ShouldCreateChannel() {
        // preparation
        final String newChannelKey = "new_channel_key";

        // test
        final Optional<Channel> result = channelService
            .createChannel(newChannelKey)
            .toCompletableFuture()
            .join();

        // assertion
        assertThat(result).hasValueSatisfying(channel -> {
            assertThat(channel.getRoles()).containsExactly(ChannelRole.INVENTORY_SUPPLY);
            assertThat(channel.getKey()).isEqualTo(newChannelKey);
        });

        //assert CTP state
        final Optional<Channel> createdChannelOptional = CTP_TARGET_CLIENT
            .execute(ChannelQuery.of().byKey(newChannelKey))
            .toCompletableFuture()
            .join()
            .head();
        assertThat(createdChannelOptional).isEqualTo(result);
    }

    @Test
    void createAndCacheChannel_ShouldCreateChannelAndCacheIt() {
        // preparation
        final String newChannelKey = "new_channel_key";

        // test
        final Optional<Channel> result = channelService
            .createAndCacheChannel(newChannelKey)
            .toCompletableFuture()
            .join();

        // assertion
        assertThat(result).hasValueSatisfying(channel -> {
            assertThat(channel.getRoles()).containsExactly(ChannelRole.INVENTORY_SUPPLY);
            assertThat(channel.getKey()).isEqualTo(newChannelKey);
        });

        //assert CTP state
        final Optional<Channel> createdChannelOptional = CTP_TARGET_CLIENT
            .execute(ChannelQuery.of().byKey(newChannelKey))
            .toCompletableFuture()
            .join()
            .head();
        assertThat(createdChannelOptional).isEqualTo(result);

        //assert cache state
        final Optional<String> newChannelId =
            channelService.fetchCachedChannelId(newChannelKey).toCompletableFuture().join();
        assertThat(newChannelId).contains(result.get().getId());
    }
}
