package com.commercetools.sync.sdk2.types.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.*;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.types.TypeSyncOptions;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

public final class TypeUpdateActionUtils {

  /**
   * Compares the {@link LocalizedString} name values of a {@link Type} and a {@link TypeDraft} and
   * returns an {@link Optional} of update action, which would contain the {@link
   * TypeChangeNameAction} if values are different.
   *
   * @param oldType the type that should be updated.
   * @param newType the type draft which contains the new name.
   * @return optional containing update action or empty optional if names are identical.
   */
  @Nonnull
  public static Optional<TypeUpdateAction> buildChangeNameUpdateAction(
      @Nonnull final Type oldType, @Nonnull final TypeDraft newType) {

    return buildUpdateAction(
        oldType.getName(),
        newType.getName(),
        () -> TypeChangeNameActionBuilder.of().name(newType.getName()).build());
  }

  /**
   * Compares the {@link LocalizedString} descriptions of a {@link Type} and a {@link TypeDraft} and
   * returns an {@link TypeSetDescriptionAction} as a result in an {@link Optional} of update action
   * if values are different.
   *
   * @param oldType the type which should be updated.
   * @param newType the type draft where we get the new description.
   * @return A filled optional with the update action or an empty optional if the descriptions are
   *     identical.
   */
  @Nonnull
  public static Optional<TypeUpdateAction> buildSetDescriptionUpdateAction(
      @Nonnull final Type oldType, @Nonnull final TypeDraft newType) {

    return buildUpdateAction(
        oldType.getDescription(),
        newType.getDescription(),
        () -> TypeSetDescriptionActionBuilder.of().description(newType.getDescription()).build());
  }

  /**
   * Compares the field definitions of a {@link Type} and a {@link TypeDraft} and returns a list of
   * {@link TypeUpdateAction} as a result if the values are different. In case, the new type draft
   * has a list of field definitions in which a duplicate name exists, the error callback is
   * triggered and an empty list is returned.
   *
   * <p>Note: Currently commercetools API doesn't support the following:
   *
   * <ul>
   *   <li>removing the EnumValue/LocalizedEnumValue of a FieldDefinition
   * </ul>
   *
   * @param oldType the type which should be updated.
   * @param newType the type draft where we get the key.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the update actions or an empty list if the field definitions are identical.
   */
  @Nonnull
  public static List<TypeUpdateAction> buildFieldDefinitionsUpdateActions(
      @Nonnull final Type oldType,
      @Nonnull final TypeDraft newType,
      @Nonnull final TypeSyncOptions syncOptions) {

    try {
      return FieldDefinitionsUpdateActionUtils.buildFieldDefinitionsUpdateActions(
          oldType.getFieldDefinitions(), newType.getFieldDefinitions());
    } catch (final BuildUpdateActionException exception) {
      syncOptions.applyErrorCallback(
          new SyncException(
              format(
                  "Failed to build update actions for the field definitions "
                      + "of the type with the key '%s'. Reason: %s",
                  newType.getKey(), exception),
              exception),
          oldType,
          newType,
          null);
      return emptyList();
    }
  }

  private TypeUpdateActionUtils() {}
}
