package com.commercetools.sync.integration.externalsource.states;

import com.commercetools.sync.states.StateSync;
import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateRole;
import io.sphere.sdk.states.StateType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.deleteStates;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.getStateByKey;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.populateTargetProject;
import static java.util.Collections.singletonList;

class StateSyncIT {

    @BeforeEach
    void setup() {
        deleteStates(CTP_TARGET_CLIENT);
        populateTargetProject();
    }

    @AfterAll
    static void tearDown() {
        deleteStates(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_WithUpdatedState_ShouldUpdateState() {
        // preparation
        final StateDraft stateDraft = StateDraftBuilder.of("state-3-key", StateType.REVIEW_STATE)
            .name(LocalizedString.ofEnglish("state-3-name-new"))
            .description(LocalizedString.ofEnglish("state-3-desc-new"))
            .roles(Collections.singleton(
               StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(singletonList(stateDraft))
            .toCompletableFuture()
            .join();

        // assertion
        assertThat(stateSyncStatistics).hasValues(1, 0, 1, 0, 0);

        final Optional<State> oldStateAfter = getStateByKey(CTP_TARGET_CLIENT, "state-3-key");

        Assertions.assertThat(oldStateAfter).hasValueSatisfying(state -> {
            Assertions.assertThat(state.getName()).isEqualTo("state-3-name-new");
            Assertions.assertThat(state.getDescription()).isEqualTo("state-3-desc-new");
        });
    }
}
