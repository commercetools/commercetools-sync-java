package com.commercetools.sync.states.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateRole;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.updateactions.AddRoles;
import io.sphere.sdk.states.commands.updateactions.ChangeInitial;
import io.sphere.sdk.states.commands.updateactions.ChangeType;
import io.sphere.sdk.states.commands.updateactions.RemoveRoles;
import io.sphere.sdk.states.commands.updateactions.SetDescription;
import io.sphere.sdk.states.commands.updateactions.SetName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.commercetools.sync.states.utils.StateSyncUtils.buildActions;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StateSyncUtilsTest {

    private static final String KEY = "key1";

    @Test
    void buildActions_WithSameValues_ShouldNotBuildUpdateActions() {
        final StateType type = StateType.LINE_ITEM_STATE;
        final LocalizedString name = LocalizedString.of(Locale.GERMANY, "name");
        final LocalizedString description = LocalizedString.of(Locale.GERMANY, "description");

        final State state = mock(State.class);
        when(state.getKey()).thenReturn(KEY);
        when(state.getType()).thenReturn(type);
        when(state.getName()).thenReturn(name);
        when(state.getDescription()).thenReturn(description);
        when(state.isInitial()).thenReturn(true);

        final StateDraft stateDraft = StateDraft.of(KEY, type)
            .withName(name)
            .withDescription(description)
            .withInitial(true);


        final List<UpdateAction<State>> result = buildActions(state, stateDraft);

        assertThat(result).isEmpty();
    }

    @Test
    void buildActions_WithDifferentValues_ShouldBuildAllUpdateActions() {
        final State state = mock(State.class);
        when(state.getKey()).thenReturn(KEY);
        when(state.getType()).thenReturn(StateType.LINE_ITEM_STATE);
        when(state.getName()).thenReturn(LocalizedString.of(Locale.GERMANY, "name"));
        when(state.getDescription()).thenReturn(LocalizedString.of(Locale.GERMANY, "description"));
        when(state.isInitial()).thenReturn(false);
        final Set<StateRole> oldStateRoles = new HashSet<>(singletonList(StateRole.RETURN));
        when(state.getRoles()).thenReturn(oldStateRoles);

        final Set<StateRole> newStateRoles = new HashSet<>(singletonList(StateRole.REVIEW_INCLUDED_IN_STATISTICS));
        final StateDraft stateDraft = StateDraft.of(KEY, StateType.PRODUCT_STATE)
            .withName(LocalizedString.of(Locale.GERMANY, "different name"))
            .withDescription(LocalizedString.of(Locale.GERMANY, "different description"))
            .withInitial(true)
            .withRoles(newStateRoles);

        final List<UpdateAction<State>> result = buildActions(state, stateDraft);

        assertAll(
            () -> assertThat(result).contains(ChangeType.of(stateDraft.getType())),
            () -> assertThat(result).contains(SetName.of(stateDraft.getName())),
            () -> assertThat(result).contains(SetDescription.of(stateDraft.getDescription())),
            () -> assertThat(result).contains(ChangeInitial.of(stateDraft.isInitial())),
            () -> assertThat(result).contains(RemoveRoles.of(oldStateRoles)),
            () -> assertThat(result).contains(AddRoles.of(newStateRoles))
        );
    }

}
