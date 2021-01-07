package com.commercetools.sync.commons;

import com.commercetools.sync.commons.models.FetchCustomObjectsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import io.sphere.sdk.client.NotFoundException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CleanupTest {
    private final SphereClient mockClient = mock(SphereClient.class);
    private static final int deleteDaysAfterLastModification = 30;

    @Test
    void of_WithSphereClient_ReturnsCleanupObject() {
        assertThat(Cleanup.of(mockClient)).isNotNull();
    }

    @Test
    void deleteUnresolvedReferences_withDeleteDaysAfterLastModification_ShouldDeleteAndReturnCleanupResults() {
        final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult = mock(ResourceKeyIdGraphQlResult.class);
        when(resourceKeyIdGraphQlResult.getResults()).thenReturn(new HashSet<>(Arrays.asList(
            new ResourceKeyId("coKey1", "coId1"),
            new ResourceKeyId("coKey2", "coId2"))));

        when(mockClient.execute(any(FetchCustomObjectsGraphQlRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(resourceKeyIdGraphQlResult));

        when(mockClient.execute(any(CustomObjectDeleteCommand.class)))
            .thenReturn(CompletableFuture.completedFuture(mock(CustomObject.class)));

        final Cleanup.Statistics statistics =
            Cleanup.of(mockClient).deleteUnresolvedReferences(deleteDaysAfterLastModification)
                   .join();

        assertThat(statistics.getTotalDeleted()).isEqualTo(4);
        assertThat(statistics.getTotalFailed()).isEqualTo(0);
        assertThat(statistics.getReportMessage())
            .isEqualTo("Summary: 4 custom objects were deleted in total (0 failed to delete).");
    }

    @Test
    void deleteUnresolvedReferences_withNotFound404Exception_ShouldNotIncrementFailedCounter() {
        final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult = mock(ResourceKeyIdGraphQlResult.class);
        when(resourceKeyIdGraphQlResult.getResults())
            .thenReturn(Collections.singleton(new ResourceKeyId("coKey1", "coId1")));

        when(mockClient.execute(any(FetchCustomObjectsGraphQlRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(resourceKeyIdGraphQlResult));

        when(mockClient.execute(any(CustomObjectDeleteCommand.class)))
            .thenReturn(CompletableFuture.completedFuture(mock(CustomObject.class)))
            .thenReturn(CompletableFutureUtils.failed(new SphereException(new NotFoundException())));

        final Cleanup.Statistics statistics =
            Cleanup.of(mockClient).deleteUnresolvedReferences(deleteDaysAfterLastModification)
                   .join();

        assertThat(statistics.getTotalDeleted()).isEqualTo(1);
        assertThat(statistics.getTotalFailed()).isEqualTo(0);
        assertThat(statistics.getReportMessage())
            .isEqualTo("Summary: 1 custom objects were deleted in total (0 failed to delete).");
    }

}
