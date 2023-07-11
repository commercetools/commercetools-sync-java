package com.commercetools.sync.integration.sdk2.commons.utils;

import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelDraft;
import com.commercetools.api.models.channel.ChannelDraftBuilder;
import com.commercetools.api.models.channel.ChannelRoleEnum;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class ChannelITUtils {
  public static final String SUPPLY_CHANNEL_KEY_1 = "channel-key_1";
  public static final String SUPPLY_CHANNEL_KEY_2 = "channel-key_2";

  public static final String CUSTOM_TYPE = "inventory-custom-type-name";
  public static final String CUSTOM_FIELD_NAME = "backgroundColor";
  /**
   * Deletes all Channels from CTP projects defined by the {@code CTP_SOURCE_CLIENT} and {@code
   * CTP_TARGET_CLIENT}.
   */
  public static void deleteChannelsFromTargetAndSource() {
    deleteChannels(CTP_TARGET_CLIENT);
    deleteChannels(CTP_SOURCE_CLIENT);
  }

  /**
   * Deletes all Channels from the CTP project defined by the {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the Channels from.
   */
  public static void deleteChannels(@Nonnull final ProjectApiRoot ctpClient) {
    QueryUtils.queryAll(
            ctpClient.channels().get(),
            channels -> {
              CompletableFuture.allOf(
                      channels.stream()
                          .map(channel -> deleteChannel(ctpClient, channel))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<Channel> deleteChannel(ProjectApiRoot ctpClient, Channel channel) {
    return ctpClient.channels().delete(channel).execute().thenApply(ApiHttpResponse::getBody);
  }

  public static void populateSourceProject() {
    final ChannelDraft channelDraft1 =
        ChannelDraftBuilder.of()
            .key(SUPPLY_CHANNEL_KEY_1)
            .roles(ChannelRoleEnum.INVENTORY_SUPPLY)
            .build();
    final ChannelDraft channelDraft2 =
        ChannelDraftBuilder.of()
            .key(SUPPLY_CHANNEL_KEY_2)
            .roles(ChannelRoleEnum.INVENTORY_SUPPLY)
            .build();

    CTP_SOURCE_CLIENT.channels().create(channelDraft1).execute().join().getBody();
    CTP_SOURCE_CLIENT.channels().create(channelDraft2).execute().join().getBody();
  }

  public static Channel populateTargetProject() {
    final ChannelDraft channelDraft =
        ChannelDraftBuilder.of()
            .key(SUPPLY_CHANNEL_KEY_1)
            .roles(ChannelRoleEnum.INVENTORY_SUPPLY)
            .build();

    return CTP_TARGET_CLIENT
        .channels()
        .create(channelDraft)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .toCompletableFuture()
        .join();
  }

  /**
   * Tries to fetch channel of key {@code channelKey} using {@code ctpClient}.
   *
   * @param ctpClient sphere client used to execute requests
   * @param channelKey key of requested channel
   * @return {@link java.util.Optional} which may contain channel of key {@code channelKey}
   */
  public static Optional<Channel> getChannelByKey(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String channelKey) {
    return ctpClient
        .channels()
        .get()
        .withWhere("key=:key")
        .withPredicateVar("key", channelKey)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .join()
        .getResults()
        .stream()
        .findFirst();
  }

  private ChannelITUtils() {}
}
