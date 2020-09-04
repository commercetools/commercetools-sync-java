package com.commercetools.sync.products.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Reference;
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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.AssetReferenceResolutionUtils.mapToAssetDrafts;
import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.commons.utils.SyncUtils.getReferenceWithKeyReplaced;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class VariantReferenceReplacementUtils {
    /**
     * Takes a list of Variants that are supposed to have their prices', assets' and attributes' references expanded in
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
                final List<PriceDraft> priceDraftsWithKeys = mapToPriceDraft(productVariant);
                final List<AttributeDraft> attributeDraftsWithKeys =
                    replaceAttributesReferencesIdsWithKeys(productVariant);
                final List<AssetDraft> assetDraftsWithKeys = mapToAssetDrafts(productVariant.getAssets());

                return ProductVariantDraftBuilder.of(productVariant)
                                                 .prices(priceDraftsWithKeys)
                                                 .attributes(attributeDraftsWithKeys)
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
    static List<PriceDraft> mapToPriceDraft(@Nonnull final ProductVariant productVariant) {

        return productVariant.getPrices().stream().map(price -> PriceDraftBuilder
            .of(price)
            .custom(mapToCustomFieldsDraft(price))
            .channel(getResourceIdentifierWithKey(price.getChannel()))
            .customerGroup(
                getReferenceWithKeyReplaced(price.getCustomerGroup(), () ->
                    CustomerGroup.referenceOfId(price.getCustomerGroup().getObj().getKey())))
            .build()).collect(toList());
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
            .map(productReference ->
                getReferenceWithKeyReplaced(productReference,
                    () -> Product.referenceOfId(productReference.getObj().getKey()))
            );
    }

    private static Optional<Reference<Product>> getProductReference(@Nonnull final Attribute attribute) {
        return Optional.of(attribute)
                       .filter(VariantReferenceReplacementUtils::isProductReference)
                       .map(productReferenceAttribute -> productReferenceAttribute
                           .getValue(AttributeAccess.ofProductReference()));
    }

    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    static Optional<Set<Reference<Product>>> replaceAttributeReferenceSetIdsWithKeys(
        @Nonnull final Attribute attribute) {
        return getProductReferenceSet(attribute).map(productReferenceSet ->
            productReferenceSet.stream()
                               .map(productReference ->
                                   getReferenceWithKeyReplaced(productReference,
                                       () -> Product.referenceOfId(productReference.getObj().getKey()))
                               )
                               .collect(toSet()));
    }

    private static Optional<Set<Reference<Product>>> getProductReferenceSet(@Nonnull final Attribute attribute) {
        return Optional.of(attribute)
                       .filter(VariantReferenceReplacementUtils::isProductReferenceSet)
                       .map(productReferenceSetAttribute -> productReferenceSetAttribute
                           .getValue(AttributeAccess.ofProductReferenceSet()));
    }

    static boolean isProductReference(@Nonnull final Attribute attribute) {
        final JsonNode valueAsJsonNode = attribute.getValueAsJsonNode();
        return !(valueAsJsonNode instanceof ArrayNode) && isValueAProductReference(valueAsJsonNode);
    }

    static boolean isProductReferenceSet(@Nonnull final Attribute attribute) {
        final JsonNode valueAsJsonNode = attribute.getValueAsJsonNode();

        if (valueAsJsonNode instanceof ArrayNode) {
            final Iterator<JsonNode> setIterator = valueAsJsonNode.elements();

            if (setIterator.hasNext()) {
                return isValueAProductReference(setIterator.next());
            }
        }

        return false;
    }

    private static boolean isValueAProductReference(@Nonnull final JsonNode valueAsJsonNode) {
        if (valueAsJsonNode.isContainerNode()) {
            final JsonNode typeIdNode = valueAsJsonNode.get(REFERENCE_TYPE_ID_FIELD);
            return typeIdNode != null && Product.referenceTypeId().equals(typeIdNode.asText());
        }
        return false;
    }

    private VariantReferenceReplacementUtils() {
    }
}
