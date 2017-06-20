package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;

import static com.commercetools.sync.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.commons.utils.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.commons.utils.SphereClientUtils.fetchAndProcess;

public class ITUtils {

    /**
     * Deletes up to {@link com.commercetools.sync.commons.utils.SphereClientUtils#QUERY_MAX_LIMIT} Types from CTP
     * projects defined by the {@code sphereClient}
     *
     * @param sphereClient defines the CTP project to delete the Types from.
     */
    public static void deleteTypes(@Nonnull final SphereClient sphereClient) {
        fetchAndProcess(sphereClient, TypeQuery.of().withLimit(QUERY_MAX_LIMIT), TypeDeleteCommand::of);
    }

    /**
     * Deletes up to {@link com.commercetools.sync.commons.utils.SphereClientUtils#QUERY_MAX_LIMIT} Types from CTP
     * projects defined by the {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteTypesFromTargetAndSource() {
        deleteTypes(CTP_TARGET_CLIENT);
        deleteTypes(CTP_SOURCE_CLIENT);
    }
}
