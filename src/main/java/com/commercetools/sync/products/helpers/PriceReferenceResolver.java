package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

public final class PriceReferenceResolver
    extends CustomReferenceResolver<PriceDraft, PriceDraftBuilder, ProductSyncOptions> {

    private ChannelService channelService;
    private static final String CHANNEL_DOES_NOT_EXIST = "Channel with key '%s' does not exist.";
    private static final String FAILED_TO_RESOLVE_CHANNEL = "Failed to resolve the channel reference on "
        + "PriceDraft with country:'%s' and value: '%s'. Reason: %s";
    private static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
        + "PriceDraft with country:'%s' and value: '%s'.";

    public PriceReferenceResolver(@Nonnull final ProductSyncOptions options,
                                  @Nonnull final TypeService typeService,
                                  @Nonnull final ChannelService channelService) {
        super(options, typeService);
        this.channelService = channelService;
    }

    @Override
    protected CompletionStage<PriceDraftBuilder> resolveCustomTypeReference(@Nonnull final PriceDraft draft) {
        final CustomFieldsDraft custom = draft.getCustom();
        final PriceDraftBuilder draftBuilder = PriceDraftBuilder.of(draft);
        if (custom != null) {
            return getCustomTypeId(custom,
                    format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getCountry(), draftBuilder.getValue()))
                .thenApply(resolvedTypeIdOptional -> resolvedTypeIdOptional
                    .map(resolvedTypeId -> draftBuilder
                        .custom(CustomFieldsDraft.ofTypeIdAndJson(resolvedTypeId, custom.getFields())))
                    .orElse(draftBuilder));
        }
        return completedFuture(draftBuilder);
    }

    /**
     * Given a {@link PriceDraft} this method attempts to resolve the custom type and channel
     * references to return a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * references. The keys of the references are either taken from the expanded references or
     * taken from the id field of the references.
     *
     * @param priceDraft the priceDraft to resolve it's references.
     * @return a {@link CompletionStage} that contains as a result a new inventoryEntryDraft instance with resolved
     *         references or, in case an error occurs during reference resolution a
     *         {@link ReferenceResolutionException}.
     */
    @Override
    public CompletionStage<PriceDraft> resolveReferences(@Nonnull final PriceDraft priceDraft) {
        return resolveCustomTypeReference(priceDraft)
            .thenCompose(this::resolveChannelReference)
            .thenApply(PriceDraftBuilder::build);
    }

    /**
     * Given a {@link PriceDraft} this method attempts to resolve the supply channel reference to return
     * a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * supply channel reference. The key of the supply channel is either taken from the expanded reference or
     * taken from the id field of the reference.
     *
     * <p>The method then tries to fetch the key of the supply channel, optimistically from a
     * cache. If the id is not found in cache nor the CTP project and {@code ensureChannel}
     * option is set to true, a new channel will be created with this key and the role {@code "InventorySupply"}.
     * However, if the {@code ensureChannel} is set to false, the future is completed exceptionally with a
     * {@link ReferenceResolutionException}.
     *
     * @param draft the inventoryEntryDraft to resolve it's channel reference.
     * @return a {@link CompletionStage} that contains as a result a new price draft builder instance with resolved
     *         supply channel or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    private CompletionStage<PriceDraftBuilder> resolveChannelReference(@Nonnull final PriceDraftBuilder draft) {
        final Reference<Channel> channelReference = draft.getChannel();
        if (channelReference != null) {
            try {
                final String keyFromExpansion = getKeyFromExpansion(channelReference);
                final String channelKey = getKeyFromExpansionOrReference(options.shouldAllowUuidKeys(),
                    keyFromExpansion, channelReference);
                return fetchOrCreateAndResolveReference(draft, channelKey);
            } catch (ReferenceResolutionException exception) {
                return CompletableFutureUtils.exceptionallyCompletedFuture(
                    new ReferenceResolutionException(format(FAILED_TO_RESOLVE_CHANNEL, draft.getCountry(),
                        draft.getValue(), exception.getMessage()), exception));
            }
        }
        return completedFuture(draft);
    }

    /**
     * Helper method that returns the value of the key field from the channel {@link Reference} object, if expanded.
     * Otherwise, returns null.
     *
     * @return the value of the key field from the channel {@link Reference} object, if expanded.
     *         Otherwise, returns null.
     */
    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE can't occur because of the isReferenceExpanded check.
    private static String getKeyFromExpansion(@Nonnull final Reference<Channel> channelReference) {
        return isReferenceExpanded(channelReference) ? channelReference.getObj().getKey() : null;
    }

    /**
     * Given an {@link ProductSyncOptions} and a {@code channelKey} this method fetches the actual id of the
     * channel corresponding to this key, ideally from a cache. Then it sets this id on the supply channel reference
     * id of the inventory entry draft. If the id is not found in cache nor the CTP project and {@code ensureChannel}
     * option is set to true, a new channel will be created with this key and the role {@code "InventorySupply"}.
     * However, if the {@code ensureChannel} is set to false, the future is completed exceptionally with a
     * {@link ReferenceResolutionException}.
     *
     * @param draft      the price draft builder where to set resolved references.
     * @param channelKey the key of the channel to resolve it's actual id on the draft.
     * @return a {@link CompletionStage} that contains as a result the same {@code draft} instance with resolved
     *         supply channel reference or an exception.
     */
    @Nonnull
    private CompletionStage<PriceDraftBuilder> fetchOrCreateAndResolveReference(
        @Nonnull final PriceDraftBuilder draft,
        @Nonnull final String channelKey) {
        final CompletionStage<PriceDraftBuilder> priceDraftCompletionStage = channelService
            .fetchCachedChannelId(channelKey)
            .thenCompose(resolvedChannelIdOptional -> resolvedChannelIdOptional
                .map(resolvedChannelId -> setChannelReference(resolvedChannelId, draft))
                .orElseGet(() -> createChannelAndSetReference(channelKey, draft)));

        final CompletableFuture<PriceDraftBuilder> result = new CompletableFuture<>();
        priceDraftCompletionStage
            .whenComplete((resolvedDraft, exception) -> {
                if (exception != null) {
                    result.completeExceptionally(
                        new ReferenceResolutionException(format(FAILED_TO_RESOLVE_CHANNEL, draft.getCountry(),
                            draft.getValue(), exception.getMessage()), exception));
                } else {
                    result.complete(resolvedDraft);
                }
            });
        return result;
    }

    /**
     * Helper method that returns a completed CompletionStage with a resolved channel reference
     * {@link PriceDraft} object as a result of setting the passed {@code channelId} as the id of channel
     * reference.
     *
     * @param channelId  the channel id to set on the price channel reference id field.
     * @param builder    the price draft builder where to update the channel reference.
     * @return a completed CompletionStage with the same instance of {@code builder} having resolved channel reference
     *         as a result of setting the passed {@code channelId} as the id of channel reference.
     */
    @Nonnull
    private static CompletionStage<PriceDraftBuilder> setChannelReference(@Nonnull final String channelId,
                                                                          @Nonnull final PriceDraftBuilder builder) {
        return completedFuture(builder.channel(Channel.referenceOfId(channelId)));
    }

    /**
     * Helper method that creates a new {@link Channel} on the CTP project with the specified {@code channelKey} and of
     * the role {@code "InventorySupply"}. Only if the {@code ensureChannels} options is set to {@code true} on the
     * {@code options} instance of {@code this} class. Then it resolves the supply channel reference on the supplied
     * {@code inventoryEntryDraft} by setting the id of it's supply channel reference with the newly created Channel.
     *
     * <p>If the {@code ensureChannels} options is set to {@code false} on the {@code options} instance of {@code this}
     * class, the future is completed exceptionally with a {@link ReferenceResolutionException}.
     *
     * <p>The method then returns a CompletionStage with a resolved channel reference {@link PriceDraft}
     * object.
     *
     * @param channelKey   the key to create the new channel with.
     * @param draftBuilder the inventory entry draft builder where to resolve it's supply channel reference to the newly
     *                     created channel.
     * @return a CompletionStage with the same {@code draftBuilder} instance having resolved channel reference.
     */
    @Nonnull
    private CompletionStage<PriceDraftBuilder> createChannelAndSetReference(
            @Nonnull final String channelKey,
            @Nonnull final PriceDraftBuilder draftBuilder) {
        if (options.shouldEnsurePriceChannels()) {
            return channelService.createAndCacheChannel(channelKey)
                .thenCompose(createdChannel -> setChannelReference(createdChannel.getId(), draftBuilder));
        } else {
            final ReferenceResolutionException referenceResolutionException =
                new ReferenceResolutionException(format(CHANNEL_DOES_NOT_EXIST, channelKey));
            return CompletableFutureUtils.exceptionallyCompletedFuture(referenceResolutionException);
        }
    }
}
