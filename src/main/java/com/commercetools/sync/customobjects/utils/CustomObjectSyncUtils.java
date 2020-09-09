package com.commercetools.sync.customobjects.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import javax.annotation.Nonnull;



public class CustomObjectSyncUtils {

    @Nonnull
    public static boolean hasIdenticalValue(
            @Nonnull final CustomObject<JsonNode> oldCustomObject,
            @Nonnull final CustomObjectDraft<JsonNode> newCustomObject) {
        JsonNode oldValue = oldCustomObject.getValue();
        JsonNode newValue = newCustomObject.getValue();

        return oldValue.equals(newValue);
    }



}
