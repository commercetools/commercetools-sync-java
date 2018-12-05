package com.commercetools.sync.types;

import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.types.helpers.TypeSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeSyncTest {
    @Test
    public void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation

        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
            "foo",
            ofEnglish("name"),
            ResourceTypeIdsSetBuilder.of().addCategories().build())
                                                       .description(ofEnglish("desc"))
                                                       .fieldDefinitions(emptyList())
                                                       .build();
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final TypeSyncOptions syncOptions = TypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();


        final TypeService mockTypeService = mock(TypeService.class);

        when(mockTypeService.fetchMatchingTypesByKeys(singleton(newTypeDraft.getKey())))
            .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final TypeSync typeSync = new TypeSync(syncOptions, mockTypeService);

        // test
        final TypeSyncStatistics typeSyncStatistics = typeSync
            .sync(singletonList(newTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to fetch existing types of keys '[foo]'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(typeSyncStatistics).hasValues(1, 0, 0, 1);
    }

}
