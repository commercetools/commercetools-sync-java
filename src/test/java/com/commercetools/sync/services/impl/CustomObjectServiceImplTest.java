package com.commercetools.sync.services.impl;

import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static io.sphere.sdk.customobjects.CustomObjectUtils.getCustomObjectJavaTypeForValue;
import static io.sphere.sdk.json.SphereJsonUtils.convertToJavaType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomObjectServiceImplTest {

    private SphereClient client = mock(SphereClient.class);

    private CustomObjectServiceImpl service;

    private String customObjectId;
    private String customObjectContainer;
    private String customObjectKey;

    @BeforeEach
    void setup() {
        customObjectId = RandomStringUtils.random(15, true, true);
        customObjectContainer = RandomStringUtils.random(15, true, true);
        customObjectKey = RandomStringUtils.random(15, true, true);

        CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(client).build();

        service = new CustomObjectServiceImpl(customObjectSyncOptions);
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

        final CustomObject mock = mock(CustomObject.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getContainer()).thenReturn(container);
        when(mock.getKey()).thenReturn(key);

        final CustomObjectPagedQueryResult result = mock(CustomObjectPagedQueryResult.class);
        when(result.getResults()).thenReturn(Collections.singletonList(mock));

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));


        final Optional<String> fetchedId = service
            .fetchCachedCustomObjectId(CustomObjectCompositeIdentifier.of(key, container))
            .toCompletableFuture().join();

        assertThat(fetchedId).contains(id);
        verify(client).execute(any(CustomObjectQuery.class));
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

        final CustomObject mock1 = mock(CustomObject.class);
        when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
        when(mock1.getKey()).thenReturn(key1);
        when(mock1.getContainer()).thenReturn(container1);

        final CustomObject mock2 = mock(CustomObject.class);
        when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
        when(mock2.getKey()).thenReturn(key2);
        when(mock2.getContainer()).thenReturn(container2);

        final CustomObjectPagedQueryResult result = mock(CustomObjectPagedQueryResult.class);
        when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

        final Set<CustomObject<JsonNode>> customObjects = service
            .fetchMatchingCustomObjects(customObjectCompositeIdentifiers)
            .toCompletableFuture().join();

        List<CustomObjectCompositeIdentifier> customObjectCompositeIdlist =
            new ArrayList<CustomObjectCompositeIdentifier>(customObjectCompositeIdentifiers);

        assertAll(
            () -> assertThat(customObjects).contains(mock1, mock2),
            () -> assertThat(service.keyToIdCache).containsKeys(
                String.valueOf(customObjectCompositeIdlist.get(0)),
                String.valueOf(customObjectCompositeIdlist.get(1)))
        );
        verify(client).execute(any(CustomObjectQuery.class));
    }

    @Test
    void fetchCustomObject_WithKeyAndContainer_ShouldFetchCustomObject() {
        final CustomObject mock = mock(CustomObject.class);
        when(mock.getId()).thenReturn(customObjectId);
        when(mock.getKey()).thenReturn(customObjectKey);
        when(mock.getContainer()).thenReturn(customObjectContainer);
        final CustomObjectPagedQueryResult result = mock(CustomObjectPagedQueryResult.class);
        when(result.head()).thenReturn(Optional.of(mock));

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));


        final Optional<CustomObject<JsonNode>> customObjectOptional = service
            .fetchCustomObject(CustomObjectCompositeIdentifier.of(customObjectKey, customObjectContainer))
            .toCompletableFuture().join();

        assertAll(
            () -> assertThat(customObjectOptional).containsSame(mock),
            () -> assertThat(
                service.keyToIdCache.get(
                    CustomObjectCompositeIdentifier.of(customObjectKey, customObjectContainer).toString())
            ).isEqualTo(customObjectId)
        );
        verify(client).execute(any(CustomObjectQuery.class));
    }

    @Test
    void createCustomObject_WithDraft_ShouldCreateCustomObject() {
        final CustomObject mock = mock(CustomObject.class);
        when(mock.getId()).thenReturn(customObjectId);
        when(mock.getKey()).thenReturn(customObjectKey);
        when(mock.getContainer()).thenReturn(customObjectContainer);

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(mock));

        final ObjectNode customObjectValue = JsonNodeFactory.instance.objectNode();
        customObjectValue.put("currentHash", "1234-5678-0912-3456");
        customObjectValue.put("convertedAmount", "100");


        final CustomObjectDraft<JsonNode> draft = CustomObjectDraft
            .ofUnversionedUpsert(customObjectContainer, customObjectKey, customObjectValue);

        final Optional<CustomObject<JsonNode>> customObjectOptional =
            service.upsertCustomObject(draft).toCompletableFuture().join();

        assertThat(customObjectOptional).containsSame(mock);
        verify(client).execute(eq(CustomObjectUpsertCommand.of(draft)));
    }

    @Test
    void createCustomObject_WithRequestException_ShouldNotCreateCustomObject() {
        final CustomObject mock = mock(CustomObject.class);
        when(mock.getId()).thenReturn(customObjectId);

        when(client.execute(any())).thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

        final CustomObjectDraft<JsonNode> draftMock = mock(CustomObjectDraft.class);
        when(draftMock.getKey()).thenReturn(customObjectKey);
        when(draftMock.getContainer()).thenReturn(customObjectContainer);
        when(draftMock.getJavaType()).thenReturn(getCustomObjectJavaTypeForValue(convertToJavaType(JsonNode.class)));


        CompletableFuture future = service.upsertCustomObject(draftMock).toCompletableFuture();

        assertAll(
            () -> assertThat(future.isCompletedExceptionally()).isTrue(),
            () -> assertThat(future).hasFailedWithThrowableThat().isExactlyInstanceOf(BadRequestException.class)
        );
    }
}
