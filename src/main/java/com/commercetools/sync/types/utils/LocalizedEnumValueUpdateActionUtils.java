package com.commercetools.sync.types.utils;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils.buildActions;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;

import com.commercetools.api.models.type.*;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** This class is only meant for the internal use of the commercetools-sync-java library. */
final class LocalizedEnumValueUpdateActionUtils {
  /**
   * Compares a list of old {@link CustomFieldLocalizedEnumValue}s with a list of new {@link
   * CustomFieldLocalizedEnumValue}s for a given field definition and builds required update actions
   * (e.g addLocalizedEnumValue, changeLocalizedEnumValueOrder). If both the {@link
   * CustomFieldLocalizedEnumValue}'s are identical, then no update action is needed and hence an
   * empty {@link List} is returned.
   *
   * <p>Note: Currently this util doesn't support the following:
   *
   * <ul>
   *   <li>removing the CustomFieldEnumValue/CustomFieldLocalizedEnumValue of a FieldDefinition
   *   <li>updating the label of a CustomFieldEnumValue/CustomFieldLocalizedEnumValue of a
   *       FieldDefinition
   * </ul>
   *
   * @param fieldDefinitionName the field name whose localized enum values are going to be synced.
   * @param oldEnumValues the old list of localized enum values.
   * @param newEnumValues the new list of localized enum values.
   * @return a list of localized enum values update actions if the list of localized enum values is
   *     not identical. Otherwise, if the localized enum values are identical, an empty list is
   *     returned.
   * @throws DuplicateKeyException in case there are localized enum values with duplicate keys.
   */
  @Nonnull
  static List<TypeUpdateAction> buildLocalizedEnumValuesUpdateActions(
      @Nonnull final String fieldDefinitionName,
      @Nonnull final List<CustomFieldLocalizedEnumValue> oldEnumValues,
      @Nullable final List<CustomFieldLocalizedEnumValue> newEnumValues) {

    return buildActions(
        fieldDefinitionName,
        oldEnumValues,
        newEnumValues,
        null,
        (definitionName, oldEnumValue, newEnumValue) ->
            LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValueUpdateActions(
                fieldDefinitionName, oldEnumValue, newEnumValue),
        (name, localizedEnumValue) ->
            TypeAddLocalizedEnumValueActionBuilder.of()
                .fieldName(name)
                .value(localizedEnumValue)
                .build(),
        null,
        (name, localizedEnumValues) ->
            TypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .fieldName(name)
                .keys(localizedEnumValues)
                .build());
  }

  /**
   * Compares all the fields of an old {@link CustomFieldLocalizedEnumValue} and a new {@link
   * CustomFieldLocalizedEnumValue} and returns a list of {@link TypeUpdateAction} as a result. If
   * both {@link CustomFieldLocalizedEnumValue} have identical fields then no update action is
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
  public static List<TypeUpdateAction> buildLocalizedEnumValueUpdateActions(
      @Nonnull final String attributeDefinitionName,
      @Nonnull final CustomFieldLocalizedEnumValue oldEnumValue,
      @Nonnull final CustomFieldLocalizedEnumValue newEnumValue) {

    return filterEmptyOptionals(
        buildChangeLabelAction(attributeDefinitionName, oldEnumValue, newEnumValue));
  }

  /**
   * Compares the {@code label} values of an old {@link CustomFieldLocalizedEnumValue} and a new
   * {@link CustomFieldLocalizedEnumValue} and returns an {@link java.util.Optional} of update
   * action, which would contain the {@code "changeLabel"} {@link
   * com.commercetools.api.models.type.TypeChangeLocalizedEnumValueLabelAction}. If both, old and
   * new {@link CustomFieldLocalizedEnumValue} have the same {@code label} values, then no update
   * action is needed and empty optional will be returned.
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
      @Nonnull final CustomFieldLocalizedEnumValue oldEnumValue,
      @Nonnull final CustomFieldLocalizedEnumValue newEnumValue) {

    return buildUpdateAction(
        oldEnumValue.getLabel(),
        newEnumValue.getLabel(),
        () ->
            TypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .fieldName(attributeDefinitionName)
                .value(newEnumValue)
                .build());
  }

  private LocalizedEnumValueUpdateActionUtils() {}
}
