package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;

public final class ChannelITUtils {
    /**
     * Deletes all Channels from CTP projects defined by the {@code CTP_SOURCE_CLIENT} and
     * {@code CTP_TARGET_CLIENT}.
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
    public static void deleteChannels(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, ChannelQuery.of(), ChannelDeleteCommand::of);
    }

    /**
     * Creates a {@link Channel} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient defines the CTP project to create the Channels in.
     * @param name      the name of the channel to create.
     * @param key       the key of the channel to create.
     * @return the created Channel.
     */
    public static Channel createChannel(@Nonnull final SphereClient ctpClient, @Nonnull final String name,
                                        @Nonnull final String key) {
        final ChannelDraft channelDraft = ChannelDraftBuilder.of(name)
                                                             .key(key)
                                                             .build();

        return executeBlocking(ctpClient.execute(ChannelCreateCommand.of(channelDraft)));
    }

    private ChannelITUtils() {
    }
}
