package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

public class DuplicateKeyException extends RuntimeException {
  public DuplicateKeyException(@Nonnull final String message) {
    super(message);
  }

  public DuplicateKeyException(@Nonnull final Throwable cause) {
    super(cause);
  }

  public DuplicateKeyException(@Nonnull final String message, @Nonnull final Throwable cause) {
    super(message, cause);
  }
}
