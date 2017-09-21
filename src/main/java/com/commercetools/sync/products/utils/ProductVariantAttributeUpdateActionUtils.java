package com.commercetools.sync.products.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDraft;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public final class ProductVariantAttributeUpdateActionUtils {

    @Nonnull
    public static List<UpdateAction<Product>>
        buildProductVariantAttributeUpdateActions(@Nonnull final Attribute oldProductVariantAttribute,
                                                  @Nonnull final AttributeDraft newProductVariantAttribute) {
        return Collections.emptyList();
    }
}
