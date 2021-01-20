package com.commercetools.sync.types.utils;

import static com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils.buildActions;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.types.commands.updateactions.ChangeLocalizedEnumValueOrder;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** This class is only meant for the internal use of the commercetools-sync-java library. */
final class LocalizedEnumValueUpdateActionUtils {
  /**
   * Compares a list of old {@link LocalizedEnumValue}s with a list of new {@link
   * LocalizedEnumValue}s for a given field definition and builds required update actions (e.g
   * addLocalizedEnumValue, changeLocalizedEnumValueOrder). If both the {@link LocalizedEnumValue}'s
   * are identical, then no update action is needed and hence an empty {@link List} is returned.
   *
   * <p>Note: Currently this util doesn't support the following:
   *
   * <ul>
   *   <li>removing the EnumValue/LocalizedEnumValue of a FieldDefinition
   *   <li>updating the label of a EnumValue/LocalizedEnumValue of a FieldDefinition
   * </ul>
   *
   * TODO: Check GITHUB ISSUE#339 for missing FieldDefinition update actions.
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
  static List<UpdateAction<Type>> buildLocalizedEnumValuesUpdateActions(
      @Nonnull final String fieldDefinitionName,
      @Nonnull final List<LocalizedEnumValue> oldEnumValues,
      @Nullable final List<LocalizedEnumValue> newEnumValues) {

    return buildActions(
        fieldDefinitionName,
        oldEnumValues,
        newEnumValues,
        null,
        null,
        AddLocalizedEnumValue::of,
        null,
        ChangeLocalizedEnumValueOrder::of);
  }

  private LocalizedEnumValueUpdateActionUtils() {}
}
