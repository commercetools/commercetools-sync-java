package com.commercetools.sync.types.utils;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.AddEnumValue;
import io.sphere.sdk.types.commands.updateactions.ChangeEnumValueOrder;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** This class is only meant for the internal use of the commercetools-sync-java library. */
final class PlainEnumValueUpdateActionUtils {

  /**
   * Compares a list of old {@link EnumValue}s with a list of new {@link EnumValue}s for a given
   * field definition and builds required update actions (e.g addEnumValue, changeEnumValueOrder).
   * If both the {@link EnumValue}'s are identical, then no update action is needed and hence an
   * empty {@link List} is returned.
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
   * @param fieldDefinitionName the field name whose plain enum values are going to be synced.
   * @param oldEnumValues the old list of plain enum values.
   * @param newEnumValues the new list of plain enum values.
   * @return a list of plain enum values update actions if the list of plain enum values is not
   *     identical. Otherwise, if the plain enum values are identical, an empty list is returned.
   * @throws DuplicateKeyException in case there are localized enum values with duplicate keys.
   */
  @Nonnull
  static List<UpdateAction<Type>> buildEnumValuesUpdateActions(
      @Nonnull final String fieldDefinitionName,
      @Nonnull final List<EnumValue> oldEnumValues,
      @Nullable final List<EnumValue> newEnumValues) {

    return EnumValuesUpdateActionUtils.buildActions(
        fieldDefinitionName,
        oldEnumValues,
        newEnumValues,
        null,
        null,
        AddEnumValue::of,
        null,
        ChangeEnumValueOrder::of);
  }

  private PlainEnumValueUpdateActionUtils() {}
}
