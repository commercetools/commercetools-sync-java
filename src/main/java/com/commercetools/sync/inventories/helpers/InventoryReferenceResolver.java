package com.commercetools.sync.inventories.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.utils.CompletableFutureUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

public final class InventoryReferenceResolver
        extends CustomReferenceResolver<InventoryEntryDraft, InventoryEntryDraftBuilder, InventorySyncOptions> {

    private static final String CHANNEL_DOES_NOT_EXIST = "Channel with key '%s' does not exist.";
    static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type resource identifier on "
        + "InventoryEntryDraft with SKU:'%s'.";
    private static final String FAILED_TO_RESOLVE_SUPPLY_CHANNEL = "Failed to resolve supply channel "
        + "resource identifier on InventoryEntryDraft with SKU:'%s'. Reason: %s";
    private ChannelService channelService;

    public InventoryReferenceResolver(@Nonnull final InventorySyncOptions options,
                                      @Nonnull final TypeService typeService,
                                      @Nonnull final ChannelService channelService) {
        super(options, typeService);
        this.channelService = channelService;
    }

    /**
     * Given a {@link InventoryEntryDraft} this method attempts to resolve the custom type and supply channel
     * resource identifiers to return a {@link CompletionStage} which contains a new instance of the draft with the
     * resolved resource identifiers.
     *
     * @param draft the inventoryEntryDraft to resolve its resource identifiers.
     * @return a {@link CompletionStage} that contains as a result a new inventoryEntryDraft instance with resolved
     *          resource identifiers or, in case an error occurs during reference resolution,
     *          a {@link ReferenceResolutionException}.
     */
    public CompletionStage<InventoryEntryDraft> resolveReferences(@Nonnull final InventoryEntryDraft draft) {
        return resolveCustomTypeReference(InventoryEntryDraftBuilder.of(draft))
            .thenCompose(this::resolveSupplyChannelReference)
            .thenApply(InventoryEntryDraftBuilder::build);
    }

    @Override
    @Nonnull
    protected CompletionStage<InventoryEntryDraftBuilder> resolveCustomTypeReference(
        @Nonnull final InventoryEntryDraftBuilder draftBuilder) {

        return resolveCustomTypeReference(draftBuilder,
            InventoryEntryDraftBuilder::getCustom,
            InventoryEntryDraftBuilder::custom,
            format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getSku()));
    }

    /**
     * Given a {@link InventoryEntryDraftBuilder} this method attempts to resolve the supply channel resource identifier
     * to return a {@link CompletionStage} which contains a new instance of the draft builder with the resolved
     * supply channel resource identifier.
     *
     * <p>The method then tries to fetch the key of the supply channel, optimistically from a
     * cache. If the id is not found in cache nor the CTP project and {@code ensureChannel}
     * option is set to true, a new channel will be created with this key and the role {@code "InventorySupply"}.
     * However, if the {@code ensureChannel} is set to false, the future is completed exceptionally with a
     * {@link ReferenceResolutionException}.
     *
     * @param draftBuilder the inventory draft builder to read it's values (key, sku, channel)
     *                     and then to write resolved resource identifier.
     * @return a {@link CompletionStage} that contains as a result the same {@code draftBuilder} inventory draft builder
     *         instance with resolved supply channel or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    CompletionStage<InventoryEntryDraftBuilder> resolveSupplyChannelReference(
            @Nonnull final InventoryEntryDraftBuilder draftBuilder) {

        final ResourceIdentifier<Channel> channelReference = draftBuilder.getSupplyChannel();
        if (channelReference != null && channelReference.getId() == null) {
            try {
                final String channelKey = getKeyFromResourceIdentifier(channelReference);
                return fetchOrCreateAndResolveReference(draftBuilder, channelKey);
            } catch (ReferenceResolutionException exception) {
                return CompletableFutureUtils.exceptionallyCompletedFuture(
                    new ReferenceResolutionException(format(FAILED_TO_RESOLVE_SUPPLY_CHANNEL, draftBuilder.getSku(),
                        exception.getMessage()), exception));
            }
        }
        return completedFuture(draftBuilder);
    }

    /**
     * Given an {@link InventoryEntryDraftBuilder} and a {@code channelKey} this method fetches the actual id of the
     * channel corresponding to this key, ideally from a cache. Then it sets this id on the supply channel reference
     * id of the inventory entry draft builder. If the id is not found in cache nor the CTP project
     * and {@code ensureChannel} option is set to true, a new channel will be created with this key
     * and the role {@code "InventorySupply"}.
     * However, if the {@code ensureChannel} is set to false, the future is completed exceptionally with a
     * {@link ReferenceResolutionException}.
     *
     * @param draftBuilder the inventory draft builder to read it's values (key, sku, channel)
     *                     and then to write resolved resource identifiers.
     * @param channelKey the key of the channel to resolve it's actual id on the draft.
     * @return a {@link CompletionStage} that contains as a result the same {@code draftBuilder} inventory draft builder
     *         instance with resolved supply channel resource identifier or an exception.
     */
    @Nonnull
    private CompletionStage<InventoryEntryDraftBuilder> fetchOrCreateAndResolveReference(
        @Nonnull final InventoryEntryDraftBuilder draftBuilder,
        @Nonnull final String channelKey) {

        final CompletionStage<InventoryEntryDraftBuilder> inventoryEntryDraftCompletionStage = channelService
            .fetchCachedChannelId(channelKey)
            .thenCompose(resolvedChannelIdOptional -> resolvedChannelIdOptional
                .map(resolvedChannelId -> setChannelReference(resolvedChannelId, draftBuilder))
                .orElseGet(() -> createChannelAndSetReference(channelKey, draftBuilder)));

        final CompletableFuture<InventoryEntryDraftBuilder> result = new CompletableFuture<>();
        inventoryEntryDraftCompletionStage
            .whenComplete((resolvedDraftBuilder, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(
                        new ReferenceResolutionException(format(FAILED_TO_RESOLVE_SUPPLY_CHANNEL, draftBuilder.getSku(),
                            exception.getCause().getMessage()), exception));
                } else {
                    result.complete(resolvedDraftBuilder);
                }
            });
        return result;

    }


    /**
     * Helper method that returns a completed CompletionStage with a resolved channel resource identifier
     * {@link InventoryEntryDraftBuilder} object as a result of setting the passed {@code channelId}
     * as the id of channel resource identifier.
     *
     * @param channelId    the channel id to set on the inventory entry supply channel resource identifier id field.
     * @param draftBuilder the inventory draft builder where to write resolved resource identifier.
     * @return a completed CompletionStage with a resolved channel resource identifier with the same
     *         {@link InventoryEntryDraftBuilder} instance as a result of setting the passed {@code channelId}
     *         as the id of channel resource identifier.
     */
    @Nonnull
    private static CompletionStage<InventoryEntryDraftBuilder> setChannelReference(
            @Nonnull final String channelId,
            @Nonnull final InventoryEntryDraftBuilder draftBuilder) {
        return completedFuture(draftBuilder.supplyChannel(ResourceIdentifier.ofId(channelId)));
    }

    /**
     * Helper method that creates a new {@link Channel} on the CTP project with the specified {@code channelKey} and of
     * the role {@code "InventorySupply"}. Only if the {@code ensureChannels} options is set to {@code true} on the
     * {@code options} instance of {@code this} class. Then it resolves the supply channel resource identifier on the
     * supplied {@code inventoryEntryDraft} by setting the id of its supply channel resource identifier with the newly
     * created Channel.
     *
     * <p>If the {@code ensureChannels} options is set to {@code false} on the {@code options} instance of {@code this}
     * class, the future is completed exceptionally with a {@link ReferenceResolutionException}.
     *
     * <p>The method then returns a CompletionStage with a resolved channel resource identifiers
     * {@link InventoryEntryDraftBuilder} object.
     *
     * @param channelKey   the key to create the new channel with.
     * @param draftBuilder the inventory draft builder where to write resolved resource identifiers.
     * @return a CompletionStage with the same {@code draftBuilder} inventory draft builder instance where channel
     *         channels are resolved.
     */
    @Nonnull
    private CompletionStage<InventoryEntryDraftBuilder> createChannelAndSetReference(
            @Nonnull final String channelKey,
            @Nonnull final InventoryEntryDraftBuilder draftBuilder) {

        if (options.shouldEnsureChannels()) {
            return channelService
                .createAndCacheChannel(channelKey)
                .thenCompose(createdChannelOptional -> {
                    if (createdChannelOptional.isPresent()) {
                        return setChannelReference(createdChannelOptional.get().getId(), draftBuilder);
                    } else {
                        final ReferenceResolutionException referenceResolutionException =
                            new ReferenceResolutionException(format(CHANNEL_DOES_NOT_EXIST, channelKey));
                        return CompletableFutureUtils.exceptionallyCompletedFuture(referenceResolutionException);
                    }
                });
        } else {
            final ReferenceResolutionException referenceResolutionException =
                new ReferenceResolutionException(format(CHANNEL_DOES_NOT_EXIST, channelKey));
            return CompletableFutureUtils.exceptionallyCompletedFuture(referenceResolutionException);
        }
    }
}
