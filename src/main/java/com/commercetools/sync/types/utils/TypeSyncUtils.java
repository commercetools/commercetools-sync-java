package com.commercetools.sync.types.utils;

import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildFieldDefinitionsUpdateActions;
import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildSetDescriptionUpdateAction;

import com.commercetools.sync.types.TypeSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import java.util.List;
import javax.annotation.Nonnull;

public final class TypeSyncUtils {

  /**
   * Compares all the fields (including the field definitions see {@link
   * TypeUpdateActionUtils#buildFieldDefinitionsUpdateActions(Type, TypeDraft, TypeSyncOptions)}) of
   * a {@link Type} and a {@link TypeDraft}. It returns a {@link List} of {@link
   * UpdateAction}&lt;{@link Type}&gt; as a result. If no update actions are needed, for example in
   * case where both the {@link Type} and the {@link TypeDraft} have the same fields, an empty
   * {@link List} is returned.
   *
   * <p>Note: Currently this util doesn't support the following:
   *
   * <ul>
   *   <li>updating the inputHint of a FieldDefinition
   *   <li>removing the EnumValue/LocalizedEnumValue of a FieldDefinition
   *   <li>updating the label of a EnumValue/LocalizedEnumValue of a FieldDefinition
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
  public static List<UpdateAction<Type>> buildActions(
      @Nonnull final Type oldType,
      @Nonnull final TypeDraft newType,
      @Nonnull final TypeSyncOptions syncOptions) {

    final List<UpdateAction<Type>> updateActions =
        filterEmptyOptionals(
            buildChangeNameUpdateAction(oldType, newType),
            buildSetDescriptionUpdateAction(oldType, newType));

    updateActions.addAll(buildFieldDefinitionsUpdateActions(oldType, newType, syncOptions));

    return updateActions;
  }

  private TypeSyncUtils() {}
}
