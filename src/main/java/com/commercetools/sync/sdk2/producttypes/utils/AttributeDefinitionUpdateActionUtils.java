package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.sdk2.producttypes.utils.LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions;
import static com.commercetools.sync.sdk2.producttypes.utils.PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeConstraintEnumDraft;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeEnumType;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumType;
import com.commercetools.api.models.product_type.AttributePlainEnumValue;
import com.commercetools.api.models.product_type.AttributeSetType;
import com.commercetools.api.models.product_type.AttributeType;
import com.commercetools.api.models.product_type.ProductTypeChangeAttributeConstraintActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeInputHintActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeIsSearchableActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeSetInputTipActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.api.models.product_type.TextInputHint;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

/** This class is only meant for the internal use of the commercetools-sync-java library. */
final class AttributeDefinitionUpdateActionUtils {
  /**
   * Compares all the fields of an {@link AttributeDefinition} and an {@link
   * AttributeDefinitionDraft} and returns a list of {@link
   * com.commercetools.api.models.product_type.ProductTypeUpdateAction} as a result. If both the
   * {@link AttributeDefinition} and the {@link AttributeDefinitionDraft} have identical fields,
   * then no update action is needed and hence an empty {@link java.util.List} is returned.
   *
   * @param oldAttributeDefinition the old attribute definition which should be updated.
   * @param newAttributeDefinitionDraft the new attribute definition draft where we get the new
   *     fields.
   * @return A list with the update actions or an empty list if the attribute definition fields are
   *     identical.
   * @throws com.commercetools.sync.sdk2.commons.exceptions.DuplicateKeyException in case there are
   *     localized enum values with duplicate keys.
   * @throws com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException in case of
   *     any other update action errors.
   */
  @Nonnull
  static List<ProductTypeUpdateAction> buildActions(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinitionDraft)
      throws UnsupportedOperationException {

    final List<ProductTypeUpdateAction> updateActions =
        filterEmptyOptionals(
            buildChangeLabelUpdateAction(oldAttributeDefinition, newAttributeDefinitionDraft),
            buildSetInputTipUpdateAction(oldAttributeDefinition, newAttributeDefinitionDraft),
            buildChangeIsSearchableUpdateAction(
                oldAttributeDefinition, newAttributeDefinitionDraft),
            buildChangeInputHintUpdateAction(oldAttributeDefinition, newAttributeDefinitionDraft),
            buildChangeAttributeConstraintUpdateAction(
                oldAttributeDefinition, newAttributeDefinitionDraft));

    updateActions.addAll(
        buildEnumUpdateActions(oldAttributeDefinition, newAttributeDefinitionDraft));
    return updateActions;
  }

  /**
   * Checks if both the supplied {@code oldAttributeDefinition} and {@code
   * newAttributeDefinitionDraft} have an {@link AttributeType} that is either an {@link
   * AttributeEnumType} or a {@link AttributeLocalizedEnumType} or a {@link AttributeSetType} with a
   * subtype that is either an {@link AttributeEnumType} or a {@link AttributeLocalizedEnumType}.
   *
   * <p>The method compares all the {@link AttributePlainEnumValue} and {@link
   * com.commercetools.api.models.product_type.AttributeLocalizedEnumValue} values of the {@link
   * AttributeDefinition} and the {@link AttributeDefinitionDraft} attribute types and returns a
   * list of {@link com.commercetools.api.models.product_type.ProductTypeUpdateAction} as a result.
   * If both the {@link AttributeDefinition} and the {@link AttributeDefinitionDraft} have identical
   * enum values, then no update action is needed and hence an empty {@link java.util.List} is
   * returned.
   *
   * <p>Note: This method expects the supplied {@link AttributeDefinition} and the {@link
   * AttributeDefinitionDraft} to have the same {@link AttributeType}. Otherwise, the behaviour is
   * not guaranteed.
   *
   * @param oldAttributeDefinition the attribute definition which should be updated.
   * @param newAttributeDefinitionDraft the new attribute definition draft where we get the new
   *     fields.
   * @return A list with the update actions or an empty list if the attribute definition enums are
   *     identical.
   * @throws com.commercetools.sync.sdk2.commons.exceptions.DuplicateKeyException in case there are
   *     enum values with duplicate keys.
   */
  @Nonnull
  static List<ProductTypeUpdateAction> buildEnumUpdateActions(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinitionDraft) {

    final AttributeType oldAttributeType = oldAttributeDefinition.getType();
    final AttributeType newAttributeType = newAttributeDefinitionDraft.getType();

    return getAttributeEnumType(oldAttributeType)
        .map(
            oldEnumAttributeType ->
                getAttributeEnumType(newAttributeType)
                    .map(
                        newEnumAttributeType ->
                            buildEnumValuesUpdateActions(
                                oldAttributeDefinition.getName(),
                                oldEnumAttributeType.getValues(),
                                newEnumAttributeType.getValues()))
                    .orElseGet(Collections::emptyList))
        .orElseGet(
            () ->
                getLocalizedEnumAttributeType(oldAttributeType)
                    .map(
                        oldLocalizedEnumAttributeType ->
                            getLocalizedEnumAttributeType(newAttributeType)
                                .map(
                                    newLocalizedEnumAttributeType ->
                                        buildLocalizedEnumValuesUpdateActions(
                                            oldAttributeDefinition.getName(),
                                            oldLocalizedEnumAttributeType.getValues(),
                                            newLocalizedEnumAttributeType.getValues()))
                                .orElseGet(Collections::emptyList))
                    .orElseGet(Collections::emptyList));
  }

  /**
   * Returns an optional containing the attribute type if is an {@link AttributeEnumType} or if the
   * {@link AttributeType} is a {@link AttributeSetType} with an {@link AttributeEnumType} as a
   * subtype, it returns this subtype in the optional. Otherwise, an empty optional.
   *
   * @param attributeType the attribute type.
   * @return an optional containing the attribute type if is an {@link AttributeEnumType} or if the
   *     {@link AttributeType} is a {@link AttributeSetType} with an {@link AttributeEnumType} as a
   *     subtype, it returns this subtype in the optional. Otherwise, an empty optional.
   */
  private static Optional<AttributeEnumType> getAttributeEnumType(
      @Nonnull final AttributeType attributeType) {

    if (attributeType instanceof AttributeEnumType) {
      return Optional.of((AttributeEnumType) attributeType);
    }

    if (attributeType instanceof AttributeSetType) {
      final AttributeSetType setFieldType = (AttributeSetType) attributeType;
      final AttributeType subType = setFieldType.getElementType();

      if (subType instanceof AttributeEnumType) {
        return Optional.of((AttributeEnumType) subType);
      }
    }

    return Optional.empty();
  }

  /**
   * Returns an optional containing the attribute type if is an {@link AttributeLocalizedEnumType}
   * or if the {@link AttributeType} is a {@link AttributeSetType} with an {@link
   * AttributeLocalizedEnumType} as a subtype, it returns this subtype in the optional. Otherwise,
   * an empty optional.
   *
   * @param attributeType the attribute type.
   * @return an optional containing the attribute type if is an {@link AttributeLocalizedEnumType}
   *     or if the {@link AttributeType} is a {@link AttributeSetType} with an {@link
   *     AttributeLocalizedEnumType} as a subtype, it returns this subtype in the optional.
   *     Otherwise, an empty optional.
   */
  private static Optional<AttributeLocalizedEnumType> getLocalizedEnumAttributeType(
      @Nonnull final AttributeType attributeType) {

    if (attributeType instanceof AttributeLocalizedEnumType) {
      return Optional.of((AttributeLocalizedEnumType) attributeType);
    }

    if (attributeType instanceof AttributeSetType) {
      final AttributeSetType setFieldType = (AttributeSetType) attributeType;
      final AttributeType subType = setFieldType.getElementType();

      if (subType instanceof AttributeLocalizedEnumType) {
        return Optional.of((AttributeLocalizedEnumType) subType);
      }
    }

    return Optional.empty();
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} labels of an {@link
   * AttributeDefinition} and an {@link AttributeDefinitionDraft} and returns an {@link
   * com.commercetools.api.models.product_type.ProductTypeUpdateAction} as a result in an {@link
   * java.util.Optional}. If both the {@link AttributeDefinition} and the {@link
   * AttributeDefinitionDraft} have the same label, then no update action is needed and hence an
   * empty {@link java.util.Optional} is returned.
   *
   * @param oldAttributeDefinition the attribute definition which should be updated.
   * @param newAttributeDefinition the attribute definition draft where we get the new label.
   * @return A filled optional with the update action or an empty optional if the labels are
   *     identical.
   */
  @Nonnull
  static Optional<ProductTypeUpdateAction> buildChangeLabelUpdateAction(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

    return buildUpdateAction(
        oldAttributeDefinition.getLabel(),
        newAttributeDefinition.getLabel(),
        () ->
            ProductTypeChangeLabelActionBuilder.of()
                .attributeName(oldAttributeDefinition.getName())
                .label(newAttributeDefinition.getLabel())
                .build());
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} input tips of an
   * {@link AttributeDefinition} and an {@link AttributeDefinitionDraft} and returns an {@link
   * com.commercetools.api.models.product_type.ProductTypeUpdateAction} as a result in an {@link
   * java.util.Optional}. If both the {@link AttributeDefinition} and the {@link
   * AttributeDefinitionDraft} have the same input tip, then no update action is needed and hence an
   * empty {@link java.util.Optional} is returned.
   *
   * @param oldAttributeDefinition the attribute definition which should be updated.
   * @param newAttributeDefinition the attribute definition draft where we get the new input tip.
   * @return A filled optional with the update action or an empty optional if the labels are
   *     identical.
   */
  @Nonnull
  static Optional<ProductTypeUpdateAction> buildSetInputTipUpdateAction(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

    return buildUpdateAction(
        oldAttributeDefinition.getInputTip(),
        newAttributeDefinition.getInputTip(),
        () ->
            ProductTypeSetInputTipActionBuilder.of()
                .attributeName(oldAttributeDefinition.getName())
                .inputTip(newAttributeDefinition.getInputTip())
                .build());
  }

  /**
   * Compares the 'isSearchable' fields of an {@link AttributeDefinition} and an {@link
   * AttributeDefinitionDraft} and returns an {@link
   * com.commercetools.api.models.product_type.ProductTypeUpdateAction} as a result in an {@link
   * java.util.Optional}. If both the {@link AttributeDefinition} and the {@link
   * AttributeDefinitionDraft} have the same 'isSearchable' field, then no update action is needed
   * and hence an empty {@link java.util.Optional} is returned.
   *
   * <p>Note: A {@code null} {@code isSearchable} value in the {@link AttributeDefinitionDraft} is
   * treated as a {@code true} value which is the default value of CTP.
   *
   * @param oldAttributeDefinition the attribute definition which should be updated.
   * @param newAttributeDefinition the attribute definition draft where we get the new
   *     'isSearchable' field.
   * @return A filled optional with the update action or an empty optional if the 'isSearchable'
   *     fields are identical.
   */
  @Nonnull
  static Optional<ProductTypeUpdateAction> buildChangeIsSearchableUpdateAction(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

    final Boolean searchable = ofNullable(newAttributeDefinition.getIsSearchable()).orElse(true);

    return buildUpdateAction(
        oldAttributeDefinition.getIsSearchable(),
        searchable,
        () ->
            ProductTypeChangeIsSearchableActionBuilder.of()
                .attributeName(oldAttributeDefinition.getName())
                .isSearchable(searchable)
                .build());
  }

  /**
   * Compares the input hints of an {@link AttributeDefinition} and an {@link
   * AttributeDefinitionDraft} and returns an {@link
   * com.commercetools.api.models.product_type.ProductTypeUpdateAction} as a result in an {@link
   * java.util.Optional}. If both the {@link AttributeDefinition} and the {@link
   * AttributeDefinitionDraft} have the same input hints, then no update action is needed and hence
   * an empty {@link java.util.Optional} is returned.
   *
   * <p>Note: A {@code null} {@code inputHint} value in the {@link AttributeDefinitionDraft} is
   * treated as a {@code TextInputHint#SINGLE_LINE} value which is the default value of CTP.
   *
   * @param oldAttributeDefinition the attribute definition which should be updated.
   * @param newAttributeDefinition the attribute definition draft where we get the new input hint.
   * @return A filled optional with the update action or an empty optional if the input hints are
   *     identical.
   */
  @Nonnull
  static Optional<ProductTypeUpdateAction> buildChangeInputHintUpdateAction(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

    final TextInputHint inputHint =
        ofNullable(newAttributeDefinition.getInputHint()).orElse(TextInputHint.SINGLE_LINE);

    return buildUpdateAction(
        oldAttributeDefinition.getInputHint(),
        inputHint,
        () ->
            ProductTypeChangeInputHintActionBuilder.of()
                .attributeName(oldAttributeDefinition.getName())
                .newValue(inputHint)
                .build());
  }

  /**
   * Compares the attribute constraints of an {@link AttributeDefinition} and an {@link
   * AttributeDefinitionDraft} and returns an {@link
   * com.commercetools.api.models.product_type.ProductTypeUpdateAction} as a result in an {@link
   * java.util.Optional}. If both the {@link AttributeDefinition} and the {@link
   * AttributeDefinitionDraft} have the same attribute constraints, then no update action is needed
   * and hence an empty {@link java.util.Optional} is returned.
   *
   * <p>Note: Only following changes are supported: SameForAll to None and Unique to None. See the
   * docs:
   * https://docs.commercetools.com/api/projects/productTypes#change-attributedefinition-attributeconstraint
   *
   * @param oldAttributeDefinition the attribute definition which should be updated.
   * @param newAttributeDefinition the attribute definition draft where we get the new attribute
   *     constraint.
   * @return A filled optional with the update action or an empty optional if the attribute
   *     constraints are identical.
   * @throws BuildUpdateActionException If newAttributeDefinition has other values than {@code
   *     AttributeConstraintEnum.NONE}
   */
  @Nonnull
  static Optional<ProductTypeUpdateAction> buildChangeAttributeConstraintUpdateAction(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinition)
      throws UnsupportedOperationException {

    final AttributeConstraintEnum newAttributeConstraint =
        newAttributeDefinition.getAttributeConstraint();
    final AttributeConstraintEnum oldAttributeConstraint =
        oldAttributeDefinition.getAttributeConstraint();
    if (newAttributeConstraint != null
        && !newAttributeConstraint.equals(oldAttributeConstraint)
        && !AttributeConstraintEnum.NONE.equals(newAttributeConstraint)) {
      throw new UnsupportedOperationException(
          format(
              "Invalid AttributeConstraint update to %s. Only following updates are allowed: SameForAll to None and Unique to None.",
              newAttributeConstraint.name()));
    }

    return buildUpdateAction(
        oldAttributeDefinition.getAttributeConstraint(),
        newAttributeDefinition.getAttributeConstraint(),
        () ->
            ProductTypeChangeAttributeConstraintActionBuilder.of()
                .attributeName(oldAttributeDefinition.getName())
                .newValue(AttributeConstraintEnumDraft.NONE)
                .build());
  }

  private AttributeDefinitionUpdateActionUtils() {}
}
