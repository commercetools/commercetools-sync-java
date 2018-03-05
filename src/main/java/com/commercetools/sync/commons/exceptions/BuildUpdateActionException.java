package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class BuildUpdateActionException extends Exception {

    public BuildUpdateActionException(@Nonnull final String message) {
        super(message);
    }

    public BuildUpdateActionException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }

    public BuildUpdateActionException(@Nonnull final Throwable cause) {
        super(cause);
    }
}
