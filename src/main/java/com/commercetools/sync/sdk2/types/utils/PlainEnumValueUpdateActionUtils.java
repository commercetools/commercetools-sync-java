package com.commercetools.sync.sdk2.types.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.commons.utils.OptionalUtils.filterEmptyOptionals;

import com.commercetools.api.models.type.*;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.sdk2.commons.utils.EnumValuesUpdateActionUtils;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** This class is only meant for the internal use of the commercetools-sync-java library. */
final class PlainEnumValueUpdateActionUtils {

  /**
   * Compares a list of old {@link CustomFieldEnumValue}s with a list of new {@link
   * CustomFieldEnumValue}s for a given field definition and builds required update actions (e.g
   * addEnumValue, changeEnumValueOrder). If both the {@link CustomFieldEnumValue}'s are identical,
   * then no update action is needed and hence an empty {@link List} is returned.
   *
   * <p>Note: Currently this util doesn't support the following:
   *
   * <ul>
   *   <li>removing the CustomFieldEnumValue/CustomFieldLocalizedEnumValue of a FieldDefinition
   *   <li>updating the label of a CustomFieldEnumValue/CustomFieldLocalizedEnumValue of a
   *       FieldDefinition
   * </ul>
   *
   * @param fieldDefinitionName the field name whose plain enum values are going to be synced.
   * @param oldEnumValues the old list of plain enum values.
   * @param newEnumValues the new list of plain enum values.
   * @return a list of plain enum values update actions if the list of plain enum values is not
   *     identical. Otherwise, if the plain enum values are identical, an empty list is returned.
   * @throws DuplicateKeyException in case there are localized enum values with duplicate keys.
   */
  @Nonnull
  static List<TypeUpdateAction> buildEnumValuesUpdateActions(
      @Nonnull final String fieldDefinitionName,
      @Nonnull final List<CustomFieldEnumValue> oldEnumValues,
      @Nullable final List<CustomFieldEnumValue> newEnumValues) {

    return EnumValuesUpdateActionUtils.buildActions(
        fieldDefinitionName,
        oldEnumValues,
        newEnumValues,
        null,
        (definitionName, oldEnumValue, newEnumValue) ->
            PlainEnumValueUpdateActionUtils.buildLocalizedEnumValueUpdateActions(
                definitionName, oldEnumValue, newEnumValue),
        (name, enumValue) ->
            TypeAddEnumValueActionBuilder.of().fieldName(name).value(enumValue).build(),
        null,
        (name, enumValues) ->
            TypeChangeEnumValueOrderActionBuilder.of().fieldName(name).keys(enumValues).build());
  }

  /**
   * Compares all the fields of an old {@link CustomFieldEnumValue} and a new {@link
   * CustomFieldEnumValue} and returns a list of {@link TypeUpdateAction} as a result. If both
   * {@link CustomFieldEnumValue} have identical fields then no update action is needed and hence an
   * empty {@link java.util.List} is returned.
   *
   * @param attributeDefinitionName the attribute definition name whose enum values belong to.
   * @param oldEnumValue the enum value which should be updated.
   * @param newEnumValue the enum value where we get the new fields.
   * @return A list with the update actions or an empty list if the enum values are identical.
   */
  @Nonnull
  public static List<TypeUpdateAction> buildLocalizedEnumValueUpdateActions(
      @Nonnull final String attributeDefinitionName,
      @Nonnull final CustomFieldEnumValue oldEnumValue,
      @Nonnull final CustomFieldEnumValue newEnumValue) {

    return filterEmptyOptionals(
        buildChangeLabelAction(attributeDefinitionName, oldEnumValue, newEnumValue));
  }

  /**
   * Compares the {@code label} values of an old {@link CustomFieldEnumValue} and a new {@link
   * CustomFieldEnumValue} and returns an {@link java.util.Optional} of update action, which would
   * contain the {@code "changeLabel"} {@link
   * com.commercetools.api.models.type.TypeChangeEnumValueLabelAction}. If both, old and new {@link
   * CustomFieldEnumValue} have the same {@code label} values, then no update action is needed and
   * empty optional will be returned.
   *
   * @param attributeDefinitionName the attribute definition name whose localized enum values belong
   *     to.
   * @param oldEnumValue the old localized enum value.
   * @param newEnumValue the new localized enum value which contains the new description.
   * @return optional containing update action or empty optional if labels are identical.
   */
  @Nonnull
  private static Optional<TypeUpdateAction> buildChangeLabelAction(
      @Nonnull final String attributeDefinitionName,
      @Nonnull final CustomFieldEnumValue oldEnumValue,
      @Nonnull final CustomFieldEnumValue newEnumValue) {

    return buildUpdateAction(
        oldEnumValue.getLabel(),
        newEnumValue.getLabel(),
        () ->
            TypeChangeEnumValueLabelActionBuilder.of()
                .fieldName(attributeDefinitionName)
                .value(newEnumValue)
                .build());
  }

  private PlainEnumValueUpdateActionUtils() {}
}
