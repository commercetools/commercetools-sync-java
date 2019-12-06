package com.commercetools.sync.commons.exceptions;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuplicateKeyExceptionTest {

    @Test
    void duplicateKeyException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new DuplicateKeyException(message);
        }).isExactlyInstanceOf(DuplicateKeyException.class)
          .hasNoCause()
          .hasMessage(message);
    }

    @Test
    void duplicateKeyException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
        final String message = "foo";
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateKeyException(message, cause);
        }).isExactlyInstanceOf(DuplicateKeyException.class)
          .hasCause(cause)
          .hasMessage(message);
    }

    @Test
    void duplicateKeyException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateKeyException(cause);
        }).isExactlyInstanceOf(DuplicateKeyException.class)
          .hasCause(cause)
          .hasMessage(cause.toString());

    }
}
