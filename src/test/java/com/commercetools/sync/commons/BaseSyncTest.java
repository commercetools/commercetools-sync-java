package com.commercetools.sync.commons;

import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.ConcurrentModificationException;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import static com.commercetools.sync.commons.BaseSync.executeSupplierIfConcurrentModificationException;
import static org.assertj.core.api.Assertions.assertThat;


public class BaseSyncTest {

    @Test
    public void
        executeSupplierIfConcurrentModificationException_WithConcModExceptionAsCause_ShouldExecuteFirstSupplier() {
        final CompletionException sphereException = new CompletionException(new ConcurrentModificationException());
        final Supplier<Optional<String>> firstSupplier = () -> Optional.of("firstSupplier");
        final Supplier<Optional<String>> secondSupplier = () -> Optional.of("SecondSupplier");
        final Optional<String> result = executeSupplierIfConcurrentModificationException(sphereException, firstSupplier,
            secondSupplier);
        assertThat(result).contains("firstSupplier");
    }

    @Test
    public void
        executeSupplierIfConcurrentModificationException_WithBadRequestExceptionAsCause_ShouldExecuteSecondSupplier() {
        final CompletionException sphereException = new CompletionException(new BadRequestException("Bad Request!"));
        final Supplier<Optional<String>> firstSupplier = () -> Optional.of("firstSupplier");
        final Supplier<Optional<String>> secondSupplier = () -> Optional.of("SecondSupplier");
        final Optional<String> result = executeSupplierIfConcurrentModificationException(sphereException, firstSupplier,
            secondSupplier);
        assertThat(result).contains("SecondSupplier");
    }

    @Test
    public void
        executeSupplierIfConcurrentModificationException_WithConcurrentModException_ShouldExecuteSecondSupplier() {
        final ConcurrentModificationException sphereException = new ConcurrentModificationException();
        final Supplier<Optional<String>> firstSupplier = () -> Optional.of("firstSupplier");
        final Supplier<Optional<String>> secondSupplier = () -> Optional.of("SecondSupplier");
        final Optional<String> result = executeSupplierIfConcurrentModificationException(sphereException, firstSupplier,
            secondSupplier);
        assertThat(result).contains("SecondSupplier");
    }
}
