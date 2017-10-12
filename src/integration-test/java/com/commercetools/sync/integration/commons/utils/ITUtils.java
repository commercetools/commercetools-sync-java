package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.fetchAndProcess;

public final class ITUtils {

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} Types from CTP
     * projects defined by the {@code sphereClient}
     *
     * @param sphereClient defines the CTP project to delete the Types from.
     */
    public static void deleteTypes(@Nonnull final SphereClient sphereClient) {
        fetchAndProcess(sphereClient, TypeQuery.of().withLimit(QUERY_MAX_LIMIT), TypeDeleteCommand::of);
    }

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} Types from CTP
     * projects defined by the {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteTypesFromTargetAndSource() {
        deleteTypes(CTP_TARGET_CLIENT);
        deleteTypes(CTP_SOURCE_CLIENT);
    }

    /**
     * Builds a JSON String that represents the fields of the supplied instance of {@link BaseSyncStatistics}.
     * Note: The order of the fields in the built JSON String depends on the order of the instance variables in this
     * class.
     *
     * @param statistics the instance of {@link BaseSyncStatistics} from which to create a JSON String.
     * @return a JSON String representation of the statistics object.
     */
    public static String getStatisticsAsJSONString(@Nonnull final BaseSyncStatistics statistics)
        throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(statistics);
    }
}
