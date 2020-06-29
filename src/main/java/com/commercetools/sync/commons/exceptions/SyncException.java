package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class SyncException extends Exception {

    public SyncException(@Nonnull final String message) {
        super(message);
    }

    public SyncException(@Nonnull final Throwable cause) {
        super(cause);
    }

    public SyncException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }

}
