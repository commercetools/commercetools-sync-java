package com.commercetools.sync.sdk2.types.utils;

import static com.commercetools.sync.sdk2.commons.utils.EnumValuesUpdateActionUtils.buildActions;

import com.commercetools.api.models.type.CustomFieldLocalizedEnumValue;
import com.commercetools.api.models.type.TypeAddLocalizedEnumValueActionBuilder;
import com.commercetools.api.models.type.TypeChangeLocalizedEnumValueOrderActionBuilder;
import com.commercetools.api.models.type.TypeUpdateAction;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateKeyException;
import java.util.List;
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
        null,
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

  private LocalizedEnumValueUpdateActionUtils() {}
}
