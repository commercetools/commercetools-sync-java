package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.SetDescription;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;

import java.util.Optional;

import static com.commercetools.sync.products.actions.ActionUtils.actionOnProductData;

final class Base {

    private Base() {
    }

    static Optional<UpdateAction<Product>> changeName(final Product product, final ProductDraft draft,
                                                      final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
            ProductData::getName, draft.getName(),
            name -> ChangeName.of(name, syncOptions.isUpdateStaged()));
    }

    static Optional<UpdateAction<Product>> changeSlug(final Product product, final ProductDraft draft,
                                                      final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
            ProductData::getSlug, draft.getSlug(),
            slug -> ChangeSlug.of(slug, syncOptions.isUpdateStaged()));
    }

    static Optional<UpdateAction<Product>> setSearchKeywords(final Product product, final ProductDraft draft,
                                                             final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
            ProductData::getSearchKeywords, draft.getSearchKeywords(),
            searchKeywords -> SetSearchKeywords.of(searchKeywords, syncOptions.isUpdateStaged()));
    }

    static Optional<UpdateAction<Product>> setDescription(final Product product, final ProductDraft draft,
                                                          final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
            ProductData::getDescription, draft.getDescription(),
            description -> SetDescription.of(description, syncOptions.isUpdateStaged()));
    }

    // TODO there is a setKey in nodeJs sync tool
}
