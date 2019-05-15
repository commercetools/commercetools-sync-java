package com.commercetools.sync.commons.exceptions;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InvalidReferenceExceptionTest {

    @Test
    public void invalidReferenceException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new InvalidReferenceException(message);
        }).isExactlyInstanceOf(InvalidReferenceException.class)
          .hasNoCause()
          .hasMessage(message);
    }

    @Test
    public void invalidReferenceException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
        final String message = "foo";
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new InvalidReferenceException(message, cause);
        }).isExactlyInstanceOf(InvalidReferenceException.class)
          .hasCause(cause)
          .hasMessage(message);
    }

    @Test
    public void invalidReferenceException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new InvalidReferenceException(cause);
        }).isExactlyInstanceOf(InvalidReferenceException.class)
          .hasCause(cause)
          .hasMessage(cause.toString());

    }
}