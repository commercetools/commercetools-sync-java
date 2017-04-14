package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class BuildUpdateActionException extends Exception {

    public BuildUpdateActionException(@Nonnull final String message) {
        super(message);
    }
}
