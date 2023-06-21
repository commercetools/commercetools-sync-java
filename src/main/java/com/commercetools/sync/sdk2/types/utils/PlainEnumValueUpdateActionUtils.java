package com.commercetools.sync.sdk2.types.utils;

import com.commercetools.api.models.type.*;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.sdk2.commons.utils.EnumValuesUpdateActionUtils;
import java.util.List;
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
        null,
        (name, enumValue) ->
            TypeAddEnumValueActionBuilder.of().fieldName(name).value(enumValue).build(),
        null,
        (name, enumValues) ->
            TypeChangeEnumValueOrderActionBuilder.of().fieldName(name).keys(enumValues).build());
  }

  private PlainEnumValueUpdateActionUtils() {}
}
