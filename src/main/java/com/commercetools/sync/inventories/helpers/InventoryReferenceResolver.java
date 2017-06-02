package com.commercetools.sync.inventories.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;

public final class InventoryReferenceResolver extends BaseReferenceResolver<InventoryEntryDraft, InventorySyncOptions> {
    private static final String CHANNEL_DOES_NOT_EXIST = "Channel with key '%s' does not exist.";
    private ChannelService channelService;

    public InventoryReferenceResolver(@Nonnull final InventorySyncOptions options,
                                      @Nonnull final TypeService typeService,
                                      @Nonnull final ChannelService channelService) {
        super(options, typeService);
        this.channelService = channelService;
    }

    @Override
    @Nonnull
    public CompletionStage<InventoryEntryDraft> resolveCustomTypeReference(@Nonnull final InventoryEntryDraft
                                                                                      inventoryEntryDraft) {
        final CustomFieldsDraft custom = inventoryEntryDraft.getCustom();
        if (custom != null) {
            return getCustomTypeId(custom).thenApply(resolvedTypeIdOptional ->
                resolvedTypeIdOptional.filter(StringUtils::isNotBlank)
                                      .map(resolvedTypeId -> InventoryEntryDraftBuilder
                                          .of(inventoryEntryDraft)
                                          .custom(CustomFieldsDraft.ofTypeIdAndJson(resolvedTypeId, custom.getFields()))
                                          .build())
                                      .orElseGet(() -> InventoryEntryDraftBuilder.of(inventoryEntryDraft).build()));
        }
        return CompletableFuture.completedFuture(InventoryEntryDraftBuilder.of(inventoryEntryDraft).build());
    }

    /**
     * Given a {@link InventoryEntryDraft} this method attempts to resolve the supply channel reference to return
     * a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * supply channel reference. The key of the supply channel is either taken from the expanded reference or
     * taken from the id field of the reference.
     *
     * <p>The method then tries to fetch the key of the supply channel, optimistically from a
     * cache. If it is is not found, the resultant draft would remain exactly the same as the passed inventory entry
     * draft (without a channel reference resolution).
     *
     * @param draft the inventoryEntryDraft to resolve it's channel reference.
     * @return a {@link CompletionStage} that contains as a result a new inventoryEntryDraft instance with resolved
     *          supply channel or, in case an error occurs during reference resolution,
     *          a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<InventoryEntryDraft> resolveSupplyChannelReference(
        @Nonnull final InventoryEntryDraft draft) {
        final Reference<Channel> channelReference = draft.getSupplyChannel();
        if (channelReference != null) {
            try {
                final String keyFromExpansion = getKeyFromExpansion(channelReference);
                final String channelKey = getKeyFromExpansionOrReference(keyFromExpansion, channelReference);
                return fetchOrCreateAndResolveReference(draft, channelKey);
            } catch (ReferenceResolutionException exception) {
                return CompletableFutureUtils.exceptionallyCompletedFuture(exception);
            }
        }
        return CompletableFuture.completedFuture(InventoryEntryDraftBuilder.of(draft).build());
    }

    /**
     * Given an {@link InventoryEntryDraft} and a {@code channelKey} this method fetches the actual id of the
     * channel corresponding to this key, ideally from a cache. Then it sets this id on the supply channel reference
     * id of the inventory entry draft. If the id is not found in cache nor the CTP project, the resultant draft would
     * remain exactly the same as the passed draft (without supply channel resolution).
     *
     * @param draft the inventory entry draft to resolve it's supply channel reference.
     * @param channelKey the key of the channel to resolve it's actual id on the draft.
     * @return a {@link CompletionStage} that contains as a result a new inventory entry draft instance with resolved
     *      supply channel reference or an exception.
     */
    @Nonnull
    private CompletionStage<InventoryEntryDraft> fetchOrCreateAndResolveReference(
        @Nonnull final InventoryEntryDraft draft,
        @Nonnull final String channelKey) {
        return channelService.fetchCachedChannelIdByKeyAndRoles(channelKey,
            Collections.singletonList(ChannelRole.INVENTORY_SUPPLY))
                             .thenCompose(resolvedChannelIdOptional -> resolvedChannelIdOptional
                                 .filter(StringUtils::isNotBlank)
                                 .map(resolvedChannelId -> setChannelReference(resolvedChannelId, draft))
                                 .orElseGet(() -> createChannelAndSetReference(channelKey, draft)));
    }

    /**
     * Helper method that returns the value of the key field from the channel {@link Reference} object, if expanded.
     * Otherwise, returns null.
     *
     * @return the value of the key field from the channel {@link Reference} object, if expanded.
     *          Otherwise, returns null.
     */
    @Nullable
    private static String getKeyFromExpansion(@Nonnull final Reference<Channel> channelReference) {
        return isReferenceExpanded(channelReference) ? channelReference.getObj().getKey() : null;
    }

    /**
     * Helper method that returns a completed CompletionStage with a resolved channel reference
     * {@link InventoryEntryDraft} object as a result of setting the passed {@code channelId} as the id of channel
     * reference.
     *
     * @param channelId the channel id to set on the inventory entry supply channel reference id field.
     * @param inventoryEntryDraft the inventory entry draft to resolve it's supply channel reference.
     * @return a completed CompletionStage with a resolved channel reference
     *      {@link InventoryEntryDraft} object as a result of setting the passed {@code channelId} as the id of channel
     *      reference.
     */
    @Nonnull
    private static CompletionStage<InventoryEntryDraft> setChannelReference(@Nonnull final String channelId,
                                                                            @Nonnull final InventoryEntryDraft
                                                                                inventoryEntryDraft) {
        return CompletableFuture.completedFuture(InventoryEntryDraftBuilder
            .of(inventoryEntryDraft)
            .supplyChannel(Reference.of(Channel.referenceTypeId(), channelId))
            .build());
    }

    /**
     * Helper method that creates a new {@link Channel} on the CTP project with the specified {@code channelKey} and of
     * the role {@code "InventorySupply"}. Only if the {@code ensureChannels} options is set to {@code true} on the
     * {@code options} instance of {@code this} class. Then it resolves the supply channel reference on the supplied
     * {@code inventoryEntryDraft} by setting the id of it's supply channel reference with the newly created Channel.
     *
     * <p>If the {@code ensureChannels} options is set to {@code false} on the {@code options} instance of {@code this}
     * class, a completed {@link CompletableFuture} with the same exact {@link InventoryEntryDraft} passed is returned
     * as a result.
     *
     * <p>The method then returns a CompletionStage with a resolved channel reference {@link InventoryEntryDraft}
     * object.
     *
     * @param channelKey          the key to create the new channel with.
     * @param inventoryEntryDraft the inventory entry draft to resolve it's supply channel reference to the newly
     *                            created channel.
     * @return a CompletionStage with a resolved channel reference {@link InventoryEntryDraft} object.
     */
    @Nonnull
    private CompletionStage<InventoryEntryDraft> createChannelAndSetReference(@Nonnull final String channelKey,
                                                                              @Nonnull final InventoryEntryDraft
                                                                                  inventoryEntryDraft) {
        if (getOptions().shouldEnsureChannels()) {
            return channelService.createAndCacheChannel(channelKey, Collections.singleton(ChannelRole.INVENTORY_SUPPLY))
                                 .thenApply(createdChannel -> InventoryEntryDraftBuilder
                                     .of(inventoryEntryDraft)
                                     .supplyChannel(Reference.of(Channel.referenceTypeId(), createdChannel.getId()))
                                     .build());
        } else {
            final ReferenceResolutionException referenceResolutionException =
                new ReferenceResolutionException(format(CHANNEL_DOES_NOT_EXIST, channelKey));
            return CompletableFutureUtils.exceptionallyCompletedFuture(referenceResolutionException);
        }
    }
}
