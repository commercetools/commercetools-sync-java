package com.commercetools.sync.states.helpers;

import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;

import javax.annotation.Nonnull;

public class StateDraftBuilderHelper {

    /**
     * Convenient method to create the {@link StateDraft} instance out of the {@link State} instance.
     *
     * @param state the state to be converted
     * @return an instance of the state draft
     */
    public static StateDraft of(@Nonnull final State state) {
        return StateDraftBuilder.of(state.getKey(), state.getType())
            .name(state.getName())
            .description(state.getDescription())
            .initial(state.isInitial())
            .roles(state.getRoles())
            .transitions(state.getTransitions())
            .build();
    }

}
