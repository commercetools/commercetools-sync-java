package com.commercetools.sync.commons.exceptions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BuildUpdateActionExceptionTest {

  @Test
  void buildUpdateActionException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
    final String message = "foo";

    assertThatThrownBy(
            () -> {
              throw new BuildUpdateActionException(message);
            })
        .isExactlyInstanceOf(BuildUpdateActionException.class)
        .hasNoCause()
        .hasMessage(message);
  }

  @Test
  void buildUpdateActionException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
    final String message = "foo";
    final IllegalArgumentException cause = new IllegalArgumentException();

    assertThatThrownBy(
            () -> {
              throw new BuildUpdateActionException(message, cause);
            })
        .isExactlyInstanceOf(BuildUpdateActionException.class)
        .hasCause(cause)
        .hasMessage(message);
  }

  @Test
  void buildUpdateActionException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
    final IllegalArgumentException cause = new IllegalArgumentException();

    assertThatThrownBy(
            () -> {
              throw new BuildUpdateActionException(cause);
            })
        .isExactlyInstanceOf(BuildUpdateActionException.class)
        .hasCause(cause)
        .hasMessage(cause.toString());
  }
}
