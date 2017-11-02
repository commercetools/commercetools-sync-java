package com.commercetools.sync.products.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.json.JsonException;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeAccess;
import io.sphere.sdk.products.attributes.AttributeDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.replaceReferenceIdWithKey;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class VariantReferenceReplacementUtils {
    /**
     * Takes a list of Variants that are supposed to have their prices and attributes references expanded in order to be
     * able to fetch the keys and replace the reference ids with the corresponding keys and then return a new list of
     * product variant drafts with their references containing keys instead of the ids.
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
                final List<AttributeDraft> attributeDraftsWithKeys =
                    replaceAttributesReferencesIdsWithKeys(productVariant);

                return ProductVariantDraftBuilder.of(productVariant)
                                                 .prices(priceDraftsWithKeys)
                                                 .attributes(attributeDraftsWithKeys)
                                                 .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Takes a product variant that is supposed to have all its prices' channels expanded in order to be able to fetch
     * the keys and replace the reference ids with the corresponding keys for the channel references. This method
     * returns as a result a {@link List} of {@link PriceDraft} that has all channel references with keys replacing the
     * ids.
     *
     * <p>Any channel reference that is not expanded will have it's id in place and not replaced by the key.
     *
     * @param productVariant the product variant to replace its prices' channel' ids with keys.
     * @return  a {@link List} of {@link PriceDraft} that has all channel references with keys replacing the ids.
     */
    @Nonnull
    static List<PriceDraft> replacePricesReferencesIdsWithKeys(@Nonnull final ProductVariant productVariant) {
        return productVariant.getPrices().stream().map(price -> {
            final Reference<Channel> channelReferenceWithKey = replaceChannelReferenceIdWithKey(price);
            return PriceDraftBuilder.of(price).channel(channelReferenceWithKey).build();
        }).collect(toList());
    }

    /**
     * Takes a price that is supposed to have its channel reference expanded in order to be able to fetch the key
     * and replace the reference id with the corresponding key and then return a new {@link Channel} {@link Reference}
     * containing the key in the id field.
     *
     * <p><b>Note:</b> The Channel reference should be expanded for the {@code price}, otherwise the reference
     * id will not be replaced with the key and will still have the id in place.
     *
     * @param price the price to replace its channel reference id with the key.
     *
     * @return a new {@link Channel} {@link Reference} containing the key in the id field.
     */
    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    static Reference<Channel> replaceChannelReferenceIdWithKey(@Nonnull final Price price) {
        final Reference<Channel> priceChannel = price.getChannel();
        return replaceReferenceIdWithKey(priceChannel, () -> Channel.referenceOfId(priceChannel.getObj().getKey()));
    }

    /**
     * Takes a product variant that is supposed to have all its attribute product references and product set references
     * expanded in order to be able to fetch the keys and replace the reference ids with the corresponding keys for the
     * references. This method returns as a result a {@link List} of {@link AttributeDraft} that has all product
     * references with keys replacing the ids.
     *
     * <p>Any product reference that is not expanded will have it's id in place and not replaced by the key.
     *
     * @param productVariant the product variant to replace its attribute product references ids with keys.
     * @return  a {@link List} of {@link AttributeDraft} that has all product references with keys replacing the ids.
     */
    @Nonnull
    static List<AttributeDraft> replaceAttributesReferencesIdsWithKeys(@Nonnull final ProductVariant productVariant) {
        return productVariant.getAttributes().stream()
                             .map(attribute -> replaceAttributeReferenceIdWithKey(attribute)
                                 .map(productReference -> AttributeDraft.of(attribute.getName(), productReference))
                                 .orElseGet(() -> replaceAttributeReferenceSetIdsWithKeys(attribute)
                                     .map(productReferenceSet ->
                                         AttributeDraft.of(attribute.getName(), productReferenceSet))
                                     .orElseGet(() ->
                                         AttributeDraft.of(attribute.getName(), attribute.getValueAsJsonNode()))))
                             .collect(toList());
    }


    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    static Optional<Reference<Product>> replaceAttributeReferenceIdWithKey(@Nonnull final Attribute attribute) {
        return getProductReference(attribute)
            .map(productReference -> replaceReferenceIdWithKey(productReference,
                () -> Product.referenceOfId(productReference.getObj().getKey())));
    }

    private static Optional<Reference<Product>> getProductReference(@Nonnull final Attribute attribute) {
        try {
            return Optional.of(attribute.getValue(AttributeAccess.ofProductReference()));
        } catch (final JsonException exception) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    static Optional<Set<Reference<Product>>> replaceAttributeReferenceSetIdsWithKeys(
        @Nonnull final Attribute attribute) {
        return getProductReferenceSet(attribute).map(productReferenceSet ->
            productReferenceSet.stream()
                               .map(productReference ->
                                   replaceReferenceIdWithKey(productReference,
                                       () -> Product.referenceOfId(productReference.getObj().getKey())))
                               .collect(toSet()));
    }

    private static Optional<Set<Reference<Product>>> getProductReferenceSet(@Nonnull final Attribute attribute) {
        try {
            return Optional.of(attribute.getValue(AttributeAccess.ofProductReferenceSet()));
        } catch (final JsonException exception) {
            return Optional.empty();
        }
    }
}
