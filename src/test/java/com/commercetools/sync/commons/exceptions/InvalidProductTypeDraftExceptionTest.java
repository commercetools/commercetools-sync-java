package com.commercetools.sync.commons.exceptions;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InvalidProductTypeDraftExceptionTest {

    @Test
    public void invalidProductTypeDraftException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new InvalidProductTypeDraftException(message);
        }).isExactlyInstanceOf(InvalidProductTypeDraftException.class)
          .hasNoCause()
          .hasMessage(message);
    }

    @Test
    public void invalidProductTypeDraftException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
        final String message = "foo";
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new InvalidProductTypeDraftException(message, cause);
        }).isExactlyInstanceOf(InvalidProductTypeDraftException.class)
          .hasCause(cause)
          .hasMessage(message);
    }

    @Test
    public void invalidProductTypeDraftException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new InvalidProductTypeDraftException(cause);
        }).isExactlyInstanceOf(InvalidProductTypeDraftException.class)
          .hasCause(cause)
          .hasMessage(cause.toString());

    }
}