package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.utils.CollectionUtils;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.commands.updateactions.AddVariant;
import io.sphere.sdk.products.commands.updateactions.ChangeMasterVariant;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.*;
import static java.util.stream.Collectors.toList;

public final class ProductVariantUpdateActionUtils {

    /**
     * @see #buildAddVariantUpdateAction(Product, ProductDraft)
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildRemoveVariantUpdateAction(@Nonnull final Product oldProduct,
                                                                             @Nonnull final ProductDraft newProduct) {
        final List<ProductVariant> oldVariants = oldProduct.getMasterData().getStaged().getVariants();
        final Set<String> newVariants = CollectionUtils.collectionToSet(newProduct.getVariants(), ProductVariantDraft::getKey);

        return CollectionUtils.filterCollection(oldVariants, oldVariant -> !newVariants.contains(oldVariant.getKey()))
                .map(RemoveVariant::of)
                .collect(toList());
    }

    /**
     * <b>Note:</b> if you do both add/remove product variants - <b>first always remove the variants</b>, and then - add.
     * The reason of such restriction: if you add first you could have duplication exception on the keys,
     * which expected to be removed first.
     *
     * @param oldProduct old product with variants
     * @param newProduct new product draft with variants <b>with resolved references prices references</b>
     * @return list of update actions to add new variants.
     * @see #buildRemoveVariantUpdateAction(Product, ProductDraft)
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildAddVariantUpdateAction(@Nonnull final Product oldProduct,
                                                                          @Nonnull final ProductDraft newProduct) {
        final List<ProductVariantDraft> newVariants = newProduct.getVariants();
        final Set<String> oldVariantKeys = CollectionUtils.collectionToSet(oldProduct.getMasterData().getStaged().getVariants(), ProductVariant::getKey);

        return CollectionUtils.filterCollection(newVariants, newVariant -> !oldVariantKeys.contains(newVariant.getKey()))
                .map(ProductVariantUpdateActionUtils::buildAddVariantUpdateActionFromDraft)
                .collect(toList());
    }

    @Nonnull
    public static Optional<UpdateAction<Product>> buildChangeMasterVariantUpdateAction(@Nonnull final Product oldProduct,
                                                                                       @Nonnull final ProductDraft newProduct) {
        return buildUpdateAction(newProduct.getMasterVariant().getKey(), oldProduct.getMasterData().getStaged().getMasterVariant().getKey(),
                // it might be that the new master variant is from new added variants, so CTP variantId is not set yet,
                // thus we can't use ChangeMasterVariant.ofVariantId()
                () -> ChangeMasterVariant.ofSku(newProduct.getMasterVariant().getSku()));
    }

    @Nonnull
    public static AddVariant buildAddVariantUpdateActionFromDraft(@Nonnull ProductVariantDraft draft) {
        return AddVariant.of(draft.getAttributes(), draft.getPrices(), draft.getSku())
                .withKey(draft.getKey())
                .withImages(draft.getImages());
    }

    private ProductVariantUpdateActionUtils() {
    }
}
