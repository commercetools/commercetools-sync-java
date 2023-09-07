package com.commercetools.sync.producttypes.utils;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;

import com.commercetools.api.models.product_type.AttributePlainEnumValue;
import com.commercetools.api.models.product_type.ProductTypeAddPlainEnumValueActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueOrderActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeRemoveEnumValuesActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlainEnumValueUpdateActionUtils {

  /**
   * Compares a list of old {@link AttributePlainEnumValue}s with a list of new {@link
   * AttributePlainEnumValue}s for a given attribute definition and builds required update actions
   * (e.g addEnumValue, removeEnumValue, changeEnumValueOrder) and 1-1 update actions on enum values
   * (e.g. changeEnumValueLabel) for the required resource. If both the {@link
   * AttributePlainEnumValue}'s are identical, then no update action is needed and hence an empty
   * {@link java.util.List} is returned.
   *
   * <p>If the list of new {@link AttributePlainEnumValue}s is {@code null}, then remove actions are
   * built for every existing plain enum value in the {@code oldEnumValues} list.
   *
   * @param attributeDefinitionName the attribute name whose plain enum values are going to be
   *     synced.
   * @param oldEnumValues the old list of plain enum values.
   * @param newEnumValues the new list of plain enum values.
   * @return a list of plain enum values update actions if the list of plain enum values is not
   *     identical. Otherwise, if the plain enum values are identical, an empty list is returned.
   * @throws com.commercetools.sync.commons.exceptions.DuplicateKeyException in case there are
   *     localized enum values with duplicate keys.
   */
  @Nonnull
  public static List<ProductTypeUpdateAction> buildEnumValuesUpdateActions(
      @Nonnull final String attributeDefinitionName,
      @Nonnull final List<AttributePlainEnumValue> oldEnumValues,
      @Nullable final List<AttributePlainEnumValue> newEnumValues) {

    return EnumValuesUpdateActionUtils.buildActions(
        attributeDefinitionName,
        oldEnumValues,
        newEnumValues,
        (defintionName, keysToRemove) ->
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(defintionName)
                .keys(keysToRemove)
                .build(),
        PlainEnumValueUpdateActionUtils::buildEnumValueUpdateActions,
        (definitionName, newEnumValue) ->
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(definitionName)
                .value(newEnumValue)
                .build(),
        (definitionName, newEnums) ->
            ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                .attributeName(definitionName)
                .values(newEnums)
                .build(),
        null);
  }

  /**
   * Compares all the fields of an old {@link AttributePlainEnumValue} and a new {@link
   * AttributePlainEnumValue} and returns a list of {@link
   * com.commercetools.api.models.product_type.ProductTypeUpdateAction} as a result. If both {@link
   * AttributePlainEnumValue} have identical fields, then no update action is needed and hence an
   * empty {@link java.util.List} is returned.
   *
   * @param attributeDefinitionName the attribute definition name whose plain enum values belong to.
   * @param oldEnumValue the enum value which should be updated.
   * @param newEnumValue the enum value where we get the new fields.
   * @return A list with the update actions or an empty list if the enum values are identical.
   */
  @Nonnull
  public static List<ProductTypeUpdateAction> buildEnumValueUpdateActions(
      @Nonnull final String attributeDefinitionName,
      @Nonnull final AttributePlainEnumValue oldEnumValue,
      @Nonnull final AttributePlainEnumValue newEnumValue) {

    return filterEmptyOptionals(
        buildChangeLabelAction(attributeDefinitionName, oldEnumValue, newEnumValue));
  }

  /**
   * Compares the {@code label} values of an old {@link AttributePlainEnumValue} and a new {@link
   * AttributePlainEnumValue} and returns an {@link java.util.Optional} of update action, which
   * would contain the {@link
   * com.commercetools.api.models.product_type.ProductTypeChangeLabelAction}. If both, old and new
   * {@link AttributePlainEnumValue} have the same {@code label} values, then no update action is
   * needed and empty optional will be returned.
   *
   * @param attributeDefinitionName the attribute definition name whose plain enum values belong to.
   * @param oldEnumValue the old plain enum value.
   * @param newEnumValue the new plain enum value which contains the new description.
   * @return optional containing update action or empty optional if labels are identical.
   */
  @Nonnull
  private static Optional<ProductTypeUpdateAction> buildChangeLabelAction(
      @Nonnull final String attributeDefinitionName,
      @Nonnull final AttributePlainEnumValue oldEnumValue,
      @Nonnull final AttributePlainEnumValue newEnumValue) {

    return buildUpdateAction(
        oldEnumValue.getLabel(),
        newEnumValue.getLabel(),
        () ->
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .newValue(newEnumValue)
                .build());
  }

  private PlainEnumValueUpdateActionUtils() {}
}
