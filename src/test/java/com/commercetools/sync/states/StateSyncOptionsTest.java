package com.commercetools.sync.states;

import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.commands.updateactions.ChangeInitial;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StateSyncOptionsTest {

    private static SphereClient CTP_CLIENT = mock(SphereClient.class);

    @Test
    void applyBeforeUpdateCallback_WithNullCallback_ShouldReturnIdenticalList() {
        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .build();
        final List<UpdateAction<State>> updateActions = singletonList(ChangeInitial.of(false));

        final List<UpdateAction<State>> filteredList =
            stateSyncOptions.applyBeforeUpdateCallback(updateActions, mock(StateDraft.class), mock(State.class));

        assertThat(filteredList).isSameAs(updateActions);
    }

    @Test
    void applyBeforeUpdateCallback_WithNullReturnCallback_ShouldReturnEmptyList() {
        final TriFunction<List<UpdateAction<State>>, StateDraft, State, List<UpdateAction<State>>>
            beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> null;
        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
        final List<UpdateAction<State>> updateActions = singletonList(ChangeInitial.of(false));

        final List<UpdateAction<State>> filteredList =
            stateSyncOptions.applyBeforeUpdateCallback(updateActions, mock(StateDraft.class), mock(State.class));

        assertAll(
            () -> assertThat(filteredList).isNotEqualTo(updateActions),
            () -> assertThat(filteredList).isEmpty()
        );
    }

    private interface MockTriFunction extends
        TriFunction<List<UpdateAction<State>>, StateDraft, State, List<UpdateAction<State>>> {
    }

    @Test
    void applyBeforeUpdateCallback_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
        final StateSyncOptionsTest.MockTriFunction beforeUpdateCallback =
            mock(StateSyncOptionsTest.MockTriFunction.class);
        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

        final List<UpdateAction<State>> filteredList =
            stateSyncOptions.applyBeforeUpdateCallback(emptyList(), mock(StateDraft.class), mock(State.class));

        assertThat(filteredList).isEmpty();
        verify(beforeUpdateCallback, never()).apply(any(), any(), any());
    }

    @Test
    void applyBeforeUpdateCallback_WithCallback_ShouldReturnFilteredList() {
        final TriFunction<List<UpdateAction<State>>, StateDraft, State, List<UpdateAction<State>>>
            beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> emptyList();
        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
        final List<UpdateAction<State>> updateActions = singletonList(ChangeInitial.of(false));

        final List<UpdateAction<State>> filteredList =
            stateSyncOptions.applyBeforeUpdateCallback(updateActions, mock(StateDraft.class), mock(State.class));

        assertThat(filteredList).isEmpty();
    }

    @Test
    void applyBeforeCreateCallback_WithCallback_ShouldReturnFilteredDraft() {
        final Function<StateDraft, StateDraft> draftFunction =
            stateDraft -> StateDraftBuilder.of(stateDraft).key(stateDraft.getKey() + "_filteredKey").build();
        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeCreateCallback(draftFunction)
            .build();
        final StateDraft resourceDraft = mock(StateDraft.class);
        when(resourceDraft.getKey()).thenReturn("myKey");

        final Optional<StateDraft> filteredDraft = stateSyncOptions.applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).hasValueSatisfying(stateDraft ->
            assertThat(stateDraft.getKey()).isEqualTo("myKey_filteredKey"));
    }

    @Test
    void applyBeforeCreateCallback_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT).build();
        final StateDraft resourceDraft = mock(StateDraft.class);

        final Optional<StateDraft> filteredDraft = stateSyncOptions.applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).containsSame(resourceDraft);
    }

    @Test
    void applyBeforeCreateCallback_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
        final Function<StateDraft, StateDraft> draftFunction = stateDraft -> null;
        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeCreateCallback(draftFunction)
            .build();
        final StateDraft resourceDraft = mock(StateDraft.class);

        final Optional<StateDraft> filteredDraft = stateSyncOptions.applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).isEmpty();
    }

}
