package com.commercetools.sync.products.utils;

import com.commercetools.api.models.product.ProductAddPriceAction;
import com.commercetools.api.models.product.ProductChangePriceAction;
import com.commercetools.api.models.product.ProductRemovePriceAction;
import com.commercetools.api.models.product.ProductSetProductPriceCustomFieldAction;
import com.commercetools.api.models.product.ProductSetProductPriceCustomTypeAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** This class is only meant for the internal use of the commercetools-sync-java library. */
public final class UpdateActionsSortUtils {
  /**
   * Given a list of update actions, this method returns a copy of the supplied list but sorted with
   * the following precedence:
   *
   * <ol>
   *   <li>{@link ProductRemovePriceAction}
   *   <li>{@link ProductChangePriceAction} or {@link ProductSetProductPriceCustomTypeAction} or
   *       {@link ProductSetProductPriceCustomFieldAction}
   *   <li>{@link ProductAddPriceAction}
   * </ol>
   *
   * <p>This is to ensure that there are no conflicts when adding a new price that might have a
   * duplicate value for a unique field, which could already be changed or removed.
   *
   * @param updateActions list of update actions to sort.
   * @return a new sorted list of update actions (remove, change, add).
   */
  @Nonnull
  public static List<ProductUpdateAction> sortPriceActions(
      @Nonnull final List<ProductUpdateAction> updateActions) {

    final List<ProductUpdateAction> actionsCopy = new ArrayList<>(updateActions);
    actionsCopy.sort(
        (action1, action2) -> {
          if (action1 instanceof ProductRemovePriceAction
              && !(action2 instanceof ProductRemovePriceAction)) {
            return -1;
          }

          if (!(action1 instanceof ProductRemovePriceAction)
              && action2 instanceof ProductRemovePriceAction) {
            return 1;
          }

          if (!(action1 instanceof ProductAddPriceAction)
              && action2 instanceof ProductAddPriceAction) {
            return -1;
          }

          if (action1 instanceof ProductAddPriceAction
              && !(action2 instanceof ProductAddPriceAction)) {

            return 1;
          }

          return 0;
        });
    return actionsCopy;
  }

  private UpdateActionsSortUtils() {}
}
