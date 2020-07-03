package com.commercetools.sync.commons.exceptions;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuplicateCountryCodeExceptionTest {

    @Test
    void duplicateCountryCodeException_WithMessageOnly_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new DuplicateCountryCodeException(message);
        }).isExactlyInstanceOf(DuplicateCountryCodeException.class)
          .hasNoCause()
          .hasMessage(message);
    }

    @Test
    void duplicateCountryCodeException_WithMessageAndCause_ShouldBuildExceptionCorrectly() {
        final String message = "foo";
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateCountryCodeException(message, cause);
        }).isExactlyInstanceOf(DuplicateCountryCodeException.class)
          .hasCause(cause)
          .hasMessage(message);
    }

    @Test
    void duplicateCountryCodeException_WithCauseOnly_ShouldBuildExceptionCorrectly() {
        final IllegalArgumentException cause = new IllegalArgumentException();

        assertThatThrownBy(() -> {
            throw new DuplicateCountryCodeException(cause);
        }).isExactlyInstanceOf(DuplicateCountryCodeException.class)
          .hasCause(cause)
          .hasMessage(cause.toString());

    }
}
