package com.commercetools.sync.commons.exceptions;

import io.sphere.sdk.client.BadGatewayException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvalidProductTypeExceptionTest {

    @Test
    void invalidProductTypeException_WithMessageAndNoCauses_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new InvalidProductTypeException(message, emptySet());
        })
            .isExactlyInstanceOf(InvalidProductTypeException.class)
            .hasNoCause()
            .hasMessage(message)
            .satisfies(exception -> {
                final InvalidProductTypeException invalidProductTypeException = (InvalidProductTypeException) exception;
                assertThat(invalidProductTypeException.getCauses()).isEmpty();
            });
    }

    @Test
    void invalidProductTypeException_WithMessageAndCausesWithoutMessages_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        final BadGatewayException cause1 = new BadGatewayException("");
        final InvalidReferenceException cause2 = new InvalidReferenceException("");
        final Set<Throwable> causes = new HashSet<>();
        causes.add(cause1);
        causes.add(cause2);

        assertThatThrownBy(() -> {
            throw new InvalidProductTypeException(message, causes);
        })
            .isExactlyInstanceOf(InvalidProductTypeException.class)
            .hasNoCause()
            .hasMessage(message)
            .satisfies(exception -> {
                final InvalidProductTypeException invalidProductTypeException = (InvalidProductTypeException) exception;
                assertThat(invalidProductTypeException.getCauses()).containsExactlyInAnyOrder(cause1, cause2);
            });
    }

    @Test
    void invalidProductTypeException_WithMessageAndCauseWithNullMessage_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        final BadGatewayException cause1 = new BadGatewayException();
        final Set<Throwable> causes = new HashSet<>();
        causes.add(cause1);

        assertThatThrownBy(() -> {
            throw new InvalidProductTypeException(message, causes);
        })
            .isExactlyInstanceOf(InvalidProductTypeException.class)
            .hasNoCause()
            .hasMessage(message)
            .satisfies(exception -> {
                final InvalidProductTypeException invalidProductTypeException = (InvalidProductTypeException) exception;
                assertThat(invalidProductTypeException.getCauses()).containsExactly(cause1);
            });
    }

    @Test
    void invalidProductTypeException_WithMessageAndCausesWithNonBlankMessages_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        final BadGatewayException cause1 = new BadGatewayException("cause1");
        final InvalidReferenceException cause2 = new InvalidReferenceException("cause2");
        final Set<Throwable> causes = new HashSet<>();
        causes.add(cause1);
        causes.add(cause2);

        assertThatThrownBy(() -> {
            throw new InvalidProductTypeException(message, causes);
        })
            .isExactlyInstanceOf(InvalidProductTypeException.class)
            .hasNoCause()
            .hasMessageContaining(format("%s Causes:%n", message))
            .hasMessageContaining(format("%n\t\t%s", cause1.getMessage()))
            .hasMessageContaining(format("%n\t\t%s", cause2.getMessage()))
            .satisfies(exception -> {
                final InvalidProductTypeException invalidProductTypeException = (InvalidProductTypeException) exception;
                assertThat(invalidProductTypeException.getCauses()).containsExactlyInAnyOrder(cause1, cause2);
            });
    }

}