package com.commercetools.sync.types.utils;

import com.commercetools.sync.types.TypeSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildFieldDefinitionUpdateActions;
import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildSetDescriptionUpdateAction;
import static java.util.stream.Collectors.toList;

public final class TypeSyncUtils {

    /**
     * Compares all the fields (including the field definitions see
     * {@link TypeUpdateActionUtils#buildFieldDefinitionUpdateActions(Type, TypeDraft, TypeSyncOptions)})
     * of a {@link Type} and a {@link TypeDraft}.
     * It returns a {@link List} of {@link UpdateAction}&lt;{@link Type}&gt; as a
     * result. If no update actions are needed, for example in case where both the {@link Type} and the
     * {@link TypeDraft} have the same fields, an empty {@link List} is returned.
     *
     * @param oldType       the {@link Type} which should be updated.
     * @param newType       the {@link TypeDraft} where we get the new data.
     * @param syncOptions   the sync options wrapper which contains options related to the sync process supplied by
     *                      the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                      on the build update action process. And other options (See {@link TypeSyncOptions}
     *                      for more info.
     * @return A list of type-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Type>> buildActions(
            @Nonnull final Type oldType,
            @Nonnull final TypeDraft newType,
            @Nonnull final TypeSyncOptions syncOptions) {

        final List<UpdateAction<Type>> updateActions = Stream.of(
                buildChangeNameUpdateAction(oldType, newType),
                buildSetDescriptionUpdateAction(oldType, newType)
        )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());

        updateActions.addAll(buildFieldDefinitionUpdateActions(oldType, newType, syncOptions));

        return updateActions;
    }

    private TypeSyncUtils() {
    }
}
