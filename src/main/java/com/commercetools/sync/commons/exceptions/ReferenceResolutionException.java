package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class ReferenceResolutionException extends Exception {
    public ReferenceResolutionException(@Nonnull final String message) {
        super(message);
    }

    public ReferenceResolutionException(@Nonnull final Throwable cause) {
        super(cause);
    }

    public ReferenceResolutionException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }
}
