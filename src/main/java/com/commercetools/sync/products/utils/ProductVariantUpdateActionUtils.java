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

import static com.commercetools.sync.commons.utils.CollectionUtils.filterCollection;
import static com.commercetools.sync.products.utils.ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

// TODO: Add JAVADOC AND TESTS
public final class ProductVariantUpdateActionUtils {
    private static final String FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION = "Failed to build a "
        + "setAttribute/setAttributeInAllVariants update action for the attribute with the name '%s' in the "
        + "ProductVariantDraft with key '%s' on the product with key '%s'. Reason: %s";
    private static final String NULL_PRODUCT_VARIANT_ATTRIBUTE = "AttributeDraft is null.";

    /**
     * Compares the attributes of a {@link ProductVariantDraft} and a {@link ProductVariant} to build either
     * {@link io.sphere.sdk.products.commands.updateactions.SetAttribute} or
     * {@link io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants} update actions.
     * TODO: Add JavaDoc
     *
     * @param productKey         TODO
     * @param oldProductVariant  TODO
     * @param newProductVariant  TODO
     * @param attributesMetaData TODO
     * @param syncOptions        TODO
     * @return TODO
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
     * Compares the prices of a {@link ProductVariantDraft} and a {@link ProductVariant}.
     * TODO: Add JavaDoc
     *
     * @param oldProductVariant TODO
     * @param newProductVariant TODO
     * @return TODO
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

        // this implementation is quite straight forward and might be slow on large arrays,
        // because of quadratic algorithms of remove/add images synchronization.
        // Unfortunately, there is not easy solution to synchronize 2 ordered lists
        // having only add/remove/moveToPos actions.
        // This solution should be re-optimized in the next releases to avoid O(N^2) for large lists.

        if (!Objects.equals(oldProductVariantImages, newProductVariantImages)) {
            final List<Image> oldImages = oldProductVariantImages != null
                    ? oldProductVariantImages : Collections.emptyList();

            final List<Image> updatedOldImages = new ArrayList<>(oldImages);

            final List<Image> newImages = newProductVariantImages != null
                    ? newProductVariantImages : Collections.emptyList();

            filterCollection(oldProductVariantImages, oldVariantImage ->
                    !newImages.contains(oldVariantImage))
                    .forEach(oldImage -> {
                        updateActions.add(RemoveImage.ofVariantId(oldProductVariantId, oldImage, true));
                        updatedOldImages.remove(oldImage);
                    });

            filterCollection(newProductVariantImages, newVariantImage ->
                    !oldImages.contains(newVariantImage))
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
     * {@link MoveImageToPosition}&lt;{@link Product}&gt; with the given {@code variantId}. If both the lists are
     * identical, then no update action is needed and hence an empty {@link List} is returned.
     *
     * <p>This method expects the two lists two contain the same images only in different order. Therefore, be cautios
     * that supplying lists of different (missing/extra) images could results in an index out of bounds exception on the
     * new position of an image.
     *
     * <p><b>Note</b>: the solution is still not optimized and may contain {@link MoveImageToPosition} actions
     * for items which are already on desired positions (after previous moves in the sequence). This will be
     * re-optimized in the next releases.
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

        if (oldImages.size() != newImages.size()) {
            throw new IllegalArgumentException(
                format("Old and new image lists must have the same size, but they have %d and %d respectively",
                    oldImages.size(), newImages.size()));
        }

        final int SIZE = oldImages.size();

        // optimization: to avoid multiple linear image index searching in the loop below - create an [image -> index]
        // map. This avoids quadratic order of growth of the implementation for large arrays.
        final Map<Image, Integer> imageIndexMap = new HashMap<>(SIZE);
        int index = 0;
        for (Image newImage : newImages) {
            imageIndexMap.put(newImage, index++);
        }

        final List<MoveImageToPosition> updateActions = new ArrayList<>();

        for (int oldIndex = 0; oldIndex < SIZE; oldIndex++) {
            final Image oldImage = oldImages.get(oldIndex);

            final Integer newIndex = ofNullable(imageIndexMap.get(oldImage)) // constant-time operation
                .orElseThrow(() ->
                    new IllegalArgumentException(format("Old image [%s] not found in the new images list", oldImage)));

            if (oldIndex != newIndex) {
                updateActions.add(
                    MoveImageToPosition.ofImageUrlAndVariantId(oldImage.getUrl(), variantId, newIndex, true));
            }
        }
        return updateActions;
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
