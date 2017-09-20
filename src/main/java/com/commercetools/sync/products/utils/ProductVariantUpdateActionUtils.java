package com.commercetools.sync.products.utils;

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

import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToSet;
import static com.commercetools.sync.commons.utils.CollectionUtils.filterCollection;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.util.stream.Collectors.toList;

public final class ProductVariantUpdateActionUtils {

    /**
     * <b>Note:</b> if you do both add/remove product variants - <b>first always remove the variants</b>,
     * and then - add.
     * The reason of such restriction: if you add first you could have duplication exception on the keys,
     * which expected to be removed first.
     *
     * @param oldProduct old product with variants
     * @param newProduct new product draft with variants <b>with resolved references prices references</b>
     * @return list of update actions to remove missing variants.
     * @see #buildAddVariantUpdateAction(Product, ProductDraft)
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildRemoveVariantUpdateAction(@Nonnull final Product oldProduct,
                                                                             @Nonnull final ProductDraft newProduct) {
        final List<ProductVariant> oldVariants = oldProduct.getMasterData().getStaged().getVariants();
        final Set<String> newVariants = collectionToSet(newProduct.getVariants(), ProductVariantDraft::getKey);

        return filterCollection(oldVariants, oldVariant -> !newVariants.contains(oldVariant.getKey()))
                .map(RemoveVariant::of)
                .collect(toList());
    }

    /**
     * <b>Note:</b> if you do both add/remove product variants - <b>first always remove the variants</b>,
     * and then - add.
     * The reason of such restriction: if you add first you could have duplication exception on the keys,
     * which expected to be removed first.
     *
     * @param oldProduct old product with variants
     * @param newProduct new product draft with variants <b>with resolved references prices references</b>
     * @return list of update actions to add new variants.
     * @see #buildRemoveVariantUpdateAction(Product, ProductDraft)
     */
    @Nonnull
    public static List<AddVariant> buildAddVariantUpdateAction(@Nonnull final Product oldProduct,
                                                                          @Nonnull final ProductDraft newProduct) {
        final List<ProductVariantDraft> newVariants = newProduct.getVariants();
        final Set<String> oldVariantKeys =
                collectionToSet(oldProduct.getMasterData().getStaged().getVariants(), ProductVariant::getKey);

        return filterCollection(newVariants, newVariant -> !oldVariantKeys.contains(newVariant.getKey()))
                .map(ProductVariantUpdateActionUtils::buildAddVariantUpdateActionFromDraft)
                .collect(toList());
    }

    /**
     * Create update action, if {@code newProduct} has {@code #masterVariant#key} different than {@code oldProduct}
     * staged {@code #masterVariant#key}.
     *
     * <p>If update action is created - it is created of
     * {@link ProductVariantDraft newProduct.getMasterVariant().getSku()}
     *
     * @param oldProduct old product with variants
     * @param newProduct new product draft with variants <b>with resolved references prices references</b>
     * @return {@link ChangeMasterVariant} if the keys are different.
     */
    @Nonnull
    public static Optional<ChangeMasterVariant> buildChangeMasterVariantUpdateAction(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct) {
        final String newKey = newProduct.getMasterVariant().getKey();
        final String oldKey = oldProduct.getMasterData().getStaged().getMasterVariant().getKey();
        return buildUpdateAction(newKey, oldKey,
            // it might be that the new master variant is from new added variants, so CTP variantId is not set yet,
            // thus we can't use ChangeMasterVariant.ofVariantId()
            () -> ChangeMasterVariant.ofSku(newProduct.getMasterVariant().getSku(), true));
    }

    /**
     * Factory method to create {@link AddVariant} action from {@link ProductVariantDraft} instance.
     *
     * <p>The {@link AddVariant} will include:<ul>
     *     <li>sku</li>
     *     <li>keys</li>
     *     <li>attributes</li>
     *     <li>prices</li>
     *     <li>images</li>
     * </ul>
     *
     * @param draft {@link ProductVariantDraft} which to add.
     * @return new {@link AddVariant} update action with properties from {@code draft}
     */
    @Nonnull
    public static AddVariant buildAddVariantUpdateActionFromDraft(@Nonnull final ProductVariantDraft draft) {
        return AddVariant.of(draft.getAttributes(), draft.getPrices(), draft.getSku())
                .withKey(draft.getKey())
                .withImages(draft.getImages());
    }

    private ProductVariantUpdateActionUtils() {
    }
}
