package com.commercetools.sync.commons.helpers;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// TODO: UNIT TEST
// TODO: DOCUMENT
public class SyncWarning {
    private final String message;
    private final String resourceInternalId;

    SyncWarning(@Nullable final String resourceInternalId,
                @Nonnull final String message) {
        this.resourceInternalId = resourceInternalId;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getResourceInternalId() {
        return resourceInternalId;
    }

    public static SyncWarning of(@Nonnull final String resourceInternalId,
                                 @Nonnull final String message) {
        return new SyncWarning(message, resourceInternalId);
    }
}
