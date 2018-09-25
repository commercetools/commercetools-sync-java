package com.commercetools.sync.types.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.AddEnumValue;
import io.sphere.sdk.types.commands.updateactions.ChangeEnumValueOrder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.types.utils.TypeUpdateEnumActionsUtils.buildAddEnumValuesUpdateActions;
import static com.commercetools.sync.types.utils.TypeUpdateEnumActionsUtils.buildChangeEnumValuesOrderUpdateAction;
import static java.util.Collections.emptyList;

public final class TypeUpdatePlainEnumActionUtils {
    /**
     * Compares a list of old {@link EnumValue}s with a list of new {@link EnumValue}s for a given
     * field definition.
     * The method serves as a generic implementation for plain enum values syncing. The method takes in functions
     * for building the required update actions (AddEnumValue, ChangeEnumValueOrder
     * and 1-1 update actions on plain enum values (e.g. changeLabel) for the required resource.
     *
     * <p>If the list of new {@link EnumValue}s is {@code null}, then remove actions are built for
     * every existing plain enum value in the {@code oldEnumValues} list.
     *
     * @param fieldDefinitionName     the field name whose plain enum values are going to be synced.
     * @param oldEnumValues           the old list of plain enum values.
     * @param newEnumValues           the new list of plain enum values.
     * @return a list of plain enum values update actions if the list of plain enum values is not identical.
     *         Otherwise, if the plain enum values are identical, an empty list is returned.
     */
    @Nonnull
    public static List<UpdateAction<Type>> buildEnumValuesUpdateActions(
        @Nonnull final String fieldDefinitionName,
        @Nonnull final List<EnumValue> oldEnumValues,
        @Nullable final List<EnumValue> newEnumValues) {

        if (newEnumValues != null && !newEnumValues.isEmpty()) {
            return buildUpdateActions(fieldDefinitionName, oldEnumValues, newEnumValues);
        }

        return emptyList();
    }


    @Nonnull
    private static List<UpdateAction<Type>> buildUpdateActions(
        @Nonnull final String fieldDefinitionName,
        @Nonnull final List<EnumValue> oldEnumValues,
        @Nonnull final List<EnumValue> newEnumValues) {


        final List<UpdateAction<Type>> addEnumValuesUpdateActions = buildAddEnumValuesUpdateActions(
            fieldDefinitionName,
            oldEnumValues,
            newEnumValues,
            AddEnumValue::of
        );

        final List<UpdateAction<Type>> changeEnumValuesOrderUpdateActions =
            buildChangeEnumValuesOrderUpdateAction(
                fieldDefinitionName,
                oldEnumValues,
                newEnumValues,
                ChangeEnumValueOrder::of
            )
            .map(Collections::singletonList)
            .orElse(emptyList());

        return Stream.concat(
                    addEnumValuesUpdateActions.stream(),
                    changeEnumValuesOrderUpdateActions.stream())
                .collect(Collectors.toList());
    }


    private TypeUpdatePlainEnumActionUtils() {
    }
}
