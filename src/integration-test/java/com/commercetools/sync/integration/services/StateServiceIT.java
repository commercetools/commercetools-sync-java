package com.commercetools.sync.integration.services;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.impl.StateServiceImpl;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.createState;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.deleteStates;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static org.assertj.core.api.Assertions.assertThat;

public class StateServiceIT {
    private static final StateType STATE_TYPE = StateType.PRODUCT_STATE;

    private State oldState;
    private StateService stateService;
    private ArrayList<String> warnings;

    /**
     * Deletes states from the target CTP projects, then it populates the project with test data.
     */
    @Before
    public void setup() {
        deleteStates(CTP_TARGET_CLIENT, STATE_TYPE);
        warnings = new ArrayList<>();
        oldState = createState(CTP_TARGET_CLIENT, STATE_TYPE);
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                               .setWarningCallBack(warnings::add)
                                                                               .build();
        stateService = new StateServiceImpl(productSyncOptions, STATE_TYPE);
    }

    /**
     * Cleans up the target test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteStates(CTP_TARGET_CLIENT, STATE_TYPE);
    }

    @Test
    public void fetchCachedStateId_WithNonExistingState_ShouldNotFetchAState() {
        final Optional<String> stateId = stateService.fetchCachedStateId("non-existing-state-key")
                                                     .toCompletableFuture()
                                                     .join();
        assertThat(stateId).isEmpty();
        assertThat(warnings).isEmpty();
    }

    @Test
    public void fetchCachedStateId_WithExistingProductType_ShouldFetchProductTypeAndCache() {
        final Optional<String> stateId = stateService.fetchCachedStateId(oldState.getKey())
                                                                 .toCompletableFuture()
                                                                 .join();
        assertThat(stateId).isNotEmpty();
        assertThat(warnings).isEmpty();
    }

    @Test
    public void fetchCachedStateId_OnSecondTime_ShouldNotFindProductTypeInCache() {
        // Fetch any key to populate cache
        stateService.fetchCachedStateId("anyKey").toCompletableFuture().join();

        // Create new state
        final String newStateKey = "new_state_key";
        final StateDraft stateDraft = StateDraftBuilder.of(newStateKey, STATE_TYPE)
                                                       .build();
        executeBlocking(CTP_TARGET_CLIENT.execute(StateCreateCommand.of(stateDraft)));


        final Optional<String> stateId = stateService.fetchCachedStateId(newStateKey)
                                                     .toCompletableFuture()
                                                     .join();

        assertThat(stateId).isEmpty();
        assertThat(warnings).isEmpty();
    }

    @Test
    public void fetchCachedStateId_WithNullKey_ShouldReturnFutureWithEmptyOptional() {
        final Optional<String> stateId = stateService.fetchCachedStateId(null)
                                                     .toCompletableFuture()
                                                     .join();

        assertThat(stateId).isEmpty();
        assertThat(warnings).isEmpty();
    }
}
