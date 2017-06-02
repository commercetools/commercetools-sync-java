package com.commercetools.sync.commons.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Provides formatter which format {@link BaseSyncStatistics} as JSON string.
 */
public class JsonSyncStatisticsFormatterImpl implements SyncStatisticsFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonSyncStatisticsFormatterImpl.class);

    /**
     * Builds a JSON String that represents the fields of the supplied instance of {@link BaseSyncStatistics}.
     * Note: The order of the fields in the built JSON String depends on the order of the instance variables in this
     * class.
     *
     * @param statistics the instance of {@link BaseSyncStatistics} from which to create a JSON String.
     * @return a JSON representation of the given {@code statistics} as a String.
     */
    @Override
    public String format(@Nonnull final BaseSyncStatistics statistics) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(statistics);
        } catch (JsonProcessingException processingException) {
            LOGGER.error("Failed to build JSON String of summary.", processingException);
        }
        return "{}";
    }
}
