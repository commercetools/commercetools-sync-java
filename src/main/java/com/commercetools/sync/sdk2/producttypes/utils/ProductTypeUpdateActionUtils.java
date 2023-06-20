package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.producttypes.utils.AttributeDefinitionsUpdateActionUtils.buildAttributeDefinitionsUpdateActions;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeChangeDescriptionActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeNameActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptions;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

public final class ProductTypeUpdateActionUtils {
  /**
   * Compares the {@code name} values of a {@link ProductType} and a {@link ProductTypeDraft} and
   * returns an {@link java.util.Optional} of update action, which would contain the {@code
   * "changeName"} {@link com.commercetools.api.models.product_type.ProductTypeChangeNameAction}. If
   * both {@link ProductType} and {@link ProductTypeDraft} have the same {@code name} values, then
   * no update action is needed and empty optional will be returned.
   *
   * @param oldProductType the product type that should be updated.
   * @param newProductType the product type draft which contains the new name.
   * @return optional containing update action or empty optional if names are identical.
   */
  @Nonnull
  public static Optional<ProductTypeUpdateAction> buildChangeNameAction(
      @Nonnull final ProductType oldProductType, @Nonnull final ProductTypeDraft newProductType) {

    return buildUpdateAction(
        oldProductType.getName(),
        newProductType.getName(),
        () -> ProductTypeChangeNameActionBuilder.of().name(newProductType.getName()).build());
  }

  /**
   * Compares the {@code description} values of a {@link ProductType} and a {@link ProductTypeDraft}
   * and returns an {@link java.util.Optional} of update action, which would contain the {@link
   * com.commercetools.api.models.product_type.ProductTypeChangeDescriptionAction}. If both {@link
   * ProductType} and {@link ProductTypeDraft} have the same {@code description} values, then no
   * update action is needed and empty optional will be returned.
   *
   * @param oldProductType the product type that should be updated.
   * @param newProductType the product type draft which contains the new description.
   * @return optional containing update action or empty optional if descriptions are identical.
   */
  @Nonnull
  public static Optional<ProductTypeUpdateAction> buildChangeDescriptionAction(
      @Nonnull final ProductType oldProductType, @Nonnull final ProductTypeDraft newProductType) {

    return buildUpdateAction(
        oldProductType.getDescription(),
        newProductType.getDescription(),
        () ->
            ProductTypeChangeDescriptionActionBuilder.of()
                .description(newProductType.getDescription())
                .build());
  }

  /**
   * Compares the attributes of a {@link ProductType} and a {@link ProductTypeDraft} and returns a
   * list of {@link com.commercetools.api.models.product_type.ProductTypeUpdateAction} as a result.
   * If both the {@link ProductType} and the {@link ProductTypeDraft} have identical attributes,
   * then no update action is needed and hence an empty {@link java.util.List} is returned. In case,
   * the new product type draft has a list of attributes in which a duplicate name exists, the error
   * callback is triggered and an empty list is returned.
   *
   * @param oldProductType the product type which should be updated.
   * @param newProductType the product type draft where we get the key.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the update actions or an empty list if the attributes are identical.
   */
  @Nonnull
  public static List<ProductTypeUpdateAction> buildAttributesUpdateActions(
      @Nonnull final ProductType oldProductType,
      @Nonnull final ProductTypeDraft newProductType,
      @Nonnull final ProductTypeSyncOptions syncOptions) {

    try {
      List<AttributeDefinition> attributes = oldProductType.getAttributes();
      if (attributes == null) {
        attributes = emptyList();
      }
      return buildAttributeDefinitionsUpdateActions(attributes, newProductType.getAttributes());
    } catch (final BuildUpdateActionException exception) {
      syncOptions.applyErrorCallback(
          new SyncException(
              format(
                  "Failed to build update actions for the attributes definitions "
                      + "of the product type with the key '%s'. Reason: %s",
                  oldProductType.getKey(), exception),
              exception),
          oldProductType,
          newProductType,
          null);
      return emptyList();
    }
  }

  private ProductTypeUpdateActionUtils() {}
}
