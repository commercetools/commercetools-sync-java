package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.ChangePlainEnumValueLabel;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.util.stream.Collectors.toList;

public final class PlainEnumUpdateActionsUtils {
    /**
     * Compares all the fields of an old {@link EnumValue} and a new {@link EnumValue} and returns a list of
     * {@link UpdateAction}&lt;{@link ProductType}&gt; as a result. If both {@link EnumValue} have identical fields,
     * then no update action is needed and hence an empty {@link List} is returned.
     *
     * @param attributeDefinitionName   the attribute definition name whose plain enum values belong to.
     * @param oldEnumValue              the enum value which should be updated.
     * @param newEnumValue              the enum value where we get the new fields.
     *
     * @return                          A list with the update actions or an empty list if the enum values are
     *                                  identical.
     */
    @Nonnull
    public static List<UpdateAction<ProductType>> buildActions(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final EnumValue oldEnumValue,
        @Nonnull final EnumValue newEnumValue) {

        return Stream.of(
            buildChangeLabelAction(attributeDefinitionName, oldEnumValue, newEnumValue)
        )
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
    }

    /**
     * Compares the {@code label} values of an old {@link EnumValue} and a new {@link EnumValue}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeLabel"}
     * {@link UpdateAction}. If both, old and new {@link EnumValue} have the same {@code label} values,
     * then no update action is needed and empty optional will be returned.
     *
     * @param attributeDefinitionName   the attribute definition name whose plain enum values belong to.
     * @param oldEnumValue              the old plain enum value.
     * @param newEnumValue              the new plain enum value which contains the new description.
     *
     * @return                          optional containing update action or empty optional if labels
     *                                  are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ProductType>> buildChangeLabelAction(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final EnumValue oldEnumValue,
        @Nonnull final EnumValue newEnumValue) {

        return buildUpdateAction(oldEnumValue.getLabel(), newEnumValue.getLabel(),
            () -> ChangePlainEnumValueLabel.of(attributeDefinitionName, newEnumValue));
    }

    private PlainEnumUpdateActionsUtils() { }
}
