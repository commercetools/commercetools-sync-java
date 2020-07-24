package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class InvalidStateDraftException extends Exception {
    public InvalidStateDraftException(@Nonnull final String message) {
        super(message);
    }

    public InvalidStateDraftException(@Nonnull final Throwable cause) {
        super(cause);
    }

    public InvalidStateDraftException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }
}
