package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;

import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductSetAttributeAction;
import com.commercetools.api.models.product.ProductSetAttributeInAllVariantsAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.products.AttributeMetaData;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ProductVariantAttributeUpdateActionUtils {
  public static final String ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA =
      "Cannot find the attribute with the name '%s'" + " in the supplied attribute metadata.";

  /**
   * Compares the attributes of a {@link Attribute} and a {@link Attribute} to build either a {@link
   * ProductSetAttributeAction} or a {@link ProductSetAttributeInAllVariantsAction}.
   *
   * <p>If the attribute is sameForAll a {@link ProductSetAttributeInAllVariantsAction} is built.
   * Otherwise, a {@link ProductSetAttributeAction} is built.
   *
   * <p>If both the {@link Attribute} and the {@link Attribute} have identical values, then no
   * update action is needed and hence an empty {@link List} is returned.
   *
   * @param variantId the id of the variant of that the attribute belong to. It is used only in the
   *     error messages if any.
   * @param oldProductVariantAttribute the {@link Attribute} which should be updated.
   * @param newProductVariantAttribute the {@link Attribute} where we get the new value.
   * @param attributesMetaData a map of attribute name -&gt; {@link AttributeMetaData}; which
   *     defines attribute information: its name and whether it has the constraint "SameForAll" or
   *     not.
   * @return A filled optional with the update action or an empty optional if the attributes are
   *     identical.
   * @throws BuildUpdateActionException thrown if attribute as not found in the {@code
   *     attributeMetaData} or if the attribute is required and the new value is null.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildProductVariantAttributeUpdateAction(
      final long variantId,
      @Nullable final Attribute oldProductVariantAttribute,
      @Nonnull final Attribute newProductVariantAttribute,
      @Nonnull final Map<String, AttributeMetaData> attributesMetaData)
      throws BuildUpdateActionException {

    final String newProductVariantAttributeName = newProductVariantAttribute.getName();
    final Object newProductVariantAttributeValue = newProductVariantAttribute.getValue();
    final Object oldProductVariantAttributeValue =
        oldProductVariantAttribute != null ? oldProductVariantAttribute.getValue() : null;

    final AttributeMetaData attributeMetaData =
        attributesMetaData.get(newProductVariantAttributeName);

    if (attributeMetaData == null) {
      final String errorMessage =
          format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, newProductVariantAttributeName);
      throw new BuildUpdateActionException(errorMessage);
    }

    return attributeMetaData.isSameForAll()
        ? buildUpdateAction(
            oldProductVariantAttributeValue,
            newProductVariantAttributeValue,
            () ->
                ProductSetAttributeInAllVariantsAction.builder()
                    .value(newProductVariantAttribute)
                    .staged(true)
                    .build())
        : buildUpdateAction(
            oldProductVariantAttributeValue,
            newProductVariantAttributeValue,
            () ->
                ProductSetAttributeAction.builder()
                    .variantId(variantId)
                    .value(newProductVariantAttribute)
                    .staged(true)
                    .build());
  }

  private ProductVariantAttributeUpdateActionUtils() {}
}
