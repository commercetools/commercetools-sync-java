package com.commercetools.sync.integration.services.impl;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.channel.*;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.integration.commons.utils.ChannelITUtils;
import com.commercetools.sync.integration.commons.utils.ITUtils;
import com.commercetools.sync.integration.commons.utils.InventoryITUtils;
import com.commercetools.sync.integration.commons.utils.TestClientUtils;
import com.commercetools.sync.sdk2.inventories.InventorySyncOptions;
import com.commercetools.sync.sdk2.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.services.ChannelService;
import com.commercetools.sync.sdk2.services.impl.ChannelServiceImpl;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChannelServiceImplIT {
  private ChannelService channelService;
  private static final String CHANNEL_KEY = "channel_key";

  /**
   * Deletes inventories and supply channels from source and target CTP projects. Populates target
   * CTP project with test data.
   */
  @BeforeEach
  void setup() {
    InventoryITUtils.deleteInventoryEntriesFromTargetAndSource();
    ITUtils.deleteTypesFromTargetAndSource();
    ChannelITUtils.deleteChannelsFromTargetAndSource();
    final Channel channel = ChannelITUtils.ensureChannelsInTargetProject();
    final ChannelResourceIdentifier supplyChannelReference =
        ChannelResourceIdentifierBuilder.of().id(channel.getId()).build();

    ITUtils.createTypeIfNotAlreadyExisting(
        ChannelITUtils.CUSTOM_TYPE,
        Locale.ENGLISH,
        ChannelITUtils.CUSTOM_TYPE,
        Collections.singletonList(ResourceTypeId.INVENTORY_ENTRY),
        TestClientUtils.CTP_TARGET_CLIENT);

    InventoryITUtils.populateInventoriesInTargetProject(supplyChannelReference);
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT).build();
    channelService =
        new ChannelServiceImpl(inventorySyncOptions, singleton(ChannelRoleEnum.INVENTORY_SUPPLY));
  }

  /** Cleans up the target and source test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    InventoryITUtils.deleteInventoryEntriesFromTargetAndSource();
    ITUtils.deleteTypesFromTargetAndSource();
    ChannelITUtils.deleteChannelsFromTargetAndSource();
  }

  @Test
  void fetchCachedChannelId_WithNonExistingChannel_ShouldNotFetchAChannel() {
    Optional<String> channelId =
        channelService.fetchCachedChannelId(CHANNEL_KEY).toCompletableFuture().join();
    assertThat(channelId).isEmpty();
  }

  @Test
  void fetchCachedChannelId_WithExistingChannel_ShouldFetchChannelAndCache() {
    final ChannelDraft draft =
        ChannelDraftBuilder.of().key(CHANNEL_KEY).roles(ChannelRoleEnum.INVENTORY_SUPPLY).build();
    TestClientUtils.CTP_TARGET_CLIENT
        .channels()
        .create(draft)
        .execute()
        .toCompletableFuture()
        .join();
    assertThat(channelService.fetchCachedChannelId(CHANNEL_KEY).toCompletableFuture().join())
        .isNotEmpty();
  }

  @Test
  void createChannel_ShouldCreateChannel() {
    // preparation
    final String newChannelKey = "new_channel_key";

    // test
    final Optional<Channel> result =
        channelService.createChannel(newChannelKey).toCompletableFuture().join();

    // assertion
    assertThat(result)
        .hasValueSatisfying(
            channel -> {
              assertThat(channel.getRoles()).containsExactly(ChannelRoleEnum.INVENTORY_SUPPLY);
              assertThat(channel.getKey()).isEqualTo(newChannelKey);
            });

    // assert CTP state
    final Optional<Channel> createdChannelOptional =
        ChannelITUtils.getChannelByKey(TestClientUtils.CTP_TARGET_CLIENT, newChannelKey);
    assertThat(createdChannelOptional).isEqualTo(result);
  }

  @Test
  void createAndCacheChannel_ShouldCreateChannelAndCacheIt() {
    // preparation
    final String newChannelKey = "new_channel_key";

    // test
    final Optional<Channel> result =
        channelService.createAndCacheChannel(newChannelKey).toCompletableFuture().join();

    // assertion
    assertThat(result)
        .hasValueSatisfying(
            channel -> {
              assertThat(channel.getRoles()).containsExactly(ChannelRoleEnum.INVENTORY_SUPPLY);
              assertThat(channel.getKey()).isEqualTo(newChannelKey);
            });

    // assert CTP state
    final Optional<Channel> createdChannelOptional =
        ChannelITUtils.getChannelByKey(TestClientUtils.CTP_TARGET_CLIENT, newChannelKey);
    assertThat(createdChannelOptional).isEqualTo(result);

    // assert cache state
    final Optional<String> newChannelId =
        channelService.fetchCachedChannelId(newChannelKey).toCompletableFuture().join();
    assertThat(newChannelId).contains(result.get().getId());
  }
}
