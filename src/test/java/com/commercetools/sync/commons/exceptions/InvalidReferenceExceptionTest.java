package com.commercetools.sync.commons.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvalidReferenceExceptionTest {

    @Test
    void invalidReferenceException_WithMessage_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new InvalidReferenceException(message);
        }).isExactlyInstanceOf(InvalidReferenceException.class)
          .hasNoCause()
          .hasMessage(message);
    }
}