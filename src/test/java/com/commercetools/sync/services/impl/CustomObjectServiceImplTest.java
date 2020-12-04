package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.FakeClient;
import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.queries.PagedQueryResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;

import static io.sphere.sdk.customobjects.CustomObjectUtils.getCustomObjectJavaTypeForValue;
import static io.sphere.sdk.json.SphereJsonUtils.convertToJavaType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class CustomObjectServiceImplTest {

    private SphereClient client = mock(SphereClient.class);

    private CustomObjectServiceImpl service;

    private String customObjectId;
    private String customObjectContainer;
    private String customObjectKey;
    private CustomObjectSyncOptions customObjectSyncOptions;

    @BeforeEach
    void setup() {
        customObjectId = RandomStringUtils.random(15, true, true);
        customObjectContainer = RandomStringUtils.random(15, true, true);
        customObjectKey = RandomStringUtils.random(15, true, true);

        initMockService(client);
    }

    @AfterEach
    void cleanup() {
        reset(client);
    }

    private interface CustomObjectPagedQueryResult extends PagedQueryResult<CustomObject> {
    }

    @Test
    void fetchCachedCustomObjectId_WithKeyAndContainer_ShouldFetchCustomObject() {
        final String key = RandomStringUtils.random(15, true, true);
        final String container = RandomStringUtils.random(15, true, true);
        final String id = RandomStringUtils.random(15, true, true);

        final CustomObject<JsonNode> mock = mock(CustomObject.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getContainer()).thenReturn(container);
        when(mock.getKey()).thenReturn(key);

        final CustomObjectPagedQueryResult result = mock(CustomObjectPagedQueryResult.class);
        when(result.getResults()).thenReturn(Collections.singletonList(mock));

        final FakeClient<CustomObjectPagedQueryResult> fakeCustomObjectClient = new FakeClient<>(result);
        initMockService(fakeCustomObjectClient);

        final Optional<String> fetchedId = service
            .fetchCachedCustomObjectId(CustomObjectCompositeIdentifier.of(key, container))
            .toCompletableFuture().join();

        assertThat(fetchedId).contains(id);
        assertThat(fakeCustomObjectClient.isExecuted()).isTrue();
    }

    @Test
    void fetchMatchingCustomObjects_WithKeySet_ShouldFetchCustomObjects() {
        final String key1 = RandomStringUtils.random(15, true, true);
        final String key2 = RandomStringUtils.random(15, true, true);
        final String container1 = RandomStringUtils.random(15, true, true);
        final String container2 = RandomStringUtils.random(15, true, true);

        final Set<CustomObjectCompositeIdentifier> customObjectCompositeIdentifiers = new HashSet<>();
        customObjectCompositeIdentifiers.add(CustomObjectCompositeIdentifier.of(key1, container1));
        customObjectCompositeIdentifiers.add(CustomObjectCompositeIdentifier.of(key2, container2));

        final CustomObject<JsonNode> mock1 = mock(CustomObject.class);
        when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
        when(mock1.getKey()).thenReturn(key1);
        when(mock1.getContainer()).thenReturn(container1);

        final CustomObject<JsonNode> mock2 = mock(CustomObject.class);
        when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
        when(mock2.getKey()).thenReturn(key2);
        when(mock2.getContainer()).thenReturn(container2);

        final CustomObjectPagedQueryResult result = mock(CustomObjectPagedQueryResult.class);
        when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

        final FakeClient<CustomObjectPagedQueryResult> fakeCustomObjectClient = new FakeClient<>(result);
        initMockService(fakeCustomObjectClient);

        final Set<CustomObject<JsonNode>> customObjects = service
            .fetchMatchingCustomObjects(customObjectCompositeIdentifiers)
            .toCompletableFuture().join();

        List<CustomObjectCompositeIdentifier> customObjectCompositeIdlist =
            new ArrayList<CustomObjectCompositeIdentifier>(customObjectCompositeIdentifiers);

        assertAll(
            () -> assertThat(customObjects).contains(mock1, mock2),
            () -> assertThat(service.keyToIdCache.asMap()).containsKeys(
                String.valueOf(customObjectCompositeIdlist.get(0)),
                String.valueOf(customObjectCompositeIdlist.get(1)))
        );
        assertThat(fakeCustomObjectClient.isExecuted()).isTrue();
    }

    @Test
    void fetchCustomObject_WithKeyAndContainer_ShouldFetchCustomObject() {
        final CustomObject mock = mock(CustomObject.class);
        when(mock.getId()).thenReturn(customObjectId);
        when(mock.getKey()).thenReturn(customObjectKey);
        when(mock.getContainer()).thenReturn(customObjectContainer);
        final CustomObjectPagedQueryResult result = mock(CustomObjectPagedQueryResult.class);
        when(result.head()).thenReturn(Optional.of(mock));

        final FakeClient<CustomObjectPagedQueryResult> fakeCustomObjectClient = new FakeClient<>(result);
        initMockService(fakeCustomObjectClient);

        final Optional<CustomObject<JsonNode>> customObjectOptional = service
            .fetchCustomObject(CustomObjectCompositeIdentifier.of(customObjectKey, customObjectContainer))
            .toCompletableFuture().join();

        assertAll(
            () -> assertThat(customObjectOptional).containsSame(mock),
            () -> assertThat(
                service.keyToIdCache.asMap().get(
                    CustomObjectCompositeIdentifier.of(customObjectKey, customObjectContainer).toString())
            ).isEqualTo(customObjectId)
        );
        assertThat(fakeCustomObjectClient.isExecuted()).isTrue();
    }

    @Test
    void createCustomObject_WithDraft_ShouldCreateCustomObject() {
        final CustomObject<JsonNode> mock = mock(CustomObject.class);
        when(mock.getId()).thenReturn(customObjectId);
        when(mock.getKey()).thenReturn(customObjectKey);
        when(mock.getContainer()).thenReturn(customObjectContainer);

        final FakeClient<CustomObject<JsonNode>> fakeCustomObjectClient =
                new FakeClient<>(mock);
        initMockService(fakeCustomObjectClient);

        final ObjectNode customObjectValue = JsonNodeFactory.instance.objectNode();
        customObjectValue.put("currentHash", "1234-5678-0912-3456");
        customObjectValue.put("convertedAmount", "100");


        final CustomObjectDraft<JsonNode> draft = CustomObjectDraft
            .ofUnversionedUpsert(customObjectContainer, customObjectKey, customObjectValue);

        final Optional<CustomObject<JsonNode>> customObjectOptional =
            service.upsertCustomObject(draft).toCompletableFuture().join();

        assertThat(customObjectOptional).containsSame(mock);
        assertThat(fakeCustomObjectClient.isExecuted()).isTrue();
    }

    @Test
    void createCustomObject_WithRequestException_ShouldNotCreateCustomObject() {
        final CustomObject<JsonNode> mock = mock(CustomObject.class);
        when(mock.getId()).thenReturn(customObjectId);

        final FakeClient<Throwable> fakeCustomObjectClient =
                new FakeClient<>(new BadRequestException("bad request"));
        initMockService(fakeCustomObjectClient);

        final CustomObjectDraft<JsonNode> draftMock = mock(CustomObjectDraft.class);
        when(draftMock.getKey()).thenReturn(customObjectKey);
        when(draftMock.getContainer()).thenReturn(customObjectContainer);
        when(draftMock.getJavaType()).thenReturn(getCustomObjectJavaTypeForValue(convertToJavaType(JsonNode.class)));

        CompletableFuture<Optional<CustomObject<JsonNode>>> future =
                service.upsertCustomObject(draftMock).toCompletableFuture();

        assertAll(
            () -> assertThat(future.isCompletedExceptionally()).isTrue(),
            () -> assertThat(future).failsWithin(1, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class)
                    .withCauseExactlyInstanceOf(BadRequestException.class)
        );
    }

    @Test
    void fetchMatchingCustomObjectsByCompositeIdentifiers_WithBadGateWayExceptionAlways_ShouldFail() {
        // Mock sphere client to return BadGatewayException on any request.
        final FakeClient<CustomObject<JsonNode>> fakeCustomObjectClient =
                new FakeClient<>(new BadGatewayException());

        final List<String> errorCallBackMessages = new ArrayList<>();
        final List<Throwable> errorCallBackExceptions = new ArrayList<>();

        customObjectSyncOptions =
                CustomObjectSyncOptionsBuilder.of(fakeCustomObjectClient)
                        .errorCallback((exception, oldResource, newResource, updateActions) -> {
                            errorCallBackMessages.add(exception.getMessage());
                            errorCallBackExceptions.add(exception.getCause());
                        })
                        .build();

        service = new CustomObjectServiceImpl(customObjectSyncOptions);

        final Set<CustomObjectCompositeIdentifier> customObjectCompositeIdentifiers = new HashSet<>();
        customObjectCompositeIdentifiers.add(CustomObjectCompositeIdentifier.of(
                "old_custom_object_key", "old_custom_object_container"));

        // test and assert
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(service
                .fetchMatchingCustomObjects(customObjectCompositeIdentifiers))
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BadGatewayException.class);
    }

    private void initMockService(@Nonnull final SphereClient mockSphereClient) {
        customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(mockSphereClient).build();

        service = new CustomObjectServiceImpl(customObjectSyncOptions);

    }
}
