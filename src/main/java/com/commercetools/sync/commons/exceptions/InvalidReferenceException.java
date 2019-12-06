package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class InvalidReferenceException extends Exception {

    public InvalidReferenceException(@Nonnull final String message) {
        super(message);
    }
}