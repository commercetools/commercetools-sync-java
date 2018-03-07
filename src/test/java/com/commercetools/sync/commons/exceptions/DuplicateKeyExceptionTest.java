package com.commercetools.sync.commons.exceptions;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DuplicateKeyExceptionTest {

    @Test
    public void duplicateKeyException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new DuplicateKeyException(message);
        }).isExactlyInstanceOf(DuplicateKeyException.class)
          .hasNoCause()
          .hasMessage(message);
    }

    @Test
    public void duplicateKeyException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
        final String message = "foo";
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateKeyException(message, cause);
        }).isExactlyInstanceOf(DuplicateKeyException.class)
          .hasCause(cause)
          .hasMessage(message);
    }

    @Test
    public void duplicateKeyException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateKeyException(cause);
        }).isExactlyInstanceOf(DuplicateKeyException.class)
          .hasCause(cause)
          .hasMessage(cause.toString());

    }
}
