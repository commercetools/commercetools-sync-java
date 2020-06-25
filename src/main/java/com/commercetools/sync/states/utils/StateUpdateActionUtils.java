package com.commercetools.sync.states.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateRole;
import io.sphere.sdk.states.commands.updateactions.AddRoles;
import io.sphere.sdk.states.commands.updateactions.ChangeInitial;
import io.sphere.sdk.states.commands.updateactions.ChangeType;
import io.sphere.sdk.states.commands.updateactions.RemoveRoles;
import io.sphere.sdk.states.commands.updateactions.SetDescription;
import io.sphere.sdk.states.commands.updateactions.SetName;
import io.sphere.sdk.states.commands.updateactions.SetTransitions;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public final class StateUpdateActionUtils {

    private StateUpdateActionUtils() {
    }

    /**
     * Compares the {@code type} values of a {@link State} and a {@link StateDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeType"}
     * {@link UpdateAction}. If both {@link State} and {@link StateDraft} have the same
     * {@code type} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldState the state that should be updated.
     * @param newState the state draft which contains the new type.
     * @return optional containing update action or empty optional if types are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<State>> buildChangeTypeAction(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState) {

        return buildUpdateAction(oldState.getType(), newState.getType(),
            () -> ChangeType.of(newState.getType()));
    }

    /**
     * Compares the {@code name} values of a {@link State} and a {@link StateDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setName"}
     * {@link UpdateAction}. If both {@link State} and {@link StateDraft} have the same
     * {@code name} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldState the state that should be updated.
     * @param newState the state draft which contains the new name.
     * @return optional containing update action or empty optional if names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<State>> buildSetNameAction(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState) {

        return buildUpdateAction(oldState.getName(), newState.getName(),
            () -> SetName.of(newState.getName()));
    }

    /**
     * Compares the {@code description} values of a {@link State} and a {@link StateDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setDescription"}
     * {@link UpdateAction}. If both {@link State} and {@link StateDraft} have the same
     * {@code description} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldState the state that should be updated.
     * @param newState the state draft which contains the new description.
     * @return optional containing update action or empty optional if descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<State>> buildSetDescriptionAction(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState) {

        return buildUpdateAction(oldState.getDescription(), newState.getDescription(),
            () -> SetDescription.of(newState.getDescription()));
    }

    /**
     * Compares the {@code initial} values of a {@link State} and a {@link StateDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeInitial"}
     * {@link UpdateAction}. If both {@link State} and {@link StateDraft} have the same
     * {@code initial} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldState the state that should be updated.
     * @param newState the state draft which contains the new initial.
     * @return optional containing update action or empty optional if initial are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<State>> buildChangeInitialAction(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState) {

        return buildUpdateAction(oldState.isInitial(), newState.isInitial(),
            () -> ChangeInitial.of(Optional.ofNullable(newState.isInitial()).orElse(false)));
    }

    /**
     * Compares the roles of a {@link State} and a {@link StateDraft} and returns a list of
     * {@link UpdateAction}&lt;{@link State}&gt; as a result. If both the {@link State} and
     * the {@link StateDraft} have identical roles, then no update action is needed and hence an empty
     * {@link List} is returned.
     *
     * @param oldState the state which should be updated.
     * @param newState the state draft where we get the key.
     * @return A list with the update actions or an empty list if the roles are identical.
     */
    @Nonnull
    public static List<UpdateAction<State>> buildRolesUpdateActions(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState) {

        boolean emptyNew = newState.getRoles() == null || newState.getRoles().isEmpty();
        boolean emptyOld = oldState.getRoles() == null || oldState.getRoles().isEmpty();

        if (emptyNew && emptyOld) {
            return emptyList();
        }

        Set<StateRole> newRoles = emptyNew ? new HashSet<>() : newState.getRoles();
        Set<StateRole> oldRoles = emptyOld ? new HashSet<>() : oldState.getRoles();

        List<UpdateAction<State>> actions = new ArrayList<>();

        Set<StateRole> remove = diffRoles(oldRoles, newRoles);

        if (!remove.isEmpty()) {
            actions.add(RemoveRoles.of(remove));
        }

        Set<StateRole> add = diffRoles(newRoles, oldRoles);

        if (!add.isEmpty()) {
            actions.add(AddRoles.of(add));
        }

        return actions;
    }

    /**
     * Compares the {@code transitions} values of a {@link State} and a {@link StateDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setTransitions"}
     * {@link UpdateAction}. If both {@link State} and {@link StateDraft} have the same
     * {@code transitions} values, then no update action is needed and empty optional will be returned. Empty optional
     * will albo be returned if an error occur while trying to create update action.
     *
     * @param oldState      the {@link State} which should be updated.
     * @param newState      the {@link StateDraft} where we get the new data.
     * @param keyToId       the {@link Map} containing {@link State} key to id mapping
     * @param errorCallback the error callback which will be called in case there was an error while trying to create
     *                      update action.
     * @return optional containing update action or empty optional if there are no changes detected or there was
     *         an error.
     */
    @Nonnull
    public static Optional<UpdateAction<State>> buildSetTransitionsAction(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState,
        @Nonnull final Map<String, String> keyToId,
        @Nonnull final Consumer<String> errorCallback) {

        boolean emptyNew = newState.getTransitions() == null || newState.getTransitions().isEmpty();
        boolean emptyOld = oldState.getTransitions() == null || oldState.getTransitions().isEmpty();

        if (emptyNew && emptyOld) {
            return Optional.empty();
        } else if (emptyNew) {
            return Optional.of(SetTransitions.of(emptySet()));
        }

        Set<String> newTransitionStateKeys = new HashSet<>();
        Set<Reference<State>> transitions = newState.getTransitions().stream()
            .filter(ref -> ref.getObj() != null && StringUtils.isNotEmpty(ref.getObj().getKey()))
            .filter(ref -> Objects.nonNull(keyToId.get(ref.getObj().getKey())))
            .map(ref -> {
                newTransitionStateKeys.add(ref.getObj().getKey());
                return State.referenceOfId(keyToId.get(ref.getObj().getKey()));
            })
            .collect(Collectors.toSet());

        if (transitions.size() != newState.getTransitions().size()) {
            errorCallback.accept(format("Failed to build transition action for state '%s' because "
                + "not all of states id were available", newState.getKey()));
            return Optional.empty();
        }

        if (!emptyOld) {
            boolean sizeMatch = newState.getTransitions().size() == oldState.getTransitions().size();

            long count = oldState.getTransitions().stream()
                .filter(ref -> ref.getObj() != null && StringUtils.isNotEmpty(ref.getObj().getKey())
                    && newTransitionStateKeys.contains(ref.getObj().getKey()))
                .count();
            boolean elementsMatch = count == newTransitionStateKeys.size();

            if (sizeMatch && elementsMatch) {
                return Optional.empty();
            }
        }

        return Optional.of(SetTransitions.of(transitions));
    }

    private static Set<StateRole> diffRoles(final Set<StateRole> src, final Set<StateRole> dst) {
        return src
            .stream()
            .map(role -> {
                if (!dst.contains(role)) {
                    return role;
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

}
