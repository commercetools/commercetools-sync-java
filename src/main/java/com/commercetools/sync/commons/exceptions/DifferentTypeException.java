package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class DifferentTypeException extends RuntimeException {
    public DifferentTypeException(@Nonnull final String message) {
        super(message);
    }

    public DifferentTypeException(@Nonnull final Throwable cause) {
        super(cause);
    }

    public DifferentTypeException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }
}
