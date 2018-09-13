package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TypeServiceTest {

    private TypeService service;
    private TypeSyncOptions typeSyncOptions;
    private List<String> errorMessages;
    private List<Throwable> errorExceptions;

    @Before
    public void setUp() {
        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
        typeSyncOptions = TypeSyncOptionsBuilder.of(mock(SphereClient.class))
                                                      .errorCallback((errorMessage, errorException) -> {
                                                          errorMessages.add(errorMessage);
                                                          errorExceptions.add(errorException);
                                                      })
                                                      .build();
        service = new TypeServiceImpl(typeSyncOptions);
    }


    @Test
    public void createType_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
        final Type mock = mock(Type.class);
        when(mock.getId()).thenReturn("typeId");

        when(typeSyncOptions.getCtpClient().execute(any()))
                .thenReturn(completedFuture(mock));

        final TypeDraft draft = mock(TypeDraft.class);
        when(draft.getKey()).thenReturn("typeKey");

        final Optional<Type> typeOptional = Optional.ofNullable(service.createType(draft)
                .toCompletableFuture().join());

        assertThat(typeOptional).isNotEmpty();
        assertThat(typeOptional).containsSame(mock);

        verify(typeSyncOptions.getCtpClient()).execute(eq(TypeCreateCommand.of(draft)));
    }

    @Test
    public void createType_WithUnSuccessfulMockCtpResponse_ShouldNotCreateType() {
        final Type mock = mock(Type.class);
        when(mock.getId()).thenReturn("typeId");

        when(typeSyncOptions.getCtpClient().execute(any()))
                .thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

        final TypeDraft draft = mock(TypeDraft.class);
        when(draft.getKey()).thenReturn("typeKey");
        final Optional<Type> typeOptional = Optional.ofNullable(service.createType(draft).toCompletableFuture().join());

        assertThat(typeOptional).isEmpty();
        assertThat(errorMessages).hasSize(1);
        assertThat(errorExceptions).hasSize(1);
        assertThat(errorExceptions.get(0)).isExactlyInstanceOf(BadRequestException.class);
        assertThat(errorMessages.get(0)).contains("Failed to create draft with key: 'typeKey'.");
        assertThat(errorMessages.get(0)).contains("BadRequestException");
    }

    @Test
    public void createType_WithDraftWithoutKey_ShouldNotCreateType() {
        final TypeDraft draft = mock(TypeDraft.class);
        final Optional<Type> typeOptional = Optional.ofNullable(service.createType(draft).toCompletableFuture().join());

        assertThat(typeOptional).isEmpty();
        assertThat(errorMessages).hasSize(1);
        assertThat(errorExceptions).hasSize(1);
        assertThat(errorMessages.get(0))
                .isEqualTo("Failed to create draft with key: 'null'. Reason: Draft key is blank!");
    }



}