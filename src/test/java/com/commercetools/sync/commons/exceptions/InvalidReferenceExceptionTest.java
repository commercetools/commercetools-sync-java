package com.commercetools.sync.commons.exceptions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InvalidReferenceExceptionTest {

  @Test
  void invalidReferenceException_WithMessage_ShouldBuildExceptionCorrectly() {
    final String message = "foo";

    assertThatThrownBy(
            () -> {
              throw new InvalidReferenceException(message);
            })
        .isExactlyInstanceOf(InvalidReferenceException.class)
        .hasNoCause()
        .hasMessage(message);
  }
}
