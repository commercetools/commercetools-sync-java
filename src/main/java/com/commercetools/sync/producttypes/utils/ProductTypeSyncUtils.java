package com.commercetools.sync.producttypes.utils;

import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateActionUtils.buildAttributesUpdateActions;
import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateActionUtils.buildChangeDescriptionAction;
import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateActionUtils.buildChangeNameAction;

import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import java.util.List;
import javax.annotation.Nonnull;

public final class ProductTypeSyncUtils {

  /**
   * Compares all the fields (including the attributes see {@link
   * ProductTypeUpdateActionUtils#buildAttributesUpdateActions}) of a {@link ProductType} and a
   * {@link ProductTypeDraft}. It returns a {@link List} of {@link UpdateAction}&lt;{@link
   * ProductType}&gt; as a result. If no update action is needed, for example in case where both the
   * {@link ProductType} and the {@link ProductTypeDraft} have the same fields, an empty {@link
   * List} is returned.
   *
   * @param oldProductType the {@link ProductType} which should be updated.
   * @param newProductType the {@link ProductTypeDraft} where we get the new data.
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   *     supplied by the user. For example, custom callbacks to call in case of warnings or errors
   *     occurring on the build update action process. And other options (See {@link
   *     ProductTypeSyncOptions} for more info.
   * @return A list of productType-specific update actions.
   */
  @Nonnull
  public static List<UpdateAction<ProductType>> buildActions(
      @Nonnull final ProductType oldProductType,
      @Nonnull final ProductTypeDraft newProductType,
      @Nonnull final ProductTypeSyncOptions syncOptions) {

    final List<UpdateAction<ProductType>> updateActions =
        filterEmptyOptionals(
            buildChangeNameAction(oldProductType, newProductType),
            buildChangeDescriptionAction(oldProductType, newProductType));

    updateActions.addAll(buildAttributesUpdateActions(oldProductType, newProductType, syncOptions));

    return updateActions;
  }

  private ProductTypeSyncUtils() {}
}
