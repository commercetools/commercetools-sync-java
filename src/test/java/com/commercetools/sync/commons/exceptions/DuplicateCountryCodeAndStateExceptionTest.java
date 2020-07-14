package com.commercetools.sync.commons.exceptions;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuplicateCountryCodeAndStateExceptionTest {

    @Test
    void duplicateCountryCodeException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new DuplicateCountryCodeAndStateException(message);
        }).isExactlyInstanceOf(DuplicateCountryCodeAndStateException.class)
          .hasNoCause()
          .hasMessage(message);
    }

    @Test
    void duplicateCountryCodeException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
        final String message = "foo";
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateCountryCodeAndStateException(message, cause);
        }).isExactlyInstanceOf(DuplicateCountryCodeAndStateException.class)
          .hasCause(cause)
          .hasMessage(message);
    }

    @Test
    void duplicateCountryCodeException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateCountryCodeAndStateException(cause);
        }).isExactlyInstanceOf(DuplicateCountryCodeAndStateException.class)
          .hasCause(cause)
          .hasMessage(cause.toString());

    }
}
