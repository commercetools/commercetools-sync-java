package com.commercetools.sync.services.impl;

import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.internals.helpers.CustomObjectCompositeId;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
//import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CustomObjectServiceImplTest {

    private SphereClient client = mock(SphereClient.class);

    // TODO - Enable here when service class is ready
    //private CustomObjectServiceImpl service;
    private List<String> errorMessages;
    private List<Throwable> errorExceptions;

    private String customObjectId;
    private String customObjectContainer;
    private String customObjectKey;

    @BeforeEach
    void setup() {
        customObjectId = RandomStringUtils.random(15, true, true);
        customObjectContainer = RandomStringUtils.random(15, true, true);
        customObjectKey = RandomStringUtils.random(15, true, true);

        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
        CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(client)
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                errorExceptions.add(exception.getCause());
            })
            .build();
        // TODO - Enable here when service class is ready
        //service = new CustomObjectServiceImpl(customObjectSyncOptions);
    }

    @AfterEach
    void cleanup() {
        reset(client);
    }

    private interface CustomObjectPagedQueryResult extends PagedQueryResult<CustomObject> {
    }

    @Test
    void fetchCachedCustomObjectId_WithKeyAndContainer_ShouldFetchCustomObject() {
        final String key = RandomStringUtils.random(15);
        final String container = RandomStringUtils.random(15);
        final String id = RandomStringUtils.random(15);

        final CustomObject mock = mock(CustomObject.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getContainer()).thenReturn(id);
        when(mock.getKey()).thenReturn(key);

        final CustomObjectPagedQueryResult result = mock(CustomObjectPagedQueryResult.class);
        when(result.getResults()).thenReturn(Collections.singletonList(mock));

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

        /* TODO - Enable this code when service class is ready */
        /*
        final Optional<String> fetchedId = service
            .fetchCachedCustomObjectId(CustomObjectCompositeId.of(key, container))
            .toCompletableFuture().join();

        assertThat(fetchedId).contains(id);

         */
    }

    @Test
    void fetchMatchingCustomObjectsByKeysAndContainers_WithKeySet_ShouldFetchCustomObjects() {
        final String key1 = RandomStringUtils.random(15);
        final String key2 = RandomStringUtils.random(15);
        final String container1 = RandomStringUtils.random(15);
        final String container2 = RandomStringUtils.random(15);

        final Set<CustomObjectCompositeId> customObjectCompositeIds = new HashSet<>();
        customObjectCompositeIds.add(CustomObjectCompositeId.of(key1, container1));
        customObjectCompositeIds.add(CustomObjectCompositeId.of(key2, container2));

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

         /* TODO - Enable this code when service class is ready */
        /*
        final Set<CustomObject> customObjects = service
            .fetchMatchingCustomObjectsByKeysAndContainers(customObjectCompositeIds)
            .toCompletableFuture().join();

        List<CustomObjectCompositeId> customObjectCompositeIdlist =
            new ArrayList<CustomObjectCompositeId>(customObjectCompositeIds);

        assertAll(
            () -> assertThat(customObjects).contains(mock1, mock2),
            () -> assertThat(service.keyToIdCache).containsKeys(
                String.valueOf(customObjectCompositeIdlist.get(0)),
                String.valueOf(customObjectCompositeIdlist.get(1)))
        );


        verify(client).execute(any(CustomObjectQuery.class));

         */
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

         /* TODO - Enable this code when service class is ready */
        /*
        final Optional<CustomObject> customObjectOptional = service
            .fetchCustomObject(CustomObjectCompositeId.of(customObjectKey, customObjectContainer))
            .toCompletableFuture().join();

        assertAll(
            () -> assertThat(customObjectOptional).containsSame(mock)
        );
        verify(client).execute(any(CustomObjectQuery.class));

         */
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


        final CustomObjectDraft draft = CustomObjectDraft
                .ofUnversionedUpsert(customObjectContainer, customObjectKey,customObjectValue);

        /* TODO - Enable this code when service class is ready */
        /*

        final Optional<CustomObject> customObjectOptional = service.createCustomObject(draft).toCompletableFuture().join();

        assertThat(customObjectOptional).containsSame(mock);

        verify(client).execute(eq(CustomObjectUpsertCommand.of(draft)));
        */

    }

    @Test
    void createCustomObject_WithRequestException_ShouldNotCreateCustomObject() {
        final CustomObject mock = mock(CustomObject.class);
        when(mock.getId()).thenReturn(customObjectId);

        when(client.execute(any())).thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

        final CustomObjectDraft draftMock = mock(CustomObjectDraft.class);
        when(draftMock.getKey()).thenReturn(customObjectKey);
        when(draftMock.getContainer()).thenReturn(customObjectContainer);
        /* TODO - Enable this code when service class is ready */
        /* final Optional<CustomObject> customObjectOptional =
            service.createTaxCategory(draftMock).toCompletableFuture().join();

        assertAll(
            () -> assertThat(customObjectOptional).isEmpty(),
            () -> assertThat(errorMessages).hasOnlyOneElementSatisfying(message -> {
                assertThat(message).contains("Failed to create draft");
                assertThat(message).contains("BadRequestException");
            }),
            () -> assertThat(errorExceptions).hasOnlyOneElementSatisfying(exception ->
                assertThat(exception).isExactlyInstanceOf(BadRequestException.class))
        );
        */

    }

    @Test
    void createCustomObject_WithDraftHasNoKey_ShouldNotCreateCustomObject() {
        final CustomObjectDraft customObjectDraft = mock(CustomObjectDraft.class);

         /* TODO - Enable this code when service class is ready */
         /*
        final Optional<CustomObject> customObjectOptional =
            service.createCustomObject(customObjectDraft).toCompletableFuture().join();

        assertAll(
            () -> assertThat(customObjectOptional).isEmpty(),
            () -> assertThat(errorMessages).hasSize(1),
            () -> assertThat(errorExceptions).hasSize(1),
            () -> assertThat(errorMessages)
                .contains("Failed to create draft with key: 'null'. Reason: Draft key is blank!")
        );
       */
    }



}
