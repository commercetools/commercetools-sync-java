package com.commercetools.sync.customobjects;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomObjectSyncOptionsBuilderTest {

    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private CustomObjectSyncOptionsBuilder customObjectSyncOptionsBuilder =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT);


    @Test
    void of_WithClient_ShouldCreateCustomObjectSyncOptionsBuilder() {
        final CustomObjectSyncOptionsBuilder builder = CustomObjectSyncOptionsBuilder.of(CTP_CLIENT);
        assertThat(builder).isNotNull();
    }

    @Test
    void build_WithClient_ShouldBuildSyncOptions() {
        final CustomObjectSyncOptions customObjectSyncOptions = customObjectSyncOptionsBuilder.build();
        assertThat(customObjectSyncOptions).isNotNull();
        assertThat(customObjectSyncOptions.getBeforeUpdateCallback()).isNull();
        assertThat(customObjectSyncOptions.getBeforeCreateCallback()).isNull();
        assertThat(customObjectSyncOptions.getErrorCallback()).isNull();
        assertThat(customObjectSyncOptions.getWarningCallback()).isNull();
        assertThat(customObjectSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
        assertThat(customObjectSyncOptions.getBatchSize()).isEqualTo(CustomObjectSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
        assertThat(customObjectSyncOptions.getCacheSize()).isEqualTo(10_000);
    }

    @Test
    void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
        customObjectSyncOptionsBuilder.beforeCreateCallback((newCustomObject) -> null);
        final CustomObjectSyncOptions customObjectSyncOptions = customObjectSyncOptionsBuilder.build();
        assertThat(customObjectSyncOptions.getBeforeCreateCallback()).isNotNull();
    }

    @Test
    void errorCallBack_WithCallBack_ShouldSetCallBack() {
        final QuadConsumer<SyncException, Optional<CustomObjectDraft<JsonNode>>,
            Optional<CustomObject<JsonNode>>, List<UpdateAction<CustomObject<JsonNode>>>>
            mockErrorCallBack = (exception, newResource, oldResource, updateActions) -> { };
        customObjectSyncOptionsBuilder.errorCallback(mockErrorCallBack);

        final CustomObjectSyncOptions customObjectSyncOptions = customObjectSyncOptionsBuilder.build();
        assertThat(customObjectSyncOptions.getErrorCallback()).isNotNull();
    }

    @Test
    void warningCallBack_WithCallBack_ShouldSetCallBack() {
        final TriConsumer<SyncException,
            Optional<CustomObjectDraft<JsonNode>>, Optional<CustomObject<JsonNode>>> mockWarningCallBack =
                (exception, newResource, oldResource) -> { };
        customObjectSyncOptionsBuilder.warningCallback(mockWarningCallBack);
        final CustomObjectSyncOptions cutomObjectSyncOptions = customObjectSyncOptionsBuilder.build();
        assertThat(cutomObjectSyncOptions.getWarningCallback()).isNotNull();
    }

    @Test
    void getThis_ShouldReturnCorrectInstance() {
        final CustomObjectSyncOptionsBuilder instance = customObjectSyncOptionsBuilder.getThis();
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(CustomObjectSyncOptionsBuilder.class);
        assertThat(instance).isEqualTo(customObjectSyncOptionsBuilder);
    }

    @Test
    void customObjectSyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(30)
            .beforeCreateCallback((newCustomObject) -> null)
            .beforeUpdateCallback((updateActions, newCustomObject, oldCustomObject) -> emptyList())
            .build();
        assertThat(customObjectSyncOptions).isNotNull();
    }

    @Test
    void batchSize_WithPositiveValue_ShouldSetBatchSize() {
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                              .batchSize(10)
                                                                                              .build();
        assertThat(customObjectSyncOptions.getBatchSize()).isEqualTo(10);
    }

    @Test
    void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
        final CustomObjectSyncOptions customObjectSyncOptionsWithZeroBatchSize =
            CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
                                          .batchSize(0)
                                          .build();
        assertThat(customObjectSyncOptionsWithZeroBatchSize.getBatchSize())
            .isEqualTo(CustomObjectSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
        final CustomObjectSyncOptions customObjectSyncOptionsWithNegativeBatchSize = CustomObjectSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(-100)
            .build();
        assertThat(customObjectSyncOptionsWithNegativeBatchSize.getBatchSize())
            .isEqualTo(CustomObjectSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

    @Test
    void applyBeforeUpdateCallBack_WithNullReturnCallbackAndEmptyUpdateActions_ShouldReturnEmptyList() {
        final TriFunction<List<UpdateAction<CustomObject<JsonNode>>>, CustomObjectDraft<JsonNode>,
            CustomObject<JsonNode>, List<UpdateAction<CustomObject<JsonNode>>>>
            beforeUpdateCallback = (updateActions, newCustomObject, oldCustomObject) -> null;

        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                              .beforeUpdateCallback(
                                                                                                  beforeUpdateCallback)
                                                                                              .build();
        assertThat(customObjectSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<CustomObject<JsonNode>>> updateActions = Collections.emptyList();

        final List<UpdateAction<CustomObject<JsonNode>>> filteredList =
            customObjectSyncOptions.applyBeforeUpdateCallback(
                updateActions, mock(CustomObjectDraft.class), mock(CustomObject.class));
        assertThat(filteredList).isEqualTo(updateActions);
        assertThat(filteredList).isEmpty();
    }

    @Test
    void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
        final Function<CustomObjectDraft<JsonNode>, CustomObjectDraft<JsonNode>> draftFunction =
            customObjectDraft -> CustomObjectDraft.ofUnversionedUpsert(
                customObjectDraft.getContainer() + "_filteredContainer",
                customObjectDraft.getKey() + "_filteredKey",
                (JsonNode) customObjectDraft.getValue());

        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                              .beforeCreateCallback(
                                                                                                  draftFunction)
                                                                                              .build();
        assertThat(customObjectSyncOptions.getBeforeCreateCallback()).isNotNull();
        final CustomObjectDraft<JsonNode> resourceDraft = mock(CustomObjectDraft.class);
        when(resourceDraft.getKey()).thenReturn("myKey");
        when(resourceDraft.getContainer()).thenReturn("myContainer");
        final Optional<CustomObjectDraft<JsonNode>> filteredDraft =
            customObjectSyncOptions.applyBeforeCreateCallback(resourceDraft);
        assertThat(filteredDraft).isNotEmpty();
        assertThat(filteredDraft.get().getKey()).isEqualTo("myKey_filteredKey");
        assertThat(filteredDraft.get().getContainer()).isEqualTo("myContainer_filteredContainer");
    }

    @Test
    void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).build();
        assertThat(customObjectSyncOptions.getBeforeCreateCallback()).isNull();
        final CustomObjectDraft<JsonNode> resourceDraft = mock(CustomObjectDraft.class);
        final Optional<CustomObjectDraft<JsonNode>> filteredDraft =
            customObjectSyncOptions.applyBeforeCreateCallback(resourceDraft);
        assertThat(filteredDraft).containsSame(resourceDraft);
    }

    @Test
    void applyBeforeCreateCallBack_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
        final Function<CustomObjectDraft<JsonNode>, CustomObjectDraft<JsonNode>> draftFunction =
            customObjectDraft -> null;
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                              .beforeCreateCallback(
                                                                                                  draftFunction)
                                                                                              .build();
        assertThat(customObjectSyncOptions.getBeforeCreateCallback()).isNotNull();
        final CustomObjectDraft<JsonNode> resourceDraft = mock(CustomObjectDraft.class);
        final Optional<CustomObjectDraft<JsonNode>> filteredDraft =
            customObjectSyncOptions.applyBeforeCreateCallback(resourceDraft);
        assertThat(filteredDraft).isEmpty();
    }

    @Test
    void cacheSize_WithPositiveValue_ShouldSetCacheSize() {
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                              .cacheSize(10)
                                                                                              .build();
        assertThat(customObjectSyncOptions.getCacheSize()).isEqualTo(10);
    }

    @Test
    void cacheSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
        final CustomObjectSyncOptions customObjectSyncOptionsWithZeroCacheSize =
            CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
                                          .cacheSize(0)
                                          .build();
        assertThat(customObjectSyncOptionsWithZeroCacheSize.getCacheSize()).isEqualTo(10_000);

        final CustomObjectSyncOptions customObjectSyncOptionsWithNegativeCacheSize = CustomObjectSyncOptionsBuilder
            .of(CTP_CLIENT)
            .cacheSize(-100)
            .build();
        assertThat(customObjectSyncOptionsWithNegativeCacheSize.getCacheSize()).isEqualTo(10_000);
    }

}
