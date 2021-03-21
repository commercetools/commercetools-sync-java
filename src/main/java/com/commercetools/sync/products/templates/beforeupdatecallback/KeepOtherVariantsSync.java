package com.commercetools.sync.products.templates.beforeupdatecallback;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class KeepOtherVariantsSync {
  /**
   * Takes product update actions, a new {@link ProductDraft}, an old existing {@link Product}. This
   * method filters out the update action if it is a {@link RemoveVariant} update action.
   *
   * <p>Using this method as a BeforeUpdateCallback would prevent the removal of not existing
   * variants in the target product.
   *
   * @param updateActions the update action built from comparing {@code newProductDraft} and {@code
   *     oldProduct}.
   * @return the same list of supplied {@code updateActions} without {@link RemoveVariant} update
   *     actions.
   */
  public static List<UpdateAction<Product>> keepOtherVariants(
      @Nonnull final List<UpdateAction<Product>> updateActions) {
    return updateActions.stream()
        .filter(updateAction -> !(updateAction instanceof RemoveVariant))
        .collect(Collectors.toList());
  }
}
