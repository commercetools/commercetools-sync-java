package com.commercetools.sync.producttypes.utils;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils.buildActions;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;

import com.commercetools.api.models.product_type.AttributeLocalizedEnumValue;
import com.commercetools.api.models.product_type.ProductTypeAddLocalizedEnumValueActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLocalizedEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLocalizedEnumValueOrderActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeRemoveEnumValuesActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class LocalizedEnumValueUpdateActionUtils {

  /**
   * Compares a list of old {@link AttributeLocalizedEnumValue}s with a list of new {@link
   * AttributeLocalizedEnumValue}s for a given attribute definition and builds required update
   * actions (e.g addLocalizedEnumValue, removeLocalizedEnumValue, changeLocalizedEnumValueOrder)
   * and 1-1 update actions on localized enum values (e.g. changeLocalizedEnumValueLabel) for the
   * required resource. If both the {@link AttributeLocalizedEnumValue}'s are identical, then no
   * update action is needed and hence an empty {@link java.util.List} is returned.
   *
   * <p>If the list of new {@link AttributeLocalizedEnumValue}s is {@code null}, then remove actions
   * are built for every existing localized enum value in the {@code oldEnumValues} list.
   *
   * @param attributeDefinitionName the attribute name whose localized enum values are going to be
   *     synced.
   * @param oldEnumValues the old list of localized enum values.
   * @param newEnumValues the new list of localized enum values.
   * @return a list of localized enum values update actions if the list of localized enum values is
   *     not identical. Otherwise, if the localized enum values are identical, an empty list is
   *     returned.
   * @throws com.commercetools.sync.commons.exceptions.DuplicateKeyException in case there are
   *     localized enum values with duplicate keys.
   */
  @Nonnull
  public static List<ProductTypeUpdateAction> buildLocalizedEnumValuesUpdateActions(
      @Nonnull final String attributeDefinitionName,
      @Nonnull final List<AttributeLocalizedEnumValue> oldEnumValues,
      @Nullable final List<AttributeLocalizedEnumValue> newEnumValues) {

    return buildActions(
        attributeDefinitionName,
        oldEnumValues,
        newEnumValues,
        (definitionName, keysToRemove) ->
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(definitionName)
                .keys(keysToRemove)
                .build(),
        (definitionName, oldEnumValue, newEnumValue) ->
            LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValueUpdateActions(
                attributeDefinitionName, oldEnumValue, newEnumValue),
        (definitionName, newEnumValue) ->
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(definitionName)
                .value(newEnumValue)
                .build(),
        (definitionName, newValues) ->
            ProductTypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .attributeName(definitionName)
                .values(newValues)
                .build(),
        null);
  }

  /**
   * Compares all the fields of an old {@link AttributeLocalizedEnumValue} and a new {@link
   * AttributeLocalizedEnumValue} and returns a list of {@link ProductTypeUpdateAction} as a result.
   * If both {@link AttributeLocalizedEnumValue} have identical fields then no update action is
   * needed and hence an empty {@link java.util.List} is returned.
   *
   * @param attributeDefinitionName the attribute definition name whose localized enum values belong
   *     to.
   * @param oldEnumValue the localized enum value which should be updated.
   * @param newEnumValue the localized enum value where we get the new fields.
   * @return A list with the update actions or an empty list if the localized enum values are
   *     identical.
   */
  @Nonnull
  public static List<ProductTypeUpdateAction> buildLocalizedEnumValueUpdateActions(
      @Nonnull final String attributeDefinitionName,
      @Nonnull final AttributeLocalizedEnumValue oldEnumValue,
      @Nonnull final AttributeLocalizedEnumValue newEnumValue) {

    return filterEmptyOptionals(
        buildChangeLabelAction(attributeDefinitionName, oldEnumValue, newEnumValue));
  }

  /**
   * Compares the {@code label} values of an old {@link AttributeLocalizedEnumValue} and a new
   * {@link AttributeLocalizedEnumValue} and returns an {@link java.util.Optional} of update action,
   * which would contain the {@code "changeLabel"} {@link
   * com.commercetools.api.models.product_type.ProductTypeChangeLocalizedEnumValueLabelAction}. If
   * both, old and new {@link AttributeLocalizedEnumValue} have the same {@code label} values, then
   * no update action is needed and empty optional will be returned.
   *
   * @param attributeDefinitionName the attribute definition name whose localized enum values belong
   *     to.
   * @param oldEnumValue the old localized enum value.
   * @param newEnumValue the new localized enum value which contains the new description.
   * @return optional containing update action or empty optional if labels are identical.
   */
  @Nonnull
  private static Optional<ProductTypeUpdateAction> buildChangeLabelAction(
      @Nonnull final String attributeDefinitionName,
      @Nonnull final AttributeLocalizedEnumValue oldEnumValue,
      @Nonnull final AttributeLocalizedEnumValue newEnumValue) {

    return buildUpdateAction(
        oldEnumValue.getLabel(),
        newEnumValue.getLabel(),
        () ->
            ProductTypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .newValue(newEnumValue)
                .build());
  }

  private LocalizedEnumValueUpdateActionUtils() {}
}
