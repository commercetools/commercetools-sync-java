package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class InvalidProductTypeDraftException extends Exception {
    public InvalidProductTypeDraftException(@Nonnull final String message) {
        super(message);
    }

    public InvalidProductTypeDraftException(@Nonnull final Throwable cause) {
        super(cause);
    }

    public InvalidProductTypeDraftException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }
}
