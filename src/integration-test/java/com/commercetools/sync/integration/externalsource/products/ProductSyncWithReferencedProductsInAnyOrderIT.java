package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteWaitingToBeResolvedCustomObjects;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.commercetools.sync.commons.models.WaitingToBeResolvedProducts;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.UnresolvedReferencesService;
import com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants;
import io.sphere.sdk.products.queries.ProductByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithReferencedProductsInAnyOrderIT {
  private static ProductType productType;

  private ProductSyncOptions syncOptions;
  private Product product;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<UpdateAction<Product>> actions;

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
    deleteWaitingToBeResolvedCustomObjects(
        CTP_TARGET_CLIENT, CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY, WaitingToBeResolvedProducts.class);
    syncOptions = buildSyncOptions();

    final ProductDraft productDraft =
        ProductDraftBuilder.of(productType, ofEnglish("foo"), ofEnglish("foo-slug"), emptyList())
            .masterVariant(ProductVariantDraftBuilder.of().key("foo").sku("foo").build())
            .key("foo")
            .build();

    product = executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
    actions = new ArrayList<>();
  }

  private ProductSyncOptions buildSyncOptions() {
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<Product>> warningCallback =
        (syncException, productDraft, product) ->
            warningCallBackMessages.add(syncException.getMessage());

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(
            (syncException, draft, product, updateActions) ->
                collectErrors(syncException.getMessage(), syncException))
        .beforeUpdateCallback(this::collectActions)
        .batchSize(1)
        .warningCallback(warningCallback)
        .build();
  }

  private void collectErrors(
      @Nullable final String errorMessage, @Nullable final Throwable exception) {
    errorCallBackMessages.add(errorMessage);
    errorCallBackExceptions.add(exception);
  }

  private List<UpdateAction<Product>> collectActions(
      @Nonnull final List<UpdateAction<Product>> actions,
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
  void sync_withSingleChildDraftSuppliedBeforeParent_shouldSyncCorrectly() {
    // preparation
    final String productReferenceAttributeName = "product-reference";
    final String parentProductKey = "parent-product-key";

    final AttributeDraft productReferenceAttribute =
        AttributeDraft.of(
            productReferenceAttributeName,
            Reference.of(Product.referenceTypeId(), parentProductKey));

    final ProductDraft childDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("foo"),
                ofEnglish("foo-slug"),
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute)
                    .build())
            .key(product.getKey())
            .build();

    final ProductDraft parentDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("bar"),
                ofEnglish("bar-slug"),
                ProductVariantDraftBuilder.of().sku("bar").key("bar").build())
            .key(parentProductKey)
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(asList(childDraft, parentDraft)).toCompletableFuture().join();

    final Product syncedParent =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(parentDraft.getKey()))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncedParent).isNotNull();
    assertThat(syncedParent.getKey()).isEqualTo(parentProductKey);
    assertThat(syncStatistics).hasValues(2, 1, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions)
        .containsExactly(
            SetAttributeInAllVariants.of(
                productReferenceAttributeName,
                createReferenceObject(syncedParent.getId(), Product.referenceTypeId()),
                true));

    final Product syncedChild =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(childDraft.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdProductReferenceAttribute =
        syncedChild
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent.getId());
            });

    assertNoWaitingDrafts(CTP_TARGET_CLIENT);
  }

  private void assertNoWaitingDrafts(@Nonnull final SphereClient client) {
    final CustomObjectQuery<WaitingToBeResolved> customObjectQuery =
        CustomObjectQuery.of(WaitingToBeResolved.class)
            .byContainer("commercetools-sync-java.UnresolvedReferencesService.productDrafts");

    final PagedQueryResult<CustomObject<WaitingToBeResolved>> queryResult =
        client.execute(customObjectQuery).toCompletableFuture().join();

    assertThat(queryResult.getResults()).isEmpty();
  }

  @Test
  void sync_withSingleChildDraftSuppliedBeforeMultipleParents_shouldSyncCorrectly() {
    // preparation
    final String productReferenceAttributeName = "product-reference-set";
    final String parentProductKey = "parent-product-key";
    final String parentProductKey2 = "parent-product-key2";

    final AttributeDraft productReferenceAttribute =
        AttributeDraft.of(
            productReferenceAttributeName,
            asSet(
                Reference.of(Product.referenceTypeId(), parentProductKey),
                Reference.of(Product.referenceTypeId(), parentProductKey2)));

    final ProductDraft childDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("foo"),
                ofEnglish("foo-slug"),
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute)
                    .build())
            .key(product.getKey())
            .build();

    final ProductDraft parentDraft1 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("bar"),
                ofEnglish("bar-slug"),
                ProductVariantDraftBuilder.of().sku("bar").key("bar").build())
            .key(parentProductKey)
            .build();

    final ProductDraft parentDraft2 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish(parentProductKey2),
                ofEnglish(parentProductKey2),
                ProductVariantDraftBuilder.of()
                    .sku(parentProductKey2)
                    .key(parentProductKey2)
                    .build())
            .key(parentProductKey2)
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(asList(childDraft, parentDraft1, parentDraft2))
            .toCompletableFuture()
            .join();

    final Product syncedParent1 =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(parentDraft1.getKey()))
            .toCompletableFuture()
            .join();

    final Product syncedParent2 =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(parentDraft2.getKey()))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncedParent1).isNotNull();
    assertThat(syncedParent2).isNotNull();
    assertThat(syncedParent1.getKey()).isEqualTo(parentProductKey);
    assertThat(syncedParent2.getKey()).isEqualTo(parentProductKey2);
    assertThat(syncStatistics).hasValues(3, 2, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final ArrayNode expectedAttributeValue = JsonNodeFactory.instance.arrayNode();
    expectedAttributeValue.add(
        createReferenceObject(syncedParent1.getId(), Product.referenceTypeId()));
    expectedAttributeValue.add(
        createReferenceObject(syncedParent2.getId(), Product.referenceTypeId()));

    assertThat(actions)
        .containsExactly(
            SetAttributeInAllVariants.of(
                productReferenceAttributeName, expectedAttributeValue, true));

    final Product syncedChild =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(childDraft.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdProductReferenceAttribute =
        syncedChild
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(
                      attribute.getValueAsJsonNode().get(0).get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(0).get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent1.getId());

              assertThat(
                      attribute.getValueAsJsonNode().get(1).get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(1).get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent2.getId());
            });

    assertNoWaitingDrafts(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withMultipleChildDraftsSuppliedBeforeParent_shouldSyncCorrectly() {
    // preparation
    final String productReferenceAttributeName = "product-reference";
    final String parentProductKey = "parent-product-key";

    final AttributeDraft productReferenceAttribute =
        AttributeDraft.of(
            productReferenceAttributeName,
            Reference.of(Product.referenceTypeId(), parentProductKey));

    final ProductDraft childDraft1 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("foo"),
                ofEnglish("foo-slug"),
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute)
                    .build())
            .key(product.getKey())
            .build();

    final ProductDraft childDraft2 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("foo-2"),
                ofEnglish("foo-slug-2"),
                ProductVariantDraftBuilder.of()
                    .key("foo-2")
                    .sku("foo-2")
                    .attributes(productReferenceAttribute)
                    .build())
            .key("foo-2")
            .build();

    final ProductDraft parentDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("bar"),
                ofEnglish("bar-slug"),
                ProductVariantDraftBuilder.of().sku("bar").key("bar").build())
            .key(parentProductKey)
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(asList(childDraft1, childDraft2, parentDraft))
            .toCompletableFuture()
            .join();

    final Product syncedParent =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(parentDraft.getKey()))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncedParent).isNotNull();
    assertThat(syncedParent.getKey()).isEqualTo(parentProductKey);
    assertThat(syncStatistics).hasValues(3, 2, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions)
        .containsExactly(
            SetAttributeInAllVariants.of(
                productReferenceAttributeName,
                createReferenceObject(syncedParent.getId(), Product.referenceTypeId()),
                true));

    final Product syncedChild1 =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(childDraft1.getKey()))
            .toCompletableFuture()
            .join();

    final Product syncedChild2 =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(childDraft2.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> syncedProductReferenceAttribute1 =
        syncedChild1
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    final Optional<Attribute> createdProductReferenceAttribute2 =
        syncedChild2
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    assertThat(syncedProductReferenceAttribute1)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent.getId());
            });

    assertThat(createdProductReferenceAttribute2)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent.getId());
            });

    assertNoWaitingDrafts(CTP_TARGET_CLIENT);
  }

  @Test
  void
      sync_withMultipleHierarchyProductReferenceAsAttribute_shouldCreateProductReferencingExistingProduct() {
    // preparation
    final String parentKey = "product-parent";
    final String parentKey1 = "product-parent-1";
    final String parentKey2 = "product-parent-2";
    final String parentKey3 = "product-parent-3";
    final String parentKey4 = "product-parent-4";
    final String productAttributeReferenceName = "product-reference";
    final String productAttributeReferenceSetName = "product-reference-set";

    final AttributeDraft productReferenceAttribute =
        AttributeDraft.of(
            productAttributeReferenceName, Reference.of(Product.referenceTypeId(), parentKey));

    final ArrayNode refSet = JsonNodeFactory.instance.arrayNode();

    final ObjectNode parentKey1RefObject =
        createReferenceObject(parentKey1, Product.referenceTypeId());
    final ObjectNode parentKey2RefObject =
        createReferenceObject(parentKey2, Product.referenceTypeId());
    final ObjectNode parentKey3RefObject =
        createReferenceObject(parentKey3, Product.referenceTypeId());
    final ObjectNode parentKey4RefObject =
        createReferenceObject(parentKey4, Product.referenceTypeId());

    refSet.add(parentKey1RefObject);
    refSet.add(parentKey2RefObject);
    refSet.add(parentKey3RefObject);
    refSet.add(parentKey4RefObject);

    final AttributeDraft productReferenceSetAttribute =
        AttributeDraft.of(productAttributeReferenceSetName, refSet);

    final ProductDraft childDraft =
        ProductDraftBuilder.of(productType, ofEnglish("foo"), ofEnglish("foo-slug"), emptyList())
            .key("foo")
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute, productReferenceSetAttribute)
                    .build())
            .build();

    final ProductDraft childDraft1 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("childDraft1"),
                ofEnglish("childDraft1"),
                ProductVariantDraftBuilder.of()
                    .key("childDraft1")
                    .sku("childDraft1")
                    .attributes(productReferenceAttribute)
                    .build())
            .key("childDraft1")
            .build();

    final ProductDraft parentDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish(parentKey),
                ofEnglish(parentKey),
                ProductVariantDraftBuilder.of().sku(parentKey).key(parentKey).build())
            .key(parentKey)
            .build();

    final ProductDraft parentDraft1 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish(parentKey1),
                ofEnglish(parentKey1),
                ProductVariantDraftBuilder.of().key(parentKey1).sku(parentKey1).build())
            .key(parentKey1)
            .build();

    final ProductDraft parentDraft2 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish(parentKey2),
                ofEnglish(parentKey2),
                ProductVariantDraftBuilder.of()
                    .key(parentKey2)
                    .sku(parentKey2)
                    .attributes(
                        AttributeDraft.of(productAttributeReferenceName, parentKey1RefObject))
                    .build())
            .key(parentKey2)
            .build();

    final ProductDraft parentDraft3 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish(parentKey3),
                ofEnglish(parentKey3),
                ProductVariantDraftBuilder.of()
                    .key(parentKey3)
                    .sku(parentKey3)
                    .attributes(
                        AttributeDraft.of(productAttributeReferenceName, parentKey2RefObject))
                    .build())
            .key(parentKey3)
            .build();

    final ProductDraft parentDraft4 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish(parentKey4),
                ofEnglish(parentKey4),
                ProductVariantDraftBuilder.of()
                    .key(parentKey4)
                    .sku(parentKey4)
                    .attributes(
                        AttributeDraft.of(productAttributeReferenceName, parentKey3RefObject))
                    .build())
            .key(parentKey4)
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(
                asList(
                    childDraft,
                    parentDraft,
                    parentDraft4,
                    childDraft1,
                    parentDraft3,
                    parentDraft2,
                    parentDraft1))
            .toCompletableFuture()
            .join();

    // assertion
    final Product syncedChild1 =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(childDraft1.getKey()))
            .toCompletableFuture()
            .join();

    final Product syncedParent =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(parentDraft.getKey()))
            .toCompletableFuture()
            .join();

    final Product syncedParent1 =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(parentDraft1.getKey()))
            .toCompletableFuture()
            .join();

    final Product syncedParent2 =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(parentDraft2.getKey()))
            .toCompletableFuture()
            .join();

    final Product syncedParent3 =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(parentDraft3.getKey()))
            .toCompletableFuture()
            .join();

    final Product syncedParent4 =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(parentDraft4.getKey()))
            .toCompletableFuture()
            .join();

    final ArrayNode expectedAttributeValue = JsonNodeFactory.instance.arrayNode();
    expectedAttributeValue.add(
        createReferenceObject(syncedParent1.getId(), Product.referenceTypeId()));
    expectedAttributeValue.add(
        createReferenceObject(syncedParent2.getId(), Product.referenceTypeId()));
    expectedAttributeValue.add(
        createReferenceObject(syncedParent3.getId(), Product.referenceTypeId()));
    expectedAttributeValue.add(
        createReferenceObject(syncedParent4.getId(), Product.referenceTypeId()));

    assertThat(syncStatistics).hasValues(7, 6, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final Product existingProduct =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(childDraft.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> existingProductReferenceSetAttribute =
        existingProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceSetAttribute.getName());

    assertThat(existingProductReferenceSetAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(
                      attribute.getValueAsJsonNode().get(0).get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(0).get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent1.getId());

              assertThat(
                      attribute.getValueAsJsonNode().get(1).get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(1).get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent2.getId());

              assertThat(
                      attribute.getValueAsJsonNode().get(2).get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(2).get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent3.getId());

              assertThat(
                      attribute.getValueAsJsonNode().get(3).get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(3).get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent4.getId());
            });

    final Optional<Attribute> existingProductReferenceAttribute =
        existingProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    assertThat(existingProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent.getId());
            });

    final Optional<Attribute> syncedProduct4Attribute =
        syncedParent4
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    assertThat(syncedProduct4Attribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent3.getId());
            });

    final Optional<Attribute> syncedProduct3Attribute =
        syncedParent3
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    assertThat(syncedProduct3Attribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent2.getId());
            });

    final Optional<Attribute> syncedProduct2Attribute =
        syncedParent2
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    assertThat(syncedProduct2Attribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent1.getId());
            });

    final Optional<Attribute> syncedChild1Attribute =
        syncedChild1
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    assertThat(syncedChild1Attribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(Product.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(syncedParent.getId());
            });

    assertNoWaitingDrafts(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withMissingParent_shouldSyncCorrectly() {
    // preparation
    final String productReferenceAttributeName = "product-reference";
    final String parentProductKey = "parent-product-key";
    final String parentProductKey2 = "parent-product-key2";

    final AttributeDraft productReferenceAttribute =
        AttributeDraft.of(
            productReferenceAttributeName,
            asSet(
                Reference.of(Product.referenceTypeId(), parentProductKey),
                Reference.of(Product.referenceTypeId(), parentProductKey2)));

    final ProductDraft childDraft1 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("foo"),
                ofEnglish("foo-slug"),
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute)
                    .build())
            .key(product.getKey())
            .build();

    final ProductDraft childDraft2 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("foo-2"),
                ofEnglish("foo-slug-2"),
                ProductVariantDraftBuilder.of()
                    .key("foo-2")
                    .sku("foo-2")
                    .attributes(productReferenceAttribute)
                    .build())
            .key("foo-2")
            .build();

    final ProductDraft parentDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish(parentProductKey2),
                ofEnglish(parentProductKey2),
                ProductVariantDraftBuilder.of()
                    .sku(parentProductKey2)
                    .key(parentProductKey2)
                    .build())
            .key(parentProductKey2)
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(asList(childDraft1, childDraft2, parentDraft))
            .toCompletableFuture()
            .join();

    final Product syncedParent =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(parentProductKey))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncedParent).isNull();
    assertThat(syncStatistics).hasValues(3, 1, 0, 0, 2);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();

    final UnresolvedReferencesService<WaitingToBeResolvedProducts> unresolvedReferencesService =
        new UnresolvedReferencesServiceImpl(syncOptions);

    final Set<WaitingToBeResolvedProducts> waitingDrafts =
        unresolvedReferencesService
            .fetch(
                asSet(childDraft1.getKey(), childDraft2.getKey()),
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    assertThat(waitingDrafts)
        .containsExactlyInAnyOrder(
            new WaitingToBeResolvedProducts(childDraft1, singleton(parentProductKey)),
            new WaitingToBeResolvedProducts(childDraft2, singleton(parentProductKey)));
  }

  @SuppressWarnings("unchecked")
  @Test
  void sync_withFailToFetchCustomObject_shouldSyncCorrectly() {
    // preparation
    final String productReferenceAttributeName = "product-reference";
    final String parentProductKey = "parent-product-key";

    final AttributeDraft productReferenceAttribute =
        AttributeDraft.of(
            productReferenceAttributeName,
            Reference.of(Product.referenceTypeId(), parentProductKey));

    final ProductDraft childDraft1 =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("foo"),
                ofEnglish("foo-slug"),
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute)
                    .build())
            .key(product.getKey())
            .build();

    final ProductDraft parentDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish(parentProductKey),
                ofEnglish(parentProductKey),
                ProductVariantDraftBuilder.of().sku(parentProductKey).key(parentProductKey).build())
            .key(parentProductKey)
            .build();

    final SphereClient ctpClient = spy(CTP_TARGET_CLIENT);

    final BadGatewayException gatewayException = new BadGatewayException("failed to respond.");
    when(ctpClient.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFutureUtils.failed(gatewayException))
        .thenCallRealMethod();

    syncOptions =
        ProductSyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (syncException, draft, product, updateActions) ->
                    collectErrors(syncException.getMessage(), syncException))
            .beforeUpdateCallback(this::collectActions)
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);

    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(childDraft1))
            .thenCompose(ignoredResult -> productSync.sync(singletonList(parentDraft)))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(2, 1, 0, 1, 0);
    assertThat(errorCallBackMessages)
        .containsExactly("Failed to fetch ProductDrafts waiting to be resolved with keys '[foo]'.");
    assertThat(errorCallBackExceptions)
        .hasOnlyOneElementSatisfying(
            exception ->
                assertThat(exception.getCause())
                    .hasCauseExactlyInstanceOf(BadGatewayException.class));
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();

    final UnresolvedReferencesService<WaitingToBeResolvedProducts> unresolvedReferencesService =
        new UnresolvedReferencesServiceImpl(syncOptions);

    final Set<WaitingToBeResolvedProducts> waitingDrafts =
        unresolvedReferencesService
            .fetch(
                asSet(childDraft1.getKey()),
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    assertThat(waitingDrafts)
        .containsExactly(new WaitingToBeResolvedProducts(childDraft1, singleton(parentProductKey)));
  }
}
