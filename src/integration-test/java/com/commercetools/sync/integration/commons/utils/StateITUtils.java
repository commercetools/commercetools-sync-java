package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.states.commands.StateDeleteCommand;

import javax.annotation.Nonnull;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.services.impl.StateServiceImpl.buildStateQuery;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;

public final class StateITUtils {

    private static final String STATE_KEY = "old_state_key";

    /**
     * Deletes all states with {@code stateType} from CTP projects defined
     * by the {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteStatesFromTargetAndSourceByType(@Nonnull final StateType stateType) {
        deleteStates(CTP_TARGET_CLIENT, stateType);
        deleteStates(CTP_SOURCE_CLIENT, stateType);
    }

    /**
     * Deletes all states with {@code stateType} from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the states from.
     */
    public static void deleteStates(@Nonnull final SphereClient ctpClient,
                                    @Nonnull final StateType stateType) {
        queryAndExecute(ctpClient, buildStateQuery(stateType), StateDeleteCommand::of);
    }

    /**
     * Creates a {@link State} with the {@link StateType} supplied in the CTP project defined by the {@code ctpClient}
     * in a blocking fashion. The create state will have a key with the value {@value STATE_KEY}.
     *
     * @param ctpClient defines the CTP project to create the state in.
     * @param stateType defines the state type to create the state with.
     * @return the created state.
     */
    public static State createState(@Nonnull final SphereClient ctpClient, @Nonnull final StateType stateType) {
        final StateDraft stateDraft = StateDraftBuilder.of(STATE_KEY, stateType)
                                                       .build();
        return executeBlocking(ctpClient.execute(StateCreateCommand.of(stateDraft)));
    }

    private StateITUtils() {
    }
}
