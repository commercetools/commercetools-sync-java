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
        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .build();
        assertThat(stateSyncOptions.getBeforeUpdateCallback()).isNull();

        List<UpdateAction<State>> updateActions = singletonList(ChangeInitial.of(false));
        List<UpdateAction<State>> filteredList =
            stateSyncOptions.applyBeforeUpdateCallBack(updateActions, mock(StateDraft.class), mock(State.class));

        assertThat(filteredList).as(" returned 'updateActions' should not be changed")
            .isSameAs(updateActions);
    }

    @Test
    void applyBeforeUpdateCallback_WithNullReturnCallback_ShouldReturnEmptyList() {
        TriFunction<List<UpdateAction<State>>, StateDraft, State, List<UpdateAction<State>>>
            beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> null;
        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
        assertThat(stateSyncOptions.getBeforeUpdateCallback()).isNotNull();

        List<UpdateAction<State>> updateActions = singletonList(ChangeInitial.of(false));
        List<UpdateAction<State>> filteredList =
            stateSyncOptions.applyBeforeUpdateCallBack(updateActions, mock(StateDraft.class), mock(State.class));

        assertAll(
            () -> assertThat(filteredList)
                .as("returned 'updateActions' should not be equal to prepared ones")
                .isNotEqualTo(updateActions),
            () -> assertThat(filteredList).as("returned 'updateActions' should be empty").isEmpty()
        );
    }

    private interface MockTriFunction extends
        TriFunction<List<UpdateAction<State>>, StateDraft, State, List<UpdateAction<State>>> {
    }

    @Test
    void applyBeforeUpdateCallback_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
        StateSyncOptionsTest.MockTriFunction beforeUpdateCallback = mock(StateSyncOptionsTest.MockTriFunction.class);

        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

        assertThat(stateSyncOptions.getBeforeUpdateCallback()).isNotNull();

        List<UpdateAction<State>> updateActions = emptyList();
        List<UpdateAction<State>> filteredList =
            stateSyncOptions.applyBeforeUpdateCallBack(updateActions, mock(StateDraft.class), mock(State.class));

        assertThat(filteredList).as("returned 'updateActions' should be empty").isEmpty();
        verify(beforeUpdateCallback, never()).apply(any(), any(), any());
    }

    @Test
    void applyBeforeUpdateCallback_WithCallback_ShouldReturnFilteredList() {
        TriFunction<List<UpdateAction<State>>, StateDraft, State, List<UpdateAction<State>>>
            beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> emptyList();
        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
        assertThat(stateSyncOptions.getBeforeUpdateCallback()).isNotNull();

        List<UpdateAction<State>> updateActions = singletonList(ChangeInitial.of(false));
        List<UpdateAction<State>> filteredList =
            stateSyncOptions.applyBeforeUpdateCallBack(updateActions, mock(StateDraft.class), mock(State.class));

        assertAll(
            () -> assertThat(filteredList)
                .as("returned 'updateActions' should not be equal to prepared ones")
                .isNotEqualTo(updateActions),
            () -> assertThat(filteredList).as("returned 'updateActions' should be empty").isEmpty()
        );
    }

    @Test
    void applyBeforeCreateCallback_WithCallback_ShouldReturnFilteredDraft() {
        Function<StateDraft, StateDraft> draftFunction =
            stateDraft -> StateDraftBuilder.of(stateDraft).key(stateDraft.getKey() + "_filteredKey").build();

        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeCreateCallback(draftFunction)
            .build();
        assertThat(stateSyncOptions.getBeforeCreateCallback()).isNotNull();

        StateDraft resourceDraft = mock(StateDraft.class);
        when(resourceDraft.getKey()).thenReturn("myKey");

        Optional<StateDraft> filteredDraft = stateSyncOptions.applyBeforeCreateCallBack(resourceDraft);

        assertAll(
            () -> assertThat(filteredDraft).as("should return draft").isNotEmpty(),
            () -> assertThat(filteredDraft.get().getKey())
                .as("returned 'draft' should have different key")
                .isEqualTo("myKey_filteredKey")
        );
    }

    @Test
    void applyBeforeCreateCallback_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT).build();
        assertThat(stateSyncOptions.getBeforeCreateCallback()).isNull();

        StateDraft resourceDraft = mock(StateDraft.class);
        Optional<StateDraft> filteredDraft = stateSyncOptions.applyBeforeCreateCallBack(resourceDraft);

        assertThat(filteredDraft).as("returned 'draft' should not be changed").containsSame(resourceDraft);
    }

    @Test
    void applyBeforeCreateCallback_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
        Function<StateDraft, StateDraft> draftFunction = stateDraft -> null;
        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeCreateCallback(draftFunction)
            .build();
        assertThat(stateSyncOptions.getBeforeCreateCallback()).isNotNull();

        StateDraft resourceDraft = mock(StateDraft.class);
        Optional<StateDraft> filteredDraft = stateSyncOptions.applyBeforeCreateCallBack(resourceDraft);

        assertThat(filteredDraft).as("should return no draft").isEmpty();
    }

}
