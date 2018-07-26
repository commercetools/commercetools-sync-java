package com.commercetools.sync.commons.exceptions;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DuplicateNameExceptionTest {
    @Test
    public void duplicateNameException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new DuplicateNameException(message);
        }).isExactlyInstanceOf(DuplicateNameException.class)
            .hasNoCause()
            .hasMessage(message);
    }

    @Test
    public void duplicateNameException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
        final String message = "foo";
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateNameException(message, cause);
        }).isExactlyInstanceOf(DuplicateNameException.class)
            .hasCause(cause)
            .hasMessage(message);
    }

    @Test
    public void duplicateNameException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateNameException(cause);
        }).isExactlyInstanceOf(DuplicateNameException.class)
            .hasCause(cause)
            .hasMessage(cause.toString());

    }
}
