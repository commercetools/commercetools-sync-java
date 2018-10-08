package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.EnumAttributeType;
import io.sphere.sdk.products.attributes.LocalizedEnumAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeConstraint;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeDefinitionLabel;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeInputHint;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeIsSearchable;
import io.sphere.sdk.producttypes.commands.updateactions.SetInputTip;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateLocalizedEnumActionUtils.buildLocalizedEnumValuesUpdateActions;
import static com.commercetools.sync.producttypes.utils.ProductTypeUpdatePlainEnumActionUtils.buildEnumValuesUpdateActions;


public final class AttributeDefinitionUpdateActionUtils {
    /**
     * Compares all the fields of an {@link AttributeDefinition} and an {@link AttributeDefinitionDraft} and returns
     * a list of {@link UpdateAction}&lt;{@link ProductType}&gt; as a result. If both the {@link AttributeDefinition}
     * and the {@link AttributeDefinitionDraft} have identical fields, then no update action is needed and hence an
     * empty {@link List} is returned.
     *
     * @param oldAttributeDefinition      the old attribute definition which should be updated.
     * @param newAttributeDefinitionDraft the new attribute definition draft where we get the new fields.
     * @return A list with the update actions or an empty list if the attribute definition fields are identical.
     */
    @Nonnull
    public static List<UpdateAction<ProductType>> buildActions(
        @Nonnull final AttributeDefinition oldAttributeDefinition,
        @Nonnull final AttributeDefinitionDraft newAttributeDefinitionDraft) {

        final List<UpdateAction<ProductType>> updateActions;

        updateActions = filterEmptyOptionals(
            buildChangeLabelUpdateAction(oldAttributeDefinition, newAttributeDefinitionDraft),
            buildSetInputTipUpdateAction(oldAttributeDefinition, newAttributeDefinitionDraft),
            buildChangeIsSearchableUpdateAction(oldAttributeDefinition, newAttributeDefinitionDraft),
            buildChangeInputHintUpdateAction(oldAttributeDefinition, newAttributeDefinitionDraft),
            buildChangeAttributeConstraintUpdateAction(oldAttributeDefinition, newAttributeDefinitionDraft)
        );


        if (isPlainEnumAttribute(oldAttributeDefinition)) {
            updateActions.addAll(buildEnumValuesUpdateActions(
                oldAttributeDefinition.getName(),
                ((EnumAttributeType) oldAttributeDefinition.getAttributeType()).getValues(),
                ((EnumAttributeType) newAttributeDefinitionDraft.getAttributeType()).getValues()
            ));
        } else if (isLocalizedEnumAttribute(oldAttributeDefinition)) {
            updateActions.addAll(buildLocalizedEnumValuesUpdateActions(
                oldAttributeDefinition.getName(),
                ((LocalizedEnumAttributeType) oldAttributeDefinition.getAttributeType()).getValues(),
                ((LocalizedEnumAttributeType) newAttributeDefinitionDraft.getAttributeType()).getValues()
            ));
        }

        return updateActions;
    }

    /**
     * Indicates if the attribute definition is a plain enum value or not.
     *
     * @param attributeDefinition the attribute definition.
     * @return true if the attribute definition is a plain enum value, false otherwise.
     */
    private static boolean isPlainEnumAttribute(@Nonnull final AttributeDefinition attributeDefinition) {
        return attributeDefinition.getAttributeType().getClass() == EnumAttributeType.class;
    }

    /**
     * Indicates if the attribute definition is a localized enum value or not.
     *
     * @param attributeDefinition the attribute definition.
     * @return true if the attribute definition is a localized enum value, false otherwise.
     */
    private static boolean isLocalizedEnumAttribute(@Nonnull final AttributeDefinition attributeDefinition) {
        return attributeDefinition.getAttributeType().getClass() == LocalizedEnumAttributeType.class;
    }

    /**
     * Compares the {@link LocalizedString} labels of an {@link AttributeDefinition} and an
     * {@link AttributeDefinitionDraft} and returns an {@link UpdateAction}&lt;{@link ProductType}&gt; as a result in
     * an {@link Optional}. If both the {@link AttributeDefinition} and the {@link AttributeDefinitionDraft} have the
     * same label, then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldAttributeDefinition the attribute definition which should be updated.
     * @param newAttributeDefinition the attribute definition draft where we get the new label.
     * @return A filled optional with the update action or an empty optional if the labels are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ProductType>> buildChangeLabelUpdateAction(
        @Nonnull final AttributeDefinition oldAttributeDefinition,
        @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

        return buildUpdateAction(oldAttributeDefinition.getLabel(), newAttributeDefinition.getLabel(),
            () -> ChangeAttributeDefinitionLabel.of(oldAttributeDefinition.getName(),
                newAttributeDefinition.getLabel())
        );
    }

    /**
     * Compares the {@link LocalizedString} input tips of an {@link AttributeDefinition} and an
     * {@link AttributeDefinitionDraft} and returns an {@link UpdateAction}&lt;{@link ProductType}&gt; as a result in
     * an {@link Optional}. If both the {@link AttributeDefinition} and the {@link AttributeDefinitionDraft} have the
     * same input tip, then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldAttributeDefinition the attribute definition which should be updated.
     * @param newAttributeDefinition the attribute definition draft where we get the new input tip.
     * @return A filled optional with the update action or an empty optional if the labels are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ProductType>> buildSetInputTipUpdateAction(
        @Nonnull final AttributeDefinition oldAttributeDefinition,
        @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

        return buildUpdateAction(oldAttributeDefinition.getInputTip(), newAttributeDefinition.getInputTip(),
            () -> SetInputTip.of(oldAttributeDefinition.getName(), newAttributeDefinition.getInputTip())
        );
    }

    /**
     * Compares the 'isSearchable' fields of an {@link AttributeDefinition} and an
     * {@link AttributeDefinitionDraft} and returns an {@link UpdateAction}&lt;{@link ProductType}&gt; as a result in
     * an {@link Optional}. If both the {@link AttributeDefinition} and the {@link AttributeDefinitionDraft} have the
     * same 'isSearchable' field, then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldAttributeDefinition the attribute definition which should be updated.
     * @param newAttributeDefinition the attribute definition draft where we get the new 'isSearchable' field.
     * @return A filled optional with the update action or an empty optional if the 'isSearchable' fields are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ProductType>> buildChangeIsSearchableUpdateAction(
        @Nonnull final AttributeDefinition oldAttributeDefinition,
        @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

        return buildUpdateAction(oldAttributeDefinition.isSearchable(), newAttributeDefinition.isSearchable(),
            () -> ChangeIsSearchable.of(oldAttributeDefinition.getName(), newAttributeDefinition.isSearchable())
        );
    }

    /**
     * Compares the input hints of an {@link AttributeDefinition} and an {@link AttributeDefinitionDraft} and returns
     * an {@link UpdateAction}&lt;{@link ProductType}&gt; as a result in an {@link Optional}. If both the
     * {@link AttributeDefinition} and the {@link AttributeDefinitionDraft} have the same input hints, then no update
     * action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldAttributeDefinition the attribute definition which should be updated.
     * @param newAttributeDefinition the attribute definition draft where we get the new input hint.
     * @return A filled optional with the update action or an empty optional if the input hints are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ProductType>> buildChangeInputHintUpdateAction(
        @Nonnull final AttributeDefinition oldAttributeDefinition,
        @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

        return buildUpdateAction(oldAttributeDefinition.getInputHint(), newAttributeDefinition.getInputHint(),
            () -> ChangeInputHint.of(oldAttributeDefinition.getName(), newAttributeDefinition.getInputHint())
        );
    }

    /**
     * Compares the attribute constraints of an {@link AttributeDefinition} and an {@link AttributeDefinitionDraft}
     * and returns an {@link UpdateAction}&lt;{@link ProductType}&gt; as a result in an {@link Optional}. If both the
     * {@link AttributeDefinition} and the {@link AttributeDefinitionDraft} have the same attribute constraints, then
     * no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldAttributeDefinition the attribute definition which should be updated.
     * @param newAttributeDefinition the attribute definition draft where we get the new attribute constraint.
     * @return A filled optional with the update action or an empty optional if the attribute constraints are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ProductType>> buildChangeAttributeConstraintUpdateAction(
        @Nonnull final AttributeDefinition oldAttributeDefinition,
        @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {

        return buildUpdateAction(oldAttributeDefinition.getAttributeConstraint(),
            newAttributeDefinition.getAttributeConstraint(),
            () -> ChangeAttributeConstraint.of(oldAttributeDefinition.getName(),
                newAttributeDefinition.getAttributeConstraint())
        );
    }

    private AttributeDefinitionUpdateActionUtils() {
    }
}
