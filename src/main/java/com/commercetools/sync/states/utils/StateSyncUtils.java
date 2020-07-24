package com.commercetools.sync.states.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;

import javax.annotation.Nonnull;
import java.util.List;

import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildChangeInitialAction;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildChangeTypeAction;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildRolesUpdateActions;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildSetDescriptionAction;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildSetNameAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildSetTransitionsAction;

public final class StateSyncUtils {

    private StateSyncUtils() {
    }

    /**
     * Compares all the fields (including the roles see
     * {@link StateUpdateActionUtils#buildRolesUpdateActions}) of a {@link State} and a
     * {@link StateDraft}. It returns a {@link List} of {@link UpdateAction}&lt;{@link State}&gt; as a
     * result. If no update action is needed, for example in case where both the {@link State} and the
     * {@link StateDraft} have the same fields, an empty {@link List} is returned.
     *
     * @param oldState the {@link State} which should be updated.
     * @param newState the {@link StateDraft} where we get the new data.
     * @return A list of state-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<State>> buildActions(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState) {

        final List<UpdateAction<State>> updateActions =
            filterEmptyOptionals(
                buildChangeTypeAction(oldState, newState),
                buildSetNameAction(oldState, newState),
                buildSetDescriptionAction(oldState, newState),
                buildChangeInitialAction(oldState, newState)
            );

        updateActions.addAll(buildRolesUpdateActions(oldState, newState));
        buildSetTransitionsAction(oldState, newState).ifPresent(updateActions::add);

        return updateActions;
    }

}
