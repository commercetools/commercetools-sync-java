package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class InvalidReferenceException extends Exception {
    public InvalidReferenceException(@Nonnull final String message) {
        super(message);
    }

    public InvalidReferenceException(@Nonnull final Throwable cause) {
        super(cause);
    }

    public InvalidReferenceException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }
}