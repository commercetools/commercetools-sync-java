package com.commercetools.sync.commons.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvalidStateDraftExceptionTest {

    @Test
    void invalidStateDraftException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new InvalidStateDraftException(message);
        }).isExactlyInstanceOf(InvalidStateDraftException.class)
          .hasNoCause()
          .hasMessage(message);
    }

    @Test
    void invalidStateDraftException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
        final String message = "foo";
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new InvalidStateDraftException(message, cause);
        }).isExactlyInstanceOf(InvalidStateDraftException.class)
          .hasCause(cause)
          .hasMessage(message);
    }

    @Test
    void invalidStateDraftException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new InvalidStateDraftException(cause);
        }).isExactlyInstanceOf(InvalidStateDraftException.class)
          .hasCause(cause)
          .hasMessage(cause.toString());

    }

}
