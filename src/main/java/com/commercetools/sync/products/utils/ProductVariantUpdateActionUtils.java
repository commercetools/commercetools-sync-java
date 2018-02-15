package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.MoveImageToPosition;
import io.sphere.sdk.products.commands.updateactions.RemoveImage;
import io.sphere.sdk.products.commands.updateactions.SetPrices;
import io.sphere.sdk.products.commands.updateactions.SetSku;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CollectionUtils.emptyIfNull;
import static com.commercetools.sync.commons.utils.CollectionUtils.filterCollection;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

// TODO: TESTS
public final class ProductVariantUpdateActionUtils {
    private static final String FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION = "Failed to build a "
        + "setAttribute/setAttributeInAllVariants update action for the attribute with the name '%s' in the "
        + "ProductVariantDraft with key '%s' on the product with key '%s'. Reason: %s";
    private static final String NULL_PRODUCT_VARIANT_ATTRIBUTE = "AttributeDraft is null.";

    /**
     * Compares the attributes of a {@link ProductVariantDraft} and a {@link ProductVariant} to build either
     * {@link io.sphere.sdk.products.commands.updateactions.SetAttribute} or
     * {@link io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants} update actions.
     * If both the {@link ProductVariantDraft} and the {@link ProductVariant} have identical list of attributes, then
     * no update action is needed and hence an empty {@link List} is returned.
     *
     * @param productKey         the key of the product that the variants belong to. It is used only in the error
     *                           messages if any.
     * @param oldProductVariant  the {@link ProductVariant} which should be updated.
     * @param newProductVariant  the {@link ProductVariantDraft} where we get the new list of attributes.
     * @param attributesMetaData a map of attribute name -&gt; {@link AttributeMetaData}; which defines attribute
     *                           information: its name, whether a value is required or not and whether it has the
     *                           constraint "SameForAll" or not.
     * @param syncOptions        the sync options wrapper which contains options related to the sync process supplied by
     *                           the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                           on the build update action process. And other options (See {@link ProductSyncOptions}
     *                           for more info.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are
     *          needed.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildProductVariantAttributesUpdateActions(
            @Nullable final String productKey,
            @Nonnull final ProductVariant oldProductVariant,
            @Nonnull final ProductVariantDraft newProductVariant,
            @Nonnull final Map<String, AttributeMetaData> attributesMetaData,
            @Nonnull final ProductSyncOptions syncOptions) {

        final List<UpdateAction<Product>> updateActions = new ArrayList<>();
        final List<AttributeDraft> newProductVariantAttributes = newProductVariant.getAttributes();
        if (newProductVariantAttributes == null) {
            return updateActions;
        }

        // TODO: NEED TO HANDLE REMOVED ATTRIBUTES FROM OLD PRODUCT VARIANT.
        for (AttributeDraft newProductVariantAttribute : newProductVariantAttributes) {
            if (newProductVariantAttribute == null) {
                final String errorMessage = format(FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION, null,
                        newProductVariant.getKey(), productKey, NULL_PRODUCT_VARIANT_ATTRIBUTE);
                syncOptions.applyErrorCallback(errorMessage, new BuildUpdateActionException(errorMessage));
                continue;
            }

            final String newProductVariantAttributeName = newProductVariantAttribute.getName();
            final Optional<Attribute> oldProductVariantAttributeOptional = oldProductVariant
                    .findAttribute(newProductVariantAttributeName);

            final Attribute oldProductVariantAttribute = oldProductVariantAttributeOptional.orElse(null);
            final AttributeMetaData attributeMetaData = attributesMetaData.get(newProductVariantAttributeName);

            try {
                final Optional<UpdateAction<Product>> variantAttributeUpdateActionOptional =
                    buildProductVariantAttributeUpdateAction(oldProductVariant.getId(), oldProductVariantAttribute,
                        newProductVariantAttribute, attributeMetaData);
                variantAttributeUpdateActionOptional.ifPresent(updateActions::add);
            } catch (@Nonnull final BuildUpdateActionException buildUpdateActionException) {
                final String errorMessage = format(FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                        newProductVariantAttributeName, newProductVariant.getKey(), productKey,
                        buildUpdateActionException.getMessage());
                syncOptions.applyErrorCallback(errorMessage, buildUpdateActionException);
            }
        }
        return updateActions;
    }

    /**
     * Compares the {@link List} of {@link io.sphere.sdk.products.Price}s of a {@link ProductVariantDraft} and a
     * {@link ProductVariant} and returns a {@link List} of {@link UpdateAction}&lt;{@link Product}&gt;. If both the
     * {@link ProductVariantDraft} and the {@link ProductVariant} have identical list of prices, then no update action
     * is needed and hence an empty {@link List} is returned.
     *
     * <p>TODO: NOTE: Right now it always builds SetPrices UpdateAction, comparison should be
     * TODO: calculated GITHUB ISSUE#101.
     *
     * @param oldProductVariant the {@link ProductVariant} which should be updated.
     * @param newProductVariant the {@link ProductVariantDraft} where we get the new list of prices.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are
     *         needed.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildProductVariantPricesUpdateActions(
        @Nonnull final ProductVariant oldProductVariant,
        @Nonnull final ProductVariantDraft newProductVariant) {
        //TODO: Right now it always builds SetPrices UpdateAction, comparison should be calculated GITHUB ISSUE#101.
        final List<PriceDraft> newProductVariantPrices =
            newProductVariant.getPrices() == null ? new ArrayList<>() : newProductVariant.getPrices();
        final SetPrices setPricesUpdateAction = SetPrices.of(oldProductVariant.getId(), newProductVariantPrices);
        return Collections.singletonList(setPricesUpdateAction);
    }

    /**
     * Compares the {@link List} of {@link Image}s of a {@link ProductVariantDraft} and a {@link ProductVariant} and
     * returns a {@link List} of {@link UpdateAction}&lt;{@link Product}&gt;. If both the {@link ProductVariantDraft}
     * and the {@link ProductVariant} have identical list of images, then no update action is needed and hence an
     * empty {@link List} is returned.
     *
     * @param oldProductVariant the {@link ProductVariant} which should be updated.
     * @param newProductVariant the {@link ProductVariantDraft} where we get the new list of images.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are
     *         needed.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildProductVariantImagesUpdateActions(
        @Nonnull final ProductVariant oldProductVariant,
        @Nonnull final ProductVariantDraft newProductVariant) {
        final List<UpdateAction<Product>> updateActions = new ArrayList<>();
        final Integer oldProductVariantId = oldProductVariant.getId();
        final List<Image> oldProductVariantImages = oldProductVariant.getImages();
        final List<Image> newProductVariantImages = newProductVariant.getImages();

        // This implementation is quite straight forward and might be slow on large arrays, this is
        // due to it's quadratic nature on images' removal/addition.
        // Unfortunately, currently there is no easy solution to sync 2 ordered lists
        // having only AddExternalImage/RemoveImage/MoveImageToPosition actions.
        // This solution should be re-optimized in the next releases to avoid O(N^2) for large lists.
        // TODO: GITHUB ISSUE#133

        if (!Objects.equals(oldProductVariantImages, newProductVariantImages)) {
            final List<Image> updatedOldImages = new ArrayList<>(oldProductVariantImages);
            final List<Image> newImages = emptyIfNull(newProductVariantImages);

            filterCollection(oldProductVariantImages, oldVariantImage ->
                    !newImages.contains(oldVariantImage))
                    .forEach(oldImage -> {
                        updateActions.add(RemoveImage.ofVariantId(oldProductVariantId, oldImage, true));
                        updatedOldImages.remove(oldImage);
                    });

            filterCollection(newProductVariantImages, newVariantImage ->
                    !oldProductVariantImages.contains(newVariantImage))
                    .forEach(newImage -> {
                        updateActions.add(AddExternalImage.ofVariantId(oldProductVariantId, newImage, true));
                        updatedOldImages.add(newImage);
                    });
            updateActions.addAll(buildMoveImageToPositionUpdateActions(oldProductVariantId,
                    updatedOldImages, newImages));
        }
        return updateActions;
    }

    /**
     * Compares an old {@link List} of {@link Image}s and a new one and returns a {@link List} of
     * {@link MoveImageToPosition} with the given {@code variantId}. If both the lists are identical, then no update
     * action is needed and hence an empty {@link List} is returned.
     *
     * <p>This method expects the two lists two contain the same images only in different order. Otherwise, an
     * {@link IllegalArgumentException} would be thrown.
     *
     * <p><b>Note</b>: the solution is still not optimized and may contain {@link MoveImageToPosition} actions
     * for items which are already on desired positions (after previous moves in the sequence). This will be
     * re-optimized in the next releases. TODO: GITHUB ISSUE#133
     *
     * @param variantId the variantId for the {@link MoveImageToPosition} update actions.
     * @param oldImages the old list of images.
     * @param newImages the new list of images.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are
     *         needed.
     * @throws IllegalArgumentException if arrays have different size or different items.
     */
    public static List<MoveImageToPosition> buildMoveImageToPositionUpdateActions(
            final int variantId,
            @Nonnull final List<Image> oldImages,
            @Nonnull final List<Image> newImages) throws IllegalArgumentException {
        final int oldImageListSize = oldImages.size();
        final int newImageListSize = newImages.size();
        if (oldImageListSize != newImageListSize) {
            throw new IllegalArgumentException(
                format("Old and new image lists must have the same size, but they have %d and %d respectively",
                    oldImageListSize, newImageListSize));
        }

        // optimization: to avoid multiple linear image index searching in the loop below - create an [image -> index]
        // map. This avoids quadratic order of growth of the implementation for large arrays.
        final Map<Image, Integer> imageIndexMap = new HashMap<>(oldImageListSize);
        int index = 0;
        for (Image newImage : newImages) {
            imageIndexMap.put(newImage, index++);
        }

        final List<MoveImageToPosition> updateActions = new ArrayList<>();

        for (int oldIndex = 0; oldIndex < oldImageListSize; oldIndex++) {
            final Image oldImage = oldImages.get(oldIndex);
            final Integer newIndex = ofNullable(imageIndexMap.get(oldImage)) // constant-time operation
                .orElseThrow(() ->
                    new IllegalArgumentException(format("Old image [%s] not found in the new images list.", oldImage)));

            if (oldIndex != newIndex) {
                updateActions.add(
                    MoveImageToPosition.ofImageUrlAndVariantId(oldImage.getUrl(), variantId, newIndex, true));
            }
        }
        return updateActions;
    }

    /**
     * Compares the SKUs of a {@link ProductVariantDraft} and a {@link ProductVariant}. It returns a {@link SetSku}
     * update action as a result in an {@link Optional}. If both the {@link ProductVariantDraft}
     * and the {@link ProductVariant} have identical identical SKUs, then no update action is needed and hence an
     * empty {@link Optional} is returned.
     *
     * @param oldProductVariant the variant which should be updated.
     * @param newProductVariant the variant draft where we get the new SKU.
     * @return A filled optional with the update action or an empty optional if the SKUs are identical.
     */
    @Nonnull
    public static Optional<SetSku> buildProductVariantSkuUpdateAction(
        @Nonnull final ProductVariant oldProductVariant,
        @Nonnull final ProductVariantDraft newProductVariant) {
        final String oldProductVariantSku = oldProductVariant.getSku();
        final String newProductVariantSku = newProductVariant.getSku();
        return buildUpdateAction(oldProductVariantSku, newProductVariantSku,
            () -> SetSku.of(oldProductVariant.getId(), newProductVariantSku, true));
    }

    private ProductVariantUpdateActionUtils() {
    }
}
