package com.commercetools.sync.commons.exceptions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferenceReplacementExceptionTest {

    @Test
    void referenceReplacementException_WithMessageAndNoCauses_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        assertThatThrownBy(() -> {
            throw new ReferenceReplacementException(message, emptySet());
        })
            .isExactlyInstanceOf(ReferenceReplacementException.class)
            .hasNoCause()
            .hasMessage(message)
            .satisfies(exception -> {
                final ReferenceReplacementException referenceReplacementException =
                    (ReferenceReplacementException) exception;
                assertThat(referenceReplacementException.getCauses()).isEmpty();
            });
    }

    @Test
    void referenceReplacementException_WithMessageAndCausesWithoutMessages_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        final IOException cause1 = new IOException("");
        final InvalidReferenceException cause2 = new InvalidReferenceException("");
        final Set<Throwable> causes = new HashSet<>();
        causes.add(cause1);
        causes.add(cause2);

        assertThatThrownBy(() -> {
            throw new ReferenceReplacementException(message, causes);
        })
            .isExactlyInstanceOf(ReferenceReplacementException.class)
            .hasNoCause()
            .hasMessage(message)
            .satisfies(exception -> {
                final ReferenceReplacementException referenceReplacementException =
                    (ReferenceReplacementException) exception;
                assertThat(referenceReplacementException.getCauses()).containsExactlyInAnyOrder(cause1, cause2);
            });
    }

    @Test
    void referenceReplacementException_WithMessageAndCauseWithNullMessage_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        final IOException cause1 = new IOException();
        final Set<Throwable> causes = new HashSet<>();
        causes.add(cause1);

        assertThatThrownBy(() -> {
            throw new ReferenceReplacementException(message, causes);
        })
            .isExactlyInstanceOf(ReferenceReplacementException.class)
            .hasNoCause()
            .hasMessage(message)
            .satisfies(exception -> {
                final ReferenceReplacementException referenceReplacementException =
                    (ReferenceReplacementException) exception;
                assertThat(referenceReplacementException.getCauses()).containsExactly(cause1);
            });
    }

    @Test
    void referenceReplacementException_WithMessageAndCausesWithNonBlankMessages_ShouldBuildExceptionCorrectly() {
        final String message = "foo";

        final InvalidReferenceException rootCause = new InvalidReferenceException("root cause");

        final Set<Throwable> rootCauses = new HashSet<>();
        rootCauses.add(rootCause);

        final InvalidProductTypeException cause1 = new InvalidProductTypeException("Failed", rootCauses);

        final Set<Throwable> causes = new HashSet<>();
        causes.add(cause1);


        assertThatThrownBy(() -> {
            throw new ReferenceReplacementException(message, causes);
        })
            .isExactlyInstanceOf(ReferenceReplacementException.class)
            .hasNoCause()
            .hasMessageContaining(format("%s Causes:%n", message))
            .hasMessageContaining(format("%n\t%s", cause1.getMessage()))
            .satisfies(exception -> {
                final ReferenceReplacementException referenceReplacementException =
                    (ReferenceReplacementException) exception;
                assertThat(referenceReplacementException.getCauses()).containsExactlyInAnyOrder(cause1);
            });
    }

}