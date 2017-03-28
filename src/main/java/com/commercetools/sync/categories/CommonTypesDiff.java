package com.commercetools.sync.categories;


import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class CommonTypesDiff {

    /**
     * Compares two {@link LocalizedString} and returns a supplied {@link UpdateAction < Category >} as a result in an
     * {@link Optional}. If no update action is needed, for example in case where both the {@link LocalizedString}
     * have the same fields and values, an empty {@link Optional} is returned.
     *
     * @param existingLocalisedString the localised string which should be updated.
     * @param newLocalisedString      the localised string with the new information.
     * @param updateAction            the update action to return in the optional.
     * @return A filled optional with the update action or an empty optional if the localised string values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForLocalizedStrings(
            @Nullable final LocalizedString existingLocalisedString,
            @Nullable final LocalizedString newLocalisedString,
            @Nonnull final UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingLocalisedString, newLocalisedString, updateAction);
    }

    /**
     * Compares two {@link Reference <Category>} and returns a supplied {@link UpdateAction<Category>} as a result in an
     * {@link Optional}. If no update action is needed, for example in case where both the {@link Reference<Category>}
     * have the same values, an empty {@link Optional} is returned.
     *
     * @param existingCategoryReference the category reference which should be updated.
     * @param newCategoryReference      the category reference with the new information.
     * @param updateAction              the update action to return in the optional.
     * @return A filled optional with the update action or an empty optional if the category reference
     * values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForReferences(
            @Nullable final Reference<Category> existingCategoryReference,
            @Nullable final Reference<Category> newCategoryReference,
            @Nonnull final UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingCategoryReference, newCategoryReference, updateAction);
    }

    /**
     * Compares two {@link String} and returns a supplied {@link UpdateAction<Category>} as a result in an
     * {@link Optional}. If no update action is needed, for example in case where both the {@link String}
     * have the same values, an empty {@link Optional} is returned.
     *
     * @param existingString the string which should be updated.
     * @param newString      the string with the new information.
     * @param updateAction   the update action to return in the optional.
     * @return A filled optional with the update action or an empty optional if the string values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForStrings(@Nullable final String existingString,
                                                                        @Nullable final String newString,
                                                                        @Nonnull final UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingString, newString, updateAction);
    }

    /**
     * Compares two {@link Object} and returns a supplied {@link UpdateAction<Category>} as a result in an
     * {@link Optional}. If no update action is needed, for example in case where both the {@link Object}
     * have the same values, an empty {@link Optional} is returned.
     *
     * @param existingObject the object which should be updated.
     * @param newObject      the object with the new information.
     * @param updateAction   the update action to return in the optional.
     * @return A filled optional with the update action or an empty optional if the object values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForObjects(@Nullable final Object existingObject,
                                                                        @Nullable final Object newObject,
                                                                        @Nonnull final UpdateAction<Category> updateAction) {
        return !Objects.equals(existingObject, newObject) ? Optional.of(updateAction) : Optional.empty();
    }
}
