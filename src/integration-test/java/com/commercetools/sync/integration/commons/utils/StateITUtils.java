package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateDeleteCommand;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.services.impl.StateServiceImpl.buildStateQuery;

public class StateITUtils {

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
        final List<CompletableFuture> stateDeleteFutures = new ArrayList<>();

        final Consumer<List<State>> statePageDelete = states -> states.forEach(state -> {
            final CompletableFuture<State> deleteFuture =
                ctpClient.execute(StateDeleteCommand.of(state)).toCompletableFuture();
            stateDeleteFutures.add(deleteFuture);
        });

        CtpQueryUtils.queryAll(ctpClient, buildStateQuery(stateType), statePageDelete)
                     .thenCompose(result -> CompletableFuture
                         .allOf(stateDeleteFutures
                             .toArray(new CompletableFuture[stateDeleteFutures.size()])))
                     .toCompletableFuture().join();
    }
}
