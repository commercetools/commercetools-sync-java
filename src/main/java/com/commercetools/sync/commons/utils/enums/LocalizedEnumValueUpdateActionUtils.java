package com.commercetools.sync.commons.utils.enums;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedEnumValue;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

public final class LocalizedEnumValueUpdateActionUtils {

    /**
     * Compares the {@code label} values of an old {@link LocalizedEnumValue} and a new {@link LocalizedEnumValue}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeLabel"}
     * {@link UpdateAction}. If both, old and new {@link LocalizedEnumValue} have the same {@code label} values,
     * then no update action is needed and empty optional will be returned.
     *
     * @param attributeDefinitionName the attribute definition name whose localized enum values belong to.
     * @param oldEnumValue            the old localized enum value.
     * @param newEnumValue            the new localized enum value which contains the new description.
     * @param <T>                     the type of the resource in which the update actions will be applied on.
     * @return optional containing update action or empty optional if labels
     *         are identical.
     */
    @Nonnull
    public static <T> Optional<UpdateAction<T>> buildChangeLabelAction(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final LocalizedEnumValue oldEnumValue,
        @Nonnull final LocalizedEnumValue newEnumValue,
        @Nonnull final BiFunction<String, LocalizedEnumValue, UpdateAction<T>> changeLocalizedEnumValueLabelAction) {

        return buildUpdateAction(oldEnumValue.getLabel(), newEnumValue.getLabel(),
            () -> changeLocalizedEnumValueLabelAction.apply(attributeDefinitionName, newEnumValue));
    }

}
