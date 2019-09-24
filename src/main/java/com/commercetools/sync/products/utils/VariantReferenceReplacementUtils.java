package com.commercetools.sync.products.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.AssetReferenceReplacementUtils.replaceAssetsReferencesIdsWithKeys;
import static com.commercetools.sync.commons.utils.CustomTypeReferenceReplacementUtils.replaceCustomTypeIdWithKeys;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKeyReplaced;
import static java.util.stream.Collectors.toList;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class VariantReferenceReplacementUtils {
    /**
     * Takes a list of Variants that are supposed to have their prices and assets references expanded in
     * order to be able to fetch the keys and replace the reference ids with the corresponding keys and then return a
     * new list of product variant drafts with their references containing keys instead of the ids.
     *
     * <p><b>Note:</b>If the references are not expanded for a product variant, the reference ids will not be replaced
     * with keys and will still have their ids in place.
     *
     * @param productVariants the product variants to replace their reference ids with keys
     * @return a list of product variant drafts with keys instead of ids for references.
     */
    @Nonnull
    public static List<ProductVariantDraft> replaceVariantsReferenceIdsWithKeys(
        @Nonnull final List<ProductVariant> productVariants) {
        return productVariants
            .stream()
            .filter(Objects::nonNull)
            .map(productVariant -> {
                final List<PriceDraft> priceDraftsWithKeys = replacePricesReferencesIdsWithKeys(productVariant);
                final List<AssetDraft> assetDraftsWithKeys =
                    replaceAssetsReferencesIdsWithKeys(productVariant.getAssets());

                return ProductVariantDraftBuilder.of(productVariant)
                                                 .prices(priceDraftsWithKeys)
                                                 .assets(assetDraftsWithKeys)
                                                 .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Takes a product variant that is supposed to have all its prices' references (channel and custom type reference)
     * expanded in order to be able to fetch the keys and replace the reference ids with the corresponding keys for the
     * references. This method returns as a result a {@link List} of {@link PriceDraft} that has all channel and custom
     * type references with keys replacing the ids.
     *
     * <p>Any reference, whether {@link Channel} or custom {@link io.sphere.sdk.types.Type}, that is not expanded will
     * have its id in place and not replaced by the key.
     *
     * @param productVariant the product variant to replace its prices' reference ids with keys.
     * @return  a {@link List} of {@link PriceDraft} that has all references with keys replacing the ids.
     */
    @Nonnull
    static List<PriceDraft> replacePricesReferencesIdsWithKeys(@Nonnull final ProductVariant productVariant) {

        return productVariant.getPrices().stream().map(price -> {
            final ResourceIdentifier<Channel> channelReferenceWithKey = replaceChannelReferenceIdWithKey(price);
            final CustomFieldsDraft customFieldsDraftWithKey = replaceCustomTypeIdWithKeys(price);

            return PriceDraftBuilder.of(price)
                                    .custom(customFieldsDraftWithKey)
                                    .channel(channelReferenceWithKey)
                                    .build();
        }).collect(toList());
    }

    /**
     * Takes a price that is supposed to have its channel reference expanded in order to be able to fetch the key
     * and replace the reference id with the corresponding key and then return a new {@link Channel}
     * {@link ResourceIdentifier} containing the key in the id field.
     *
     * <p><b>Note:</b> The Channel reference should be expanded for the {@code price}, otherwise the reference
     * id will not be replaced with the key and will still have the id in place.
     *
     * @param price the price to replace its channel reference id with the key.
     *
     * @return a new {@link Channel} {@link ResourceIdentifier} containing the key in the id field.
     */
    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    static ResourceIdentifier<Channel> replaceChannelReferenceIdWithKey(@Nonnull final Price price) {

        final Reference<Channel> priceChannel = price.getChannel();
        return getResourceIdentifierWithKeyReplaced(priceChannel,
            () -> ResourceIdentifier.ofId(priceChannel.getObj().getKey()));
    }

    private VariantReferenceReplacementUtils() {
    }
}
