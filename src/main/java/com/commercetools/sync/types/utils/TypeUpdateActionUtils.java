package com.commercetools.sync.types.utils;


import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.types.TypeSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.commands.updateactions.ChangeName;
import io.sphere.sdk.types.commands.updateactions.SetDescription;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.types.utils.FieldDefinitionsUpdateActionUtils.buildFieldDefinitionsUpdateActions;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

public final class TypeUpdateActionUtils {

    /**
     * Compares the {@link LocalizedString} name values of a {@link Type} and a {@link TypeDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeName"}
     * {@link UpdateAction} if values are different.
     *
     * @param oldType the type that should be updated.
     * @param newType the type draft which contains the new name.
     * @return optional containing update action or empty optional if names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Type>> buildChangeNameUpdateAction(
        @Nonnull final Type oldType,
        @Nonnull final TypeDraft newType) {

        return buildUpdateAction(oldType.getName(), newType.getName(),
            () -> ChangeName.of(newType.getName()));
    }


    /**
     * Compares the {@link LocalizedString} descriptions of a {@link Type} and a {@link TypeDraft} and
     * returns an {@link UpdateAction}&lt;{@link Type}&gt; as a result in an {@link Optional}
     * of update action if values are different.
     *
     * @param oldType the type which should be updated.
     * @param newType the type draft where we get the new description.
     * @return A filled optional with the update action or an empty optional if the descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Type>> buildSetDescriptionUpdateAction(
        @Nonnull final Type oldType,
        @Nonnull final TypeDraft newType) {
        return buildUpdateAction(oldType.getDescription(), newType.getDescription(),
            () -> SetDescription.of(newType.getDescription()));
    }

    /**
     * Compares the field definitions of a {@link Type} and a {@link TypeDraft} and returns a list of
     * {@link UpdateAction}&lt;{@link Type}&gt; as a result in an {@link Optional} of update action
     * if values are different.  In case, the new type draft has a list of field definitions in which a
     * duplicate name exists, the error callback is triggered and an empty list is returned.
     *
     * <p>
     *  TODO: Check GITHUB ISSUE#339 for missing FieldDefinition update actions.
     * </p>
     *
     * @param oldType        the type which should be updated.
     * @param newType        the type draft where we get the key.
     * @param syncOptions    responsible for supplying the sync options to the sync utility method.
     *                       It is used for triggering the error callback within the utility, in case of
     *                       errors.
     * @return A list with the update actions or an empty list if the field definitions are identical.
     */
    @Nonnull
    public static List<UpdateAction<Type>> buildFieldDefinitionUpdateActions(
        @Nonnull final Type oldType,
        @Nonnull final TypeDraft newType,
        @Nonnull final TypeSyncOptions syncOptions) {
        try {
            return buildFieldDefinitionsUpdateActions(
                oldType.getFieldDefinitions(),
                newType.getFieldDefinitions()
            );
        } catch (final BuildUpdateActionException exception) {
            syncOptions.applyErrorCallback(format("Failed to build update actions for the field definitions "
                    + "of the type with the key '%s'. Reason: %s", newType.getKey(), exception),
                exception);
            return emptyList();
        }
    }

    private TypeUpdateActionUtils() {
    }
}
