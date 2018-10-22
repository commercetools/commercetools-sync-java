package com.commercetools.sync.commons.utils.enums;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

public final class PlainEnumValueUpdateActionUtils {

    /**
     * Compares the {@code label} values of an old {@link EnumValue} and a new {@link EnumValue}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeLabel"}
     * {@link UpdateAction}. If both, old and new {@link EnumValue} have the same {@code label} values,
     * then no update action is needed and empty optional will be returned.
     *
     * @param attributeDefinitionName the attribute definition name whose plain enum values belong to.
     * @param oldEnumValue            the old plain enum value.
     * @param newEnumValue            the new plain enum value which contains the new description.
     * @param <T>                     the type of the resource in which the update actions will be applied on.
     * @return optional containing update action or empty optional if labels
     *         are identical.
     */
    @Nonnull
    public static <T> Optional<UpdateAction<T>> buildChangeLabelAction(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final EnumValue oldEnumValue,
        @Nonnull final EnumValue newEnumValue,
        @Nonnull final BiFunction<String, EnumValue, UpdateAction<T>> changePlainEnumValueLabelAction) {

        return buildUpdateAction(oldEnumValue.getLabel(), newEnumValue.getLabel(),
            () -> changePlainEnumValueLabelAction.apply(attributeDefinitionName, newEnumValue));
    }

}
