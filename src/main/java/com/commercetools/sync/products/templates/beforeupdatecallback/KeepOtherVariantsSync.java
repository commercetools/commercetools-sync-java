package com.commercetools.sync.products.templates.beforeupdatecallback;

import com.commercetools.api.models.product.ProductRemoveVariantAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class KeepOtherVariantsSync {
  /**
   * Takes a list of {@link ProductUpdateAction}'s. This method filters out the update action if it
   * is a {@link ProductRemoveVariantAction} update action.
   *
   * <p>Using this method as a BeforeUpdateCallback would prevent the removal of not existing
   * variants in the target product.
   *
   * @param updateActions the update action built from comparing {@code newProductDraft} and {@code
   *     oldProduct}.
   * @return the same list of supplied {@code updateActions} without {@link
   *     ProductRemoveVariantAction} update actions.
   */
  public static List<ProductUpdateAction> keepOtherVariants(
      @Nonnull final List<ProductUpdateAction> updateActions) {
    return updateActions.stream()
        .filter(updateAction -> !(updateAction instanceof ProductRemoveVariantAction))
        .collect(Collectors.toList());
  }
}
