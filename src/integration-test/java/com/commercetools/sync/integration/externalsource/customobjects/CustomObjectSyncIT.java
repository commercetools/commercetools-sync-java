package com.commercetools.sync.integration.externalsource.customobjects;

import com.commercetools.sync.customobjects.CustomObjectSync;
import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import java.util.Collections;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.createCustomObject;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteCustomObject;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;

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
}