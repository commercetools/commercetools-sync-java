package com.commercetools.sync.customobjects.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import javax.annotation.Nonnull;

public class CustomObjectSyncUtils {

    /**
     * Compares the value of a {@link CustomObject} to the value of a {@link CustomObjectDraft}.
     * It returns a boolean whether the values are identical or not.
     *
     * @param oldCustomObject the {@link CustomObject} which should be synced.
     * @param newCustomObject the {@link CustomObjectDraft} with the new data.
     * @return A boolean whether the value of the CustomObject and CustomObjectDraft is identical or not.
     */

    public static boolean hasIdenticalValue(
        @Nonnull final CustomObject<JsonNode> oldCustomObject,
        @Nonnull final CustomObjectDraft<JsonNode> newCustomObject) {
        JsonNode oldValue = oldCustomObject.getValue();
        JsonNode newValue = newCustomObject.getValue();

        return oldValue.equals(newValue);
    }
}