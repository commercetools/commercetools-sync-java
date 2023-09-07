package com.commercetools.sync.states.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateAddRolesActionBuilder;
import com.commercetools.api.models.state.StateChangeInitialActionBuilder;
import com.commercetools.api.models.state.StateChangeTypeActionBuilder;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateRemoveRolesActionBuilder;
import com.commercetools.api.models.state.StateRoleEnum;
import com.commercetools.api.models.state.StateSetDescriptionActionBuilder;
import com.commercetools.api.models.state.StateSetNameActionBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.api.models.state.StateUpdateAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class StateSyncUtilsTest {

  private static final String KEY = "key1";

  @Test
  void buildActions_WithSameValues_ShouldNotBuildUpdateActions() {
    final StateTypeEnum type = StateTypeEnum.LINE_ITEM_STATE;
    final LocalizedString name = LocalizedString.of(Locale.GERMANY, "name");
    final LocalizedString description = LocalizedString.of(Locale.GERMANY, "description");

    final State state = mock(State.class);
    when(state.getKey()).thenReturn(KEY);
    when(state.getType()).thenReturn(type);
    when(state.getName()).thenReturn(name);
    when(state.getDescription()).thenReturn(description);
    when(state.getInitial()).thenReturn(true);

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(KEY)
            .type(type)
            .name(name)
            .description(description)
            .initial(true)
            .build();

    final List<StateUpdateAction> result = StateSyncUtils.buildActions(state, stateDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildActions_WithDifferentValues_ShouldBuildAllUpdateActions() {
    final State state = mock(State.class);
    when(state.getKey()).thenReturn(KEY);
    when(state.getType()).thenReturn(StateTypeEnum.LINE_ITEM_STATE);
    when(state.getName()).thenReturn(LocalizedString.of(Locale.GERMANY, "name"));
    when(state.getDescription()).thenReturn(LocalizedString.of(Locale.GERMANY, "description"));
    when(state.getInitial()).thenReturn(false);
    final List<StateRoleEnum> oldStateRoles = new ArrayList<>(List.of(StateRoleEnum.RETURN));
    when(state.getRoles()).thenReturn(oldStateRoles);

    final List<StateRoleEnum> newStateRoles =
        new ArrayList<>(List.of(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS));
    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(KEY)
            .type(StateTypeEnum.PRODUCT_STATE)
            .name(LocalizedString.of(Locale.GERMANY, "different name"))
            .description(LocalizedString.of(Locale.GERMANY, "different description"))
            .initial(true)
            .roles(newStateRoles)
            .build();

    final List<StateUpdateAction> result = StateSyncUtils.buildActions(state, stateDraft);

    assertAll(
        () ->
            assertThat(result)
                .contains(StateChangeTypeActionBuilder.of().type(stateDraft.getType()).build()),
        () ->
            assertThat(result)
                .contains(StateSetNameActionBuilder.of().name(stateDraft.getName()).build()),
        () ->
            assertThat(result)
                .contains(
                    StateSetDescriptionActionBuilder.of()
                        .description(stateDraft.getDescription())
                        .build()),
        () ->
            assertThat(result)
                .contains(
                    StateChangeInitialActionBuilder.of().initial(stateDraft.getInitial()).build()),
        () ->
            assertThat(result)
                .contains(StateRemoveRolesActionBuilder.of().roles(oldStateRoles).build()),
        () ->
            assertThat(result)
                .contains(StateAddRolesActionBuilder.of().roles(newStateRoles).build()));
  }
}
