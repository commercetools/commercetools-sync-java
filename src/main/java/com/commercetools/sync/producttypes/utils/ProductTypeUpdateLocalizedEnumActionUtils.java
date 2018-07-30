package com.commercetools.sync.producttypes.utils;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueOrder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateEnumActionsUtils.buildRemoveEnumValuesUpdateActions;
import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateEnumActionsUtils.buildMatchingEnumValuesUpdateActions;
import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateEnumActionsUtils.buildAddEnumValuesUpdateActions;
import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateEnumActionsUtils.buildChangeEnumValuesOrderUpdateAction;
import static java.util.Collections.emptyList;

public final class ProductTypeUpdateLocalizedEnumActionUtils {
    /**
     * Compares a list of old {@link LocalizedEnumValue}s with a list of new {@link LocalizedEnumValue}s for a given
     * attribute definition.
     * The method serves as a generic implementation for localized enum values syncing. The method takes in functions
     * for building the required update actions (AddLocalizedEnumValue, RemoveEnumValue, ChangeLocalizedEnumValueOrder
     * and 1-1 update actions on localized enum values (e.g. changeLabel) for the required resource.
     *
     * <p>If the list of new {@link LocalizedEnumValue}s is {@code null}, then remove actions are built for
     * every existing localized enum value in the {@code oldEnumValues} list.
     *
     * @param attributeDefinitionName the attribute name whose localized enum values are going to be synced.
     * @param oldEnumValues           the old list of localized enum values.
     * @param newEnumValues           the new list of localized enum values.
     * @return a list of localized enum values update actions if the list of localized enum values is not identical.
     *         Otherwise, if the localized enum values are identical, an empty list is returned.
     * @throws DuplicateKeyException in case there are localized enum values with duplicate keys.
     */
    @Nonnull
    public static List<UpdateAction<ProductType>> buildLocalizedEnumValuesUpdateActions(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final List<LocalizedEnumValue> oldEnumValues,
        @Nullable final List<LocalizedEnumValue> newEnumValues)
        throws DuplicateKeyException {

        if (newEnumValues != null && !newEnumValues.isEmpty()) {
            return buildUpdateActions(
                attributeDefinitionName,
                oldEnumValues,
                newEnumValues
            );
        } else {
            return buildRemoveEnumValuesUpdateActions(
                attributeDefinitionName,
                oldEnumValues,
                newEnumValues
            )
                .map(Collections::singletonList)
                .orElse(emptyList());
        }
    }

    /**
     * Compares a list of old {@link LocalizedEnumValue}s with a list of new {@link LocalizedEnumValue}s for a given
     * attribute definition.
     * The method serves as a generic implementation for localized enum values syncing. The method takes in functions
     * for building the required update actions (AddEnumValue, RemoveEnumValue, ChangeEnumValueOrder and 1-1
     * update actions on localized enum values (e.g. changeLabel) for the required resource.
     *
     * @param attributeDefinitionName the attribute name whose localized enum values are going to be synced.
     * @param oldEnumValues           the old list of localized enum values.
     * @param newEnumValues           the new list of localized enum values.
     * @return a list of localized enum values update actions if the list of localized enum values is not identical.
     *         Otherwise, if the localized enum values are identical, an empty list is returned.
     * @throws DuplicateKeyException in case there are localized enum values with duplicate keys.
     */
    @Nonnull
    private static List<UpdateAction<ProductType>> buildUpdateActions(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final List<LocalizedEnumValue> oldEnumValues,
        @Nonnull final List<LocalizedEnumValue> newEnumValues)
        throws DuplicateKeyException {

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
                LocalizedEnumUpdateActionsUtils::buildActions
            );

        final List<UpdateAction<ProductType>> addEnumValuesUpdateActions = buildAddEnumValuesUpdateActions(
            attributeDefinitionName,
            oldEnumValues,
            newEnumValues,
            AddLocalizedEnumValue::of
        );

        final List<UpdateAction<ProductType>> changeEnumValuesOrderUpdateActions =
            buildChangeEnumValuesOrderUpdateAction(
                attributeDefinitionName,
                oldEnumValues,
                newEnumValues,
                ChangeLocalizedEnumValueOrder::of
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

    private ProductTypeUpdateLocalizedEnumActionUtils() {
    }
}
