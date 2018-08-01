package com.commercetools.sync.integration.producttypes;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.EnumAttributeType;
import io.sphere.sdk.products.attributes.LocalizedEnumAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;

import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_1;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_2;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_3;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_1;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_2;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_1;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_2;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_1;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_2;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.deleteProductTypesFromTargetAndSource;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.getProductTypeByKey;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.populateSourceProject;
import static com.commercetools.sync.integration.producttypes.utils.ProductTypeITUtils.populateTargetProject;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ProductTypeSyncIT {

    /**
     * Deletes inventories and supply channels from source and target CTP projects.
     * Populates source and target CTP projects with test data.
     */
    @Before
    public void setup() {
        deleteProductTypesFromTargetAndSource();
        populateSourceProject();
        populateTargetProject();
    }

    /**
     * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT} projects that
     * were set up in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteProductTypesFromTargetAndSource();
    }

    @Test
    public void sync_WithUpdatedProductType_ShouldUpdateProductType() {
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

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0);


        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

        assertThat(oldProductTypeAfter).isNotEmpty();
        assertThat(oldProductTypeAfter.get().getName()).isEqualTo(PRODUCT_TYPE_NAME_2);
        assertThat(oldProductTypeAfter.get().getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_2);
        assertAttributesAreEqual(oldProductTypeAfter.get().getAttributes(),
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1));

    }

    @Test
    public void sync_WithNewProductType_ShouldCreateProductType() {
        final Optional<ProductType> oldProductTypeBefore = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_2);
        assertThat(oldProductTypeBefore).isEmpty();

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

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(1, 1, 0, 0);


        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_2);

        assertThat(oldProductTypeAfter).isNotEmpty();
        assertThat(oldProductTypeAfter.get().getName()).isEqualTo(PRODUCT_TYPE_NAME_2);
        assertThat(oldProductTypeAfter.get().getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_2);
        assertAttributesAreEqual(oldProductTypeAfter.get().getAttributes(),
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1));
    }

    @Test
    public void sync_WithUpdatedProductType_WithNewAttribute_ShouldUpdateProductTypeAddingAttribute() {
        final Optional<ProductType> oldProductTypeBefore = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
        assertThat(oldProductTypeBefore).isNotEmpty();

        // Adding ATTRIBUTE_DEFINITION_DRAFT_3
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            Arrays.asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, ATTRIBUTE_DEFINITION_DRAFT_3)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0);


        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

        assertThat(oldProductTypeAfter).isNotEmpty();
        assertAttributesAreEqual(oldProductTypeAfter.get().getAttributes(), Arrays.asList(
            ATTRIBUTE_DEFINITION_DRAFT_1,
            ATTRIBUTE_DEFINITION_DRAFT_2,
            ATTRIBUTE_DEFINITION_DRAFT_3)
        );
    }

    @Test
    public void sync_WithUpdatedProductType_WithoutOldAttribute_ShouldUpdateProductTypeRemovingAttribute() {
        final Optional<ProductType> oldProductTypeBefore = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
        assertThat(oldProductTypeBefore).isNotEmpty();

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

        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0);


        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

        assertThat(oldProductTypeAfter).isNotEmpty();
        assertAttributesAreEqual(oldProductTypeAfter.get().getAttributes(),
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1));
    }

    @Test
    public void sync_WithUpdatedProductType_ChangingAttributeOrder_ShouldUpdateProductTypeChangingAttributeOrder() {
        final Optional<ProductType> oldProductTypeBefore = getProductTypeByKey(CTP_SOURCE_CLIENT, PRODUCT_TYPE_KEY_1);
        assertThat(oldProductTypeBefore).isNotEmpty();

        // Changing order from ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2 to
        // ATTRIBUTE_DEFINITION_DRAFT_2, ATTRIBUTE_DEFINITION_DRAFT_1
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            Arrays.asList(ATTRIBUTE_DEFINITION_DRAFT_2, ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_SOURCE_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0);


        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_SOURCE_CLIENT, PRODUCT_TYPE_KEY_1);

        assertThat(oldProductTypeAfter).isNotEmpty();
        assertAttributesAreEqual(oldProductTypeAfter.get().getAttributes(), Arrays.asList(
            ATTRIBUTE_DEFINITION_DRAFT_2,
            ATTRIBUTE_DEFINITION_DRAFT_1)
        );
    }

    @Test
    public void sync_WithUpdatedProductType_WithUpdatedAttributeDefinition_ShouldUpdateProductTypeUpdatingAttribute() {
        final Optional<ProductType> oldProductTypeBefore = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
        assertThat(oldProductTypeBefore).isNotEmpty();

        // Updating ATTRIBUTE_DEFINITION_1 (name = "attr_name_1") changing the label, attribute contraint, input tip,
        // input hint, isSearchable fields.
        final AttributeDefinitionDraft attributeDefinitionDraftUpdated = AttributeDefinitionDraftBuilder
            .of(
                StringAttributeType.of(),
                "attr_name_1",
                LocalizedString.ofEnglish("attr_label_updated"),
                true
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip_updated"))
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

        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0);


        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

        assertThat(oldProductTypeAfter).isNotEmpty();
        assertAttributesAreEqual(oldProductTypeAfter.get().getAttributes(),
            singletonList(attributeDefinitionDraftUpdated));
    }

    @Test
    public void sync_FromSourceToTargetProjectWithoutUpdates_ShouldReturnProperStatistics() {
        //Fetch new product types from source project. Convert them to drafts.
        final List<ProductType> productTypes = CTP_SOURCE_CLIENT
            .execute(ProductTypeQuery.of())
            .toCompletableFuture().join().getResults();

        final List<ProductTypeDraft> productTypeDrafts = productTypes
            .stream()
            .map(productType -> {
                List<AttributeDefinitionDraft> attributeDefinitionDrafts = productType
                    .getAttributes()
                    .stream()
                    .map(attribute -> AttributeDefinitionDraftBuilder.of(attribute).build())
                    .collect(Collectors.toList());

                return ProductTypeDraftBuilder
                    .of(
                        productType.getKey(),
                        productType.getName(),
                        productType.getDescription(),
                        attributeDefinitionDrafts)
                    .build();
            })
            .collect(Collectors.toList());

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(productTypeDrafts)
            .toCompletableFuture().join();

        // Attribute Definition with key "key_1" already exists in target, so it's not updated
        // Attribute Definition with key "key_2" doesn't exist in target, so it's created
        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(2, 1, 0, 0);
    }

    @Test
    public void sync_FromSourceToTargetProjectWithUpdates_ShouldReturnProperStatistics() {
        //Fetch new product types from source project. Convert them to drafts.
        final List<ProductType> productTypes = CTP_SOURCE_CLIENT
            .execute(ProductTypeQuery.of())
            .toCompletableFuture().join().getResults();

        final List<ProductTypeDraft> productTypeDrafts = productTypes
            .stream()
            .map(productType -> {
                List<AttributeDefinitionDraft> attributeDefinitionDrafts = productType
                    .getAttributes()
                    .stream()
                    .map(attribute -> AttributeDefinitionDraftBuilder.of(attribute).build())
                    .collect(Collectors.toList());

                return ProductTypeDraftBuilder
                    .of(
                        productType.getKey(),
                        productType.getName() + "_updated", // name updated
                        productType.getDescription(),
                        attributeDefinitionDrafts)
                    .build();
            })
            .collect(Collectors.toList());

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(productTypeDrafts)
            .toCompletableFuture().join();

        // Attribute Definition with key "key_1" already exists in target, so it's updated
        // Attribute Definition with key "key_2" doesn't exist in target, so it's created
        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(2, 1, 1, 0);
    }

    @Test
    public void sync_FromSourceToTargetProjectWithUpdates_ShouldReturnProperStatisticsMessage() {
        //Fetch new product types from source project. Convert them to drafts.
        final List<ProductType> productTypes = CTP_SOURCE_CLIENT
            .execute(ProductTypeQuery.of())
            .toCompletableFuture().join().getResults();

        final List<ProductTypeDraft> productTypeDrafts = productTypes
            .stream()
            .map(productType -> {
                List<AttributeDefinitionDraft> attributeDefinitionDrafts = productType
                    .getAttributes()
                    .stream()
                    .map(attribute -> AttributeDefinitionDraftBuilder.of(attribute).build())
                    .collect(Collectors.toList());

                return ProductTypeDraftBuilder
                    .of(
                        productType.getKey(),
                        productType.getName() + "_updated", // name updated
                        productType.getDescription(),
                        attributeDefinitionDrafts)
                    .build();
            })
            .collect(Collectors.toList());

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(productTypeDrafts)
            .toCompletableFuture().join();

        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 products types were processed in total"
                + " (1 created, 1 updated and 0 failed to sync).");
    }

    @Test
    public void sync_WithoutKey_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // Draft without key throws an error
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            null,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            Arrays.asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        final ProductTypeSync productTypeSync = new ProductTypeSync(spyProductTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        verify(spyProductTypeSyncOptions).applyErrorCallback("Failed to process product type draft without key.", null);

        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithoutName_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // Draft without "name" throws a commercetools exception because "name" is a required value
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            null,
            PRODUCT_TYPE_DESCRIPTION_1,
            Arrays.asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        final ProductTypeSync productTypeSync = new ProductTypeSync(spyProductTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // Since the error message and exception is coming from commercetools, we don't test the actual message and
        // exception
        verify(spyProductTypeSyncOptions).applyErrorCallback(any(), any());

        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithoutAttributeType_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                null,
                "attr_name_1",
                LocalizedString.ofEnglish("attr_label_1"),
                true
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            null,
            PRODUCT_TYPE_DESCRIPTION_1,
            singletonList(attributeDefinitionDraft)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        final ProductTypeSync productTypeSync = new ProductTypeSync(spyProductTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // Since the error message and exception is coming from commercetools, we don't test the actual message and
        // exception
        verify(spyProductTypeSyncOptions).applyErrorCallback(any(), any());

        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1);
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
                Arrays.asList(ATTRIBUTE_DEFINITION_DRAFT_1)
            ))
            .collect(Collectors.toList());

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(productTypeDrafts)
            .toCompletableFuture().join();

        AssertionsForStatistics.assertThat(productTypeSyncStatistics).hasValues(100, 100, 0, 0);
    }


    @Test
    public void sync_beforeCreate_ShouldCallBeforeCreateCallback() {
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_2,
            PRODUCT_TYPE_NAME_2,
            PRODUCT_TYPE_DESCRIPTION_2,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        final ProductTypeSync productTypeSync = new ProductTypeSync(spyProductTypeSyncOptions);

        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

        verify(spyProductTypeSyncOptions).applyBeforeCreateCallBack(newProductTypeDraft);
    }

    @Test
    public void sync_beforeCreate_ShouldNotCallBeforeUpdateCallback() {
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_2,
            PRODUCT_TYPE_NAME_2,
            PRODUCT_TYPE_DESCRIPTION_2,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        final ProductTypeSync productTypeSync = new ProductTypeSync(spyProductTypeSyncOptions);

        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

        verify(spyProductTypeSyncOptions, never()).applyBeforeUpdateCallBack(any(), any(), any());
    }

    @Test
    public void sync_beforeUpdate_ShouldCallBeforeUpdateCallback() {
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_2,
            PRODUCT_TYPE_DESCRIPTION_2,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        final ProductTypeSync productTypeSync = new ProductTypeSync(spyProductTypeSyncOptions);

        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

        verify(spyProductTypeSyncOptions).applyBeforeUpdateCallBack(any(), any(), any());
    }

    @Test
    public void sync_beforeUpdate_ShouldNotCallBeforeCreateCallback() {
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_2,
            PRODUCT_TYPE_DESCRIPTION_2,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        final ProductTypeSync productTypeSync = new ProductTypeSync(spyProductTypeSyncOptions);

        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

        verify(spyProductTypeSyncOptions, never()).applyBeforeCreateCallBack(newProductTypeDraft);
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
