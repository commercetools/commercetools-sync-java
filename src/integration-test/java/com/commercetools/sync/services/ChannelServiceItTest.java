package com.commercetools.sync.services;

import com.commercetools.sync.services.impl.ChannelServiceImpl;
import com.commercetools.sync.services.impl.InventoryServiceImpl;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.queries.ChannelQuery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.inventories.utils.InventoryItTestUtils.SUPPLY_CHANNEL_KEY_1;
import static com.commercetools.sync.inventories.utils.InventoryItTestUtils.SUPPLY_CHANNEL_KEY_2;
import static com.commercetools.sync.inventories.utils.InventoryItTestUtils.deleteInventoryRelatedResources;
import static com.commercetools.sync.inventories.utils.InventoryItTestUtils.populateTargetProject;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

public class ChannelServiceItTest {

    private ChannelService channelService;

    /**
     * Deletes inventories and supply channels from source and target CTP projects.
     * Populates target CTP project with test data.
     */
    @Before
    public void setup() {
        deleteInventoryRelatedResources();
        populateTargetProject();
        channelService = new ChannelServiceImpl(CTP_TARGET_CLIENT, singleton(ChannelRole.INVENTORY_SUPPLY));
    }

    @AfterClass
    public static void delete() {
        deleteInventoryRelatedResources();
    }

    @Test
    public void fetchAllSupplyChannels_ShouldReturnProperChannels() {
        //assert CTP state
        final Optional<Channel> channelInCtp = CTP_TARGET_CLIENT.execute(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_1))
            .toCompletableFuture().join().head();
        assertThat(channelInCtp).isNotNull();
        assertThat(channelInCtp).isNotEmpty();
        assertThat(channelInCtp.get().getRoles()).containsOnly(ChannelRole.INVENTORY_SUPPLY);

        //fetch channel id
        final Optional<String> result = channelService.fetchCachedChannelId(SUPPLY_CHANNEL_KEY_1)
            .toCompletableFuture()
            .join();

        //assert result
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.get()).isEqualTo(channelInCtp.get().getId());
    }

    @Test
    public void createSupplyChannel_ShouldCreateProperChannel() {
        //create channel
        final Channel result = channelService.createChannel(SUPPLY_CHANNEL_KEY_2)
            .toCompletableFuture()
            .join();

        //assert returned data
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).containsExactly(ChannelRole.INVENTORY_SUPPLY);
        assertThat(result.getKey()).isEqualTo(SUPPLY_CHANNEL_KEY_2);

        //assert CTP state
        final Optional<Channel> createdChannelOptional = CTP_TARGET_CLIENT
            .execute(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_2))
            .toCompletableFuture()
            .join()
            .head();
        assertThat(createdChannelOptional).isNotEmpty();
        assertThat(createdChannelOptional.get()).isEqualTo(result);
    }
}
