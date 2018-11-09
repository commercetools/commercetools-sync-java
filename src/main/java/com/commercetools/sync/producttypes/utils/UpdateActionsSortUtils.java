package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddAttributeDefinition;
import io.sphere.sdk.producttypes.commands.updateactions.AddEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeConstraint;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeDefinitionLabel;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeOrder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeEnumValueOrder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeInputHint;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeIsSearchable;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueOrder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangePlainEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveAttributeDefinition;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveEnumValues;
import io.sphere.sdk.producttypes.commands.updateactions.SetInputTip;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

final class UpdateActionsSortUtils {
    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link RemoveAttributeDefinition}</li>
     * <li>{@link ChangeAttributeDefinitionLabel} OR {@link SetInputTip} OR {@link ChangeIsSearchable} OR
     * {@link ChangeInputHint} OR {@link ChangeAttributeConstraint} OR
     * {@link RemoveEnumValues} -> ({@link ChangeLocalizedEnumValueLabel} -> {@link AddLocalizedEnumValue} ->
     * {@link ChangeLocalizedEnumValueOrder} OR {@link ChangePlainEnumValueLabel} -> {@link AddEnumValue}
     * -> {@link ChangeEnumValueOrder}</li>
     * <li>{@link AddAttributeDefinition}</li>
     * <li>{@link ChangeAttributeOrder}</li>
     * </ol>
     *
     *
     * <p>This is to ensure that there are no conflicts when adding a new attribute definitions.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    static List<UpdateAction<ProductType>> sortAttributeDefinitionActions(
        @Nonnull final List<UpdateAction<ProductType>> updateActions) {

        final List<UpdateAction<ProductType>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {

            if (action1 instanceof RemoveAttributeDefinition && !(action2 instanceof RemoveAttributeDefinition)) {
                return -1;
            }

            if (!(action1 instanceof RemoveAttributeDefinition) && action2 instanceof RemoveAttributeDefinition) {
                return 1;
            }

            if (!(action1 instanceof ChangeAttributeOrder) && action2 instanceof ChangeAttributeOrder) {
                return -1;
            }

            if (action1 instanceof ChangeAttributeOrder && !(action2 instanceof ChangeAttributeOrder)) {
                return 1;
            }

            if (!(action1 instanceof AddAttributeDefinition) && action2 instanceof AddAttributeDefinition) {
                return -1;
            }

            if (action1 instanceof AddAttributeDefinition && !(action2 instanceof AddAttributeDefinition)) {
                return 1;
            }

            return 0;
        });
        return actionsCopy;
    }

    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link RemoveEnumValues}</li>
     * <li>{@link ChangePlainEnumValueLabel}</li>
     * <li>{@link AddEnumValue}</li>
     * <li>{@link ChangeEnumValueOrder}</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when adding a new enum value.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    static List<UpdateAction<ProductType>> sortEnumActions(
        @Nonnull final List<UpdateAction<ProductType>> updateActions) {

        final List<UpdateAction<ProductType>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {

            if (action1 instanceof RemoveEnumValues && !(action2 instanceof RemoveEnumValues)) {
                return -1;
            }

            if (!(action1 instanceof RemoveEnumValues) && action2 instanceof RemoveEnumValues) {
                return 1;
            }

            if (!(action1 instanceof ChangeEnumValueOrder) && action2 instanceof ChangeEnumValueOrder) {
                return -1;
            }

            if (action1 instanceof ChangeEnumValueOrder && !(action2 instanceof ChangeEnumValueOrder)) {
                return 1;
            }

            if (!(action1 instanceof AddEnumValue) && action2 instanceof AddEnumValue) {
                return -1;
            }

            if (action1 instanceof AddEnumValue && !(action2 instanceof AddEnumValue)) {
                return 1;
            }
            return 0;
        });
        return actionsCopy;
    }

    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link RemoveEnumValues}</li>
     * <li>{@link ChangeLocalizedEnumValueLabel}</li>
     * <li>{@link AddLocalizedEnumValue}</li>
     * <li>{@link ChangeLocalizedEnumValueOrder}</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when adding a new enum value.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    static List<UpdateAction<ProductType>> sortLocalizedEnumActions(
        @Nonnull final List<UpdateAction<ProductType>> updateActions) {

        final List<UpdateAction<ProductType>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {

            if (action1 instanceof RemoveEnumValues && !(action2 instanceof RemoveEnumValues)) {
                return -1;
            }

            if (!(action1 instanceof RemoveEnumValues) && action2 instanceof RemoveEnumValues) {
                return 1;
            }

            if (!(action1 instanceof ChangeLocalizedEnumValueOrder)
                && action2 instanceof ChangeLocalizedEnumValueOrder) {
                return -1;
            }

            if (action1 instanceof ChangeLocalizedEnumValueOrder
                && !(action2 instanceof ChangeLocalizedEnumValueOrder)) {
                return 1;
            }

            if (!(action1 instanceof AddLocalizedEnumValue)
                && action2 instanceof AddLocalizedEnumValue) {
                return -1;
            }

            if (action1 instanceof AddLocalizedEnumValue
                && !(action2 instanceof AddLocalizedEnumValue)) {
                return 1;
            }
            return 0;
        });
        return actionsCopy;
    }

    private UpdateActionsSortUtils() {
    }
}
