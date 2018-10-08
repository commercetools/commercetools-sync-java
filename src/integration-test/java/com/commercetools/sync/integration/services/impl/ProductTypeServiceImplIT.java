package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeName;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_1;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_1;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_1;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_1;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductTypeServiceImplIT {
    private ProductTypeService productTypeService;
    private static final String OLD_PRODUCT_TYPE_KEY = "old_product_type_key";
    private static final String OLD_PRODUCT_TYPE_NAME = "old_product_type_name";
    private static final Locale OLD_PRODUCT_TYPE_LOCALE = Locale.ENGLISH;

    /**
     * Deletes product types from the target CTP project, then it populates the project with test data.
     */
    @Before
    public void setup() {
        deleteProductTypes(CTP_TARGET_CLIENT);
        createProductType(OLD_PRODUCT_TYPE_KEY, OLD_PRODUCT_TYPE_LOCALE, OLD_PRODUCT_TYPE_NAME, CTP_TARGET_CLIENT);
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                               .build();
        productTypeService = new ProductTypeServiceImpl(productSyncOptions);
    }

    /**
     * Cleans up the target test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteProductTypes(CTP_TARGET_CLIENT);
    }

    @Test
    public void fetchCachedProductTypeId_WithNonExistingProductType_ShouldNotFetchAProductType() {
        final Optional<String> productTypeId = productTypeService.fetchCachedProductTypeId("non-existing-type-key")
                                                          .toCompletableFuture()
                                                          .join();
        assertThat(productTypeId).isEmpty();
    }

    @Test
    public void fetchCachedProductTypeId_WithExistingProductType_ShouldFetchProductTypeAndCache() {
        final Optional<String> productTypeId = productTypeService.fetchCachedProductTypeId(OLD_PRODUCT_TYPE_KEY)
                                                          .toCompletableFuture()
                                                          .join();
        assertThat(productTypeId).isNotEmpty();
    }

    @Test
    public void fetchCachedProductTypeId_OnSecondTime_ShouldNotFindProductTypeInCache() {
        // Fetch any product type to populate cache
        productTypeService.fetchCachedProductTypeId("anyTypeKey").toCompletableFuture().join();

        // Create new type
        final String newProductTypeKey = "new_type_key";
        final ProductTypeDraft draft = ProductTypeDraftBuilder
            .of(newProductTypeKey, "typeName", "typeDescription", new ArrayList<>()).build();
        CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(draft)).toCompletableFuture().join();

        final Optional<String> newProductTypeId =
            productTypeService.fetchCachedProductTypeId(newProductTypeKey).toCompletableFuture().join();

        assertThat(newProductTypeId).isEmpty();
    }

    @Test
    public void fetchMatchingProductTypesByKeys_WithEmptySetOfKeys_ShouldReturnEmptyList() {
        final Set<String> typeKeys = new HashSet<>();
        final List<ProductType> matchingProductTypes = productTypeService.fetchMatchingProductsTypesByKeys(typeKeys)
                .toCompletableFuture()
                .join();

        assertThat(matchingProductTypes).isEmpty();
    }

    @Test
    public void fetchMatchingProductTypesByKeys_WithNonExistingKeys_ShouldReturnEmptyList() {
        final Set<String> typeKeys = new HashSet<>();
        typeKeys.add("type_key_1");
        typeKeys.add("type_key_2");

        final List<ProductType> matchingProductTypes = productTypeService.fetchMatchingProductsTypesByKeys(typeKeys)
                .toCompletableFuture()
                .join();

        assertThat(matchingProductTypes).isEmpty();
    }

    @Test
    public void fetchMatchingProductTypesByKeys_WithAnyExistingKeys_ShouldReturnAListOfProductTypes() {
        final Set<String> typeKeys = new HashSet<>();
        typeKeys.add(OLD_PRODUCT_TYPE_KEY);

        final List<ProductType> matchingProductTypes = productTypeService.fetchMatchingProductsTypesByKeys(typeKeys)
                .toCompletableFuture()
                .join();

        assertThat(matchingProductTypes).isNotEmpty();
        assertThat(matchingProductTypes).hasSize(1);
    }

    @Test
    public void createProductType_WithValidProductType_ShouldCreateProductType() {
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_1,
                PRODUCT_TYPE_NAME_1,
                PRODUCT_TYPE_DESCRIPTION_1,
                singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductType createdProductType = productTypeService.createProductType(newProductTypeDraft)
                .toCompletableFuture().join();

        assertThat(createdProductType).isNotNull();

        final Optional<ProductType> productTypeOptional = CTP_TARGET_CLIENT
                .execute(ProductTypeQuery.of()
                    .withPredicates(productTypeQueryModel ->
                            productTypeQueryModel.key().is(createdProductType.getKey())))
                .toCompletableFuture().join().head();

        assertThat(productTypeOptional).isNotEmpty();
        final ProductType fetchedProductType = productTypeOptional.get();
        assertThat(fetchedProductType.getKey()).isEqualTo(newProductTypeDraft.getKey());
        assertThat(fetchedProductType.getDescription()).isEqualTo(createdProductType.getDescription());
        assertThat(fetchedProductType.getName()).isEqualTo(createdProductType.getName());
        assertThat(fetchedProductType.getAttributes()).isEqualTo(createdProductType.getAttributes());
    }

    @Test
    public void createProductType_WithInvalidProductType_ShouldNotCreateProductType() {
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_1,
                PRODUCT_TYPE_NAME_1,
                PRODUCT_TYPE_DESCRIPTION_1,
                singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        productTypeService.createProductType(newProductTypeDraft)
                .exceptionally(exception -> {
                    assertThat(exception).isNotNull();
                    assertThat(exception.getMessage()).contains("Request body does not contain valid JSON.");
                    return null;
                })
                .toCompletableFuture().join();
    }

    @Test
    public void updateProductType_WithValidChanges_ShouldUpdateProductTypeCorrectly() {
        final Optional<ProductType> productTypeOptional = CTP_TARGET_CLIENT
                .execute(ProductTypeQuery.of()
                        .withPredicates(productTypeQueryModel -> productTypeQueryModel.key().is(OLD_PRODUCT_TYPE_KEY)))
                .toCompletableFuture().join().head();
        assertThat(productTypeOptional).isNotNull();

        final ChangeName changeNameUpdateAction = ChangeName.of("new_product_type_name");

        final ProductType updatedProductType =
                productTypeService.updateProductType(productTypeOptional.get(), singletonList(changeNameUpdateAction))
                .toCompletableFuture().join();
        assertThat(updatedProductType).isNotNull();

        final Optional<ProductType> updatedProductTypeOptional = CTP_TARGET_CLIENT
                .execute(ProductTypeQuery.of()
                        .withPredicates(productTypeQueryModel -> productTypeQueryModel.key().is(OLD_PRODUCT_TYPE_KEY)))
                .toCompletableFuture().join().head();

        assertThat(productTypeOptional).isNotEmpty();
        final ProductType fetchedProductType = updatedProductTypeOptional.get();
        assertThat(fetchedProductType.getKey()).isEqualTo(updatedProductType.getKey());
        assertThat(fetchedProductType.getDescription()).isEqualTo(updatedProductType.getDescription());
        assertThat(fetchedProductType.getName()).isEqualTo(updatedProductType.getName());
        assertThat(fetchedProductType.getAttributes()).isEqualTo(updatedProductType.getAttributes());
    }

    @Test
    public void updateProductType_WithInvalidChanges_ShouldNotUpdateProductType() {
        final Optional<ProductType> typeOptional = CTP_TARGET_CLIENT
                .execute(ProductTypeQuery.of()
                        .withPredicates(typeQueryModel -> typeQueryModel.key().is(OLD_PRODUCT_TYPE_KEY)))
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
