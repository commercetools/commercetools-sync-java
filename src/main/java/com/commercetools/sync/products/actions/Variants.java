package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.helpers.ProductSyncUtils;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.SetSku;

import java.util.Optional;

final class Variants {
    private Variants() {
    }

    static Optional<UpdateAction<Product>> setMasterVariantSku(final Product product, final ProductDraft draft,
                                                               final ProductSyncOptions syncOptions) {
        // suppress NPE inspection as null-check is already done in wrapper method
        //noinspection ConstantConditions
        return ActionUtils.actionOnProductData(product, syncOptions,
            productData -> productData.getMasterVariant().getSku(), draft.getMasterVariant().getSku(),
            newSku -> SetSku.of(ProductSyncUtils.masterData(product, syncOptions).getMasterVariant().getId(), newSku,
                syncOptions.isUpdateStaged()));
        // TODO beware that this change is staged and needs to be published
    }
}
