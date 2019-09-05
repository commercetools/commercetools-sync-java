package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_ID_FIELD;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ProductSyncWithNestedReferencedProductsIT {
    private static ProductType productType;
    private static ProductType nestedProductType;


    private ProductSyncOptions syncOptions;
    private Product product;
    private Product product2;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private List<UpdateAction<Product>> actions;

    private static final String ATTRIBUTE_NAME_FIELD = "name";
    private static final String ATTRIBUTE_VALUE_FIELD = "value";

    /**
     * Delete all product related test data from the target project. Then creates for the target CTP project
     * a product type.
     */
    @BeforeAll
    static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        nestedProductType = createProductType(PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH, CTP_TARGET_CLIENT);


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
    }

    /**
     * Deletes Products from the target CTP project, then it populates target CTP project with product test
     * data.
     */
    @BeforeEach
    void setupTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        syncOptions = buildSyncOptions();

        final ProductDraft productDraft = ProductDraftBuilder
            .of(productType, ofEnglish("foo"), ofEnglish("foo-slug"), emptyList())
            .key("foo")
            .build();

        final ProductDraft productDraft2 = ProductDraftBuilder
            .of(productType, ofEnglish("foo2"), ofEnglish("foo-slug-2"), emptyList())
            .key("foo2")
            .build();

        product = executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
        product2 = executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft2)));
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
    void sync_withNestedAttributeWithTextAttribute_shouldCreateProduct() {
        // preparation
        final ArrayNode nestedAttributeValue = JsonNodeFactory.instance.arrayNode();
        final ObjectNode nestedProductTypeAttribute = JsonNodeFactory.instance.objectNode();
        nestedAttributeValue.add(nestedProductTypeAttribute);
        nestedProductTypeAttribute.put(ATTRIBUTE_NAME_FIELD, "text-attr");
        nestedProductTypeAttribute.put(ATTRIBUTE_VALUE_FIELD, "text-attr-value");

        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("nestedAttribute", nestedAttributeValue);

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();


        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(productDraftWithProductReference))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_VALUE_FIELD).asText())
                .isEqualTo("text-attr-value");
            assertThat(attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_NAME_FIELD).asText()).isEqualTo("text-attr");
        });
    }

    @Test
    void sync_withNestedProductReferenceAsAttribute_shouldCreateProductReferencingExistingProduct() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueReferences("product-reference",
                createReferenceValue(product.getKey(), Product.referenceTypeId()));

        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();


        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(productDraftWithProductReference))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            final JsonNode nestedAttributeNameField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_NAME_FIELD);
            final JsonNode nestedAttributeReferenceValueField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_VALUE_FIELD);

            assertThat(nestedAttributeNameField.asText()).isEqualTo("product-reference");
            assertThat(nestedAttributeReferenceValueField.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
            assertThat(nestedAttributeReferenceValueField.get(REFERENCE_ID_FIELD).asText()).isEqualTo(product.getId());
        });
    }

    @Test
    void sync_withSameNestedProductReferenceAsAttribute_shouldNotSyncAnythingNew() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueReferences("product-reference",
                createReferenceValue(product.getId(), Product.referenceTypeId()));

        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraftWithProductReference))
                         .toCompletableFuture()
                         .join();

        final ObjectNode newNestedAttributeValue =
            createNestedAttributeValueReferences("product-reference",
                createReferenceValue(product.getKey(), Product.referenceTypeId()));

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
            .execute(ProductByKeyGet.of(productDraftWithProductReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            final JsonNode nestedAttributeNameField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_NAME_FIELD);
            final JsonNode nestedAttributeValueField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_VALUE_FIELD);
            assertThat(nestedAttributeNameField.asText()).isEqualTo("product-reference");
            assertThat(nestedAttributeValueField.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
            assertThat(nestedAttributeValueField.get(REFERENCE_ID_FIELD).asText()).isEqualTo(product.getId());
        });
    }

    @Test
    void sync_withChangedNestedProductReferenceAsAttribute_shouldUpdateProductReferencingExistingProduct() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueReferences("product-reference",
                createReferenceValue(product.getId(), Product.referenceTypeId()));

        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraftWithProductReference))
                         .toCompletableFuture()
                         .join();

        final ObjectNode newNestedAttributeValue =
            createNestedAttributeValueReferences("product-reference",
                createReferenceValue(product2.getKey(), Product.referenceTypeId()));

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
        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        final ObjectNode expectedNestedAttributeValue =
            createNestedAttributeValueReferences("product-reference",
                createReferenceValue(product2.getId(), Product.referenceTypeId()));

        final AttributeDraft expectedProductReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(expectedNestedAttributeValue));
        assertThat(actions).containsExactly(SetAttribute.of(1, expectedProductReferenceAttribute, true));


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            final JsonNode nestedAttributeNameField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_NAME_FIELD);
            final JsonNode nestedAttributeValueField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_VALUE_FIELD);


            assertThat(nestedAttributeNameField.asText()).isEqualTo("product-reference");
            assertThat(nestedAttributeValueField.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
            assertThat(nestedAttributeValueField.get(REFERENCE_ID_FIELD).asText()).isEqualTo(product2.getId());
        });
    }

    @Test
    void sync_withNonExistingNestedProductReferenceAsAttribute_ShouldFailCreatingTheProduct() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueReferences("product-reference",
                createReferenceValue("nonExistingKey", Product.referenceTypeId()));

        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(productDraftWithProductReference))
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
                    .contains("The value '{\"typeId\":\"product\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'nestedAttribute.product-reference'");
            });
        assertThat(errorCallBackMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message)
                    .contains("The value '{\"typeId\":\"product\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'nestedAttribute.product-reference'"));
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();
    }

    @Test
    void sync_withNestedProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingProducts() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueSetOfReferences("product-reference-set",
                createReferenceValue(product.getKey(), Product.referenceTypeId()),
                createReferenceValue(product2.getKey(), Product.referenceTypeId()));

        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(productDraftWithProductReference))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();

        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {

            final JsonNode nestedAttributeNameField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_NAME_FIELD);
            final JsonNode nestedAttributeValueField = attribute
                .getValueAsJsonNode()
                .get(0)
                .get(ATTRIBUTE_VALUE_FIELD);

            assertThat(nestedAttributeNameField.asText()).isEqualTo("product-reference-set");
            assertThat(nestedAttributeValueField).isInstanceOf(ArrayNode.class);
            final ArrayNode referenceSet = (ArrayNode) nestedAttributeValueField;
            assertThat(referenceSet)
                .hasSize(2)
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(product.getId());
                })
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(product2.getId());
                });
        });
    }

    @Test
    void sync_withNestedProductReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
        // preparation
        final ObjectNode nestedAttributeValue =
            createNestedAttributeValueSetOfReferences("product-reference-set",
                createReferenceValue(product.getKey(), Product.referenceTypeId()),
                createReferenceValue("nonExistingKey", Product.referenceTypeId()));

        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(productDraftWithProductReference))
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
                    .contains("The value '{\"typeId\":\"product\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'nestedAttribute.product-reference-set'");
            });
        assertThat(errorCallBackMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message)
                    .contains("The value '{\"typeId\":\"product\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'nestedAttribute.product-reference-set'"));
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();
    }

    @Test
    void sync_withSetOfNestedProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingProducts() {
        // preparation
        final ArrayNode nestedAttributeValue =
            createArrayNode(
                createNestedAttributeValueSetOfReferences("product-reference-set",
                    createReferenceValue(product.getKey(), Product.referenceTypeId()),
                    createReferenceValue(product2.getKey(), Product.referenceTypeId())));

        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("setOfNestedAttribute", createArrayNode(nestedAttributeValue));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(singletonList(productDraftWithProductReference))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {

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

            assertThat(setOfNestedAttributeNameField.asText()).isEqualTo("product-reference-set");
            assertThat(setOfNestedAttributeValueField).isInstanceOf(ArrayNode.class);
            final ArrayNode referenceSet = (ArrayNode) setOfNestedAttributeValueField;
            assertThat(referenceSet)
                .hasSize(2)
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(product.getId());
                })
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(product2.getId());
                });
        });
    }

    @Nonnull
    private ObjectNode createNestedAttributeValueReferences(
        @Nonnull final String attributeName,
        @Nonnull final ObjectNode referenceValue) {

        final ObjectNode referenceAttribute = JsonNodeFactory.instance.objectNode();
        referenceAttribute.put(ATTRIBUTE_NAME_FIELD, attributeName);
        referenceAttribute.set(ATTRIBUTE_VALUE_FIELD, referenceValue);

        return referenceAttribute;
    }

    @Nonnull
    private ObjectNode createNestedAttributeValueSetOfReferences(
        @Nonnull final String attributeName,
        @Nonnull final ObjectNode... referenceValues) {

        final ObjectNode setOfReferencesAttribute = JsonNodeFactory.instance.objectNode();
        setOfReferencesAttribute.put(ATTRIBUTE_NAME_FIELD, attributeName);
        setOfReferencesAttribute.set(ATTRIBUTE_VALUE_FIELD, createArrayNode(referenceValues));

        return setOfReferencesAttribute;
    }

    @Nonnull
    private ArrayNode createArrayNode(@Nonnull final ObjectNode... objectNodes) {
        final ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        asList(objectNodes).forEach(arrayNode::add);
        return arrayNode;
    }

    @Nonnull
    private ArrayNode createArrayNode(@Nonnull final ArrayNode arrayNode) {
        final ArrayNode containingArrayNode = JsonNodeFactory.instance.arrayNode();
        containingArrayNode.add(arrayNode);
        return containingArrayNode;
    }

    @Nonnull
    private ObjectNode createReferenceValue(@Nonnull final String id, @Nonnull final String typeId) {
        final ObjectNode referenceObjectNode = JsonNodeFactory.instance.objectNode();
        referenceObjectNode.put(REFERENCE_TYPE_ID_FIELD, typeId);
        referenceObjectNode.put(REFERENCE_ID_FIELD, id);
        return referenceObjectNode;
    }
}
