package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SyncException extends Exception {

    public SyncException(@Nonnull final String message) {
        super(message);
    }

    public SyncException(@Nonnull final Throwable cause) {
        super(cause);
    }

    public SyncException(@Nonnull final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }

}
