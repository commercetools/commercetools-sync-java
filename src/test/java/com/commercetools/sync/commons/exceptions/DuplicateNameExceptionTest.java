package com.commercetools.sync.commons.exceptions;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuplicateNameExceptionTest {
    @Test
    void duplicateNameException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new DuplicateNameException(message);
        }).isExactlyInstanceOf(DuplicateNameException.class)
          .hasNoCause()
          .hasMessage(message);
    }

    @Test
    void duplicateNameException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
        final String message = "foo";
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateNameException(message, cause);
        }).isExactlyInstanceOf(DuplicateNameException.class)
          .hasCause(cause)
          .hasMessage(message);
    }

    @Test
    void duplicateNameException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateNameException(cause);
        }).isExactlyInstanceOf(DuplicateNameException.class)
          .hasCause(cause)
          .hasMessage(cause.toString());

    }
}
