package com.commercetools.sync.integration.services;

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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteChannelsFromTargetAndSource;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteInventoryEntriesFromTargetAndSource;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.populateTargetProject;
import static org.assertj.core.api.Assertions.assertThat;

public class ChannelServiceIT {
    private ChannelService channelService;
    private static final String CHANNEL_KEY = "channel_key";

    /**
     * Deletes inventories and supply channels from source and target CTP projects.
     * Populates target CTP project with test data.
     */
    @Before
    public void setup() {
        deleteInventoryEntriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        deleteChannelsFromTargetAndSource();
        populateTargetProject();
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                                    .build();
        channelService = new ChannelServiceImpl(inventorySyncOptions,
            Collections.singleton(ChannelRole.INVENTORY_SUPPLY));
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteInventoryEntriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        deleteChannelsFromTargetAndSource();
    }

    @Test
    public void fetchCachedChannelId_WithNonExistingChannel_ShouldNotFetchAChannel() {
        Optional<String> channelId = channelService.fetchCachedChannelId(CHANNEL_KEY)
                                                    .toCompletableFuture()
                                                    .join();
        assertThat(channelId).isEmpty();
    }

    @Test
    public void fetchCachedChannelId_WithExistingChannel_ShouldFetchChannelAndCache() {
        final ChannelDraft draft = ChannelDraftBuilder.of(CHANNEL_KEY)
                                                      .roles(Collections.singleton(ChannelRole.INVENTORY_SUPPLY))
                                                      .build();
        CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(draft)).toCompletableFuture().join();
        assertThat(channelService.fetchCachedChannelId(CHANNEL_KEY).toCompletableFuture().join()).isNotEmpty();
    }

    @Test
    public void fetchCachedChannelId_WithNonInvalidatedCache_ShouldFetchFromCache() {
        // Fetch any channel to populate cache
        channelService.fetchCachedChannelId("anyChannelKey").toCompletableFuture().join();

        // Create new channel
        final String newChannelKey = "new_channel_key";
        final ChannelDraft draft = ChannelDraftBuilder.of(newChannelKey)
                                                      .roles(Collections.singleton(ChannelRole.INVENTORY_SUPPLY))
                                                      .build();
        CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(draft)).toCompletableFuture().join();

        final Optional<String> newChannelId =
            channelService.fetchCachedChannelId(newChannelKey).toCompletableFuture().join();

        assertThat(newChannelId).isEmpty();
    }

    @Test
    public void fetchCachedChannelId_WithInvalidatedCache_ShouldFetchFreshCopyAndRepopulateCache() {
        // Fetch any channel to populate cache
        channelService.fetchCachedChannelId(CHANNEL_KEY).toCompletableFuture().join();

        // Create new channel
        final String newChannelKey = "new_channel_key";
        final ChannelDraft draft = ChannelDraftBuilder.of(newChannelKey)
                                                      .roles(Collections.singleton(ChannelRole.INVENTORY_SUPPLY))
                                                      .build();
        CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(draft)).toCompletableFuture().join();

        channelService.invalidateCache();

        final Optional<String> newChannelId =
            channelService.fetchCachedChannelId(newChannelKey).toCompletableFuture().join();

        assertThat(newChannelId).isNotEmpty();
    }

    @Test
    public void createChannel_ShouldCreateChannel() {
        final String newChannelKey = "new_channel_key";
        final Channel result = channelService.createChannel(newChannelKey)
                                             .toCompletableFuture()
                                             .join();

        assertThat(result).isNotNull();
        assertThat(result.getRoles()).containsExactly(ChannelRole.INVENTORY_SUPPLY);
        assertThat(result.getKey()).isEqualTo(newChannelKey);

        //assert CTP state
        final Optional<Channel> createdChannelOptional = CTP_TARGET_CLIENT
            .execute(ChannelQuery.of().byKey(newChannelKey))
            .toCompletableFuture()
            .join()
            .head();
        assertThat(createdChannelOptional).isNotEmpty();
        assertThat(createdChannelOptional.get()).isEqualTo(result);
    }


    @Test
    public void createAndCacheChannel_ShouldCreateChannelAndCacheIt() {
        final String newChannelKey = "new_channel_key";
        final Channel result = channelService.createAndCacheChannel(newChannelKey)
                                               .toCompletableFuture()
                                               .join();

        assertThat(result).isNotNull();
        assertThat(result.getRoles()).containsExactly(ChannelRole.INVENTORY_SUPPLY);
        assertThat(result.getKey()).isEqualTo(newChannelKey);

        //assert CTP state
        final Optional<Channel> createdChannelOptional = CTP_TARGET_CLIENT
            .execute(ChannelQuery.of().byKey(newChannelKey))
            .toCompletableFuture()
            .join()
            .head();
        assertThat(createdChannelOptional).isNotEmpty();
        assertThat(createdChannelOptional.get()).isEqualTo(result);

        //assert cache state
        final Optional<String> newChannelId =
            channelService.fetchCachedChannelId(newChannelKey).toCompletableFuture().join();
        assertThat(newChannelId).isNotEmpty();
    }
}
