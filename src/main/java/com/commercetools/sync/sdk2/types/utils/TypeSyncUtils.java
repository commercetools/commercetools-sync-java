package com.commercetools.sync.sdk2.types.utils;

import static com.commercetools.sync.sdk2.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.sdk2.types.utils.TypeUpdateActionUtils.*;

import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeUpdateAction;
import com.commercetools.sync.sdk2.types.TypeSyncOptions;
import java.util.List;
import javax.annotation.Nonnull;

public final class TypeSyncUtils {

  /**
   * Compares all the fields (including the field definitions see {@link
   * TypeUpdateActionUtils#buildFieldDefinitionsUpdateActions(Type, TypeDraft, TypeSyncOptions)}) of
   * a {@link Type} and a {@link TypeDraft}. It returns a {@link List} of {@link TypeUpdateAction}
   * as a result. If no update actions are needed, for example in case where both the {@link Type}
   * and the {@link TypeDraft} have the same fields, an empty {@link List} is returned.
   *
   * <p>Note: The commercetools API doesn't support the following:
   *
   * <ul>
   *   <li>updating the inputHint of a FieldDefinition
   *   <li>removing the EnumValue/LocalizedEnumValue of a FieldDefinition
   * </ul>
   *
   * @param oldType the {@link Type} which should be updated.
   * @param newType the {@link TypeDraft} where we get the new data.
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   *     supplied by the user. For example, custom callbacks to call in case of warnings or errors
   *     occurring on the build update action process. And other options (See {@link
   *     TypeSyncOptions} for more info.
   * @return A list of type-specific update actions.
   */
  @Nonnull
  public static List<TypeUpdateAction> buildActions(
      @Nonnull final Type oldType,
      @Nonnull final TypeDraft newType,
      @Nonnull final TypeSyncOptions syncOptions) {

    final List<TypeUpdateAction> updateActions =
        filterEmptyOptionals(
            buildChangeNameUpdateAction(oldType, newType),
            buildSetDescriptionUpdateAction(oldType, newType));

    updateActions.addAll(buildFieldDefinitionsUpdateActions(oldType, newType, syncOptions));

    return updateActions;
  }

  private TypeSyncUtils() {}
}
