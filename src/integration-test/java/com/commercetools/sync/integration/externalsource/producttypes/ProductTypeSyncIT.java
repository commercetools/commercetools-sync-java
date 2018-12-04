package com.commercetools.sync.integration.externalsource.producttypes;


import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftDsl;
import io.sphere.sdk.products.attributes.EnumAttributeType;
import io.sphere.sdk.products.attributes.LocalizedEnumAttributeType;
import io.sphere.sdk.products.attributes.MoneyAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeUpdateCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_2;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_3;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_2;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_2;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_2;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.getProductTypeByKey;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.populateTargetProject;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ProductTypeSyncIT {

    /**
     * Deletes product types from target CTP project.
     * Populates target CTP project with test data.
     */
    @Before
    public void setup() {
        deleteProductTypes(CTP_TARGET_CLIENT);
        populateTargetProject();
    }

    /**
     * Deletes all the test data from the {@code CTP_TARGET_CLIENT} project that
     * were set up in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteProductTypes(CTP_TARGET_CLIENT);
    }

    @Test
    public void sync_WithUpdatedProductType_ShouldUpdateProductType() {
        // preparation
        final Optional<ProductType> oldProductTypeBefore = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
        assertThat(oldProductTypeBefore).isNotEmpty();


        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_2,
            PRODUCT_TYPE_DESCRIPTION_2,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture()
            .join();

        // assertion
        assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType -> {
            assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_2);
            assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_2);
            assertAttributesAreEqual(productType.getAttributes(), singletonList(ATTRIBUTE_DEFINITION_DRAFT_1));
        });
    }

    @Test
    public void sync_WithNewProductType_ShouldCreateProductType() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_2,
            PRODUCT_TYPE_NAME_2,
            PRODUCT_TYPE_DESCRIPTION_2,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // tests
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();


        // assertions
        assertThat(productTypeSyncStatistics).hasValues(1, 1, 0, 0);


        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_2);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType -> {
            assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_2);
            assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_2);
            assertAttributesAreEqual(productType.getAttributes(), singletonList(ATTRIBUTE_DEFINITION_DRAFT_1));
        });
    }

    @Test
    public void sync_WithUpdatedProductType_WithNewAttribute_ShouldUpdateProductTypeAddingAttribute() {
        // preparation
        // Adding ATTRIBUTE_DEFINITION_DRAFT_3
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, ATTRIBUTE_DEFINITION_DRAFT_3)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // tests
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType ->
            assertAttributesAreEqual(productType.getAttributes(),
                asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2,
                    ATTRIBUTE_DEFINITION_DRAFT_3)
            ));
    }

    @Test
    public void sync_WithUpdatedProductType_WithoutOldAttribute_ShouldUpdateProductTypeRemovingAttribute() {
        // Removing ATTRIBUTE_DEFINITION_DRAFT_2
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0);


        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType ->
            assertAttributesAreEqual(productType.getAttributes(), singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)));
    }

    @Test
    public void sync_WithUpdatedProductType_ChangingAttributeOrder_ShouldUpdateProductTypeChangingAttributeOrder() {
        // Changing order from ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2 to
        // ATTRIBUTE_DEFINITION_DRAFT_2, ATTRIBUTE_DEFINITION_DRAFT_1
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            asList(ATTRIBUTE_DEFINITION_DRAFT_2, ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType ->
            assertAttributesAreEqual(productType.getAttributes(),
                asList(
                    ATTRIBUTE_DEFINITION_DRAFT_2,
                    ATTRIBUTE_DEFINITION_DRAFT_1)
            ));
    }

    @Test
    public void sync_WithUpdatedProductType_WithUpdatedAttributeDefinition_ShouldUpdateProductTypeUpdatingAttribute() {
        // Updating ATTRIBUTE_DEFINITION_1 (name = "attr_name_1") changing the label, attribute constraint, input tip,
        // input hint, isSearchable fields.
        final AttributeDefinitionDraft attributeDefinitionDraftUpdated = AttributeDefinitionDraftBuilder
            .of(
                StringAttributeType.of(),
                "attr_name_1",
                ofEnglish("attr_label_updated"),
                true
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(ofEnglish("inputTip_updated"))
            .inputHint(TextInputHint.MULTI_LINE)
            .isSearchable(true)
            .build();

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            singletonList(attributeDefinitionDraftUpdated)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0);


        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType ->
            assertAttributesAreEqual(productType.getAttributes(), singletonList(attributeDefinitionDraftUpdated)));
    }

    @Test
    public void sync_WithoutKey_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // Draft without key throws an error
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            null,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to process product type draft without key.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> assertThat(throwable).isNull());

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithNullDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = null;
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to process null product type draft.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> assertThat(throwable).isNull());

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithErrorCreatingTheProductType_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation

        // Invalid attribute definition due to having the same name as an already existing one but different
        // type.
        final AttributeDefinitionDraftDsl invalidAttrDefinition = AttributeDefinitionDraftBuilder
            .of(ATTRIBUTE_DEFINITION_DRAFT_1)
            .attributeType(MoneyAttributeType.of())
            .attributeConstraint(AttributeConstraint.COMBINATION_UNIQUE)
            .build();

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_2,
            PRODUCT_TYPE_NAME_2,
            PRODUCT_TYPE_DESCRIPTION_2,
            singletonList(invalidAttrDefinition)
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();


        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).contains("Failed to create draft with key: 'key_2'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(ErrorResponseException.class);
                assertThat(throwable).hasMessageContaining("AttributeDefinitionTypeConflict");
            });

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithErrorUpdatingTheProductType_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation

        // Invalid attribute definition due to having an invalid name.
        final AttributeDefinitionDraftDsl invalidAttrDefinition = AttributeDefinitionDraftBuilder
            .of(MoneyAttributeType.of(), "*invalidName*", ofEnglish("description"), true)
            .searchable(false)
            .build();

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            singletonList(invalidAttrDefinition)
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).contains("Failed to update product type with key: 'key_1'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(ErrorResponseException.class);
                assertThat(throwable).hasMessageContaining("InvalidInput");
            });

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithoutName_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        // Draft without "name" throws a commercetools exception because "name" is a required value
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            null,
            PRODUCT_TYPE_DESCRIPTION_1,
            asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2)
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).contains("Failed to update product type with key: 'key_1'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(ErrorResponseException.class);
                assertThat(throwable).hasMessageContaining("Missing required value");
            });

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithoutAttributeType_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                null,
                "attr_name_1",
                ofEnglish("attr_label_1"),
                true
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            null,
            PRODUCT_TYPE_DESCRIPTION_1,
            singletonList(attributeDefinitionDraft)
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).contains("Failed to update product type with key: 'key_1'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(ErrorResponseException.class);
                assertThat(throwable).hasMessageContaining("Missing required value");
            });

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void syncDrafts_WithConcurrentModificationException_ShouldRetryToUpdateNewCategoryWithSuccess() {
        // Preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdate();

        final ProductTypeDraft productTypeDraft =
            ProductTypeDraftBuilder.of("key", "foo", "description", emptyList())
                                   .build();

        CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft)).toCompletableFuture().join();

        final String newProductTypeName = "bar";
        final ProductTypeDraft updatedDraft =
            ProductTypeDraftBuilder.of(productTypeDraft)
                                   .name(newProductTypeName)
                                   .build();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder.of(spyClient).build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

        // Test
        final ProductTypeSyncStatistics statistics = productTypeSync.sync(singletonList(updatedDraft))
                                                                    .toCompletableFuture()
                                                                    .join();

        // Assertion
        assertThat(statistics).hasValues(1, 0, 1, 0);

        // Assert CTP state.
        final PagedQueryResult<ProductType> queryResult =
            CTP_TARGET_CLIENT.execute(ProductTypeQuery.of().plusPredicates(queryModel ->
                queryModel.key().is(productTypeDraft.getKey())))
                             .toCompletableFuture()
                             .join();

        assertThat(queryResult.head()).hasValueSatisfying(productType ->
            assertThat(productType.getName()).isEqualTo(newProductTypeName));
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdate() {

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final ProductTypeUpdateCommand anyProductTypeUpdate = any(ProductTypeUpdateCommand.class);

        when(spyClient.execute(anyProductTypeUpdate))
            .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
            .thenCallRealMethod();

        return spyClient;
    }

    @Test
    public void syncDrafts_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
        // Preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

        final ProductTypeDraft productTypeDraft =
            ProductTypeDraftBuilder.of("key", "foo", "description", emptyList())
                                   .build();

        CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft)).toCompletableFuture().join();

        final String newProductTypeName = "bar";
        final ProductTypeDraft updatedDraft =
            ProductTypeDraftBuilder.of(productTypeDraft)
                                   .name(newProductTypeName)
                                   .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, error) -> {
                errorMessages.add(errorMessage);
                errors.add(error);
            })
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

        // Test
        final ProductTypeSyncStatistics statistics = productTypeSync.sync(singletonList(updatedDraft))
                                                                    .toCompletableFuture()
                                                                    .join();

        // Assertion
        assertThat(statistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages).hasSize(1);
        assertThat(errors).hasSize(1);

        assertThat(errors.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorMessages.get(0)).contains(
            format("Failed to update product type with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                + "after concurrency modification.", productTypeDraft.getKey()));
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(ProductTypeQuery.class)))
            .thenCallRealMethod() // Call real fetch on fetching matching categories
            .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()));

        final ProductTypeUpdateCommand anyProductTypeUpdate = any(ProductTypeUpdateCommand.class);

        when(spyClient.execute(anyProductTypeUpdate))
            .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
            .thenCallRealMethod();

        return spyClient;
    }

    @Test
    public void syncDrafts_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
        // Preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

        final ProductTypeDraft productTypeDraft =
            ProductTypeDraftBuilder.of("key", "foo", "description", emptyList())
                                   .build();

        CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft)).toCompletableFuture().join();

        final String newProductTypeName = "bar";
        final ProductTypeDraft updatedDraft =
            ProductTypeDraftBuilder.of(productTypeDraft)
                                   .name(newProductTypeName)
                                   .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, error) -> {
                errorMessages.add(errorMessage);
                errors.add(error);
            })
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

        // Test
        final ProductTypeSyncStatistics statistics = productTypeSync.sync(singletonList(updatedDraft))
                                                                    .toCompletableFuture()
                                                                    .join();

        // Assertion
        assertThat(statistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages).hasSize(1);
        assertThat(errors).hasSize(1);
        assertThat(errorMessages.get(0)).contains(
            format("Failed to update product type with key: '%s'. Reason: Not found when attempting to fetch while "
                + "retrying after concurrency modification.", productTypeDraft.getKey()));
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        final ProductTypeQuery anyProductTypeQuery = any(ProductTypeQuery.class);

        when(spyClient.execute(anyProductTypeQuery))
            .thenCallRealMethod() // Call real fetch on fetching matching categories
            .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));


        final ProductTypeUpdateCommand anyProductTypeUpdateCmd = any(ProductTypeUpdateCommand.class);
        when(spyClient.execute(anyProductTypeUpdateCmd))
            .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()));


        return spyClient;
    }


    @Test
    public void sync_WithSeveralBatches_ShouldReturnProperStatistics() {
        // Default batch size is 50 (check ProductTypeSyncOptionsBuilder) so we have 2 batches of 50
        final List<ProductTypeDraft> productTypeDrafts = IntStream
            .range(0, 100)
            .mapToObj(i -> ProductTypeDraft.ofAttributeDefinitionDrafts(
                "product_type_key_" + Integer.toString(i),
                "product_type_name_" + Integer.toString(i),
                "product_type_description_" + Integer.toString(i),
                singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
            ))
            .collect(Collectors.toList());

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(productTypeDrafts)
            .toCompletableFuture().join();

        assertThat(productTypeSyncStatistics).hasValues(100, 100, 0, 0);
    }

    private static void assertAttributesAreEqual(@Nonnull final List<AttributeDefinition> attributes,
                                                 @Nonnull final List<AttributeDefinitionDraft> attributesDrafts) {

        IntStream.range(0, attributesDrafts.size())
                 .forEach(index -> {
                     final AttributeDefinition attribute = attributes.get(index);
                     final AttributeDefinitionDraft attributeDraft = attributesDrafts.get(index);

                     assertThat(attribute.getName()).isEqualTo(attributeDraft.getName());
                     assertThat(attribute.getLabel()).isEqualTo(attributeDraft.getLabel());
                     assertThat(attribute.getAttributeType()).isEqualTo(attributeDraft.getAttributeType());
                     assertThat(attribute.getInputHint()).isEqualTo(attributeDraft.getInputHint());
                     assertThat(attribute.getInputTip()).isEqualTo(attributeDraft.getInputTip());
                     assertThat(attribute.isRequired()).isEqualTo(attributeDraft.isRequired());
                     assertThat(attribute.isSearchable()).isEqualTo(attributeDraft.isSearchable());
                     assertThat(attribute.getAttributeConstraint()).isEqualTo(attributeDraft.getAttributeConstraint());

                     if (attribute.getAttributeType().getClass() == EnumAttributeType.class) {
                         assertPlainEnumsValuesAreEqual(
                             ((EnumAttributeType) attribute.getAttributeType()).getValues(),
                             ((EnumAttributeType) attributeDraft.getAttributeType()).getValues()
                         );
                     } else if (attribute.getAttributeType().getClass() == LocalizedEnumAttributeType.class) {
                         assertLocalizedEnumsValuesAreEqual(
                             ((LocalizedEnumAttributeType) attribute.getAttributeType()).getValues(),
                             ((LocalizedEnumAttributeType) attributeDraft.getAttributeType()).getValues()
                         );
                     }
                 });
    }

    private static void assertPlainEnumsValuesAreEqual(@Nonnull final List<EnumValue> enumValues,
                                                       @Nonnull final List<EnumValue> enumValuesDrafts) {

        IntStream.range(0, enumValuesDrafts.size())
                 .forEach(index -> {
                     final EnumValue enumValue = enumValues.get(index);
                     final EnumValue enumValueDraft = enumValuesDrafts.get(index);

                     assertThat(enumValue.getKey()).isEqualTo(enumValueDraft.getKey());
                     assertThat(enumValue.getLabel()).isEqualTo(enumValueDraft.getLabel());
                 });
    }

    private static void assertLocalizedEnumsValuesAreEqual(@Nonnull final List<LocalizedEnumValue> enumValues,
                                                           @Nonnull final List<LocalizedEnumValue> enumValuesDrafts) {

        IntStream.range(0, enumValuesDrafts.size())
                 .forEach(index -> {
                     final LocalizedEnumValue enumValue = enumValues.get(index);
                     final LocalizedEnumValue enumValueDraft = enumValuesDrafts.get(index);

                     assertThat(enumValue.getKey()).isEqualTo(enumValueDraft.getKey());
                     assertThat(enumValue.getLabel()).isEqualTo(enumValueDraft.getLabel());
                 });
    }
}