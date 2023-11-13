package com.commercetools.sync.integration.commons.utils;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.channel.*;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    deleteChannels(TestClientUtils.CTP_TARGET_CLIENT);
    deleteChannels(TestClientUtils.CTP_SOURCE_CLIENT);
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

  public static List<Channel> ensureChannelsInSourceProject() {
    final Channel channel1 =
        createChannelIfNotAlreadyExisting(
            TestClientUtils.CTP_SOURCE_CLIENT,
            SUPPLY_CHANNEL_KEY_1,
            ChannelRoleEnum.INVENTORY_SUPPLY);
    final Channel channel2 =
        createChannelIfNotAlreadyExisting(
            TestClientUtils.CTP_SOURCE_CLIENT,
            SUPPLY_CHANNEL_KEY_2,
            ChannelRoleEnum.INVENTORY_SUPPLY);

    return Arrays.asList(channel1, channel2);
  }

  public static Channel ensureChannelsInTargetProject() {
    return createChannelIfNotAlreadyExisting(
        TestClientUtils.CTP_TARGET_CLIENT, SUPPLY_CHANNEL_KEY_1, ChannelRoleEnum.INVENTORY_SUPPLY);
  }

  private static Channel createChannelIfNotAlreadyExisting(
      @Nonnull ProjectApiRoot ctpClient,
      @Nonnull String channelKey,
      @Nullable ChannelRoleEnum channelRole) {
    return getChannelByKey(ctpClient, channelKey)
        .orElseGet(
            () -> {
              final ChannelDraft channelDraft =
                  ChannelDraftBuilder.of().key(channelKey).roles(channelRole).build();

              return ctpClient
                  .channels()
                  .post(channelDraft)
                  .execute()
                  .thenApply(ApiHttpResponse::getBody)
                  .toCompletableFuture()
                  .join();
            });
  }

  /**
   * Tries to fetch channel of key {@code channelKey} using {@code ctpClient}.
   *
   * @param ctpClient client used to execute requests
   * @param channelKey key of requested channel
   * @return {@link java.util.Optional} which may contain channel of key {@code channelKey}
   */
  public static Optional<Channel> getChannelByKey(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String channelKey) {
    final ApiHttpResponse<ChannelPagedQueryResponse> channelResponse =
        ctpClient
            .channels()
            .get()
            .withWhere("key=:key")
            .withPredicateVar("key", channelKey)
            .execute()
            .toCompletableFuture()
            .join();

    return channelResponse.getBody().getResults().stream().findFirst();
  }

  private ChannelITUtils() {}
}
