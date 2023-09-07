package com.commercetools.sync.commons.exceptions;

import java.util.Set;
import javax.annotation.Nonnull;

public class InvalidProductTypeException extends Exception {
  private final Set<Throwable> causes;

  public InvalidProductTypeException(
      @Nonnull final String message, @Nonnull final Set<Throwable> causes) {
    super(ExceptionUtils.buildMessage(message, causes, 2));
    this.causes = causes;
  }

  public Set<Throwable> getCauses() {
    return causes;
  }
}
