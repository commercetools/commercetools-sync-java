package com.commercetools.sync.states.helpers;

import com.commercetools.sync.commons.exceptions.InvalidReferenceException;
import com.commercetools.sync.commons.exceptions.InvalidStateDraftException;
import com.commercetools.sync.states.StateSync;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class StateBatchProcessor {
    static final String STATE_DRAFT_KEY_NOT_SET = "StateDraft with name: %s doesn't have a key. "
        + "Please make sure all states have keys.";
    static final String STATE_DRAFT_IS_NULL = "StateDraft is null.";
    static final String STATE_HAS_INVALID_REFERENCES = "StateDraft with key: '%s' has invalid state transitions";

    private final List<StateDraft> stateDrafts;
    private final StateSync stateSync;
    private final Set<StateDraft> validDrafts = new HashSet<>();
    private final Set<String> keysToCache = new HashSet<>();

    public StateBatchProcessor(@Nonnull final List<StateDraft> stateDrafts,
                               @Nonnull final StateSync stateSync) {
        this.stateDrafts = stateDrafts;
        this.stateSync = stateSync;
    }

    /**
     * This method validates the batch of drafts, and only for valid drafts it adds the valid draft
     * to a {@code validDrafts} set, the keys of their referenced states and
     * the keys of the missing parents to a {@code keysToCache} set.
     *
     * <p>A valid state draft is one which satisfies the following conditions:
     * <ol>
     * <li>It is not null</li>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has no invalid state reference on an transition
     * A valid reference is simply one which has its id field's value not blank (null/empty)</li>
     * </ol>
     */
    public void validateBatch() {
        for (StateDraft stateDraft : stateDrafts) {
            if (stateDraft != null) {
                final String stateDraftKey = stateDraft.getKey();
                if (isNotBlank(stateDraftKey)) {
                    try {
                        keysToCache.add(stateDraftKey);
                        final Set<String> referencesStateKeys = getTransitionKeys(stateDraft);
                        keysToCache.addAll(referencesStateKeys);
                        validDrafts.add(stateDraft);
                    } catch (InvalidStateDraftException exception) {
                        handleError(exception);
                    }
                } else {
                    final String errorMessage = format(STATE_DRAFT_KEY_NOT_SET, stateDraft.getName());
                    handleError(new InvalidStateDraftException(errorMessage));
                }
            } else {
                handleError(new InvalidStateDraftException(STATE_DRAFT_IS_NULL));
            }
        }
    }

    @Nonnull
    private static Set<String> getTransitionKeys(@Nonnull final StateDraft stateDraft)
        throws InvalidStateDraftException {

        final Set<Reference<State>> transitions = stateDraft.getTransitions();
        if (transitions == null || transitions.isEmpty()) {
            return emptySet();
        }

        final Set<String> referencedStateKeys = new HashSet<>();
        final List<Reference<State>> invalidStates = new ArrayList<>();
        for (Reference<State> transition : transitions) {
            if (transition != null) {
                try {
                    referencedStateKeys.add(getStateKey(transition));
                } catch (InvalidReferenceException invalidReferenceException) {
                    invalidStates.add(transition);
                }
            }
        }

        if (!invalidStates.isEmpty()) {
            final String errorMessage = format(STATE_HAS_INVALID_REFERENCES, stateDraft.getKey());
            throw new InvalidStateDraftException(errorMessage,
                new InvalidReferenceException(BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
        }
        return referencedStateKeys;
    }

    @Nonnull
    private static String getStateKey(@Nonnull final Reference<State> stateReference)
        throws InvalidReferenceException {

        final String key = stateReference.getId();
        if (isBlank(key)) {
            throw new InvalidReferenceException(BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER);
        }
        return key;
    }

    private void handleError(@Nonnull final Throwable throwable) {
        stateSync.getSyncOptions().applyErrorCallback(throwable.getMessage(), throwable);
        stateSync.getStatistics().incrementFailed();
    }

    public Set<StateDraft> getValidDrafts() {
        return validDrafts;
    }

    public Set<String> getKeysToCache() {
        return keysToCache;
    }
}
