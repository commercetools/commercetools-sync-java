package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.PriceUtils.createPriceTierDraft;
import static java.lang.String.format;

import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductChangePriceAction;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.helpers.PriceCustomActionBuilder;
import com.commercetools.sync.sdk2.products.models.PriceCustomTypeAdapter;
import com.commercetools.sync.sdk2.products.models.PriceDraftCustomTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.money.MonetaryAmount;

public final class ProductVariantPriceUpdateActionUtils {
  private static final String VARIANT_CHANGE_PRICE_EMPTY_VALUE =
      "Cannot unset 'value' field of price with id" + " '%s'.";

  /**
   * Compares all the fields of a {@link Price} and a {@link PriceDraft} and returns a list of
   *
   * <p>{@link ProductUpdateAction}&lt;{@link Product}&gt; as a result. If both the {@link Price}
   * and the {@link PriceDraft} have identical fields, then no update action is needed and hence an
   * empty {@link List} is returned.
   *
   * @param newProduct new product draft, which provides the prices to update.
   * @param variantId the variantId needed for building the update action.
   * @param oldPrice the price which should be updated.
   * @param newPrice the price draft where we get the new fields.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the update actions or an empty list if the price fields are identical.
   */
  @Nonnull
  public static List<ProductUpdateAction> buildActions(
      @Nonnull final ProductDraft newProduct,
      @Nonnull final Long variantId,
      @Nonnull final Price oldPrice,
      @Nonnull final PriceDraft newPrice,
      @Nonnull final ProductSyncOptions syncOptions) {

    final List<ProductUpdateAction> updateActions = new ArrayList<>();
    buildChangePriceUpdateAction(oldPrice, newPrice, syncOptions).ifPresent(updateActions::add);
    updateActions.addAll(
        buildCustomUpdateActions(newProduct, variantId, oldPrice, newPrice, syncOptions));

    return updateActions;
  }

  /**
   * Builds a {@link ProductChangePriceAction} action based on the comparison of the following
   * fields of the supplied {@link Price} and {@link PriceDraft}:
   *
   * <ul>
   *   <li>{@link Price#getValue()} and {@link PriceDraft#getValue()}
   *   <li>{@link Price#getTiers()} and {@link PriceDraft#getTiers()}
   * </ul>
   *
   * <p>If any of the aforementioned fields are different a {@link ProductChangePriceAction} update
   * action will be returned in an {@link Optional}, otherwise if both are identical in the {@link
   * Price} and the {@link PriceDraft}, then no update action is needed and hence an empty {@link
   * Optional} is returned.
   *
   * @param oldPrice the price which should be updated.
   * @param newPrice the price draft where we get the new name.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A filled optional with the update action or an empty optional if the names are
   *     identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildChangePriceUpdateAction(
      @Nonnull final Price oldPrice,
      @Nonnull final PriceDraft newPrice,
      @Nonnull final ProductSyncOptions syncOptions) {

    final MonetaryAmount oldPriceValue = oldPrice.getValue();
    final MonetaryAmount newPriceValue = newPrice.getValue();

    if (newPriceValue == null) {
      syncOptions.applyWarningCallback(
          new SyncException(format(VARIANT_CHANGE_PRICE_EMPTY_VALUE, oldPrice.getId())),
          null,
          null);
      return Optional.empty();
    }

    final Optional<ProductUpdateAction> actionAfterValuesDiff =
        buildUpdateAction(
            oldPriceValue,
            newPriceValue,
            () ->
                ProductChangePriceAction.builder()
                    .priceId(oldPrice.getId())
                    .price(newPrice)
                    .staged(true)
                    .build());

    return actionAfterValuesDiff
        .map(Optional::of)
        .orElseGet(
            () ->
                // If values are not different, compare tiers.
                buildUpdateAction(
                    createPriceTierDraft(oldPrice.getTiers()),
                    newPrice.getTiers(),
                    () ->
                        ProductChangePriceAction.builder()
                            .priceId(oldPrice.getId())
                            .price(newPrice)
                            .staged(true)
                            .build()));
  }

  /**
   * Compares the custom fields and custom types of a {@link Price} and a {@link PriceDraft} and
   *
   * <p>returns a list of {@link ProductUpdateAction}&lt;{@link Product}&gt; as a result. If both
   * the {@link Price} and the {@link PriceDraft} have identical custom fields and types, then no
   * update action is needed and hence an empty {@link List} is returned.
   *
   * @param newProduct new product draft, which provides the prices to update.
   * @param variantId the variantId needed for building the update action.
   * @param oldPrice the price which should be updated.
   * @param newPrice the price draft where we get the new custom fields and types.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the custom field/type update actions or an empty list if the custom
   *     fields/types are identical.
   */
  @Nonnull
  public static List<ProductUpdateAction> buildCustomUpdateActions(
      @Nonnull final ProductDraft newProduct,
      @Nonnull final Long variantId,
      @Nonnull final Price oldPrice,
      @Nonnull final PriceDraft newPrice,
      @Nonnull final ProductSyncOptions syncOptions) {

    PriceCustomTypeAdapter priceAdapter = PriceCustomTypeAdapter.of(oldPrice);
    PriceDraftCustomTypeAdapter priceDraftAdapter = PriceDraftCustomTypeAdapter.of(newPrice);

    List<ProductUpdateAction> customUpdateAction =
        CustomUpdateActionUtils.buildCustomUpdateActions(
            newProduct,
            priceAdapter,
            priceDraftAdapter,
            new PriceCustomActionBuilder(),
            variantId,
            PriceCustomTypeAdapter::getId,
            priceCustomTypeAdapter ->
                ResourceTypeId.PRODUCT_PRICE.getJsonName(), // return resource ID "product-price"
            PriceCustomTypeAdapter::getId,
            syncOptions);

    List<ProductUpdateAction> productUpdateActions = new ArrayList<ProductUpdateAction>();
    customUpdateAction.forEach(
        resourceUpdateAction ->
            productUpdateActions.add((ProductUpdateAction) resourceUpdateAction));
    return productUpdateActions;
  }

  private ProductVariantPriceUpdateActionUtils() {}
}
