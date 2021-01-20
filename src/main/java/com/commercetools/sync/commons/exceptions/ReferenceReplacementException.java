package com.commercetools.sync.commons.exceptions;

import static com.commercetools.sync.commons.exceptions.ExceptionUtils.buildMessage;

import java.util.Set;
import javax.annotation.Nonnull;

public class ReferenceReplacementException extends RuntimeException {
  private final Set<Throwable> causes;

  public ReferenceReplacementException(
      @Nonnull final String message, @Nonnull final Set<Throwable> causes) {
    super(buildMessage(message, causes, 1));
    this.causes = causes;
  }

  public Set<Throwable> getCauses() {
    return causes;
  }
}
