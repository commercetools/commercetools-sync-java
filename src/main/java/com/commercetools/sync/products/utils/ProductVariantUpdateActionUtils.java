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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ProductVariantUpdateActionUtils {

    /**
     * TODO: Add JavaDoc
     * @param oldProductVariant TODO
     * @param newProductVariant
     * @param syncOptions
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Product>>
    buildProductVariantAttributesUpdateActions(@Nonnull final ProductVariant oldProductVariant,
                                               @Nonnull final ProductVariantDraft newProductVariant,
                                               @Nonnull final ProductSyncOptions syncOptions,
                                               @Nonnull final Map<String, ProductVariantAttribute>
                                                   attributesMetaData) {
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
     * TODO: Add JavaDoc
     * @param oldProductVariant
     * @param newProductVariant
     * @param syncOptions
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Product>>
    buildProductVariantPricesUpdateActions(@Nonnull final ProductVariant oldProductVariant,
                                           @Nonnull final ProductVariantDraft newProductVariant,
                                           @Nonnull final ProductSyncOptions syncOptions) {
        //TODO: IMPLEMENTATION GITHUB ISSUE#99
        return Collections.emptyList();
    }

    // TODO: Add JavaDoc..
    @Nonnull
    public static List<UpdateAction<Product>>
    buildProductVariantImagesUpdateActions(@Nonnull final ProductVariant oldProductVariant,
                                           @Nonnull final ProductVariantDraft newProductVariant,
                                           @Nonnull final ProductSyncOptions syncOptions) {
        //TODO: IMPLEMENTATION GITHUB ISSUE#100
        return Collections.emptyList();
    }
}
