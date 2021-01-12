package com.commercetools.sync.producttypes.utils;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.producttypes.utils.LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions;
import static com.commercetools.sync.producttypes.utils.PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions;
import static java.util.Optional.ofNullable;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeType;
import io.sphere.sdk.products.attributes.EnumAttributeType;
import io.sphere.sdk.products.attributes.LocalizedEnumAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeConstraint;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeDefinitionLabel;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeInputHint;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeIsSearchable;
import io.sphere.sdk.producttypes.commands.updateactions.SetInputTip;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

/** This class is only meant for the internal use of the commercetools-sync-java library. */
final class AttributeDefinitionUpdateActionUtils {
  /**
   * Compares all the fields of an {@link AttributeDefinition} and an {@link
   * AttributeDefinitionDraft} and returns a list of {@link UpdateAction}&lt;{@link ProductType}&gt;
   * as a result. If both the {@link AttributeDefinition} and the {@link AttributeDefinitionDraft}
   * have identical fields, then no update action is needed and hence an empty {@link List} is
   * returned.
   *
   * @param oldAttributeDefinition the old attribute definition which should be updated.
   * @param newAttributeDefinitionDraft the new attribute definition draft where we get the new
   *     fields.
   * @return A list with the update actions or an empty list if the attribute definition fields are
   *     identical.
   * @throws DuplicateKeyException in case there are localized enum values with duplicate keys.
   */
  @Nonnull
  static List<UpdateAction<ProductType>> buildActions(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinitionDraft) {

    final List<UpdateAction<ProductType>> updateActions =
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
   * EnumAttributeType} or a {@link LocalizedEnumAttributeType} or a {@link SetAttributeType} with a
   * subtype that is either an {@link EnumAttributeType} or a {@link LocalizedEnumAttributeType}.
   *
   * <p>The method compares all the {@link EnumValue} and {@link LocalizedEnumValue} values of the
   * {@link AttributeDefinition} and the {@link AttributeDefinitionDraft} attribute types and
   * returns a list of {@link UpdateAction}&lt;{@link ProductType}&gt; as a result. If both the
   * {@link AttributeDefinition} and the {@link AttributeDefinitionDraft} have identical enum
   * values, then no update action is needed and hence an empty {@link List} is returned.
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
   * @throws DuplicateKeyException in case there are enum values with duplicate keys.
   */
  @Nonnull
  static List<UpdateAction<ProductType>> buildEnumUpdateActions(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinitionDraft) {

    final AttributeType oldAttributeType = oldAttributeDefinition.getAttributeType();
    final AttributeType newAttributeType = newAttributeDefinitionDraft.getAttributeType();

    return getEnumAttributeType(oldAttributeType)
        .map(
            oldEnumAttributeType ->
                getEnumAttributeType(newAttributeType)
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
   * Returns an optional containing the attribute type if is an {@link EnumAttributeType} or if the
   * {@link AttributeType} is a {@link SetAttributeType} with an {@link EnumAttributeType} as a
   * subtype, it returns this subtype in the optional. Otherwise, an empty optional.
   *
   * @param attributeType the attribute type.
   * @return an optional containing the attribute type if is an {@link EnumAttributeType} or if the
   *     {@link AttributeType} is a {@link SetAttributeType} with an {@link EnumAttributeType} as a
   *     subtype, it returns this subtype in the optional. Otherwise, an empty optional.
   */
  private static Optional<EnumAttributeType> getEnumAttributeType(
      @Nonnull final AttributeType attributeType) {

    if (attributeType instanceof EnumAttributeType) {
      return Optional.of((EnumAttributeType) attributeType);
    }

    if (attributeType instanceof SetAttributeType) {
      final SetAttributeType setFieldType = (SetAttributeType) attributeType;
      final AttributeType subType = setFieldType.getElementType();

      if (subType instanceof EnumAttributeType) {
        return Optional.of((EnumAttributeType) subType);
      }
    }

    return Optional.empty();
  }

  /**
   * Returns an optional containing the attribute type if is an {@link LocalizedEnumAttributeType}
   * or if the {@link AttributeType} is a {@link SetAttributeType} with an {@link
   * LocalizedEnumAttributeType} as a subtype, it returns this subtype in the optional. Otherwise,
   * an empty optional.
   *
   * @param attributeType the attribute type.
   * @return an optional containing the attribute type if is an {@link LocalizedEnumAttributeType}
   *     or if the {@link AttributeType} is a {@link SetAttributeType} with an {@link
   *     LocalizedEnumAttributeType} as a subtype, it returns this subtype in the optional.
   *     Otherwise, an empty optional.
   */
  private static Optional<LocalizedEnumAttributeType> getLocalizedEnumAttributeType(
      @Nonnull final AttributeType attributeType) {

    if (attributeType instanceof LocalizedEnumAttributeType) {
      return Optional.of((LocalizedEnumAttributeType) attributeType);
    }

    if (attributeType instanceof SetAttributeType) {
      final SetAttributeType setFieldType = (SetAttributeType) attributeType;
      final AttributeType subType = setFieldType.getElementType();

      if (subType instanceof LocalizedEnumAttributeType) {
        return Optional.of((LocalizedEnumAttributeType) subType);
      }
    }

    return Optional.empty();
  }

  /**
   * Compares the {@link LocalizedString} labels of an {@link AttributeDefinition} and an {@link
   * AttributeDefinitionDraft} and returns an {@link UpdateAction}&lt;{@link ProductType}&gt; as a
   * result in an {@link Optional}. If both the {@link AttributeDefinition} and the {@link
   * AttributeDefinitionDraft} have the same label, then no update action is needed and hence an
   * empty {@link Optional} is returned.
   *
   * @param oldAttributeDefinition the attribute definition which should be updated.
   * @param newAttributeDefinition the attribute definition draft where we get the new label.
   * @return A filled optional with the update action or an empty optional if the labels are
   *     identical.
   */
  @Nonnull
  static Optional<UpdateAction<ProductType>> buildChangeLabelUpdateAction(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

    return buildUpdateAction(
        oldAttributeDefinition.getLabel(),
        newAttributeDefinition.getLabel(),
        () ->
            ChangeAttributeDefinitionLabel.of(
                oldAttributeDefinition.getName(), newAttributeDefinition.getLabel()));
  }

  /**
   * Compares the {@link LocalizedString} input tips of an {@link AttributeDefinition} and an {@link
   * AttributeDefinitionDraft} and returns an {@link UpdateAction}&lt;{@link ProductType}&gt; as a
   * result in an {@link Optional}. If both the {@link AttributeDefinition} and the {@link
   * AttributeDefinitionDraft} have the same input tip, then no update action is needed and hence an
   * empty {@link Optional} is returned.
   *
   * @param oldAttributeDefinition the attribute definition which should be updated.
   * @param newAttributeDefinition the attribute definition draft where we get the new input tip.
   * @return A filled optional with the update action or an empty optional if the labels are
   *     identical.
   */
  @Nonnull
  static Optional<UpdateAction<ProductType>> buildSetInputTipUpdateAction(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

    return buildUpdateAction(
        oldAttributeDefinition.getInputTip(),
        newAttributeDefinition.getInputTip(),
        () ->
            SetInputTip.of(oldAttributeDefinition.getName(), newAttributeDefinition.getInputTip()));
  }

  /**
   * Compares the 'isSearchable' fields of an {@link AttributeDefinition} and an {@link
   * AttributeDefinitionDraft} and returns an {@link UpdateAction}&lt;{@link ProductType}&gt; as a
   * result in an {@link Optional}. If both the {@link AttributeDefinition} and the {@link
   * AttributeDefinitionDraft} have the same 'isSearchable' field, then no update action is needed
   * and hence an empty {@link Optional} is returned.
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
  static Optional<UpdateAction<ProductType>> buildChangeIsSearchableUpdateAction(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

    final Boolean searchable = ofNullable(newAttributeDefinition.isSearchable()).orElse(true);

    return buildUpdateAction(
        oldAttributeDefinition.isSearchable(),
        searchable,
        () -> ChangeIsSearchable.of(oldAttributeDefinition.getName(), searchable));
  }

  /**
   * Compares the input hints of an {@link AttributeDefinition} and an {@link
   * AttributeDefinitionDraft} and returns an {@link UpdateAction}&lt;{@link ProductType}&gt; as a
   * result in an {@link Optional}. If both the {@link AttributeDefinition} and the {@link
   * AttributeDefinitionDraft} have the same input hints, then no update action is needed and hence
   * an empty {@link Optional} is returned.
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
  static Optional<UpdateAction<ProductType>> buildChangeInputHintUpdateAction(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

    final TextInputHint inputHint =
        ofNullable(newAttributeDefinition.getInputHint()).orElse(TextInputHint.SINGLE_LINE);

    return buildUpdateAction(
        oldAttributeDefinition.getInputHint(),
        inputHint,
        () -> ChangeInputHint.of(oldAttributeDefinition.getName(), inputHint));
  }

  /**
   * Compares the attribute constraints of an {@link AttributeDefinition} and an {@link
   * AttributeDefinitionDraft} and returns an {@link UpdateAction}&lt;{@link ProductType}&gt; as a
   * result in an {@link Optional}. If both the {@link AttributeDefinition} and the {@link
   * AttributeDefinitionDraft} have the same attribute constraints, then no update action is needed
   * and hence an empty {@link Optional} is returned.
   *
   * <p>Note: A {@code null} {@code AttributeConstraint} value in the {@link
   * AttributeDefinitionDraft} is treated as a {@code AttributeConstraint#NONE} value which is the
   * default value of CTP.
   *
   * @param oldAttributeDefinition the attribute definition which should be updated.
   * @param newAttributeDefinition the attribute definition draft where we get the new attribute
   *     constraint.
   * @return A filled optional with the update action or an empty optional if the attribute
   *     constraints are identical.
   */
  @Nonnull
  static Optional<UpdateAction<ProductType>> buildChangeAttributeConstraintUpdateAction(
      @Nonnull final AttributeDefinition oldAttributeDefinition,
      @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

    final AttributeConstraint attributeConstraint =
        ofNullable(newAttributeDefinition.getAttributeConstraint())
            .orElse(AttributeConstraint.NONE);

    return buildUpdateAction(
        oldAttributeDefinition.getAttributeConstraint(),
        attributeConstraint,
        () -> ChangeAttributeConstraint.of(oldAttributeDefinition.getName(), attributeConstraint));
  }

  private AttributeDefinitionUpdateActionUtils() {}
}
