package com.commercetools.sync.commons.exceptions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReferenceResolutionExceptionTest {
  @Test
  void referenceResolutionException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
    final String message = "foo";

    assertThatThrownBy(
            () -> {
              throw new ReferenceResolutionException(message);
            })
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasNoCause()
        .hasMessage(message);
  }

  @Test
  void referenceResolutionException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
    final String message = "foo";
    final IllegalArgumentException cause = new IllegalArgumentException();

    assertThatThrownBy(
            () -> {
              throw new ReferenceResolutionException(message, cause);
            })
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasCause(cause)
        .hasMessage(message);
  }

  @Test
  void referenceResolutionException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
    final IllegalArgumentException cause = new IllegalArgumentException();

    assertThatThrownBy(
            () -> {
              throw new ReferenceResolutionException(cause);
            })
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasCause(cause)
        .hasMessage(cause.toString());
  }
}
