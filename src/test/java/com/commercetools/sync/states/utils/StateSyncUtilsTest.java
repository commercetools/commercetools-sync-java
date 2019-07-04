package com.commercetools.sync.states.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.states.utils.StateSyncUtils.buildActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StateSyncUtilsTest {

    private static String KEY = "key1";

    private State old;
    private StateDraft draft;

    @BeforeEach
    void setup() {
        final StateType type = StateType.LINE_ITEM_STATE;
        final LocalizedString name = LocalizedString.of(Locale.GERMANY, "name");
        final LocalizedString description = LocalizedString.of(Locale.GERMANY, "description");

        old = mock(State.class);
        when(old.getKey()).thenReturn(KEY);
        when(old.getType()).thenReturn(type);
        when(old.getName()).thenReturn(name);
        when(old.getDescription()).thenReturn(description);
        when(old.isInitial()).thenReturn(true);

        draft = StateDraft.of(KEY, type)
            .withName(name)
            .withDescription(description)
            .withInitial(true);
    }

    @Test
    void buildActions_WithSameValues_ShouldNotBuildAction() {
        List<UpdateAction<State>> result = buildActions(old, draft);

        assertThat(result).as("Should be empty").isEmpty();
    }

}
