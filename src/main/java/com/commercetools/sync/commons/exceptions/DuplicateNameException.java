package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class DuplicateNameException extends RuntimeException {
    public DuplicateNameException(@Nonnull final String message) {
        super(message);
    }

    public DuplicateNameException(@Nonnull final Throwable cause) {
        super(cause);
    }

    public DuplicateNameException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }
}
