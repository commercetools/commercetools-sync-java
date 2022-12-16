package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.commons.utils.CollectionUtils.*;
import static com.commercetools.sync.sdk2.commons.utils.AssetsUpdateActionUtils.buildAssetsUpdateActions;
import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.internals.utils.UnorderedCollectionSyncUtils.buildRemoveUpdateActions;
import static com.commercetools.sync.sdk2.internals.utils.UpdateActionsSortUtils.sortPriceActions;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantPriceUpdateActionUtils.buildActions;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.Image;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductAddExternalImageAction;
import com.commercetools.api.models.product.ProductAddPriceAction;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductMoveImageToPositionAction;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductRemoveImageAction;
import com.commercetools.api.models.product.ProductRemovePriceAction;
import com.commercetools.api.models.product.ProductSetAttributeAction;
import com.commercetools.api.models.product.ProductSetAttributeInAllVariantsAction;
import com.commercetools.api.models.product.ProductSetSkuAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.products.AttributeMetaData;
import com.commercetools.sync.sdk2.internals.helpers.PriceCompositeId;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.helpers.ProductAssetActionFactory;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ProductVariantUpdateActionUtils {
  public static final String FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION =
      "Failed to build a "
          + "setAttribute/setAttributeInAllVariants update action for the attribute with the name '%s' in the "
          + "ProductVariantDraft with key '%s' on the product with key '%s'. Reason: %s";
  public static final String NULL_PRODUCT_VARIANT_ATTRIBUTE = "AttributeDraft is null.";
  private static final String NULL_PRODUCT_VARIANT_PRICE = "New price is null.";

  /**
   * Compares the SKUs of a {@link ProductVariantDraft} and a {@link ProductVariant}. It returns a

   * {@link ProductSetSkuAction} update action as a result in an {@link Optional}. If both the
   * {@link ProductVariantDraft} and the {@link ProductVariant} have identical identical SKUs, then
   * no update action is needed and hence an empty {@link Optional} is returned.

   *
   * @param oldProductVariant the variant which should be updated.
   * @param newProductVariant the variant draft where we get the new SKU.
   * @return A filled optional with the update action or an empty optional if the SKUs are
   *     identical.
   */
  @Nonnull

  public static Optional<ProductSetSkuAction> buildProductVariantSkuUpdateAction(

      @Nonnull final ProductVariant oldProductVariant,
      @Nonnull final ProductVariantDraft newProductVariant) {
    final String oldProductVariantSku = oldProductVariant.getSku();
    final String newProductVariantSku = newProductVariant.getSku();
    return buildUpdateAction(
        oldProductVariantSku,
        newProductVariantSku,

        () ->
            ProductSetSkuAction.builder()
                .variantId(oldProductVariant.getId())
                .sku(newProductVariantSku)
                .staged(true)
                .build());

  }

  /**
   * Compares the {@link List} of {@link Price}s of a {@link ProductVariantDraft} and a {@link

   * ProductVariant} and returns a {@link List} of {@link ProductUpdateAction}&lt;{@link
   * Product}&gt;. If both the {@link ProductVariantDraft} and the {@link ProductVariant} have
   * identical list of prices, then no update action is needed and hence an empty {@link List} is
   * returned.

   *
   * @param oldProduct the product which should be updated.
   * @param newProduct the product draft.
   * @param oldProductVariant the {@link ProductVariant} which should be updated.
   * @param newProductVariant the {@link ProductVariantDraft} where we get the new list of prices.
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   *     supplied by the user. For example, custom callbacks to call in case of warnings or errors
   *     occurring on the build update action process. And other options (See {@link
   *     ProductSyncOptions} for more info).
   * @return a list that contains all the update actions needed, otherwise an empty list if no
   *     update actions are needed.
   */
  @Nonnull

  public static List<ProductUpdateAction> buildProductVariantPricesUpdateActions(

      @Nullable final ProductProjection oldProduct,
      @Nonnull final ProductDraft newProduct,
      @Nonnull final ProductVariant oldProductVariant,
      @Nonnull final ProductVariantDraft newProductVariant,
      @Nonnull final ProductSyncOptions syncOptions) {

    final List<Price> oldPrices = oldProductVariant.getPrices();
    final List<PriceDraft> newPrices = newProductVariant.getPrices();


    final List<ProductUpdateAction> updateActions =

        buildRemoveUpdateActions(
            oldPrices,
            newPrices,
            PriceCompositeId::of,
            PriceCompositeId::of,

            price ->
                ProductRemovePriceAction.builder().priceId(price.getId()).staged(true).build());

    final Long variantId = oldProductVariant.getId();

    final Map<PriceCompositeId, Price> oldPricesMap =
        collectionToMap(oldPrices, PriceCompositeId::of);

    emptyIfNull(newPrices)
        .forEach(
            newPrice -> {
              if (newPrice == null) {
                syncOptions.applyErrorCallback(
                    new SyncException(
                        format(
                            "Failed to build prices update actions "
                                + "for one price on the variant with id '%d' and key '%s'. Reason: %s",
                            variantId, oldProductVariant.getKey(), NULL_PRODUCT_VARIANT_PRICE)),
                    oldProduct,
                    newProduct,
                    null);
              } else {
                final PriceCompositeId newPriceCompositeId = PriceCompositeId.of(newPrice);
                final Price matchingOldPrice = oldPricesMap.get(newPriceCompositeId);

                final List<ProductUpdateAction> updateOrAddPrice =

                    ofNullable(matchingOldPrice)
                        .map(
                            oldPrice ->
                                buildActions(
                                    newProduct, variantId, oldPrice, newPrice, syncOptions))
                        .orElseGet(

                            () ->
                                singletonList(
                                    ProductAddPriceAction.builder()
                                        .variantId(variantId)
                                        .price(newPrice)
                                        .staged(true)
                                        .build()));

                updateActions.addAll(updateOrAddPrice);
              }
            });

    return sortPriceActions(updateActions);
  }

  /**
   * Compares the {@link List} of {@link Image}s of a {@link ProductVariantDraft} and a {@link

   * ProductVariant} and returns a {@link List} of {@link ProductUpdateAction}&lt;{@link
   * Product}&gt;. If both the {@link ProductVariantDraft} and the {@link ProductVariant} have
   * identical list of images, then no update action is needed and hence an empty {@link List} is
   * returned.

   *
   * @param oldProductVariant the {@link ProductVariant} which should be updated.
   * @param newProductVariant the {@link ProductVariantDraft} where we get the new list of images.
   * @return a list that contains all the update actions needed, otherwise an empty list if no
   *     update actions are needed.
   */
  @Nonnull

  public static List<ProductUpdateAction> buildProductVariantImagesUpdateActions(
      @Nonnull final ProductVariant oldProductVariant,
      @Nonnull final ProductVariantDraft newProductVariant) {
    final List<ProductUpdateAction> updateActions = new ArrayList<>();
    final Long oldProductVariantId = oldProductVariant.getId();

    final List<Image> oldProductVariantImages = oldProductVariant.getImages();
    final List<Image> newProductVariantImages = newProductVariant.getImages();

    // This implementation is quite straight forward and might be slow on large arrays, this is
    // due to it's quadratic nature on images' removal/addition.
    // Unfortunately, currently there is no easy solution to sync 2 ordered lists
    // having only AddExternalImage/RemoveImage/MoveImageToPosition actions.
    // This solution should be re-optimized in the next releases to avoid O(N^2) for large lists.

    if (!Objects.equals(oldProductVariantImages, newProductVariantImages)) {
      final List<Image> updatedOldImages = new ArrayList<>(oldProductVariantImages);
      final List<Image> newImages = emptyIfNull(newProductVariantImages);

      filterCollection(
              oldProductVariantImages, oldVariantImage -> !newImages.contains(oldVariantImage))
          .forEach(
              oldImage -> {

                updateActions.add(
                    ProductRemoveImageAction.builder()
                        .variantId(oldProductVariantId)
                        .imageUrl(oldImage.getUrl())
                        .staged(true)
                        .build());

                updatedOldImages.remove(oldImage);
              });

      filterCollection(
              newProductVariantImages,
              newVariantImage -> !oldProductVariantImages.contains(newVariantImage))
          .forEach(
              newImage -> {
                updateActions.add(

                    ProductAddExternalImageAction.builder()
                        .variantId(oldProductVariantId)
                        .image(newImage)
                        .staged(true)
                        .build());

                updatedOldImages.add(newImage);
              });
      updateActions.addAll(
          buildMoveImageToPositionUpdateActions(oldProductVariantId, updatedOldImages, newImages));
    }
    return updateActions;
  }

  /**
   * Compares an old {@link List} of {@link Image}s and a new one and returns a {@link List} of

   * {@link ProductMoveImageToPositionAction} with the given {@code variantId}. If both the lists
   * are identical, then no update action is needed and hence an empty {@link List} is returned.

   *
   * <p>This method expects the two lists two contain the same images only in different order.
   * Otherwise, an {@link IllegalArgumentException} would be thrown.
   *

   * <p><b>Note</b>: the solution is still not optimized and may contain {@link
   * ProductMoveImageToPositionAction} actions for items which are already on desired positions
   * (after previous moves in the sequence). This will be re-optimized in the next releases.
   *
   * @param variantId the variantId for the {@link ProductMoveImageToPositionAction} update actions.

   * @param oldImages the old list of images.
   * @param newImages the new list of images.
   * @return a list that contains all the update actions needed, otherwise an empty list if no
   *     update actions are needed.
   */

  public static List<ProductMoveImageToPositionAction> buildMoveImageToPositionUpdateActions(
      final long variantId,

      @Nonnull final List<Image> oldImages,
      @Nonnull final List<Image> newImages) {
    final int oldImageListSize = oldImages.size();
    final int newImageListSize = newImages.size();
    if (oldImageListSize != newImageListSize) {
      throw new IllegalArgumentException(
          format(
              "Old and new image lists must have the same size, but they have %d and %d respectively",
              oldImageListSize, newImageListSize));
    }

    // optimization: to avoid multiple linear image index searching in the loop below - create an
    // [image -> index]
    // map. This avoids quadratic order of growth of the implementation for large arrays.

    final Map<Image, Long> imageIndexMap = new HashMap<>(oldImageListSize);
    long index = 0;

    for (Image newImage : newImages) {
      imageIndexMap.put(newImage, index++);
    }


    final List<ProductMoveImageToPositionAction> updateActions = new ArrayList<>();

    for (int oldIndex = 0; oldIndex < oldImageListSize; oldIndex++) {
      final Image oldImage = oldImages.get(oldIndex);
      final Long newIndex =

          ofNullable(imageIndexMap.get(oldImage))
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          format("Old image [%s] not found in the new images list.", oldImage)));

      if (oldIndex != newIndex) {
        updateActions.add(

            ProductMoveImageToPositionAction.builder()
                .imageUrl(oldImage.getUrl())
                .variantId(variantId)
                .position(newIndex)
                .staged(true)
                .build());

      }
    }
    return updateActions;
  }

  /**
   * Compares the {@link List} of {@link AssetDraft}s of a {@link ProductVariantDraft} and a {@link

   * ProductVariant} and returns a {@link List} of {@link ProductUpdateAction}&lt;{@link
   * Product}&gt;. If both the {@link ProductVariantDraft} and the {@link ProductVariant} have
   * identical list of assets, then no update action is needed and hence an empty {@link List} is
   * returned. In case, the new product variant draft has a list of assets in which a duplicate key
   * exists, the error callback is triggered and an empty list is returned.

   *
   * @param oldProduct old Product, whose variant assets should be updated.
   * @param newProduct new product draft, which provides the assets to update.
   * @param oldProductVariant the {@link ProductVariant} which should be updated.
   * @param newProductVariant the {@link ProductVariantDraft} where we get the new list of assets.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return a list that contains all the update actions needed, otherwise an empty list if no
   *     update actions are needed.
   */
  @Nonnull

  public static List<ProductUpdateAction> buildProductVariantAssetsUpdateActions(

      @Nonnull final ProductProjection oldProduct,
      @Nonnull final ProductDraft newProduct,
      @Nonnull final ProductVariant oldProductVariant,
      @Nonnull final ProductVariantDraft newProductVariant,
      @Nonnull final ProductSyncOptions syncOptions) {

    try {
      return buildAssetsUpdateActions(
          newProduct,
          oldProductVariant.getAssets(),
          newProductVariant.getAssets(),
          new ProductAssetActionFactory(oldProductVariant.getId(), syncOptions), // AssetActionFactory : T = ProductUpdateAction, D = ProductDraft
          syncOptions);
    } catch (final BuildUpdateActionException exception) {
      SyncException syncException =
          new SyncException(
              format(
                  "Failed to build update actions for the assets "
                      + "of the product variant with the sku '%s'. Reason: %s",
                  oldProductVariant.getSku(), exception),
              exception);
      syncOptions.applyErrorCallback(syncException, oldProduct, newProduct, null);
      return emptyList();
    }
  }

  /**
   * Compares the attributes of a {@link ProductVariantDraft} and a {@link ProductVariant} to build

   * either {@link ProductSetAttributeAction} or {@link ProductSetAttributeInAllVariantsAction}
   * update actions. If both the {@link ProductVariantDraft} and the {@link ProductVariant} have
   * identical list of attributes, then no update action is needed and hence an empty {@link List}
   * is returned.

   *
   * @param oldProduct the product that the variants belong to. It is used only in the error
   *     messages if any.
   * @param newProduct the new product draft.
   * @param oldProductVariant the {@link ProductVariant} which should be updated.
   * @param newProductVariant the {@link ProductVariantDraft} where we get the new list of
   *     attributes.
   * @param attributesMetaData a map of attribute name -&gt; {@link AttributeMetaData}; which
   *     defines attribute information: its name, whether a value is required or not and whether it
   *     has the constraint "SameForAll" or not.
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   *     supplied by the user. For example, custom callbacks to call in case of warnings or errors
   *     occurring on the build update action process. And other options (See {@link
   *     ProductSyncOptions} for more info).
   * @return a list that contains all the update actions needed, otherwise an empty list if no
   *     update actions are needed.
   */
  @Nonnull

  public static List<ProductUpdateAction> buildProductVariantAttributesUpdateActions(

      @Nonnull final ProductProjection oldProduct,
      @Nonnull final ProductDraft newProduct,
      @Nonnull final ProductVariant oldProductVariant,
      @Nonnull final ProductVariantDraft newProductVariant,
      @Nonnull final Map<String, AttributeMetaData> attributesMetaData,
      @Nonnull final ProductSyncOptions syncOptions) {

    final String productKey = oldProduct.getKey();


    final long oldProductVariantId = oldProductVariant.getId();
    final List<Attribute> newProductVariantAttributes = newProductVariant.getAttributes();
    final List<Attribute> oldProductVariantAttributes = oldProductVariant.getAttributes();

    final List<ProductUpdateAction> updateActions =

        buildRemoveUpdateActions(
            oldProductVariantAttributes,
            newProductVariantAttributes,
            Attribute::getName,

            Attribute::getName,

            attribute -> {
              try {
                return buildUnSetAttribute(
                    oldProductVariantId, attribute.getName(), attributesMetaData);
              } catch (final BuildUpdateActionException buildUpdateActionException) {
                final String errorMessage =
                    format(
                        FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                        attribute.getName(),
                        newProductVariant.getKey(),
                        productKey,
                        buildUpdateActionException.getMessage());
                syncOptions.applyErrorCallback(
                    new SyncException(
                        errorMessage,
                        new BuildUpdateActionException(errorMessage, buildUpdateActionException)),
                    oldProduct,
                    newProduct,
                    null);
                return null;
              }
            });

    final Map<String, Attribute> oldAttributesMap =
        collectionToMap(oldProductVariantAttributes, Attribute::getName);

    emptyIfNull(newProductVariantAttributes)
        .forEach(
            newAttribute -> {
              if (newAttribute == null) {
                final String errorMessage =
                    format(
                        FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                        null,
                        newProductVariant.getKey(),
                        productKey,
                        NULL_PRODUCT_VARIANT_ATTRIBUTE);
                syncOptions.applyErrorCallback(
                    new SyncException(errorMessage, new BuildUpdateActionException(errorMessage)),
                    oldProduct,
                    newProduct,
                    updateActions);
              } else {
                final String newAttributeName = newAttribute.getName();
                final Attribute matchingOldAttribute = oldAttributesMap.get(newAttributeName);

                try {

                  buildProductVariantAttributeUpdateAction(
                          oldProductVariantId,
                          matchingOldAttribute,
                          newAttribute,
                          attributesMetaData)
                      .ifPresent(updateActions::add);

                } catch (final BuildUpdateActionException buildUpdateActionException) {
                  final String errorMessage =
                      format(
                          FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                          newAttributeName,
                          newProductVariant.getKey(),
                          productKey,
                          buildUpdateActionException.getMessage());
                  syncOptions.applyErrorCallback(
                      new SyncException(
                          errorMessage,
                          new BuildUpdateActionException(
                              errorMessage, buildUpdateActionException)));
                }
              }
            });

    return updateActions;
  }


  private static ProductUpdateAction buildUnSetAttribute(
      @Nonnull final long variantId,

      @Nonnull final String attributeName,
      @Nonnull final Map<String, AttributeMetaData> attributesMetaData)
      throws BuildUpdateActionException {

    final AttributeMetaData attributeMetaData = attributesMetaData.get(attributeName);

    if (attributeMetaData == null) {
      final String errorMessage = format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, attributeName);
      throw new BuildUpdateActionException(errorMessage);
    }

    return attributeMetaData.isSameForAll()

        ? ProductSetAttributeInAllVariantsAction.builder()
            .name(attributeName)
            .value(null)
            .staged(true)
            .build()
        : ProductSetAttributeAction.builder()
            .variantId(variantId)
            .name(attributeName)
            .value(null)
            .staged(true)
            .build();

  }

  private ProductVariantUpdateActionUtils() {}
}
