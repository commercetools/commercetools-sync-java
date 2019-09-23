package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.impl.CustomObjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;

public class CustomObjectServiceImplIT {

    private final String containerKey = "test-container-key";
    private CustomObjectService customObjectService;

    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;


    @BeforeEach
    void setupTest() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                .errorCallback(
                        (errorMessage, exception) -> {
                            errorCallBackMessages
                                    .add(errorMessage);
                            errorCallBackExceptions
                                    .add(exception);
                        })
                .warningCallback(warningMessage ->
                        warningCallBackMessages
                                .add(warningMessage))
                .build();


        customObjectService = new CustomObjectServiceImpl(productSyncOptions);
    }

    // TODO: Need to implement Tests

}
