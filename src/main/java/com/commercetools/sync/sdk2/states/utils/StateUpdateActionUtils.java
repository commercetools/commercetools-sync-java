package com.commercetools.sync.sdk2.states.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;

import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateAddRolesActionBuilder;
import com.commercetools.api.models.state.StateChangeInitialActionBuilder;
import com.commercetools.api.models.state.StateChangeTypeActionBuilder;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateRemoveRolesActionBuilder;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateRoleEnum;
import com.commercetools.api.models.state.StateSetDescriptionActionBuilder;
import com.commercetools.api.models.state.StateSetNameActionBuilder;
import com.commercetools.api.models.state.StateSetTransitionsActionBuilder;
import com.commercetools.api.models.state.StateUpdateAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public final class StateUpdateActionUtils {

  private StateUpdateActionUtils() {}

  /**
   * Compares the {@code type} values of a {@link State} and a {@link StateDraft} and returns an
   * {@link java.util.Optional} of update action, which would contain the {@code "changeType"}
   * {@link StateUpdateAction}. If both {@link State} and {@link StateDraft} have the same {@code
   * type} values, then no update action is needed and empty optional will be returned.
   *
   * @param oldState the state that should be updated.
   * @param newState the state draft which contains the new type.
   * @return optional containing update action or empty optional if types are identical.
   */
  @Nonnull
  public static Optional<StateUpdateAction> buildChangeTypeAction(
      @Nonnull final State oldState, @Nonnull final StateDraft newState) {

    return buildUpdateAction(
        oldState.getType(),
        newState.getType(),
        () -> StateChangeTypeActionBuilder.of().type(newState.getType()).build());
  }

  /**
   * Compares the {@code name} values of a {@link State} and a {@link StateDraft} and returns an
   * {@link java.util.Optional} of update action, which would contain the {@code "setName"} {@link
   * StateUpdateAction}. If both {@link State} and {@link StateDraft} have the same {@code name}
   * values, then no update action is needed and empty optional will be returned.
   *
   * @param oldState the state that should be updated.
   * @param newState the state draft which contains the new name.
   * @return optional containing update action or empty optional if names are identical.
   */
  @Nonnull
  public static Optional<StateUpdateAction> buildSetNameAction(
      @Nonnull final State oldState, @Nonnull final StateDraft newState) {

    return buildUpdateAction(
        oldState.getName(),
        newState.getName(),
        () -> StateSetNameActionBuilder.of().name(newState.getName()).build());
  }

  /**
   * Compares the {@code description} values of a {@link State} and a {@link StateDraft} and returns
   * an {@link java.util.Optional} of update action, which would contain the {@code
   * "setDescription"} {@link StateUpdateAction}. If both {@link State} and {@link StateDraft} have
   * the same {@code description} values, then no update action is needed and empty optional will be
   * returned.
   *
   * @param oldState the state that should be updated.
   * @param newState the state draft which contains the new description.
   * @return optional containing update action or empty optional if descriptions are identical.
   */
  @Nonnull
  public static Optional<StateUpdateAction> buildSetDescriptionAction(
      @Nonnull final State oldState, @Nonnull final StateDraft newState) {

    return buildUpdateAction(
        oldState.getDescription(),
        newState.getDescription(),
        () -> StateSetDescriptionActionBuilder.of().description(newState.getDescription()).build());
  }

  /**
   * Compares the {@code initial} values of a {@link State} and a {@link StateDraft} and returns an
   * {@link java.util.Optional} of update action, which would contain the {@code "changeInitial"}
   * {@link StateUpdateAction}. If both {@link State} and {@link StateDraft} have the same {@code
   * initial} values, then no update action is needed and empty optional will be returned.
   *
   * @param oldState the state that should be updated.
   * @param newState the state draft which contains the new initial.
   * @return optional containing update action or empty optional if initial are identical.
   */
  @Nonnull
  public static Optional<StateUpdateAction> buildChangeInitialAction(
      @Nonnull final State oldState, @Nonnull final StateDraft newState) {

    final boolean isNewStateInitial = toBoolean(newState.getInitial());
    final boolean isOldStateInitial = toBoolean(oldState.getInitial());

    return buildUpdateAction(
        isOldStateInitial,
        isNewStateInitial,
        () -> StateChangeInitialActionBuilder.of().initial(isNewStateInitial).build());
  }

  /**
   * Compares the roles of a {@link State} and a {@link StateDraft} and returns a list of {@link
   * StateUpdateAction} as a result. If both the {@link State} and the {@link StateDraft} have
   * identical roles, then no update action is needed and hence an empty {@link java.util.List} is
   * returned.
   *
   * @param oldState the state which should be updated.
   * @param newState the state draft where we get the key.
   * @return A list with the update actions or an empty list if the roles are identical.
   */
  @Nonnull
  public static List<StateUpdateAction> buildRolesUpdateActions(
      @Nonnull final State oldState, @Nonnull final StateDraft newState) {

    boolean emptyNew = newState.getRoles() == null || newState.getRoles().isEmpty();
    boolean emptyOld = oldState.getRoles() == null || oldState.getRoles().isEmpty();

    if (emptyNew && emptyOld) {
      return List.of();
    }

    final List<StateRoleEnum> newRoles = emptyNew ? new ArrayList<>() : newState.getRoles();
    final List<StateRoleEnum> oldRoles = emptyOld ? new ArrayList<>() : oldState.getRoles();

    final List<StateUpdateAction> actions = new ArrayList<>();

    final List<StateRoleEnum> remove = diffRoles(oldRoles, newRoles);

    if (!remove.isEmpty()) {
      actions.add(StateRemoveRolesActionBuilder.of().roles(remove).build());
    }

    final List<StateRoleEnum> add = diffRoles(newRoles, oldRoles);

    if (!add.isEmpty()) {
      actions.add(StateAddRolesActionBuilder.of().roles(add).build());
    }

    return actions;
  }

  /**
   * Compares the {@code transitions} values of a {@link State} and a {@link StateDraft} and returns
   * an {@link java.util.Optional} of update action, which would contain the {@code
   * "setTransitions"} {@link StateUpdateAction}. If both {@link State} and {@link StateDraft} have
   * the same {@code transitions} values, then no update action is needed and empty optional will be
   * returned. if not, the transition of the old State gets overwritten with the transitions of the
   * statedraft
   *
   * @param oldState the {@link State} which should be updated.
   * @param newState the {@link StateDraft} where we get the new data.
   * @return optional containing update action or empty optional if there are no changes detected or
   *     there was an error.
   */
  @Nonnull
  public static Optional<StateUpdateAction> buildSetTransitionsAction(
      @Nonnull final State oldState, @Nonnull final StateDraft newState) {

    final boolean emptyNew =
        newState.getTransitions() == null
            || newState.getTransitions().isEmpty()
            || newState.getTransitions().stream().noneMatch(Objects::nonNull);

    final boolean emptyOld =
        oldState.getTransitions() == null
            || oldState.getTransitions().isEmpty()
            || oldState.getTransitions().stream().noneMatch(Objects::nonNull);

    if (emptyNew && emptyOld) {
      return Optional.empty();
    } else if (emptyNew) {
      return Optional.of(
          StateSetTransitionsActionBuilder.of()
              .transitions((List<StateResourceIdentifier>) null)
              .build());
    }

    final Set<StateResourceIdentifier> newTransitions =
        newState.getTransitions().stream().filter(Objects::nonNull).collect(Collectors.toSet());

    final Set<StateReference> oldTransitions =
        oldState.getTransitions().stream().filter(Objects::nonNull).collect(Collectors.toSet());

    if (hasDifferentTransitions(newTransitions, oldTransitions)) {
      final List<StateResourceIdentifier> transitions =
          newTransitions.stream()
              .map(
                  transition ->
                      StateResourceIdentifierBuilder.of()
                          .id(transition.getId())
                          .key(transition.getKey())
                          .build())
              .collect(Collectors.toList());
      return Optional.of(StateSetTransitionsActionBuilder.of().transitions(transitions).build());
    }

    return Optional.empty();
  }

  private static boolean hasDifferentTransitions(
      final Set<StateResourceIdentifier> newTransitions, final Set<StateReference> oldTransitions) {
    final List<String> newTransitionIds =
        newTransitions.stream().map(StateResourceIdentifier::getId).collect(Collectors.toList());
    final List<String> oldTransitionIds =
        oldTransitions.stream().map(StateReference::getId).collect(Collectors.toList());

    return !Objects.equals(newTransitionIds, oldTransitionIds);
  }

  private static List<StateRoleEnum> diffRoles(
      final List<StateRoleEnum> src, final List<StateRoleEnum> dst) {
    return src.stream()
        .map(
            role -> {
              if (!dst.contains(role)) {
                return role;
              }
              return null;
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
