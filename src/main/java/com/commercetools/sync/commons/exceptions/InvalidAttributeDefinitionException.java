package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class InvalidAttributeDefinitionException extends Exception {

    public InvalidAttributeDefinitionException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }
}