package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class ReferenceTransformException extends RuntimeException {

  public ReferenceTransformException(
      @Nonnull final String message, @Nonnull final Throwable cause) {
    super(message, cause);
  }
}
