package com.commercetools.sync.customobjects;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomObjectSyncOptionsTest {

    private static SphereClient CTP_CLIENT = mock(SphereClient.class);

    @Test
    void applyBeforeCreateCallback_WithCallback_ShouldReturnFilteredDraft() {

        final Function<CustomObjectDraft<JsonNode>, CustomObjectDraft<JsonNode>> draftFunction =
            customObjectDraft -> CustomObjectDraft.ofUnversionedUpsert(
                        customObjectDraft.getContainer() + "_filteredContainer",
                        customObjectDraft.getKey() + "_filteredKey", customObjectDraft.getValue());
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
                .beforeCreateCallback(draftFunction)
                .build();
        final CustomObjectDraft<JsonNode> resourceDraft = mock(CustomObjectDraft.class);
        when(resourceDraft.getKey()).thenReturn("myKey");
        when(resourceDraft.getContainer()).thenReturn("myContainer");

        final Optional<CustomObjectDraft<JsonNode>> filteredDraft = customObjectSyncOptions
                .applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).hasValueSatisfying(customObjectDraft ->
                assertAll(
                    () -> assertThat(customObjectDraft.getKey()).isEqualTo("myKey_filteredKey"),
                    () -> assertThat(customObjectDraft.getContainer()).isEqualTo("myContainer_filteredContainer")
                ));
    }

    @Test
    void applyBeforeCreateCallback_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).build();
        final CustomObjectDraft<JsonNode> resourceDraft = mock(CustomObjectDraft.class);

        final Optional<CustomObjectDraft<JsonNode>> filteredDraft = customObjectSyncOptions
                .applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).containsSame(resourceDraft);
    }

    @Test
    void applyBeforeCreateCallback_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
        final Function<CustomObjectDraft<JsonNode>, CustomObjectDraft<JsonNode>> draftFunction =
            customObjectDraft -> null;
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
                .beforeCreateCallback(draftFunction)
                .build();
        final CustomObjectDraft<JsonNode> resourceDraft = mock(CustomObjectDraft.class);

        final Optional<CustomObjectDraft<JsonNode>> filteredDraft = customObjectSyncOptions
                .applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).isEmpty();
    }
}
