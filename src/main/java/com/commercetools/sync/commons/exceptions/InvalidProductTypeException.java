package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;
import java.util.Set;

import static com.commercetools.sync.commons.exceptions.ExceptionUtils.buildMessage;

public class InvalidProductTypeException extends Exception {
    private final Set<Throwable> causes;

    public InvalidProductTypeException(@Nonnull final String message, @Nonnull final Set<Throwable> causes) {
        super(buildMessage(message, causes, 2));
        this.causes = causes;
    }

    public Set<Throwable> getCauses() {
        return causes;
    }
}