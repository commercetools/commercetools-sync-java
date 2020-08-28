package com.commercetools.sync.internals.helpers;

import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.http.HttpHeaders;
import io.sphere.sdk.models.Versioned;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeUpdateCommand;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;


import static com.commercetools.sync.commons.utils.SyncSolutionInfo.LIB_NAME;
import static com.commercetools.sync.commons.utils.SyncSolutionInfo.LIB_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


public class CustomHeaderSphereClientDecoratorTest {


    @Test
    void executeRequest_shouldHaveLibVersionInRequestHeader() {

        SphereClient sphereClient = CustomHeaderSphereClientDecorator.of(mock(SphereClient.class));
        List<UpdateAction<ProductType>> updateActionList = (ArrayList<UpdateAction<ProductType>>)mock(ArrayList.class);
        Versioned<ProductType> version = (Versioned<ProductType>)mock(Versioned.class);
        SphereRequest<ProductType> productTypeUpdateSphereRequest =
                ProductTypeUpdateCommand.of(version, updateActionList);

        sphereClient.execute(productTypeUpdateSphereRequest);
        HttpRequestIntent requestIntent = ((CustomHeaderSphereClientDecorator)sphereClient).getHttpRequestIntent();
        HttpHeaders headers = requestIntent.getHeaders();
        assertThat(headers.getHeader(LIB_NAME).size()).isGreaterThan(0);
        assertThat(headers.getHeader(LIB_NAME).get(0)).isEqualTo(LIB_VERSION);

    }
}
