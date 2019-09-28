package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.SetAttribute;
import io.sphere.sdk.products.queries.ProductByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeUpdateCommand;
import io.sphere.sdk.producttypes.commands.updateactions.AddAttributeDefinition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.externalsource.products.ProductSyncWithNestedReferencedProductsIT.createArrayNode;
import static com.commercetools.sync.integration.externalsource.products.ProductSyncWithNestedReferencedProductsIT.createNestedAttributeValueReferences;
import static com.commercetools.sync.integration.externalsource.products.ProductSyncWithNestedReferencedProductsIT.createNestedAttributeValueSetOfReferences;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ProductSyncWithNestedReferencedCategoriesIT {
    private static ProductType productType;
    private static Category testCategory1;
    private static Category testCategory2;

    private ProductSyncOptions syncOptions;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private List<UpdateAction<Product>> actions;

    private static final String ATTRIBUTE_NAME_FIELD = "name";
    private static final String ATTRIBUTE_VALUE_FIELD = "value";

    @BeforeAll
    static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        final ProductType nestedProductType = createProductType(PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH,
            CTP_TARGET_CLIENT);


        final AttributeDefinitionDraft nestedAttributeDef = AttributeDefinitionDraftBuilder
            .of(NestedAttributeType.of(nestedProductType), "nestedAttribute", ofEnglish("nestedAttribute"), false)
            .searchable(false)
            .build();

        final AttributeDefinitionDraft setOfNestedAttributeDef = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(NestedAttributeType.of(nestedProductType)), "setOfNestedAttribute",
                ofEnglish("setOfNestedAttribute"), false)
            .searchable(false)
            .build();


        final ProductTypeUpdateCommand productTypeUpdateCommand = ProductTypeUpdateCommand.of(productType,
            asList(AddAttributeDefinition.of(nestedAttributeDef), AddAttributeDefinition.of(setOfNestedAttributeDef)));

        CTP_TARGET_CLIENT.execute(productTypeUpdateCommand).toCompletableFuture().join();

        final CategoryDraft category1Draft = CategoryDraftBuilder
            .of(ofEnglish("cat1-name"), ofEnglish("cat1-slug"))
            .key("cat1-key")
            .build();

        testCategory1 = CTP_TARGET_CLIENT
            .execute(CategoryCreateCommand.of(category1Draft))
            .toCompletableFuture()
            .join();

        final CategoryDraft category2Draft = CategoryDraftBuilder
            .of(ofEnglish("cat2-name"), ofEnglish("cat2-slug"))
            .key("cat2-key")
            .build();

        testCategory2 = CTP_TARGET_CLIENT
            .execute(CategoryCreateCommand.of(category2Draft))
            .toCompletableFuture()
            .join();
    }

    @BeforeEach
    void setupTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        syncOptions = buildSyncOptions();
    }

    private void clearSyncTestCollections() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
        actions = new ArrayList<>();
    }

    private ProductSyncOptions buildSyncOptions() {
        final Consumer<String> warningCallBack = warningMessage -> warningCallBackMessages.add(warningMessage);

        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(this::collectErrors)
                                        .beforeUpdateCallback(this::collectActions)
                                        .warningCallback(warningCallBack)
                                        .build();
    }

    private void collectErrors(final String errorMessage, final Throwable exception) {
        errorCallBackMessages.add(errorMessage);
        errorCallBackExceptions.add(exception);
    }

    private List<UpdateAction<Product>> collectActions(@Nonnull final List<UpdateAction<Product>> actions,
                                                       @Nonnull final ProductDraft productDraft,
                                                       @Nonnull final Product product) {
        this.actions.addAll(actions);
        return actions;
    }

    @AfterAll
    static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_withNestedCategoryReferenceAsAttribute_shouldCreateProductReferencingExistingCategory() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueReferences("category-reference",
                createReferenceObject(testCategory1.getKey(), Category.referenceTypeId()));

        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();


        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(productDraftWithCategoryReference))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithCategoryReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdCategoryReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(categoryReferenceAttribute.getName());

        assertThat(createdCategoryReferenceAttribute).hasValueSatisfying(attribute -> {
            final JsonNode nestedAttributeNameField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_NAME_FIELD);
            final JsonNode nestedAttributeReferenceValueField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_VALUE_FIELD);

            assertThat(nestedAttributeNameField.asText()).isEqualTo("category-reference");
            assertThat(nestedAttributeReferenceValueField.get(REFERENCE_TYPE_ID_FIELD).asText())
                .isEqualTo(Category.referenceTypeId());
            assertThat(nestedAttributeReferenceValueField.get(REFERENCE_ID_FIELD).asText())
                .isEqualTo(testCategory1.getId());
        });
    }

    @Test
    void sync_withSameNestedCategoryReferenceAsAttribute_shouldNotSyncAnythingNew() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueReferences("category-reference",
                createReferenceObject(testCategory1.getId(), Category.referenceTypeId()));

        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraftWithCategoryReference))
                         .toCompletableFuture()
                         .join();

        final ObjectNode newNestedAttributeValue =
            createNestedAttributeValueReferences("category-reference",
                createReferenceObject(testCategory1.getKey(), Category.referenceTypeId()));

        final AttributeDraft newProductReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(newNestedAttributeValue));

        final ProductVariantDraft newMasterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newProductReferenceAttribute)
            .build();

        final ProductDraft newProductDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), newMasterVariant)
            .key("new-product")
            .build();


        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(newProductDraftWithProductReference))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 0, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithCategoryReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdCategoryReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(categoryReferenceAttribute.getName());

        assertThat(createdCategoryReferenceAttribute).hasValueSatisfying(attribute -> {
            final JsonNode nestedAttributeNameField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_NAME_FIELD);
            final JsonNode nestedAttributeValueField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_VALUE_FIELD);
            assertThat(nestedAttributeNameField.asText()).isEqualTo("category-reference");
            assertThat(nestedAttributeValueField.get(REFERENCE_TYPE_ID_FIELD).asText())
                .isEqualTo(Category.referenceTypeId());
            assertThat(nestedAttributeValueField.get(REFERENCE_ID_FIELD).asText())
                .isEqualTo(testCategory1.getId());
        });
    }

    @Test
    void sync_withChangedNestedCategoryReferenceAsAttribute_shouldUpdateProductReferencingExistingCategory() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueReferences("category-reference",
                createReferenceObject(testCategory1.getId(), Category.referenceTypeId()));

        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraftWithCategoryReference))
                         .toCompletableFuture()
                         .join();

        final ObjectNode newNestedAttributeValue =
            createNestedAttributeValueReferences("category-reference",
                createReferenceObject(testCategory2.getKey(), Category.referenceTypeId()));

        final AttributeDraft newProductReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(newNestedAttributeValue));

        final ProductVariantDraft newMasterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newProductReferenceAttribute)
            .build();

        final ProductDraft newProductDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), newMasterVariant)
            .key("new-product")
            .build();


        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(newProductDraftWithCategoryReference))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        final ObjectNode expectedNestedAttributeValue =
            createNestedAttributeValueReferences("category-reference",
                createReferenceObject(testCategory2.getId(), Category.referenceTypeId()));

        final AttributeDraft expectedCategoryReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(expectedNestedAttributeValue));
        assertThat(actions).containsExactly(SetAttribute.of(1, expectedCategoryReferenceAttribute, true));


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithCategoryReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdCategoryReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(categoryReferenceAttribute.getName());

        assertThat(createdCategoryReferenceAttribute).hasValueSatisfying(attribute -> {
            final JsonNode nestedAttributeNameField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_NAME_FIELD);
            final JsonNode nestedAttributeValueField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_VALUE_FIELD);


            assertThat(nestedAttributeNameField.asText()).isEqualTo("category-reference");
            assertThat(nestedAttributeValueField.get(REFERENCE_TYPE_ID_FIELD).asText())
                .isEqualTo(Category.referenceTypeId());
            assertThat(nestedAttributeValueField.get(REFERENCE_ID_FIELD).asText()).isEqualTo(testCategory2.getId());
        });
    }

    @Test
    void sync_withNonExistingNestedCategoryReferenceAsAttribute_ShouldFailCreatingTheProduct() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueReferences("category-reference",
                createReferenceObject("nonExistingKey", Category.referenceTypeId()));

        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(productDraftWithCategoryReference))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackExceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(error -> {
                assertThat(error).isInstanceOf(ErrorResponseException.class);
                final ErrorResponseException errorResponseException = (ErrorResponseException) error;
                assertThat(errorResponseException.getStatusCode()).isEqualTo(400);
                assertThat(error.getMessage())
                    .contains("The value '{\"typeId\":\"category\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'nestedAttribute.category-reference'");
            });
        assertThat(errorCallBackMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message)
                    .contains("The value '{\"typeId\":\"category\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'nestedAttribute.category-reference'"));
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();
    }

    @Test
    void sync_withNestedCategoryReferenceSetAsAttribute_shouldCreateProductReferencingExistingCategories() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueSetOfReferences("category-reference-set",
                createReferenceObject(testCategory1.getKey(), Category.referenceTypeId()),
                createReferenceObject(testCategory2.getKey(), Category.referenceTypeId()));

        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(productDraftWithCategoryReference))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();

        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithCategoryReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdCategoryReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(categoryReferenceAttribute.getName());

        assertThat(createdCategoryReferenceAttribute).hasValueSatisfying(attribute -> {

            final JsonNode nestedAttributeNameField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_NAME_FIELD);
            final JsonNode nestedAttributeValueField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_VALUE_FIELD);

            assertThat(nestedAttributeNameField.asText()).isEqualTo("category-reference-set");
            assertThat(nestedAttributeValueField).isInstanceOf(ArrayNode.class);
            final ArrayNode referenceSet = (ArrayNode) nestedAttributeValueField;
            assertThat(referenceSet)
                .hasSize(2)
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo(Category.referenceTypeId());
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(testCategory1.getId());
                })
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo(Category.referenceTypeId());
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(testCategory2.getId());
                });
        });
    }

    @Test
    void sync_withNestedCategoryReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueSetOfReferences("category-reference-set",
                createReferenceObject(testCategory1.getKey(), Category.referenceTypeId()),
                createReferenceObject("nonExistingKey", Category.referenceTypeId()));

        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(productDraftWithCategoryReference))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackExceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(error -> {
                assertThat(error).isInstanceOf(ErrorResponseException.class);
                final ErrorResponseException errorResponseException = (ErrorResponseException) error;
                assertThat(errorResponseException.getStatusCode()).isEqualTo(400);
                assertThat(error.getMessage())
                    .contains("The value '{\"typeId\":\"category\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'nestedAttribute.category-reference-set'");
            });
        assertThat(errorCallBackMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message)
                    .contains("The value '{\"typeId\":\"category\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'nestedAttribute.category-reference-set'"));
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();
    }

    @Test
    void sync_withSetOfNestedProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingCategories() {
        // preparation
        final ArrayNode nestedAttributeValue =
            createArrayNode(
                createNestedAttributeValueSetOfReferences("category-reference-set",
                    createReferenceObject(testCategory1.getKey(), Category.referenceTypeId()),
                    createReferenceObject(testCategory2.getKey(), Category.referenceTypeId())));

        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("setOfNestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(productDraftWithCategoryReference))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithCategoryReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdCategoryReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(categoryReferenceAttribute.getName());

        assertThat(createdCategoryReferenceAttribute).hasValueSatisfying(attribute -> {

            final JsonNode setOfNestedAttributeNameField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(0)
                .get(ATTRIBUTE_NAME_FIELD);
            final JsonNode setOfNestedAttributeValueField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(0)
                .get(ATTRIBUTE_VALUE_FIELD);

            assertThat(setOfNestedAttributeNameField.asText()).isEqualTo("category-reference-set");
            assertThat(setOfNestedAttributeValueField).isInstanceOf(ArrayNode.class);
            final ArrayNode referenceSet = (ArrayNode) setOfNestedAttributeValueField;
            assertThat(referenceSet)
                .hasSize(2)
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo(Category.referenceTypeId());
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(testCategory1.getId());
                })
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo(Category.referenceTypeId());
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(testCategory2.getId());
                });
        });
    }
}
