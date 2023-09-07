package com.commercetools.sync.states.utils;

import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.*;

import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateUpdateAction;
import java.util.List;
import javax.annotation.Nonnull;

public final class StateSyncUtils {

  private StateSyncUtils() {}

  /**
   * Compares all the fields (including the roles see {@link
   * StateUpdateActionUtils#buildRolesUpdateActions}) of a {@link State} and a {@link StateDraft}.
   * It returns a {@link java.util.List} of {@link StateUpdateAction} as a result. If no update
   * action is needed, for example in case where both the {@link State} and the {@link StateDraft}
   * have the same fields, an empty {@link java.util.List} is returned.
   *
   * @param oldState the {@link State} which should be updated.
   * @param newState the {@link StateDraft} where we get the new data.
   * @return A list of state-specific update actions.
   */
  @Nonnull
  public static List<StateUpdateAction> buildActions(
      @Nonnull final State oldState, @Nonnull final StateDraft newState) {

    final List<StateUpdateAction> updateActions =
        filterEmptyOptionals(
            buildChangeTypeAction(oldState, newState),
            buildSetNameAction(oldState, newState),
            buildSetDescriptionAction(oldState, newState),
            buildChangeInitialAction(oldState, newState));

    updateActions.addAll(buildRolesUpdateActions(oldState, newState));
    buildSetTransitionsAction(oldState, newState).ifPresent(updateActions::add);

    return updateActions;
  }
}
