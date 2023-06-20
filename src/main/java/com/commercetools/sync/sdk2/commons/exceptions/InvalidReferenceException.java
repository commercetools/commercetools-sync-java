package com.commercetools.sync.sdk2.commons.exceptions;

import javax.annotation.Nonnull;

public class InvalidReferenceException extends Exception {

  public InvalidReferenceException(@Nonnull final String message) {
    super(message);
  }
}
