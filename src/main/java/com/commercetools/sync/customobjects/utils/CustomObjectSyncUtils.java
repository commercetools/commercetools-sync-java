package com.commercetools.sync.customobjects.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class CustomObjectSyncUtils {

    /**
     * Compares the value of a {@link CustomObject} to the value of a {@link CustomObjectDraft}.
     * It returns a boolean whether the values are identical or not.
     *
     * @param oldCustomObject the {@link CustomObject} which should be synced.
     * @param newCustomObject the {@link CustomObjectDraft} with the new data.
     * @return A boolean whether the value of the CustomObject and CustomObjectDraft is identical or not.
     */

    @Nonnull
    public static boolean hasIdenticalValue(
            @Nonnull final CustomObject<JsonNode> oldCustomObject,
            @Nonnull final CustomObjectDraft<JsonNode> newCustomObject) {
        JsonNode oldValue = oldCustomObject.getValue();
        JsonNode newValue = newCustomObject.getValue();

        return oldValue.equals(newValue);
    }

    /**
     * Given a list of {@link CustomObjectDraft} and a {@code batchSize}, this method distributes the custom
     * object drafts into batches with the {@code batchSize}. Each batch is represented by a {@link List} of drafts and
     * all the batches are grouped and represented by a {@link List}&lt;{@link List}&gt; of elements, which is returned
     * by the method.
     *
     * @param elements  the list of custom object drafts to split into batches.
     * @param batchSize the size of each batch.
     * @return a list of lists where each list represents a batch of {@link CustomObjectDraft}.
     */

    @Nonnull
    public static List<List<CustomObjectDraft<JsonNode>>> batchElements(
            @Nonnull final List<CustomObjectDraft<JsonNode>> elements,
            final int batchSize) {
        List<List<CustomObjectDraft<JsonNode>>> batches = new ArrayList<>();
        for (int i = 0; i < elements.size() && batchSize > 0; i += batchSize) {
            batches.add(elements.subList(i, Math.min(i + batchSize, elements.size())));
        }
        return batches;
    }

}
