package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueLabel;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.util.stream.Collectors.toList;

public final class LocalizedEnumUpdateActionsUtils {
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
    public static List<UpdateAction<ProductType>> buildActions(
        @Nonnull final String attributeDefinitionName,
        @Nonnull final LocalizedEnumValue oldEnumValue,
        @Nonnull final LocalizedEnumValue newEnumValue) {

        return Stream
            .of(
                buildChangeLabelAction(attributeDefinitionName, oldEnumValue, newEnumValue)
            )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList());
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

    private LocalizedEnumUpdateActionsUtils() {
    }
}
