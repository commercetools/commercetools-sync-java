package com.commercetools.sync.commons.exceptions;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DifferentTypeExceptionTest {
    @Test
    public void differentTypeException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new DifferentTypeException(message);
        }).isExactlyInstanceOf(DifferentTypeException.class)
          .hasNoCause()
          .hasMessage(message);
    }

    @Test
    public void differentTypeException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
        final String message = "foo";
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DifferentTypeException(message, cause);
        }).isExactlyInstanceOf(DifferentTypeException.class)
          .hasCause(cause)
          .hasMessage(message);
    }

    @Test
    public void differentTypeException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DifferentTypeException(cause);
        }).isExactlyInstanceOf(DifferentTypeException.class)
          .hasCause(cause)
          .hasMessage(cause.toString());

    }
}
