package com.commercetools.sync.producttypes.utils;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueOrder;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveEnumValues;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils.buildActions;

public final class LocalizedEnumValueUpdateActionUtils {

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
        @Nullable final List<LocalizedEnumValue> newEnumValues) {

        return buildActions(
                attributeDefinitionName,
                oldEnumValues,
                newEnumValues,
                RemoveEnumValues::of,
                LocalizedEnumValueUpdateActionUtils::buildLocalizedEnumValueUpdateActions,
                AddLocalizedEnumValue::of,
                ChangeLocalizedEnumValueOrder::of,
                null
        );
    }


    /**
     * Compares all the fields of an old {@link LocalizedEnumValue} and a new {@link LocalizedEnumValue} and returns a
     * list of {@link UpdateAction}&lt;{@link ProductType}&gt; as a result. If both {@link LocalizedEnumValue} have
     * identical fields then no update action is needed and hence an empty {@link List} is returned.
     *
     * @param attributeDefinitionName the attribute definition name whose localized enum values belong to.
     * @param oldEnumValue            the localized enum value which should be updated.
     * @param newEnumValue            the localized enum value where we get the new fields.
     * @return A list with the update actions or an empty list if the localized enum values are
     *         identical.
     */
    @Nonnull
    public static List<UpdateAction<ProductType>> buildLocalizedEnumValueUpdateActions(
            @Nonnull final String attributeDefinitionName,
            @Nonnull final LocalizedEnumValue oldEnumValue,
            @Nonnull final LocalizedEnumValue newEnumValue) {

        return filterEmptyOptionals(
            buildChangeLabelAction(attributeDefinitionName, oldEnumValue, newEnumValue)
        );
    }

    /**
     * Compares the {@code label} values of an old {@link LocalizedEnumValue} and a new {@link LocalizedEnumValue}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeLabel"}
     * {@link UpdateAction}. If both, old and new {@link LocalizedEnumValue} have the same {@code label} values,
     * then no update action is needed and empty optional will be returned.
     *
     * @param attributeDefinitionName the attribute definition name whose localized enum values belong to.
     * @param oldEnumValue            the old localized enum value.
     * @param newEnumValue            the new localized enum value which contains the new description.
     * @return optional containing update action or empty optional if labels
     *         are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ProductType>> buildChangeLabelAction(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final LocalizedEnumValue oldEnumValue,
        @Nonnull final LocalizedEnumValue newEnumValue) {

        return buildUpdateAction(oldEnumValue.getLabel(), newEnumValue.getLabel(),
            () -> ChangeLocalizedEnumValueLabel.of(attributeDefinitionName, newEnumValue));
    }

    private LocalizedEnumValueUpdateActionUtils() {
    }
}
