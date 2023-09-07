package com.commercetools.sync.types.utils;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.types.utils.PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions;
import static java.util.Optional.ofNullable;

import com.commercetools.api.models.type.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

/** This class is only meant for the internal use of the commercetools-sync-java library. */
final class FieldDefinitionUpdateActionUtils {

  /**
   * Compares all the fields of an old {@link FieldDefinition} with a new {@link FieldDefinition}
   * and returns a list of {@link TypeUpdateAction} as a result. If both the {@link FieldDefinition}
   * and the {@link FieldDefinition} have identical fields, then no update action is needed and
   * hence an empty {@link java.util.List} is returned.
   *
   * <p>Note: Updating the required-field of a FieldDefinition is not possible with commercetools
   * API.
   *
   * @param oldFieldDefinition the old field definition which should be updated.
   * @param newFieldDefinition the new field definition where we get the new fields.
   * @return A list with the update actions or an empty list if the field definition fields are
   *     identical.
   * @throws com.commercetools.sync.commons.exceptions.DuplicateKeyException in case there are
   *     localized enum values with duplicate keys.
   */
  @Nonnull
  static List<TypeUpdateAction> buildActions(
      @Nonnull final FieldDefinition oldFieldDefinition,
      @Nonnull final FieldDefinition newFieldDefinition) {

    final List<TypeUpdateAction> updateActions =
        filterEmptyOptionals(
            buildChangeLabelUpdateAction(oldFieldDefinition, newFieldDefinition),
            buildChangeInputHintUpdateAction(oldFieldDefinition, newFieldDefinition));

    updateActions.addAll(buildEnumUpdateActions(oldFieldDefinition, newFieldDefinition));

    return updateActions;
  }

  /**
   * Checks if both the supplied {@code oldFieldDefinition} and {@code newFieldDefinition} have an
   * {@link FieldType} that is either an {@link CustomFieldEnumType} or a {@link
   * CustomFieldLocalizedEnumType} or a {@link CustomFieldSetType} with a subtype that is either an
   * {@link CustomFieldEnumType} or a {@link CustomFieldLocalizedEnumType}.
   *
   * <p>The method compares all the {@link com.commercetools.api.models.type.CustomFieldEnumValue}
   * and {@link com.commercetools.api.models.type.CustomFieldLocalizedEnumValue} values of the
   * {@link FieldType} and the {@link FieldDefinition} types and returns a list of {@link
   * TypeUpdateAction} as a result. If both the {@code oldFieldDefinition} and {@code
   * newFieldDefinition} have identical enum values, then no update action is needed and hence an
   * empty {@link java.util.List} is returned.
   *
   * <p>Note: This method expects the supplied {@code oldFieldDefinition} and {@code
   * newFieldDefinition} to have the same {@link FieldType}. Otherwise, the behaviour is not
   * guaranteed.
   *
   * @param oldFieldDefinition the field definition which should be updated.
   * @param newFieldDefinition the new field definition draft where we get the new fields.
   * @return A list with the update actions or an empty list if the field definition enums are
   *     identical.
   * @throws com.commercetools.sync.commons.exceptions.DuplicateKeyException in case there are enum
   *     values with duplicate keys.
   */
  @Nonnull
  static List<TypeUpdateAction> buildEnumUpdateActions(
      @Nonnull final FieldDefinition oldFieldDefinition,
      @Nonnull final FieldDefinition newFieldDefinition) {

    final FieldType oldFieldDefinitionType = oldFieldDefinition.getType();
    final FieldType newFieldDefinitionType = newFieldDefinition.getType();

    final CustomFieldEnumType oldEnumFieldType = getEnumFieldType(oldFieldDefinitionType);

    if (oldEnumFieldType == null) {
      final CustomFieldLocalizedEnumType oldLocalizedEnumFieldType =
          getLocalizedEnumFieldType(oldFieldDefinitionType);
      if (oldLocalizedEnumFieldType == null) {
        return Collections.emptyList();
      } else {
        final CustomFieldLocalizedEnumType newLocalizedEnumFieldType =
            getLocalizedEnumFieldType(newFieldDefinitionType);
        if (newLocalizedEnumFieldType == null) {
          return Collections.emptyList();
        } else {
          return LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
              oldFieldDefinition.getName(),
              oldLocalizedEnumFieldType.getValues(),
              newLocalizedEnumFieldType.getValues());
        }
      }
    } else {
      final CustomFieldEnumType newEnumFieldType = getEnumFieldType(newFieldDefinitionType);
      if (newEnumFieldType == null) {
        return Collections.emptyList();
      } else {
        return buildEnumValuesUpdateActions(
            oldFieldDefinition.getName(),
            oldEnumFieldType.getValues(),
            newEnumFieldType.getValues());
      }
    }
  }

  /**
   * @param fieldType the field type.
   * @return the field type if is an {@link CustomFieldEnumType} or if the {@link FieldType} is a
   *     {@link CustomFieldSetType} with an {@link CustomFieldEnumType} as a subtype. Otherwise, it
   *     returns null.
   */
  private static CustomFieldEnumType getEnumFieldType(@Nonnull final FieldType fieldType) {

    if (fieldType instanceof CustomFieldEnumType) {
      return (CustomFieldEnumType) fieldType;
    }

    if (fieldType instanceof CustomFieldSetType) {

      final CustomFieldSetType setFieldType = (CustomFieldSetType) fieldType;
      final FieldType subType = setFieldType.getElementType();

      if (subType instanceof CustomFieldEnumType) {
        return (CustomFieldEnumType) subType;
      }
    }

    return null;
  }

  /**
   * @param fieldType the field type.
   * @return the field type if is an {@link CustomFieldLocalizedEnumType} or if the {@link
   *     FieldType} is a {@link CustomFieldSetType} with an {@link CustomFieldLocalizedEnumType} as
   *     a subtype. Otherwise, it returns null.
   */
  private static CustomFieldLocalizedEnumType getLocalizedEnumFieldType(
      @Nonnull final FieldType fieldType) {

    if (fieldType instanceof CustomFieldLocalizedEnumType) {
      return (CustomFieldLocalizedEnumType) fieldType;
    }

    if (fieldType instanceof CustomFieldSetType) {
      final CustomFieldSetType setFieldType = (CustomFieldSetType) fieldType;
      final FieldType subType = setFieldType.getElementType();

      if (subType instanceof CustomFieldLocalizedEnumType) {
        return (CustomFieldLocalizedEnumType) subType;
      }
    }

    return null;
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} labels of old {@link
   * FieldDefinition} with new {@link FieldDefinition} and returns an {@link TypeUpdateAction} as a
   * result in an {@link java.util.Optional}. If both the {@link FieldDefinition} and the {@link
   * FieldDefinition} have the same label, then no update action is needed and hence an empty {@link
   * java.util.Optional} is returned.
   *
   * @param oldFieldDefinition the old field definition which should be updated.
   * @param newFieldDefinition the new field definition draft where we get the new label.
   * @return A filled optional with the update action or an empty optional if the labels are
   *     identical.
   */
  @Nonnull
  static Optional<TypeUpdateAction> buildChangeLabelUpdateAction(
      @Nonnull final FieldDefinition oldFieldDefinition,
      @Nonnull final FieldDefinition newFieldDefinition) {

    return buildUpdateAction(
        oldFieldDefinition.getLabel(),
        newFieldDefinition.getLabel(),
        () ->
            TypeChangeLabelActionBuilder.of()
                .fieldName(oldFieldDefinition.getName())
                .label(newFieldDefinition.getLabel())
                .build());
  }

  /**
   * Compares the {@link TypeTextInputHint} inputHint of old {@link FieldDefinition} with new {@link
   * FieldDefinition} and returns a {@link TypeChangeInputHintAction} as a result in an {@link
   * java.util.Optional}. If both the {@link FieldDefinition} and the {@link FieldDefinition} have
   * the same inputHint, then no update action is needed and hence an empty {@link
   * java.util.Optional} is returned.
   *
   * <p>Note: A null inputHint value in the AttributeDefinitionDraft is treated as a
   * TextInputHint#SINGLE_LINE value which is the default value of CTP.
   *
   * @param oldFieldDefinition the old field definition which should be updated.
   * @param newFieldDefinition the new field definition draft where we get the new inputHint.
   * @return A filled optional with the update action or an empty optional if the inputHints are
   *     identical.
   */
  @Nonnull
  static Optional<TypeUpdateAction> buildChangeInputHintUpdateAction(
      @Nonnull final FieldDefinition oldFieldDefinition,
      @Nonnull final FieldDefinition newFieldDefinition) {

    final TypeTextInputHint newFieldInputHint =
        ofNullable(newFieldDefinition.getInputHint()).orElse(TypeTextInputHint.SINGLE_LINE);

    return buildUpdateAction(
        oldFieldDefinition.getInputHint(),
        newFieldInputHint,
        () ->
            TypeChangeInputHintActionBuilder.of()
                .fieldName(oldFieldDefinition.getName())
                .inputHint(newFieldInputHint)
                .build());
  }

  private FieldDefinitionUpdateActionUtils() {}
}
