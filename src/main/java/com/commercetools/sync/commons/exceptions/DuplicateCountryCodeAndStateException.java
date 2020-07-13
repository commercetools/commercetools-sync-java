package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class DuplicateCountryCodeAndStateException extends RuntimeException {
    public DuplicateCountryCodeAndStateException(@Nonnull final String message) {
        super(message);
    }

    public DuplicateCountryCodeAndStateException(@Nonnull final Throwable cause) {
        super(cause);
    }

    public DuplicateCountryCodeAndStateException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }
}
