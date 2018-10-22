package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeEnumValueOrder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.producttypes.utils.EnumsUpdateActionUtils.buildAddEnumValuesUpdateActions;
import static com.commercetools.sync.producttypes.utils.EnumsUpdateActionUtils.buildChangeEnumValuesOrderUpdateAction;
import static com.commercetools.sync.producttypes.utils.EnumsUpdateActionUtils.buildMatchingEnumValuesUpdateActions;
import static com.commercetools.sync.producttypes.utils.EnumsUpdateActionUtils.buildRemoveEnumValuesUpdateActions;
import static java.util.Collections.emptyList;

public final class PlainEnumsUpdateActionUtils {
    /**
     * Compares a list of old {@link EnumValue}s with a list of new {@link EnumValue}s for a given
     * attribute definition.
     * The method serves as a generic implementation for plain enum values syncing. The method takes in functions
     * for building the required update actions (AddEnumValue, RemoveEnumValue, ChangeEnumValueOrder
     * and 1-1 update actions on plain enum values (e.g. changeLabel) for the required resource.
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

        if (newEnumValues != null && !newEnumValues.isEmpty()) {
            return buildUpdateActions(attributeDefinitionName, oldEnumValues, newEnumValues);
        } else {
            return buildRemoveEnumValuesUpdateActions(attributeDefinitionName, oldEnumValues, newEnumValues)
                .map(Collections::singletonList)
                .orElse(emptyList());
        }
    }


    /**
     * Compares a list of old {@link EnumValue}s with a list of new {@link EnumValue}s for a given attribute
     * definition.
     * The method serves as an implementation for plain enum values syncing. The method takes in functions
     * for building the required update actions (AddEnumValue, RemoveEnumValue, ChangeEnumValueOrder and 1-1
     * update actions on plain enum values (e.g. changeLabel) for the required resource.
     *
     * @param attributeDefinitionName the attribute name whose plain enum values are going to be synced.
     * @param oldEnumValues           the old list of plain enum values.
     * @param newEnumValues           the new list of plain enum values.
     * @return a list of plain enum values update actions if the list of plain enum values is not identical.
     *         Otherwise, if the plain enum values are identical, an empty list is returned.
     */
    @Nonnull
    private static List<UpdateAction<ProductType>> buildUpdateActions(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final List<EnumValue> oldEnumValues,
        @Nonnull final List<EnumValue> newEnumValues) {

        final List<UpdateAction<ProductType>> removeEnumValuesUpdateActions = buildRemoveEnumValuesUpdateActions(
            attributeDefinitionName,
            oldEnumValues,
            newEnumValues
        )
            .map(Collections::singletonList)
            .orElse(emptyList());

        final List<UpdateAction<ProductType>> matchingEnumValuesUpdateActions =
            buildMatchingEnumValuesUpdateActions(
                attributeDefinitionName,
                oldEnumValues,
                newEnumValues,
                PlainEnumUpdateActionUtils::buildActions
            );

        final List<UpdateAction<ProductType>> addEnumValuesUpdateActions = buildAddEnumValuesUpdateActions(
            attributeDefinitionName,
            oldEnumValues,
            newEnumValues,
            AddEnumValue::of
        );

        final List<UpdateAction<ProductType>> changeEnumValuesOrderUpdateActions =
            buildChangeEnumValuesOrderUpdateAction(
                attributeDefinitionName,
                oldEnumValues,
                newEnumValues,
                ChangeEnumValueOrder::of
            )
                .map(Collections::singletonList)
                .orElse(emptyList());

        return Stream.concat(
            Stream.concat(
                removeEnumValuesUpdateActions.stream(),
                matchingEnumValuesUpdateActions.stream()
            ),
            Stream.concat(
                addEnumValuesUpdateActions.stream(),
                changeEnumValuesOrderUpdateActions.stream()
            )

        ).collect(Collectors.toList());
    }


    private PlainEnumsUpdateActionUtils() {
    }
}
