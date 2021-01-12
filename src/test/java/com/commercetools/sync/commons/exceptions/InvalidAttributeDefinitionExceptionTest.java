package com.commercetools.sync.commons.exceptions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InvalidAttributeDefinitionExceptionTest {

  @Test
  void invalidAttributeDefinitionException_WithMessageAndNoCauses_ShouldBuildExceptionCorrectly() {
    final String message = "invalid attribute definition";
    final InvalidReferenceException invalidReferenceException =
        new InvalidReferenceException("invalid reference");

    assertThatThrownBy(
            () -> {
              throw new InvalidAttributeDefinitionException(message, invalidReferenceException);
            })
        .isExactlyInstanceOf(InvalidAttributeDefinitionException.class)
        .hasCause(invalidReferenceException)
        .hasMessage(message);
  }
}
