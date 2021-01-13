package com.commercetools.sync.states.helpers;

import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_REFERENCE;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.sync.commons.exceptions.InvalidReferenceException;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.states.StateSyncOptions;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class StateBatchValidator
    extends BaseBatchValidator<StateDraft, StateSyncOptions, StateSyncStatistics> {

  static final String STATE_DRAFT_KEY_NOT_SET =
      "StateDraft with name: %s doesn't have a key. "
          + "Please make sure all state drafts have keys.";
  static final String STATE_DRAFT_IS_NULL = "StateDraft is null.";
  static final String STATE_HAS_INVALID_REFERENCES =
      "StateDraft with key: '%s' has invalid state transitions";

  public StateBatchValidator(
      @Nonnull final StateSyncOptions syncOptions,
      @Nonnull final StateSyncStatistics syncStatistics) {
    super(syncOptions, syncStatistics);
  }

  /**
   * Given the {@link List}&lt;{@link StateDraft}&gt; of drafts this method attempts to validate
   * drafts and collect referenced keys from the draft and return an {@link ImmutablePair}&lt;{@link
   * Set}&lt;{@link StateDraft}&gt; ,{@link Set}&lt;{@link String}&gt;&gt; which contains the {@link
   * Set} of valid drafts and referenced state transition keys.
   *
   * <p>A valid state draft is one which satisfies the following conditions:
   *
   * <ol>
   *   <li>It is not null
   *   <li>It has a key which is not blank (null/empty)
   *   <li>It has no invalid state reference on an transition A valid reference is simply one which
   *       has its id field's value not blank (null/empty)
   * </ol>
   *
   * @param stateDrafts the state drafts to validate and collect referenced state transition keys.
   * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link ProductTypeDraft}&gt;, {@link
   *     Set}&lt;{@link String}&gt;&gt; which contains the {@link Set} of valid drafts and
   *     referenced product type keys.
   */
  @Override
  public ImmutablePair<Set<StateDraft>, Set<String>> validateAndCollectReferencedKeys(
      @Nonnull final List<StateDraft> stateDrafts) {

    final Set<String> stateTransitionKeys = new HashSet<>();

    final Set<StateDraft> validDrafts =
        stateDrafts.stream()
            .filter(stateDraft -> isValidStateDraft(stateDraft, stateTransitionKeys))
            .collect(Collectors.toSet());

    return ImmutablePair.of(validDrafts, stateTransitionKeys);
  }

  private boolean isValidStateDraft(
      @Nullable final StateDraft stateDraft, @Nonnull final Set<String> stateTransitionKeys) {

    if (stateDraft == null) {
      handleError(STATE_DRAFT_IS_NULL);
    } else if (isBlank(stateDraft.getKey())) {
      handleError(format(STATE_DRAFT_KEY_NOT_SET, stateDraft.getName()));
    } else {
      try {
        final Set<String> referencesStateKeys = getTransitionKeys(stateDraft);
        stateTransitionKeys.addAll(referencesStateKeys);
        return true;
      } catch (SyncException exception) {
        handleError(exception);
      }
    }

    return false;
  }

  @Nonnull
  private static Set<String> getTransitionKeys(@Nonnull final StateDraft stateDraft)
      throws SyncException {

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
      throw new SyncException(
          errorMessage, new InvalidReferenceException(BLANK_ID_VALUE_ON_REFERENCE));
    }
    return referencedStateKeys;
  }

  @Nonnull
  private static String getStateKey(@Nonnull final Reference<State> stateReference)
      throws InvalidReferenceException {

    final String key = stateReference.getId();
    if (isBlank(key)) {
      throw new InvalidReferenceException(BLANK_ID_VALUE_ON_REFERENCE);
    }
    return key;
  }
}
