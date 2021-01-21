package com.commercetools.sync.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.models.FetchCustomObjectsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.NotFoundException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CleanupUnresolvedReferenceCustomObjectsTest {
  private final SphereClient mockClient = mock(SphereClient.class);
  private static final int deleteDaysAfterLastModification = 30;

  @Test
  void cleanup_withDeleteDaysAfterLastModification_ShouldDeleteAndReturnCleanupStatistics() {
    final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult =
        mock(ResourceKeyIdGraphQlResult.class);
    when(resourceKeyIdGraphQlResult.getResults())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new ResourceKeyId("coKey1", "coId1"), new ResourceKeyId("coKey2", "coId2"))));

    when(mockClient.execute(any(FetchCustomObjectsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(resourceKeyIdGraphQlResult));

    final Throwable badRequestException = new BadRequestException("key is not valid");
    when(mockClient.execute(any(CustomObjectDeleteCommand.class)))
        .thenReturn(CompletableFutureUtils.failed(badRequestException))
        .thenReturn(CompletableFuture.completedFuture(mock(CustomObject.class)));

    final CleanupUnresolvedReferenceCustomObjects.Statistics statistics =
        CleanupUnresolvedReferenceCustomObjects.of(mockClient)
            .cleanup(deleteDaysAfterLastModification)
            .join();

    assertThat(statistics.getTotalDeleted()).isEqualTo(5);
    assertThat(statistics.getTotalFailed()).isEqualTo(1);
    assertThat(statistics.getReportMessage())
        .isEqualTo("Summary: 5 custom objects were deleted in total (1 failed to delete).");
  }

  @Test
  void cleanup_withNotFound404Exception_ShouldNotIncrementFailedCounter() {
    final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult =
        mock(ResourceKeyIdGraphQlResult.class);
    when(resourceKeyIdGraphQlResult.getResults())
        .thenReturn(Collections.singleton(new ResourceKeyId("coKey1", "coId1")));

    when(mockClient.execute(any(FetchCustomObjectsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(resourceKeyIdGraphQlResult));

    when(mockClient.execute(any(CustomObjectDeleteCommand.class)))
        .thenReturn(CompletableFuture.completedFuture(mock(CustomObject.class)))
        .thenReturn(CompletableFutureUtils.failed(new NotFoundException()));

    final CleanupUnresolvedReferenceCustomObjects.Statistics statistics =
        CleanupUnresolvedReferenceCustomObjects.of(mockClient)
            .cleanup(deleteDaysAfterLastModification)
            .join();

    assertThat(statistics.getTotalDeleted()).isEqualTo(1);
    assertThat(statistics.getTotalFailed()).isEqualTo(0);
    assertThat(statistics.getReportMessage())
        .isEqualTo("Summary: 1 custom objects were deleted in total (0 failed to delete).");
  }

  @Test
  void cleanup_withBadRequest400Exception_ShouldIncrementFailedCounterAndTriggerErrorCallback() {
    final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult =
        mock(ResourceKeyIdGraphQlResult.class);
    when(resourceKeyIdGraphQlResult.getResults())
        .thenReturn(Collections.singleton(new ResourceKeyId("coKey1", "coId1")));

    when(mockClient.execute(any(FetchCustomObjectsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(resourceKeyIdGraphQlResult));

    final Throwable badRequestException = new BadRequestException("key is not valid");
    when(mockClient.execute(any(CustomObjectDeleteCommand.class)))
        .thenReturn(CompletableFuture.completedFuture(mock(CustomObject.class)))
        .thenReturn(CompletableFutureUtils.failed(badRequestException));

    final List<Throwable> exceptions = new ArrayList<>();
    final CleanupUnresolvedReferenceCustomObjects.Statistics statistics =
        CleanupUnresolvedReferenceCustomObjects.of(mockClient)
            .errorCallback(exceptions::add)
            .cleanup(deleteDaysAfterLastModification)
            .join();

    assertThat(statistics.getTotalDeleted()).isEqualTo(1);
    assertThat(statistics.getTotalFailed()).isEqualTo(2);
    assertThat(exceptions).contains(badRequestException);
    assertThat(statistics.getReportMessage())
        .isEqualTo("Summary: 1 custom objects were deleted in total (2 failed to delete).");
  }
}
