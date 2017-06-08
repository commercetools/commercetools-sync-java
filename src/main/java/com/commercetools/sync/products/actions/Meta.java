package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;

import java.util.Optional;

import static com.commercetools.sync.products.actions.ActionUtils.actionOnProductData;

final class Meta {

    private Meta() {
    }

    static Optional<UpdateAction<Product>> setMetaDescription(final Product product, final ProductDraft draft,
                                                              final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
            ProductData::getMetaDescription, draft.getMetaDescription(),
            SetMetaDescription::of);
    }

    static Optional<UpdateAction<Product>> setMetaKeywords(final Product product, final ProductDraft draft,
                                                           final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
            ProductData::getMetaKeywords, draft.getMetaKeywords(),
            SetMetaKeywords::of);
    }

    static Optional<UpdateAction<Product>> setMetaTitle(final Product product, final ProductDraft draft,
                                                        final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
            ProductData::getMetaTitle, draft.getMetaTitle(),
            SetMetaTitle::of);
    }
}
