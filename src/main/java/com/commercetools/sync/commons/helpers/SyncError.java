package com.commercetools.sync.commons.helpers;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// TODO: UNIT TEST
// TODO: DOCUMENT
public class SyncError extends SyncWarning {
    private final Throwable exception;

    public SyncError(@Nullable final String resourceInternalId,
                     @Nonnull final String message,
                     @Nonnull final Throwable exception) {
        super(resourceInternalId, message);
        this.exception = exception;
    }

    public Throwable getException() {
        return exception;
    }

    public static SyncError of(@Nullable final String resourceInternalId,
                               @Nonnull final String message,
                               @Nonnull final Throwable exception) {
        return new SyncError(resourceInternalId, message, exception);
    }
}
