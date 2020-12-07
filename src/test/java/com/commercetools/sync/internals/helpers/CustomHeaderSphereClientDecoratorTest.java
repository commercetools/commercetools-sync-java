package com.commercetools.sync.internals.helpers;

import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.http.HttpHeaders;
import io.sphere.sdk.models.Versioned;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeUpdateCommand;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeName;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.utils.SyncSolutionInfo.LIB_VERSION;
import static com.commercetools.sync.commons.utils.SyncSolutionInfo.LIB_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CustomHeaderSphereClientDecoratorTest {

    @Test
    void executeRequest_shouldHaveLibVersionInRequestHeader() {

        SphereClient sphereClient = CustomHeaderSphereClientDecorator.of(mock(SphereClient.class));
        SphereRequest<ProductType> productTypeUpdateSphereRequest =
                ProductTypeUpdateCommand.of(
                        Versioned.of("ID", 1L),
                        Collections.singletonList(ChangeName.of("newName")));

        sphereClient.execute(productTypeUpdateSphereRequest);
        HttpRequestIntent requestIntent = ((CustomHeaderSphereClientDecorator)sphereClient).getHttpRequestIntent();
        HttpHeaders headers = requestIntent.getHeaders();
        assertThat(headers.getHeader(HttpHeaders.USER_AGENT).size()).isGreaterThan(0);
        assertThat(headers.getHeader(HttpHeaders.USER_AGENT).get(0))
                .isEqualTo(LIB_NAME + " (ver : " + LIB_VERSION + ")");
    }
}
