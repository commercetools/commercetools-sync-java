package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;
import java.util.Set;

import static com.commercetools.sync.commons.exceptions.ExceptionUtils.buildMessage;

public class ReferenceReplacementException extends RuntimeException {
    private final Set<Throwable> causes;

    public ReferenceReplacementException(@Nonnull final String message, @Nonnull final Set<Throwable> causes) {
        super(buildMessage(message, causes, 1));
        this.causes = causes;
    }

    public Set<Throwable> getCauses() {
        return causes;
    }
}
