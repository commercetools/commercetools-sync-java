package com.commercetools.sync.sdk2.commons.exceptions;

import static com.commercetools.sync.sdk2.commons.exceptions.ExceptionUtils.buildMessage;

import java.util.Set;
import javax.annotation.Nonnull;

public class InvalidProductTypeException extends Exception {
  private final Set<Throwable> causes;

  public InvalidProductTypeException(
      @Nonnull final String message, @Nonnull final Set<Throwable> causes) {
    super(buildMessage(message, causes, 2));
    this.causes = causes;
  }

  public Set<Throwable> getCauses() {
    return causes;
  }
}
