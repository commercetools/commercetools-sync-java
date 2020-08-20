package com.commercetools.sync.commons.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncExceptionTest {

    @Test
    void syncException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new SyncException(message);
        }).isExactlyInstanceOf(SyncException.class)
          .hasNoCause()
          .hasMessage(message);
    }

    @Test
    void syncException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
        final String message = "foo";
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new SyncException(message, cause);
        }).isExactlyInstanceOf(SyncException.class)
          .hasCause(cause)
          .hasMessage(message);
    }

    @Test
    void syncException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new SyncException(cause);
        }).isExactlyInstanceOf(SyncException.class)
          .hasCause(cause)
          .hasMessage(cause.toString());
    }
}
