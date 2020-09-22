package com.commercetools.sync.customobjects;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class CustomObjectSyncOptions extends BaseSyncOptions<CustomObject<JsonNode>,
    CustomObjectDraft<JsonNode>> {

    CustomObjectSyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final QuadConsumer<SyncException, Optional<CustomObjectDraft<JsonNode>>,
            Optional<CustomObject<JsonNode>>,
            List<UpdateAction<CustomObject<JsonNode>>>> errorCallBack,
        @Nullable final TriConsumer<SyncException, Optional<CustomObjectDraft<JsonNode>>,
            Optional<CustomObject<JsonNode>>>
            warningCallBack,
        final int batchSize,
        @Nullable final TriFunction<List<UpdateAction<CustomObject<JsonNode>>>, CustomObjectDraft<JsonNode>,
            CustomObject<JsonNode>,
            List<UpdateAction<CustomObject<JsonNode>>>> beforeUpdateCallback,
        @Nullable final Function<CustomObjectDraft<JsonNode>, CustomObjectDraft<JsonNode>> beforeCreateCallback) {
        super(
            ctpClient,
            errorCallBack,
            warningCallBack,
            batchSize,
            beforeUpdateCallback,
            beforeCreateCallback);
    }
}
