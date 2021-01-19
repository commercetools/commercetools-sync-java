package com.commercetools.sync.products.utils;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.helpers.PriceCustomActionBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangePrice;
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
   * {@link UpdateAction}&lt;{@link Product}&gt; as a result. If both the {@link Price} and the
   * {@link PriceDraft} have identical fields, then no update action is needed and hence an empty
   * {@link List} is returned.
   *
   * @param oldProduct old Product, whose prices should be updated.
   * @param newProduct new product draft, which provides the prices to update.
   * @param variantId the variantId needed for building the update action.
   * @param oldPrice the price which should be updated.
   * @param newPrice the price draft where we get the new fields.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the update actions or an empty list if the price fields are identical.
   */
  @Nonnull
  public static List<UpdateAction<Product>> buildActions(
      @Nonnull final Product oldProduct,
      @Nonnull final ProductDraft newProduct,
      @Nonnull final Integer variantId,
      @Nonnull final Price oldPrice,
      @Nonnull final PriceDraft newPrice,
      @Nonnull final ProductSyncOptions syncOptions) {

    final List<UpdateAction<Product>> updateActions = new ArrayList<>();

    buildChangePriceUpdateAction(oldPrice, newPrice, syncOptions).ifPresent(updateActions::add);
    updateActions.addAll(
        buildCustomUpdateActions(
            oldProduct, newProduct, variantId, oldPrice, newPrice, syncOptions));

    return updateActions;
  }

  /**
   * Builds a {@link ChangePrice} action based on the comparison of the following fields of the
   * supplied {@link Price} and {@link PriceDraft}:
   *
   * <ul>
   *   <li>{@link Price#getValue()} and {@link PriceDraft#getValue()}
   *   <li>{@link Price#getTiers()} and {@link PriceDraft#getTiers()}
   * </ul>
   *
   * <p>If any of the aforementioned fields are different a {@link ChangePrice} update action will
   * be returned in an {@link Optional}, otherwise if both are identical in the {@link Price} and
   * the {@link PriceDraft}, then no update action is needed and hence an empty {@link Optional} is
   * returned.
   *
   * @param oldPrice the price which should be updated.
   * @param newPrice the price draft where we get the new name.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A filled optional with the update action or an empty optional if the names are
   *     identical.
   */
  @Nonnull
  public static Optional<ChangePrice> buildChangePriceUpdateAction(
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

    final Optional<ChangePrice> actionAfterValuesDiff =
        buildUpdateAction(
            oldPriceValue, newPriceValue, () -> ChangePrice.of(oldPrice, newPrice, true));

    return actionAfterValuesDiff
        .map(Optional::of)
        .orElseGet(
            () ->
                // If values are not different, compare tiers.
                buildUpdateAction(
                    oldPrice.getTiers(),
                    newPrice.getTiers(),
                    () -> ChangePrice.of(oldPrice, newPrice, true)));
  }

  /**
   * Compares the custom fields and custom types of a {@link Price} and a {@link PriceDraft} and
   * returns a list of {@link UpdateAction}&lt;{@link Product}&gt; as a result. If both the {@link
   * Price} and the {@link PriceDraft} have identical custom fields and types, then no update action
   * is needed and hence an empty {@link List} is returned.
   *
   * @param oldProduct old Product, whose prices should be updated.
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
  public static List<UpdateAction<Product>> buildCustomUpdateActions(
      @Nonnull final Product oldProduct,
      @Nonnull final ProductDraft newProduct,
      @Nonnull final Integer variantId,
      @Nonnull final Price oldPrice,
      @Nonnull final PriceDraft newPrice,
      @Nonnull final ProductSyncOptions syncOptions) {

    return CustomUpdateActionUtils.buildCustomUpdateActions(
        oldProduct,
        newProduct,
        oldPrice,
        newPrice,
        new PriceCustomActionBuilder(),
        variantId,
        Price::getId,
        price -> Price.resourceTypeId(),
        Price::getId,
        syncOptions);
  }

  private ProductVariantPriceUpdateActionUtils() {}
}
