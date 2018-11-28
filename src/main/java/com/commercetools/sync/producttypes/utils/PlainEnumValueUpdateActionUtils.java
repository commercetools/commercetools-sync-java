package com.commercetools.sync.producttypes.utils;

import com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeEnumValueOrder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangePlainEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveEnumValues;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;

public final class PlainEnumValueUpdateActionUtils {

    /**
     * Compares a list of old {@link EnumValue}s with a list of new {@link EnumValue}s for a given
     * attribute definition and builds required update actions (e.g addEnumValue, removeEnumValue,
     * changeEnumValueOrder) and 1-1 update actions on enum values (e.g. changeEnumValueLabel) for the required
     * resource. If both the {@link EnumValue}'s are identical, then no update action is needed and hence
     * an empty {@link List} is returned.
     *
     * <p>If the list of new {@link EnumValue}s is {@code null}, then remove actions are built for
     * every existing plain enum value in the {@code oldEnumValues} list.
     *
     * @param attributeDefinitionName the attribute name whose plain enum values are going to be synced.
     * @param oldEnumValues           the old list of plain enum values.
     * @param newEnumValues           the new list of plain enum values.
     * @return a list of plain enum values update actions if the list of plain enum values is not identical.
     *         Otherwise, if the plain enum values are identical, an empty list is returned.
     */
    @Nonnull
    public static List<UpdateAction<ProductType>> buildEnumValuesUpdateActions(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final List<EnumValue> oldEnumValues,
        @Nullable final List<EnumValue> newEnumValues) {

        return EnumValuesUpdateActionUtils.buildActions(attributeDefinitionName,
                oldEnumValues,
                newEnumValues,
                RemoveEnumValues::of,
                PlainEnumValueUpdateActionUtils::buildEnumValueUpdateActions,
                AddEnumValue::of,
                ChangeEnumValueOrder::of,
                null);
    }

    /**
     * Compares all the fields of an old {@link EnumValue} and a new {@link EnumValue} and returns a list of
     * {@link UpdateAction}&lt;{@link ProductType}&gt; as a result. If both {@link EnumValue} have identical fields,
     * then no update action is needed and hence an empty {@link List} is returned.
     *
     * @param attributeDefinitionName the attribute definition name whose plain enum values belong to.
     * @param oldEnumValue            the enum value which should be updated.
     * @param newEnumValue            the enum value where we get the new fields.
     * @return A list with the update actions or an empty list if the enum values are
     *         identical.
     */
    @Nonnull
    public static List<UpdateAction<ProductType>> buildEnumValueUpdateActions(
            @Nonnull final String attributeDefinitionName,
            @Nonnull final EnumValue oldEnumValue,
            @Nonnull final EnumValue newEnumValue) {

        return filterEmptyOptionals(
                buildChangeLabelAction(attributeDefinitionName, oldEnumValue, newEnumValue)
        );
    }

    /**
     * Compares the {@code label} values of an old {@link EnumValue} and a new {@link EnumValue}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeLabel"}
     * {@link UpdateAction}. If both, old and new {@link EnumValue} have the same {@code label} values,
     * then no update action is needed and empty optional will be returned.
     *
     * @param attributeDefinitionName the attribute definition name whose plain enum values belong to.
     * @param oldEnumValue            the old plain enum value.
     * @param newEnumValue            the new plain enum value which contains the new description.
     * @return optional containing update action or empty optional if labels
     *         are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ProductType>> buildChangeLabelAction(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final EnumValue oldEnumValue,
        @Nonnull final EnumValue newEnumValue) {

        return buildUpdateAction(oldEnumValue.getLabel(), newEnumValue.getLabel(),
            () -> ChangePlainEnumValueLabel.of(attributeDefinitionName, newEnumValue));
    }


    private PlainEnumValueUpdateActionUtils() {
    }
}
