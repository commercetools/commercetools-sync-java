package com.commercetools.sync.types.utils;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.types.commands.updateactions.ChangeLocalizedEnumValueOrder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.types.utils.EnumUpdateActionsUtils.buildAddEnumValuesUpdateActions;
import static com.commercetools.sync.types.utils.EnumUpdateActionsUtils.buildChangeEnumValuesOrderUpdateAction;
import static java.util.Collections.emptyList;

public final class LocalizedEnumUpdateActionUtils {
    /**
     * Compares a list of old {@link LocalizedEnumValue}s with a list of new {@link LocalizedEnumValue}s for a given
     * field definition.
     * The method serves as a generic implementation for localized enum values syncing. The method takes in functions
     * for building the required update actions (AddLocalizedEnumValue, RemoveEnumValue, ChangeLocalizedEnumValueOrder
     * and 1-1 update actions on localized enum values (e.g. changeLabel) for the required resource.
     *
     * <p>If the list of new {@link LocalizedEnumValue}s is {@code null}, then remove actions are built for
     * every existing localized enum value in the {@code oldEnumValues} list.
     *
     * @param fieldDefinitionName     the field name whose localized enum values are going to be synced.
     * @param oldEnumValues           the old list of localized enum values.
     * @param newEnumValues           the new list of localized enum values.
     * @return a list of localized enum values update actions if the list of localized enum values is not identical.
     *         Otherwise, if the localized enum values are identical, an empty list is returned.
     * @throws DuplicateKeyException in case there are localized enum values with duplicate keys.
     */
    @Nonnull
    public static List<UpdateAction<Type>> buildLocalizedEnumValuesUpdateActions(
        @Nonnull final String fieldDefinitionName,
        @Nonnull final List<LocalizedEnumValue> oldEnumValues,
        @Nullable final List<LocalizedEnumValue> newEnumValues) {

        if (newEnumValues != null && !newEnumValues.isEmpty()) {
            return buildUpdateActions(
                fieldDefinitionName,
                oldEnumValues,
                newEnumValues
            );
        }

        return emptyList();
    }


    @Nonnull
    private static List<UpdateAction<Type>> buildUpdateActions(
        @Nonnull final String fieldDefinitionName,
        @Nonnull final List<LocalizedEnumValue> oldEnumValues,
        @Nonnull final List<LocalizedEnumValue> newEnumValues) {


        final List<UpdateAction<Type>> addEnumValuesUpdateActions = buildAddEnumValuesUpdateActions(
            fieldDefinitionName,
            oldEnumValues,
            newEnumValues,
            AddLocalizedEnumValue::of
        );

        final List<UpdateAction<Type>> changeEnumValuesOrderUpdateActions =
            buildChangeEnumValuesOrderUpdateAction(
                fieldDefinitionName,
                oldEnumValues,
                newEnumValues,
                ChangeLocalizedEnumValueOrder::of
            )
                .map(Collections::singletonList)
                .orElse(emptyList());

        return Stream.concat(

            addEnumValuesUpdateActions.stream(),
            changeEnumValuesOrderUpdateActions.stream()

        ).collect(Collectors.toList());
    }

    private LocalizedEnumUpdateActionUtils() {
    }
}
