package com.commercetools.sync.commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CleanupTest {
    final SphereClient sphereClient = mock(SphereClient.class);

    @Test
    void of_WithSphereClient_ReturnsCleanupObject() {
        assertThat(Cleanup.of(sphereClient)).isNotNull();
    }

    @Test
    void deleteUnresolvedReferences_withDeleteDaysAfterLastModification_ShouldDeleteAndReturnCleanupResults() {
        final int deleteDaysAfterLastModification = 30;

        final String customFieldName = "name";
        final JsonNode customFieldValue =
            JsonNodeFactory.instance.objectNode()
                                    .put("key", "productKey1")
                                    .putArray("dependantProductKeys")
                                    .add("productKey2")
                                    .add("productKey3");

        final CustomObject customObjectMock = mock(CustomObject.class);
        when(customObjectMock.getValue()).thenReturn(customFieldValue);

        final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        when(pagedQueryResult.getResults()).thenReturn(Arrays.asList(customObjectMock));

        when(sphereClient.execute(any(CustomObjectQuery.class)))
            .thenReturn(completedFuture(pagedQueryResult));

        when(sphereClient.execute(any(CustomObjectDeleteCommand.class)))
            .thenReturn(completedFuture(customObjectMock));

        final CompletionStage<Cleanup.Statistics> statisticsCompletionStage =
            Cleanup.of(sphereClient).deleteUnresolvedReferences(deleteDaysAfterLastModification);


    }

}
