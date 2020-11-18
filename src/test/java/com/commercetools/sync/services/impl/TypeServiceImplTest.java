package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.FakeClient;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeServiceImplTest {

    @Test
    void fetchMatchingTypesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
        // Mock sphere client to return BadGatewayException on any request.
        final FakeClient<Type> fakeTypeClient = new FakeClient<>(new BadGatewayException());

        final List<String> errorCallBackMessages = new ArrayList<>();
        final List<Throwable> errorCallBackExceptions = new ArrayList<>();

        final TypeSyncOptions spyOptions =
                TypeSyncOptionsBuilder.of(fakeTypeClient)
                        .errorCallback((exception, oldResource, newResource, updateActions) -> {
                            errorCallBackMessages.add(exception.getMessage());
                            errorCallBackExceptions.add(exception.getCause());
                        })
                        .build();

        final TypeService spyTypeService = new TypeServiceImpl(spyOptions);


        final Set<String> keys = new HashSet<>();
        keys.add("old_type_key");

        // test and assert
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(spyTypeService.fetchMatchingTypesByKeys(keys))
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BadGatewayException.class);
    }
}