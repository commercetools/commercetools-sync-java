package com.commercetools.sync.types.utils;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.WithKey;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveEnumValues;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.*;
import static java.util.stream.Collectors.toMap;

public final class TypeUpdateEnumActionsUtils {

    /**
     * Given a list of new {@link EnumValue}s, gets a map where the keys are the enum value key, and the values
     * are the enum instances.
     *
     * @param fieldDefinitionName the field definition name whose the enum values belong to.
     * @param enumValues              the list of enum values.
     * @param <T>                     the enum type of the elements of the list.
     * @return a map with the enum value key as a key of the map, and the enum
     *         value as a value of the map.
     */
    @Nonnull
    public static <T extends WithKey> Map<String, T> getEnumValuesKeyMapWithKeyValidation(
        @Nonnull final String fieldDefinitionName,
        @Nonnull final List<T> enumValues) {

        return enumValues.stream().collect(
            toMap(WithKey::getKey, enumValue -> enumValue,
                (enumValueA, enumValueB) -> {
                    throw new DuplicateKeyException(format("Enum Values have duplicated keys. "
                            + "Field definition name: '%s', Duplicated enum value: '%s'. "
                            + "Enum Values are expected to be unique inside their attribute definition.",
                        fieldDefinitionName, enumValueA.getKey()));
                }
            ));
    }

    /**
     * Checks if there are any old enum values which are not existing in the {@code newEnumValues}.
     * If there are, then "remove" enum values update actions are built.
     * Otherwise, if there are no old enum values, then an empty list is returned.
     *
     * @param attributeDefinitionName the attribute definition name whose enum values belong to.
     * @param oldEnumValues           the list of old enum values.
     * @param newEnumValues           the list of new enum values.
     * @param <T>                     the enum type of the elements of the list.
     * @return a list of enum values update actions if there are old enum value
     *         that should be removed.
     *         Otherwise, if the enum values are identical, an empty optional is returned.
     */
    @Nonnull
    public static <T extends WithKey> Optional<UpdateAction<ProductType>> buildRemoveEnumValuesUpdateActions(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final List<T> oldEnumValues,
        @Nullable final List<T> newEnumValues) {

        final Map<String, T> newEnumValuesKeyMap = getEnumValuesKeyMapWithKeyValidation(
            attributeDefinitionName,
            ofNullable(newEnumValues).orElse(emptyList())
        );

        final List<String> keysToRemove = oldEnumValues
            .stream()
            .map(WithKey::getKey)
            .filter(oldEnumValueKey -> !newEnumValuesKeyMap.containsKey(oldEnumValueKey))
            .collect(Collectors.toList());

        return keysToRemove.isEmpty() ? empty() : of(RemoveEnumValues.of(attributeDefinitionName, keysToRemove));
    }

    /**
     * Compares the order of a list of old enum values and a list of new enum values. If there is a change in order,
     * then a change of enum values order (with the new order) is built. If there are no changes in order an empty
     * optional is returned.
     *
     * @param attributeDefinitionName the attribute definition name whose enum values belong to.
     * @param oldEnumValues           the list of old enum values.
     * @param newEnumValues           the list of new enum values.
     * @param changeOrderEnumCallback the function that is called to apply the change in the order.
     * @param <T>                     the enum type of the elements to change the order for.
     * @return an optional update action if the the order of the enum values is not identical.
     *         Otherwise, if the enum values order is identical, an empty optional is returned.
     */
    @Nonnull
    public static <T extends WithKey> Optional<UpdateAction<Type>> buildChangeEnumValuesOrderUpdateAction(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final List<T> oldEnumValues,
        @Nonnull final List<T> newEnumValues,
        @Nonnull final BiFunction<String, List<String>, UpdateAction<Type>> changeOrderEnumCallback) {

        final List<String> newKeys = newEnumValues
            .stream()
            .map(WithKey::getKey)
            .collect(Collectors.toList());

        final List<String> existingKeys = oldEnumValues
            .stream()
            .map(WithKey::getKey)
            .filter(newKeys::contains)
            .collect(Collectors.toList());

        final List<String> notExistingKeys = newKeys
            .stream()
            .filter(newKey -> !existingKeys.contains(newKey))
            .collect(Collectors.toList());

        final List<String> allKeys = Stream.concat(existingKeys.stream(), notExistingKeys.stream())
                                           .collect(Collectors.toList());

        return buildUpdateAction(
            allKeys,
            newKeys,
            () -> changeOrderEnumCallback.apply(attributeDefinitionName, newKeys)
        );
    }

    /**
     * Checks if there are any new enum values which are not existing in the {@code oldEnumValues}.
     * If there are, then "add" enum values update actions are built.
     * Otherwise, if there are no new enum values, then an empty list is returned.
     *
     * @param attributeDefinitionName the attribute definition name whose enum values belong to.
     * @param oldEnumValues           the list of olf enum values.
     * @param newEnumValues           the list of new enum values.
     * @param addEnumCallback         the function that is called in order to add the new enum instance
     * @param <T>                     the enum type of the element to add.
     * @return a list of enum values update actions if there are new enum value that should be added.
     *         Otherwise, if the enum values are identical, an empty optional is returned.
     */
    @Nonnull
    public static <T extends WithKey> List<UpdateAction<Type>> buildAddEnumValuesUpdateActions(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final List<T> oldEnumValues,
        @Nonnull final List<T> newEnumValues,
        @Nonnull final BiFunction<String, T, UpdateAction<Type>> addEnumCallback) {

        final Map<String, T> oldEnumValuesKeyMap = getEnumValuesKeyMapWithKeyValidation(
            attributeDefinitionName,
            oldEnumValues
        );

        return newEnumValues
            .stream()
            .filter(newEnumValue -> !oldEnumValuesKeyMap.containsKey(newEnumValue.getKey()))
            .map(newEnumValue -> addEnumCallback.apply(attributeDefinitionName, newEnumValue))
            .collect(Collectors.toList());
    }

    private TypeUpdateEnumActionsUtils() {
    }
}
