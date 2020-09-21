package com.commercetools.sync.integration.externalsource.customobjects;

import com.commercetools.sync.customobjects.CustomObjectSync;
import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedQueryResult;

import io.sphere.sdk.utils.CompletableFutureUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.createCustomObject;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteCustomObject;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CustomObjectSyncIT {
    private ObjectNode customObject1Value;

    @BeforeEach
    void setup() {
        deleteCustomObject(CTP_TARGET_CLIENT, "key1", "container1");
        deleteCustomObject(CTP_TARGET_CLIENT, "key2", "container2");
        customObject1Value =
            JsonNodeFactory.instance.objectNode().put("name", "value1");

        createCustomObject(CTP_TARGET_CLIENT, "key1", "container1", customObject1Value);
    }

    @AfterAll
    static void tearDown() {
        deleteCustomObject(CTP_TARGET_CLIENT, "key1", "container1");
        deleteCustomObject(CTP_TARGET_CLIENT, "key2", "container2");
    }

    @Test
    void sync_withNewCustomObject_shouldCreateCustomObject() {

        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(
            CTP_TARGET_CLIENT).build();

        final CustomObjectSync customObjectSync = new CustomObjectSync(customObjectSyncOptions);

        final ObjectNode customObject2Value =
            JsonNodeFactory.instance.objectNode().put("name", "value1");

        final CustomObjectDraft<JsonNode> customObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container2",
            "key2", customObject2Value);

        final CustomObjectSyncStatistics customObjectSyncStatistics = customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture().join();

        assertThat(customObjectSyncStatistics).hasValues(1, 1, 0, 0);
    }

    @Test
    void sync_withExistingCustomObjectThatHasDifferentValue_shouldUpdateCustomObject() {
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(
            CTP_TARGET_CLIENT).build();
        final CustomObjectSync customObjectSync = new CustomObjectSync(customObjectSyncOptions);

        final ObjectNode customObject2Value =
            JsonNodeFactory.instance.objectNode().put("name", "value2");

        final CustomObjectDraft<JsonNode> customObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container1",
            "key1", customObject2Value);

        final CustomObjectSyncStatistics customObjectSyncStatistics = customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture().join();

        assertThat(customObjectSyncStatistics).hasValues(1, 0, 1, 0);
    }

    @Test
    void sync_withExistingCustomObjectThatHasSameValue_shouldNotUpdateCustomObject() {

        final CustomObjectDraft<JsonNode> customObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container1",
            "key1", customObject1Value);

        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(
            CTP_TARGET_CLIENT).build();

        final CustomObjectSync customObjectSync = new CustomObjectSync(customObjectSyncOptions);

        final CustomObjectSyncStatistics customObjectSyncStatistics = customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture().join();

        assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 0);
    }

    @Test
    void sync_withChangedCustomObjectAndConcurrentModificationException_shouldRetryAndUpdateCustomObject() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final CustomObjectUpsertCommand customObjectUpsertCommand = any(CustomObjectUpsertCommand.class);
        when(spyClient.execute(customObjectUpsertCommand))
                .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
                .thenCallRealMethod();

        final ObjectNode newCustomObjectValue = JsonNodeFactory.instance.objectNode().put("name", "value2");
        List<String> errorCallBackMessages = new ArrayList<>();
        List<String> warningCallBackMessages = new ArrayList<>();
        List<Throwable> errorCallBackExceptions = new ArrayList<>();

        final CustomObjectSyncOptions spyOptions = CustomObjectSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorCallBackMessages.add(exception.getMessage());
                errorCallBackExceptions.add(exception.getCause());
            })
            .build();

        final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions);

        final CustomObjectDraft<JsonNode> customObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container1",
            "key1", newCustomObjectValue);

        //test
        final CustomObjectSyncStatistics customObjectSyncStatistics = customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture().join();

        assertThat(customObjectSyncStatistics).hasValues(1, 0, 1, 0);
        Assertions.assertThat(errorCallBackExceptions).isEmpty();
        Assertions.assertThat(errorCallBackMessages).isEmpty();
        Assertions.assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    void sync_withChangedCustomObjectWithBadGatewayExceptionInsideUpdateRetry_shouldFailToUpdate() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final CustomObjectUpsertCommand upsertCommand = any(CustomObjectUpsertCommand.class);
        when(spyClient.execute(upsertCommand))
                .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
                .thenCallRealMethod();

        final CustomObjectQuery customObjectQuery = any(CustomObjectQuery.class);
        when(spyClient.execute(customObjectQuery))
                .thenCallRealMethod()
                .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

        final ObjectNode newCustomObjectValue = JsonNodeFactory.instance.objectNode().put("name", "value2");
        List<String> errorCallBackMessages = new ArrayList<>();
        List<Throwable> errorCallBackExceptions = new ArrayList<>();

        final CustomObjectSyncOptions spyOptions = CustomObjectSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorCallBackMessages.add(exception.getMessage());
                errorCallBackExceptions.add(exception.getCause());
            })
            .build();

        final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions);

        final CustomObjectDraft<JsonNode> customObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container1",
            "key1", newCustomObjectValue);

        final CustomObjectSyncStatistics customObjectSyncStatistics = customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture().join();

        assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 1);
        Assertions.assertThat(errorCallBackMessages).hasSize(1);
        Assertions.assertThat(errorCallBackExceptions).hasSize(1);
        Assertions.assertThat(errorCallBackMessages.get(0)).contains(
            format("Failed to update custom object with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                + "after concurrency modification.", CustomObjectCompositeIdentifier.of(customObjectDraft)));
    }

    @Test
    void sync_withConcurrentModificationExceptionAndUnexpectedDelete_shouldFailToReFetchAndUpdate() {

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final CustomObjectUpsertCommand customObjectUpsertCommand = any(CustomObjectUpsertCommand.class);
        when(spyClient.execute(customObjectUpsertCommand))
                .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
                .thenCallRealMethod();

        final CustomObjectQuery customObjectQuery = any(CustomObjectQuery.class);

        when(spyClient.execute(customObjectQuery))
                .thenCallRealMethod()
                .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

        final ObjectNode newCustomObjectValue = JsonNodeFactory.instance.objectNode().put("name", "value2");
        List<String> errorCallBackMessages = new ArrayList<>();
        List<Throwable> errorCallBackExceptions = new ArrayList<>();

        final CustomObjectSyncOptions spyOptions = CustomObjectSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorCallBackMessages.add(exception.getMessage());
                errorCallBackExceptions.add(exception.getCause());
            })
            .build();

        final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions);

        final CustomObjectDraft<JsonNode> customObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container1",
            "key1", newCustomObjectValue);

        final CustomObjectSyncStatistics customObjectSyncStatistics = customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture().join();

        assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 1);
        Assertions.assertThat(errorCallBackMessages).hasSize(1);
        Assertions.assertThat(errorCallBackExceptions).hasSize(1);

        Assertions.assertThat(errorCallBackMessages.get(0)).contains(
            format("Failed to update custom object with key: '%s'. Reason: Not found when attempting to fetch while"
                + " retrying after concurrency modification.", CustomObjectCompositeIdentifier.of(customObjectDraft)));
    }

    @Test
    void sync_withNewCustomObjectAndBadRequest_shouldNotCreateButHandleError() {

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final CustomObjectUpsertCommand upsertCommand = any(CustomObjectUpsertCommand.class);
        when(spyClient.execute(upsertCommand))
                .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadRequestException("bad request")))
                .thenCallRealMethod();

        final ObjectNode newCustomObjectValue = JsonNodeFactory.instance.objectNode().put("name", "value2");
        final CustomObjectDraft<JsonNode> newCustomObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container2",
                "key2", newCustomObjectValue);

        List<String> errorCallBackMessages = new ArrayList<>();
        List<Throwable> errorCallBackExceptions = new ArrayList<>();

        final CustomObjectSyncOptions spyOptions = CustomObjectSyncOptionsBuilder
                .of(spyClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                })
                .build();

        final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions);

        final CustomObjectSyncStatistics customObjectSyncStatistics = customObjectSync
                .sync(Collections.singletonList(newCustomObjectDraft))
                .toCompletableFuture().join();

        assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 1);
        Assertions.assertThat(errorCallBackMessages).hasSize(1);
        Assertions.assertThat(errorCallBackExceptions).hasSize(1);
        Assertions.assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        Assertions.assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(BadRequestException.class);
        Assertions.assertThat(errorCallBackMessages.get(0)).contains(
                format("Failed to create custom object with key: '%s'.",
                        CustomObjectCompositeIdentifier.of(newCustomObjectDraft)));
    }
}