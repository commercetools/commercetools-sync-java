package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.errors.DuplicateFieldError;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeName;
import io.sphere.sdk.producttypes.commands.updateactions.SetKey;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductTypeServiceImplIT {
    private ProductTypeService productTypeService;
    private static final String OLD_PRODUCT_TYPE_KEY = "old_product_type_key";
    private static final String OLD_PRODUCT_TYPE_NAME = "old_product_type_name";
    private static final Locale OLD_PRODUCT_TYPE_LOCALE = Locale.ENGLISH;

    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Deletes product types from the target CTP project, then it populates the project with test data.
     */
    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();

        deleteProductTypes(CTP_TARGET_CLIENT);
        createProductType(OLD_PRODUCT_TYPE_KEY, OLD_PRODUCT_TYPE_LOCALE, OLD_PRODUCT_TYPE_NAME, CTP_TARGET_CLIENT);
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                                           .build();
        productTypeService = new ProductTypeServiceImpl(productTypeSyncOptions);
    }

    /**
     * Cleans up the target test data that were built in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteProductTypes(CTP_TARGET_CLIENT);
    }

    @Test
    void fetchCachedProductTypeId_WithNonExistingProductType_ShouldNotFetchAProductType() {
        final Optional<String> productTypeId = productTypeService.fetchCachedProductTypeId("non-existing-type-key")
                                                                 .toCompletableFuture()
                                                                 .join();
        assertThat(productTypeId).isEmpty();
    }

    @Test
    void fetchCachedProductTypeId_WithExistingProductType_ShouldFetchProductTypeAndCache() {
        final Optional<String> productTypeId = productTypeService.fetchCachedProductTypeId(OLD_PRODUCT_TYPE_KEY)
                                                                 .toCompletableFuture()
                                                                 .join();
        assertThat(productTypeId).isNotEmpty();
    }

    @Test
    void fetchCachedProductAttributeMetaDataMap_WithMetadataCache_ShouldReturnAnyAttributeMetadata() {
        final Optional<String> productTypeId = productTypeService.fetchCachedProductTypeId(OLD_PRODUCT_TYPE_KEY)
                                                                 .toCompletableFuture()
                                                                 .join();

        assertThat(productTypeId).isNotEmpty();

        Optional<Map<String, AttributeMetaData>> fetchCachedProductAttributeMetaDataMap
            = productTypeService.fetchCachedProductAttributeMetaDataMap(productTypeId.get())
                                .toCompletableFuture()
                                .join();

        assertThat(fetchCachedProductAttributeMetaDataMap).isNotEmpty();
    }

    @Test
    void fetchCachedProductAttributeMetaDataMap_WithoutMetadataCache_ShouldReturnAnyAttributeMetadata() {
        final Optional<ProductType> productTypeOptional = CTP_TARGET_CLIENT
            .execute(ProductTypeQuery.of().byKey(OLD_PRODUCT_TYPE_KEY))
            .toCompletableFuture()
            .join().head();

        assertThat(productTypeOptional).isNotEmpty();

        if (productTypeOptional.isPresent()) {
            Optional<Map<String, AttributeMetaData>> fetchCachedProductAttributeMetaDataMap
                = productTypeService.fetchCachedProductAttributeMetaDataMap(productTypeOptional.get().getId())
                                    .toCompletableFuture()
                                    .join();

            assertThat(fetchCachedProductAttributeMetaDataMap).isNotEmpty();
        }
    }

    @Test
    void fetchMatchingProductTypesByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
        final Set<String> typeKeys = new HashSet<>();
        final Set<ProductType> matchingProductTypes = productTypeService.fetchMatchingProductTypesByKeys(typeKeys)
                                                                        .toCompletableFuture()
                                                                        .join();

        assertThat(matchingProductTypes).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void fetchMatchingProductTypesByKeys_WithNonExistingKeys_ShouldReturnEmptySet() {
        final Set<String> typeKeys = new HashSet<>();
        typeKeys.add("type_key_1");
        typeKeys.add("type_key_2");

        final Set<ProductType> matchingProductTypes = productTypeService.fetchMatchingProductTypesByKeys(typeKeys)
                                                                        .toCompletableFuture()
                                                                        .join();

        assertThat(matchingProductTypes).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void fetchMatchingProductTypesByKeys_WithAnyExistingKeys_ShouldReturnASetOfProductTypes() {
        final Set<String> typeKeys = new HashSet<>();
        typeKeys.add(OLD_PRODUCT_TYPE_KEY);

        final Set<ProductType> matchingProductTypes = productTypeService.fetchMatchingProductTypesByKeys(typeKeys)
                                                                        .toCompletableFuture()
                                                                        .join();

        assertThat(matchingProductTypes).isNotEmpty();
        assertThat(matchingProductTypes).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void fetchMatchingProductTypesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
        // Mock sphere client to return BadGatewayException on any request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(ProductTypeQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
            .thenCallRealMethod();
        final ProductTypeSyncOptions spyOptions =
            ProductTypeSyncOptionsBuilder.of(spyClient)
                                         .errorCallback((errorMessage, exception) -> {
                                             errorCallBackMessages.add(errorMessage);
                                             errorCallBackExceptions.add(exception);
                                         })
                                         .build();

        final ProductTypeService spyProductTypeService = new ProductTypeServiceImpl(spyOptions);


        final Set<String> keys = new HashSet<>();
        keys.add(OLD_PRODUCT_TYPE_KEY);

        // test and assert
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(spyProductTypeService.fetchMatchingProductTypesByKeys(keys))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(BadGatewayException.class);
    }

    @Test
    void fetchMatchingProductTypesByKeys_WithAllExistingSetOfKeys_ShouldCacheFetchedProductTypeIds() {
        final Set<ProductType> fetchedProductTypes = productTypeService.fetchMatchingProductTypesByKeys(
            singleton(OLD_PRODUCT_TYPE_KEY))
                                                                       .toCompletableFuture().join();
        assertThat(fetchedProductTypes).hasSize(1);

        final Optional<ProductType> productTypeOptional = CTP_TARGET_CLIENT
            .execute(ProductTypeQuery.of()
                                     .withPredicates(productTypeQueryModel -> productTypeQueryModel.key().is(
                                         OLD_PRODUCT_TYPE_KEY)))
            .toCompletableFuture().join().head();
        assertThat(productTypeOptional).isNotNull();

        // Change product oldKey on ctp
        final String newKey = "newKey";
        productTypeService.updateProductType(productTypeOptional.get(), Collections.singletonList(SetKey.of(newKey)))
                          .toCompletableFuture()
                          .join();

        // Fetch cached id by old key
        final Optional<String> cachedProductTypeId = productTypeService.fetchCachedProductTypeId(OLD_PRODUCT_TYPE_KEY)
                                                                       .toCompletableFuture().join();

        assertThat(cachedProductTypeId).isNotEmpty();
        assertThat(cachedProductTypeId).contains(productTypeOptional.get().getId());
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void createProductType_WithValidProductType_ShouldCreateProductTypeAndCacheId() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        final ProductTypeSyncOptions spyOptions = ProductTypeSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, exception) -> {
                errorCallBackMessages.add(errorMessage);
                errorCallBackExceptions.add(exception);
            })
            .build();

        final ProductTypeService spyProductTypeService = new ProductTypeServiceImpl(spyOptions);

        // test
        final Optional<ProductType> createdOptional = spyProductTypeService
            .createProductType(newProductTypeDraft)
            .toCompletableFuture().join();
        // assertion
        final Optional<ProductType> queriedOptional = CTP_TARGET_CLIENT
            .execute(ProductTypeQuery.of().withPredicates(productTypeQueryModel ->
                productTypeQueryModel.key().is(PRODUCT_TYPE_KEY_1)))
            .toCompletableFuture().join().head();

        assertThat(queriedOptional)
            .hasValueSatisfying(queried -> assertThat(createdOptional)
                .hasValueSatisfying(created -> {
                    assertThat(created.getKey()).isEqualTo(queried.getKey());
                    assertThat(created.getDescription()).isEqualTo(queried.getDescription());
                    assertThat(created.getName()).isEqualTo(queried.getName());
                    assertThat(created.getAttributes()).isEqualTo(queried.getAttributes());
                }));

        // Assert that the created product type is cached
        final Optional<String> productTypeId =
            spyProductTypeService.fetchCachedProductTypeId(PRODUCT_TYPE_KEY_1).toCompletableFuture().join();
        assertThat(productTypeId).isPresent();
        verify(spyClient, times(0)).execute(any(ProductTypeQuery.class));
    }

    @Test
    void createProductType_WithInvalidProductType_ShouldHaveEmptyOptionalAsAResult() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "",
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions options = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorCallBackMessages.add(errorMessage);
                errorCallBackExceptions.add(exception);
            })
            .build();

        final ProductTypeService service = new ProductTypeServiceImpl(options);

        // test
        final Optional<ProductType> result =
            service.createProductType(newProductTypeDraft).toCompletableFuture().join();

        // assertion
        assertThat(result).isEmpty();
        assertThat(errorCallBackMessages)
            .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
    }

    @Test
    void createProductType_WithDuplicateKey_ShouldHaveEmptyOptionalAsAResult() {
        //preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            OLD_PRODUCT_TYPE_KEY,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions options = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorCallBackMessages.add(errorMessage);
                errorCallBackExceptions.add(exception);
            })
            .build();

        final ProductTypeService service = new ProductTypeServiceImpl(options);

        // test
        final Optional<ProductType> result =
            service.createProductType(newProductTypeDraft).toCompletableFuture().join();

        // assertion
        assertThat(result).isEmpty();
        assertThat(errorCallBackMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(msg -> assertThat(msg).contains("A duplicate value"));
        assertThat(errorCallBackExceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(exception -> {
                assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class);
                final ErrorResponseException errorResponseException = (ErrorResponseException) exception;

                final List<DuplicateFieldError> fieldErrors = errorResponseException
                    .getErrors()
                    .stream()
                    .map(sphereError -> {
                        assertThat(sphereError.getCode()).isEqualTo(DuplicateFieldError.CODE);
                        return sphereError.as(DuplicateFieldError.class);
                    })
                    .collect(toList());
                assertThat(fieldErrors).hasSize(1);
            });
    }

    @Test
    void updateProductType_WithValidChanges_ShouldUpdateProductTypeCorrectly() {
        final Optional<ProductType> productTypeOptional = CTP_TARGET_CLIENT
            .execute(ProductTypeQuery.of()
                                     .withPredicates(productTypeQueryModel -> productTypeQueryModel.key().is(
                                         OLD_PRODUCT_TYPE_KEY)))
            .toCompletableFuture().join().head();
        assertThat(productTypeOptional).isNotNull();

        final ChangeName changeNameUpdateAction = ChangeName.of("new_product_type_name");

        final ProductType updatedProductType =
            productTypeService.updateProductType(productTypeOptional.get(), singletonList(changeNameUpdateAction))
                              .toCompletableFuture().join();
        assertThat(updatedProductType).isNotNull();

        final Optional<ProductType> updatedProductTypeOptional = CTP_TARGET_CLIENT
            .execute(ProductTypeQuery.of()
                                     .withPredicates(productTypeQueryModel -> productTypeQueryModel.key().is(
                                         OLD_PRODUCT_TYPE_KEY)))
            .toCompletableFuture().join().head();

        assertThat(productTypeOptional).isNotEmpty();
        final ProductType fetchedProductType = updatedProductTypeOptional.get();
        assertThat(fetchedProductType.getKey()).isEqualTo(updatedProductType.getKey());
        assertThat(fetchedProductType.getDescription()).isEqualTo(updatedProductType.getDescription());
        assertThat(fetchedProductType.getName()).isEqualTo(updatedProductType.getName());
        assertThat(fetchedProductType.getAttributes()).isEqualTo(updatedProductType.getAttributes());
    }

    @Test
    void updateProductType_WithInvalidChanges_ShouldCompleteExceptionally() {
        final Optional<ProductType> typeOptional = CTP_TARGET_CLIENT
            .execute(ProductTypeQuery
                .of()
                .withPredicates(productTypeQueryModel -> productTypeQueryModel.key().is(OLD_PRODUCT_TYPE_KEY)))
            .toCompletableFuture().join().head();
        assertThat(typeOptional).isNotNull();

        final ChangeName changeNameUpdateAction = ChangeName.of(null);
        productTypeService.updateProductType(typeOptional.get(), singletonList(changeNameUpdateAction))
                          .exceptionally(exception -> {
                              assertThat(exception).isNotNull();
                              assertThat(exception.getMessage()).contains("Request body does not contain valid JSON.");
                              return null;
                          })
                          .toCompletableFuture().join();
    }
}