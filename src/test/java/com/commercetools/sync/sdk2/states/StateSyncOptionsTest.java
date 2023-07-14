package com.commercetools.sync.sdk2.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateChangeInitialActionBuilder;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.api.models.state.StateUpdateAction;
import com.commercetools.sync.sdk2.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class StateSyncOptionsTest {

  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);

  @Test
  void applyBeforeUpdateCallback_WithNullCallback_ShouldReturnIdenticalList() {
    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT).build();
    final List<StateUpdateAction> updateActions =
        List.of(StateChangeInitialActionBuilder.of().initial(false).build());

    final List<StateUpdateAction> filteredList =
        stateSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(StateDraft.class), mock(State.class));

    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void applyBeforeUpdateCallback_WithNullReturnCallback_ShouldReturnEmptyList() {
    final TriFunction<List<StateUpdateAction>, StateDraft, State, List<StateUpdateAction>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> null;
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_CLIENT).beforeUpdateCallback(beforeUpdateCallback).build();
    final List<StateUpdateAction> updateActions =
        List.of(StateChangeInitialActionBuilder.of().initial(false).build());

    final List<StateUpdateAction> filteredList =
        stateSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(StateDraft.class), mock(State.class));

    assertAll(
        () -> assertThat(filteredList).isNotEqualTo(updateActions),
        () -> assertThat(filteredList).isEmpty());
  }

  private interface MockTriFunction
      extends TriFunction<List<StateUpdateAction>, StateDraft, State, List<StateUpdateAction>> {}

  @Test
  void applyBeforeUpdateCallback_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_CLIENT).beforeUpdateCallback(beforeUpdateCallback).build();

    final List<StateUpdateAction> filteredList =
        stateSyncOptions.applyBeforeUpdateCallback(
            List.of(), mock(StateDraft.class), mock(State.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeUpdateCallback_WithCallback_ShouldReturnFilteredList() {
    final TriFunction<List<StateUpdateAction>, StateDraft, State, List<StateUpdateAction>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> List.of();
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_CLIENT).beforeUpdateCallback(beforeUpdateCallback).build();
    final List<StateUpdateAction> updateActions =
        List.of(StateChangeInitialActionBuilder.of().initial(false).build());

    final List<StateUpdateAction> filteredList =
        stateSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(StateDraft.class), mock(State.class));

    assertThat(filteredList).isEmpty();
  }

  @Test
  void applyBeforeCreateCallback_WithCallback_ShouldReturnFilteredDraft() {
    final Function<StateDraft, StateDraft> draftFunction =
        stateDraft ->
            StateDraftBuilder.of(stateDraft).key(stateDraft.getKey() + "_filteredKey").build();
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    final StateDraft resourceDraft = mock(StateDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");
    when(resourceDraft.getType()).thenReturn(StateTypeEnum.PRODUCT_STATE);

    final Optional<StateDraft> filteredDraft =
        stateSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft)
        .hasValueSatisfying(
            stateDraft -> assertThat(stateDraft.getKey()).isEqualTo("myKey_filteredKey"));
  }

  @Test
  void applyBeforeCreateCallback_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT).build();
    final StateDraft resourceDraft = mock(StateDraft.class);

    final Optional<StateDraft> filteredDraft =
        stateSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallback_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
    final Function<StateDraft, StateDraft> draftFunction = stateDraft -> null;
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    final StateDraft resourceDraft = mock(StateDraft.class);

    final Optional<StateDraft> filteredDraft =
        stateSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isEmpty();
  }
}
