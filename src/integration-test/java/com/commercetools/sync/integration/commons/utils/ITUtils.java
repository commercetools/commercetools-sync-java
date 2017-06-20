package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;

public class ITUtils {

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} Types from CTP
     * projects defined by the {@code sphereClient}
     *
     * @param sphereClient defines the CTP project to delete the Types from.
     */
    public static void deleteTypes(@Nonnull final SphereClient sphereClient) {
        SphereClientUtils.fetchAndProcess(sphereClient, TypeQuery.of().withLimit(SphereClientUtils.QUERY_MAX_LIMIT), TypeDeleteCommand::of);
    }

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} Types from CTP
     * projects defined by the {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteTypesFromTargetAndSource() {
        deleteTypes(SphereClientUtils.CTP_TARGET_CLIENT);
        deleteTypes(SphereClientUtils.CTP_SOURCE_CLIENT);
    }
}
