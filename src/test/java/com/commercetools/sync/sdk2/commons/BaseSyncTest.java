package com.commercetools.sync.sdk2.commons;

import static com.commercetools.sync.sdk2.commons.BaseSync.executeSupplierIfConcurrentModificationException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vrap.rmf.base.client.error.BadRequestException;
import io.vrap.rmf.base.client.error.ConcurrentModificationException;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class BaseSyncTest {

  @Test
  void
      executeSupplierIfConcurrentModificationException_WithConcModExceptionAsCause_ShouldExecuteFirstSupplier() {
    final ConcurrentModificationException concurrentModificationException =
        mock(ConcurrentModificationException.class);
    when(concurrentModificationException.getCause()).thenReturn(concurrentModificationException);
    final CompletionException completionException =
        new CompletionException(concurrentModificationException);
    final Supplier<Optional<String>> firstSupplier = () -> Optional.of("firstSupplier");
    final Supplier<Optional<String>> secondSupplier = () -> Optional.of("SecondSupplier");
    final Optional<String> result =
        executeSupplierIfConcurrentModificationException(
            completionException, firstSupplier, secondSupplier);
    assertThat(result).contains("firstSupplier");
  }

  @Test
  void
      executeSupplierIfConcurrentModificationException_WithBadRequestExceptionAsCause_ShouldExecuteSecondSupplier() {
    final BadRequestException badRequestException = mock(BadRequestException.class);

    final CompletionException completionException = new CompletionException(badRequestException);
    final Supplier<Optional<String>> firstSupplier = () -> Optional.of("firstSupplier");
    final Supplier<Optional<String>> secondSupplier = () -> Optional.of("SecondSupplier");
    final Optional<String> result =
        executeSupplierIfConcurrentModificationException(
            completionException, firstSupplier, secondSupplier);
    assertThat(result).contains("SecondSupplier");
  }

  @Test
  void
      executeSupplierIfConcurrentModificationException_WithConcurrentModException_ShouldExecuteSecondSupplier() {
    final ConcurrentModificationException concurrentModificationException =
        mock(ConcurrentModificationException.class);
    when(concurrentModificationException.getCause()).thenReturn(new RuntimeException());

    final Supplier<Optional<String>> firstSupplier = () -> Optional.of("firstSupplier");
    final Supplier<Optional<String>> secondSupplier = () -> Optional.of("SecondSupplier");
    final Optional<String> result =
        executeSupplierIfConcurrentModificationException(
            concurrentModificationException, firstSupplier, secondSupplier);
    assertThat(result).contains("SecondSupplier");
  }
}
