package com.commercetools.sync.states.helpers;

import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.states.helpers.StateBatchValidator.STATE_DRAFT_IS_NULL;
import static com.commercetools.sync.states.helpers.StateBatchValidator.STATE_DRAFT_KEY_NOT_SET;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StateBatchValidatorTest {

    private StateSyncOptions syncOptions;
    private StateSyncStatistics syncStatistics;
    private List<String> errorCallBackMessages;

    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);

        syncOptions = StateSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, updateActions) ->
                errorCallBackMessages.add(exception.getMessage()))
            .build();
        syncStatistics = mock(StateSyncStatistics.class);
    }

    @Test
    void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
        final Set<StateDraft> validDrafts = getValidDrafts(Collections.emptyList());

        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithNullStateDraft_ShouldHaveValidationErrorAndEmptyResult() {
        final Set<StateDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(STATE_DRAFT_IS_NULL);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithStateDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
        final StateDraft StateDraft = mock(StateDraft.class);
        final Set<StateDraft> validDrafts = getValidDrafts(Collections.singletonList(StateDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format(STATE_DRAFT_KEY_NOT_SET, StateDraft.getName()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithStateDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
        final StateDraft stateDraft = mock(StateDraft.class);
        when(stateDraft.getKey()).thenReturn(EMPTY);
        final Set<StateDraft> validDrafts = getValidDrafts(Collections.singletonList(stateDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format(STATE_DRAFT_KEY_NOT_SET, stateDraft.getName()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithValidDrafts_ShouldReturnCorrectResults() {
        final StateDraft validStateDraft = mock(StateDraft.class);
        when(validStateDraft.getKey()).thenReturn("validDraftKey");
        final Set<Reference<State>> transitionRefs = new HashSet<>();
        transitionRefs.add(State.referenceOfId("transition-key-1"));
        transitionRefs.add(State.referenceOfId("transition-key-2"));
        when(validStateDraft.getTransitions()).thenReturn(transitionRefs);

        final StateDraft validMainStateDraft = mock(StateDraft.class);
        when(validMainStateDraft.getKey()).thenReturn("validDraftKey1");

        final StateDraft invalidStateDraft = mock(StateDraft.class);

        final StateBatchValidator StateBatchValidator = new StateBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<StateDraft>, Set<String>> pair
            = StateBatchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(validStateDraft, invalidStateDraft, validMainStateDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(STATE_DRAFT_KEY_NOT_SET, invalidStateDraft.getName()));
        assertThat(pair.getLeft())
            .containsExactlyInAnyOrder(validStateDraft, validMainStateDraft);
        assertThat(pair.getRight())
            .containsExactlyInAnyOrder("transition-key-1", "transition-key-2");
    }

    @Nonnull
    private Set<StateDraft> getValidDrafts(@Nonnull final List<StateDraft> stateDrafts) {
        final StateBatchValidator StateBatchValidator = new StateBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<StateDraft>, Set<String>> pair =
            StateBatchValidator.validateAndCollectReferencedKeys(stateDrafts);
        return pair.getLeft();
    }
}
