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
import io.sphere.sdk.utils.CompletableFutureUtils;

import javax.annotation.Nonnull;
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
        return resolveCustomTypeReference(PriceDraftBuilder.of(priceDraft))
            .thenCompose(this::resolveChannelReference)
            .thenApply(PriceDraftBuilder::build);
    }

    @Override
    @Nonnull
    protected CompletionStage<PriceDraftBuilder> resolveCustomTypeReference(
        @Nonnull final PriceDraftBuilder draftBuilder) {

        return resolveCustomTypeReference(draftBuilder,
            PriceDraftBuilder::getCustom,
            PriceDraftBuilder::custom,
            format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getCountry(), draftBuilder.getValue()));
    }

    /**
     * Given a {@link PriceDraftBuilder} this method attempts to resolve the supply channel reference to return
     * a {@link CompletionStage} which contains the same instance of draft builder with the resolved
     * supply channel reference. The key of the supply channel is either taken from the expanded reference or
     * taken from the id field of the reference.
     *
     * <p>The method then tries to fetch the key of the supply channel, optimistically from a
     * cache. If the id is not found in cache nor the CTP project and {@code ensureChannel}
     * option is set to true, a new channel will be created with this key and the role {@code "InventorySupply"}.
     * However, if the {@code ensureChannel} is set to false, the future is completed exceptionally with a
     * {@link ReferenceResolutionException}.
     *
     * @param draftBuilder the PriceDraftBuilder to resolve its channel reference.
     * @return a {@link CompletionStage} that contains as a result a new price draft builder instance with resolved
     *         supply channel or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    CompletionStage<PriceDraftBuilder> resolveChannelReference(@Nonnull final PriceDraftBuilder draftBuilder) {

        return resolveReference(draftBuilder, draftBuilder.getChannel(), channelService::fetchCachedChannelId,
            Channel::referenceOfId,
            (priceDraftBuilder, reference) -> completedFuture(priceDraftBuilder.channel(reference)),
            (priceDraftBuilder, channelKey) -> {
                final CompletableFuture<PriceDraftBuilder> result = new CompletableFuture<>();
                createChannelAndSetReference(channelKey, priceDraftBuilder)
                    .whenComplete((draftWithCreatedChannel, exception) -> {
                        if (exception != null) {
                            result.completeExceptionally(
                                new ReferenceResolutionException(format(FAILED_TO_RESOLVE_REFERENCE,
                                    Channel.referenceTypeId(), draftBuilder.getCountry(), draftBuilder.getValue(),
                                    exception.getMessage()), exception));
                        } else {
                            result.complete(draftWithCreatedChannel);
                        }
                    });
                return result;
            });
    }

    /**
     * Common function to resolve references from key.
     *
     * @param draftBuilder                    {@link PriceDraftBuilder} to update
     * @param reference                       reference instance from which key is read
     * @param keyToIdMapper                   function which calls respective service to fetch the reference by key
     * @param idToReferenceMapper             function which creates {@link Reference} instance from fetched id
     * @param referenceSetter                 function which will set the resolved reference to the {@code draftBuilder}
     * @param nonExistingReferenceDraftMapper function which will be used to map the draft builder in case the reference
     *                                        to exist after applying the {@code idToReferenceMapper}.
     * @param <T>                             type of reference (e.g. {@link Channel}, {@link CustomerGroup}
     * @return {@link CompletionStage} containing {@link PriceDraftBuilder} with resolved &lt;T&gt; reference.
     */
    @Nonnull
    private <T> CompletionStage<PriceDraftBuilder> resolveReference(
        @Nonnull final PriceDraftBuilder draftBuilder,
        @Nullable final Reference<T> reference,
        @Nonnull final Function<String, CompletionStage<Optional<String>>> keyToIdMapper,
        @Nonnull final Function<String, Reference<T>> idToReferenceMapper,
        @Nonnull final BiFunction<PriceDraftBuilder, Reference<T>, CompletionStage<PriceDraftBuilder>> referenceSetter,
        @Nonnull final BiFunction<PriceDraftBuilder, String, CompletionStage<PriceDraftBuilder>>
            nonExistingReferenceDraftMapper) {

        if (reference == null) {
            return completedFuture(draftBuilder);
        }

        try {
            final String resourceKey = getKeyFromResourceIdentifier(reference, options.shouldAllowUuidKeys());
            return keyToIdMapper.apply(resourceKey)
                                .thenCompose(resourceIdOptional -> resourceIdOptional
                                    .map(idToReferenceMapper)
                                    .map(referenceToSet -> referenceSetter.apply(draftBuilder, referenceToSet))
                                    .orElseGet(() -> nonExistingReferenceDraftMapper.apply(draftBuilder, resourceKey)));
        } catch (ReferenceResolutionException referenceResolutionException) {
            return exceptionallyCompletedFuture(
                new ReferenceResolutionException(
                    format(FAILED_TO_RESOLVE_REFERENCE, reference.getTypeId(), draftBuilder.getCountry(),
                        draftBuilder.getValue(), referenceResolutionException.getMessage())));
        }
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
     * <p>The method then returns a CompletionStage with a resolved channel reference {@link PriceDraftBuilder}
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
