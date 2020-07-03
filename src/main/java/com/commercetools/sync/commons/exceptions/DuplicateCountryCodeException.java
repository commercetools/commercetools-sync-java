package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class DuplicateCountryCodeException extends RuntimeException {
    public DuplicateCountryCodeException(@Nonnull final String message) {
        super(message);
    }

    public DuplicateCountryCodeException(@Nonnull final Throwable cause) {
        super(cause);
    }

    public DuplicateCountryCodeException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }
}
