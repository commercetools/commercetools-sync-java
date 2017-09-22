package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductVariantAttribute;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.updateactions.SetSku;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public final class ProductVariantUpdateActionUtils {

    /**
     * Compares the attributes of a {@link ProductVariantDraft} and a {@link ProductVariant}.
     * TODO: Add JavaDoc
     *
     * @param oldProductVariant  TODO
     * @param newProductVariant  TODO
     * @param syncOptions        TODO
     * @param attributesMetaData TODO
     * @return TODO
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildProductVariantAttributesUpdateActions(
            @Nonnull final ProductVariant oldProductVariant,
            @Nonnull final ProductVariantDraft newProductVariant,
            @Nonnull final ProductSyncOptions syncOptions,
            @Nonnull final Map<String, ProductVariantAttribute> attributesMetaData) {
        final List<UpdateAction<Product>> updateActions = new ArrayList<>();
        final List<Attribute> oldProductVariantAttributes = oldProductVariant.getAttributes();
        final List<AttributeDraft> newProductVariantAttributes = newProductVariant.getAttributes();
        newProductVariantAttributes.stream()
                .filter(Objects::nonNull)
                .forEach(newProductVariantAttribute -> {
                    //TODO: IMPLEMENTATION GITHUB ISSUE#98
                    final String newProductVariantAttributeName =
                            newProductVariantAttribute.getName();
                    final JsonNode newProductVariantAttributeValue =
                            newProductVariantAttribute.getValue();
                });
        return updateActions;
    }

    /**
     * Compares the prices of a {@link ProductVariantDraft} and a {@link ProductVariant}.
     * TODO: Add JavaDoc
     *
     * @param oldProductVariant TODO
     * @param newProductVariant TODO
     * @param syncOptions       TODO
     * @return TODO
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildProductVariantPricesUpdateActions(
            @Nonnull final ProductVariant oldProductVariant,
            @Nonnull final ProductVariantDraft newProductVariant,
            @Nonnull final ProductSyncOptions syncOptions) {
        //TODO: IMPLEMENTATION GITHUB ISSUE#99
        return Collections.emptyList();
    }

    /**
     * Compares the images of a {@link ProductVariantDraft} and a {@link ProductVariant}.
     * TODO: Add JavaDoc..
     *
     * @param oldProductVariant TODO
     * @param newProductVariant TODO
     * @param syncOptions       TODO
     * @return TODO
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildProductVariantImagesUpdateActions(
            @Nonnull final ProductVariant oldProductVariant,
            @Nonnull final ProductVariantDraft newProductVariant,
            @Nonnull final ProductSyncOptions syncOptions) {
        //TODO: IMPLEMENTATION GITHUB ISSUE#100
        return Collections.emptyList();
    }

    /**
     * Update variants' SKUs by key:
     * In old and new variant lists find those pairs which have the the same {@code key} and different {@code sku}
     * and create {@link SetSku} update action for them, using {@link ProductVariant#getId() oldVariant#id} and
     * {@link ProductVariantDraft#getSku() newVariant#sku}.
     *
     * @param oldVariant old product with variants
     * @param newVariant new product draft with variants
     * @return list of {@link SetSku} actions. Empty list if no SKU changed.
     */
    @Nonnull
    public static List<SetSku> buildProductVariantSkuUpdateActions(@Nonnull final ProductVariant oldVariant,
                                                                   @Nonnull final ProductVariantDraft newVariant) {

        return Objects.equals(oldVariant.getSku(), newVariant.getSku())
                ? emptyList()
                : singletonList(SetSku.of(oldVariant.getId(), newVariant.getSku(), true));
    }

    private ProductVariantUpdateActionUtils() {
    }
}
