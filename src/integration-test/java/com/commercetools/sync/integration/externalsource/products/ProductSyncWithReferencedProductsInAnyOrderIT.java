package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteWaitingToBeResolvedCustomObjects;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createBadGatewayException;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceTypeId;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectPagedQueryResponse;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.sync.commons.exceptions.SyncException;
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
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.*;

class ProductSyncWithReferencedProductsInAnyOrderIT {
  private static ProductType productType;

  private ProductSyncOptions syncOptions;
  private Product product;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<ProductUpdateAction> actions;

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
    syncOptions = buildSyncOptions();

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("foo-slug"))
            .masterVariant(ProductVariantDraftBuilder.of().key("foo").sku("foo").build())
            .key("foo")
            .build();

    product =
        CTP_TARGET_CLIENT
            .products()
            .create(productDraft)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
    actions = new ArrayList<>();
  }

  private ProductSyncOptions buildSyncOptions() {
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallback =
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

  private List<ProductUpdateAction> collectActions(
      @Nonnull final List<ProductUpdateAction> actions,
      @Nonnull final ProductDraft productDraft,
      @Nonnull final ProductProjection product) {
    this.actions.addAll(actions);
    return actions;
  }

  @AfterEach
  void clear() {
    deleteWaitingToBeResolvedCustomObjects(
        CTP_TARGET_CLIENT, "commercetools-sync-java.UnresolvedReferencesService.productDrafts");
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

    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name(productReferenceAttributeName)
            .value(ProductReferenceBuilder.of().id(parentProductKey).build())
            .build();

    final ProductDraft childDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("foo-slug"))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute)
                    .build())
            .key(product.getKey())
            .build();

    final ProductDraft parentDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("bar"))
            .slug(ofEnglish("bar-slug"))
            .masterVariant(ProductVariantDraftBuilder.of().sku("bar").key("bar").build())
            .key(parentProductKey)
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(asList(childDraft, parentDraft)).toCompletableFuture().join();

    final Product syncedParent =
        CTP_TARGET_CLIENT
            .products()
            .withKey(parentDraft.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    // assertion
    assertThat(syncedParent).isNotNull();
    assertThat(syncedParent.getKey()).isEqualTo(parentProductKey);
    assertThat(syncStatistics).hasValues(2, 1, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions)
        .containsExactly(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name(productReferenceAttributeName)
                .value(createReferenceObjectNode(syncedParent.getId(), ProductReference.PRODUCT))
                .staged(true)
                .build());

    final Product syncedChild =
        CTP_TARGET_CLIENT
            .products()
            .withKey(childDraft.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdProductReferenceAttribute =
        syncedChild
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
              assertThat(reference.getId()).isEqualTo(syncedParent.getId());
            });

    assertNoWaitingDrafts(CTP_TARGET_CLIENT);
  }

  private void assertNoWaitingDrafts(@Nonnull final ProjectApiRoot client) {
    final List<CustomObject> response =
        client
            .customObjects()
            .withContainer("commercetools-sync-java.UnresolvedReferencesService.productDrafts")
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CustomObjectPagedQueryResponse::getResults)
            .toCompletableFuture()
            .join();

    assertThat(response).isEmpty();
  }

  @Test
  void sync_withSingleChildDraftSuppliedBeforeMultipleParents_shouldSyncCorrectly() {
    // preparation
    final String productReferenceAttributeName = "product-reference-set";
    final String parentProductKey = "parent-product-key";
    final String parentProductKey2 = "parent-product-key2";

    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name(productReferenceAttributeName)
            .value(
                List.of(
                    ProductReferenceBuilder.of().id(parentProductKey).build(),
                    ProductReferenceBuilder.of().id(parentProductKey2).build()))
            .build();

    final ProductDraft childDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("foo-slug"))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute)
                    .build())
            .key("foo")
            .build();

    final ProductDraft parentDraft1 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("bar"))
            .slug(ofEnglish("bar-slug"))
            .masterVariant(ProductVariantDraftBuilder.of().sku("bar").key("bar").build())
            .key(parentProductKey)
            .build();

    final ProductDraft parentDraft2 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(parentProductKey2))
            .slug(ofEnglish(parentProductKey2))
            .masterVariant(
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
            .sync(List.of(childDraft, parentDraft1, parentDraft2))
            .toCompletableFuture()
            .join();

    final Product syncedParent1 =
        CTP_TARGET_CLIENT
            .products()
            .withKey(parentDraft1.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Product syncedParent2 =
        CTP_TARGET_CLIENT
            .products()
            .withKey(parentDraft2.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

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
        createReferenceObjectNode(syncedParent1.getId(), ProductReference.PRODUCT));
    expectedAttributeValue.add(
        createReferenceObjectNode(syncedParent2.getId(), ProductReference.PRODUCT));

    assertThat(actions)
        .containsExactly(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name(productReferenceAttributeName)
                .value(expectedAttributeValue)
                .staged(true)
                .build());

    final Product syncedChild =
        CTP_TARGET_CLIENT
            .products()
            .withKey(childDraft.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdProductReferenceAttribute =
        syncedChild
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Reference> referenceSet = AttributeAccessor.asSetReference(attribute);
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(syncedParent1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(syncedParent2.getId());
                      });
            });

    assertNoWaitingDrafts(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withMultipleChildDraftsSuppliedBeforeParent_shouldSyncCorrectly() {
    // preparation
    final String productReferenceAttributeName = "product-reference";
    final String parentProductKey = "parent-product-key";

    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name(productReferenceAttributeName)
            .value(ProductReferenceBuilder.of().id(parentProductKey).build())
            .build();

    final ProductDraft childDraft1 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("foo-slug"))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute)
                    .build())
            .key(product.getKey())
            .build();

    final ProductDraft childDraft2 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo-2"))
            .slug(ofEnglish("foo-2"))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .key("foo-2")
                    .sku("foo-2")
                    .attributes(productReferenceAttribute)
                    .build())
            .key("foo-2")
            .build();

    final ProductDraft parentDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("bar"))
            .slug(ofEnglish("bar-slug"))
            .masterVariant(ProductVariantDraftBuilder.of().sku("bar").key("bar").build())
            .key(parentProductKey)
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(List.of(childDraft1, childDraft2, parentDraft))
            .toCompletableFuture()
            .join();

    final Product syncedParent =
        CTP_TARGET_CLIENT
            .products()
            .withKey(parentDraft.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    // assertion
    assertThat(syncedParent).isNotNull();
    assertThat(syncedParent.getKey()).isEqualTo(parentProductKey);
    assertThat(syncStatistics).hasValues(3, 2, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions)
        .containsExactly(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name(productReferenceAttributeName)
                .value(createReferenceObjectNode(syncedParent.getId(), ProductReference.PRODUCT))
                .staged(true)
                .build());

    final Product syncedChild1 =
        CTP_TARGET_CLIENT
            .products()
            .withKey(childDraft1.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Product syncedChild2 =
        CTP_TARGET_CLIENT
            .products()
            .withKey(childDraft2.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

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
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
              assertThat(reference.getId()).isEqualTo(syncedParent.getId());
            });

    assertThat(createdProductReferenceAttribute2)
        .hasValueSatisfying(
            attribute -> {
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
              assertThat(reference.getId()).isEqualTo(syncedParent.getId());
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

    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name(productAttributeReferenceName)
            .value(ProductReferenceBuilder.of().id(parentKey).build())
            .build();

    final Reference parentKey1RefObject =
        createReferenceObject(parentKey1, ProductReference.PRODUCT);
    final Reference parentKey2RefObject =
        createReferenceObject(parentKey2, ProductReference.PRODUCT);
    final Reference parentKey3RefObject =
        createReferenceObject(parentKey3, ProductReference.PRODUCT);
    final Reference parentKey4RefObject =
        createReferenceObject(parentKey4, ProductReference.PRODUCT);

    final List<Reference> referenceList =
        List.of(parentKey1RefObject, parentKey2RefObject, parentKey3RefObject, parentKey4RefObject);

    final Attribute productReferenceSetAttribute =
        AttributeBuilder.of().name(productAttributeReferenceSetName).value(referenceList).build();

    final ProductDraft childDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("foo-slug"))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute, productReferenceSetAttribute)
                    .build())
            .key("foo")
            .build();

    final ProductDraft childDraft1 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("childDraft1"))
            .slug(ofEnglish("childDraft1"))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .key("childDraft1")
                    .sku("childDraft1")
                    .attributes(productReferenceAttribute)
                    .build())
            .key("childDraft1")
            .build();

    final ProductDraft parentDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(parentKey))
            .slug(ofEnglish(parentKey))
            .masterVariant(ProductVariantDraftBuilder.of().sku(parentKey).key(parentKey).build())
            .key(parentKey)
            .build();

    final ProductDraft parentDraft1 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(parentKey1))
            .slug(ofEnglish(parentKey1))
            .masterVariant(ProductVariantDraftBuilder.of().sku(parentKey1).key(parentKey1).build())
            .key(parentKey1)
            .build();

    final ProductDraft parentDraft2 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(parentKey2))
            .slug(ofEnglish(parentKey2))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .sku(parentKey2)
                    .key(parentKey2)
                    .attributes(
                        AttributeBuilder.of()
                            .name(productAttributeReferenceName)
                            .value(parentKey1RefObject)
                            .build())
                    .build())
            .key(parentKey2)
            .build();

    final ProductDraft parentDraft3 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(parentKey3))
            .slug(ofEnglish(parentKey3))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .sku(parentKey3)
                    .key(parentKey3)
                    .attributes(
                        AttributeBuilder.of()
                            .name(productAttributeReferenceName)
                            .value(parentKey2RefObject)
                            .build())
                    .build())
            .key(parentKey3)
            .build();

    final ProductDraft parentDraft4 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(parentKey4))
            .slug(ofEnglish(parentKey4))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .sku(parentKey4)
                    .key(parentKey4)
                    .attributes(
                        AttributeBuilder.of()
                            .name(productAttributeReferenceName)
                            .value(parentKey3RefObject)
                            .build())
                    .build())
            .key(parentKey4)
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(
                List.of(
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
            .products()
            .withKey(childDraft1.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Product syncedParent =
        CTP_TARGET_CLIENT
            .products()
            .withKey(parentDraft.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Product syncedParent1 =
        CTP_TARGET_CLIENT
            .products()
            .withKey(parentDraft1.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Product syncedParent2 =
        CTP_TARGET_CLIENT
            .products()
            .withKey(parentDraft2.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Product syncedParent3 =
        CTP_TARGET_CLIENT
            .products()
            .withKey(parentDraft3.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Product syncedParent4 =
        CTP_TARGET_CLIENT
            .products()
            .withKey(parentDraft4.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(syncStatistics).hasValues(7, 6, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final Product existingProduct =
        CTP_TARGET_CLIENT
            .products()
            .withKey(childDraft.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> existingProductReferenceSetAttribute =
        existingProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceSetAttribute.getName());

    assertThat(existingProductReferenceSetAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              final List<Reference> referenceSet = AttributeAccessor.asSetReference(attribute);
              assertThat(referenceSet)
                  .hasSize(4)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(syncedParent1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(syncedParent2.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(syncedParent3.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(syncedParent4.getId());
                      });
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
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
              assertThat(reference.getId()).isEqualTo(syncedParent.getId());
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
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
              assertThat(reference.getId()).isEqualTo(syncedParent3.getId());
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
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
              assertThat(reference.getId()).isEqualTo(syncedParent2.getId());
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
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
              assertThat(reference.getId()).isEqualTo(syncedParent1.getId());
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
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
              assertThat(reference.getId()).isEqualTo(syncedParent.getId());
            });

    assertNoWaitingDrafts(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withMissingParent_shouldSyncCorrectly() {
    // preparation
    final String productReferenceAttributeName = "product-reference";
    final String parentProductKey = "parent-product-key";
    final String parentProductKey2 = "parent-product-key2";

    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name(productReferenceAttributeName)
            .value(
                List.of(
                    ProductReferenceBuilder.of().id(parentProductKey).build(),
                    ProductReferenceBuilder.of().id(parentProductKey2).build()))
            .build();

    final ProductDraft childDraft1 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("foo-slug"))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute)
                    .build())
            .key("foo")
            .build();

    final ProductDraft childDraft2 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo-2"))
            .slug(ofEnglish("foo-slug-2"))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .key("foo-2")
                    .sku("foo-2")
                    .attributes(productReferenceAttribute)
                    .build())
            .key("foo-2")
            .build();

    final ProductDraft parentDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(parentProductKey2))
            .slug(ofEnglish(parentProductKey2))
            .masterVariant(
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
            .sync(List.of(childDraft1, childDraft2, parentDraft))
            .toCompletableFuture()
            .join();

    final ProductProjectionPagedQueryResponse syncedParent =
        CTP_TARGET_CLIENT
            .productProjections()
            .get()
            .withWhere("key=\"" + parentProductKey + "\"")
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    // assertion
    assertThat(syncedParent.getResults()).isEmpty();
    assertThat(syncStatistics).hasValues(3, 1, 0, 0, 2);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();

    final UnresolvedReferencesService<WaitingToBeResolvedProducts> unresolvedReferencesService =
        new UnresolvedReferencesServiceImpl<>(syncOptions);

    final Set<WaitingToBeResolvedProducts> waitingDrafts =
        unresolvedReferencesService
            .fetch(
                Set.of(childDraft1.getKey(), childDraft2.getKey()),
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

    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name(productReferenceAttributeName)
            .value(ProductReferenceBuilder.of().id(parentProductKey).build())
            .build();

    final ProductDraft childDraft1 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("foo-slug"))
            .masterVariant(
                ProductVariantDraftBuilder.of()
                    .key("foo")
                    .sku("foo")
                    .attributes(productReferenceAttribute)
                    .build())
            .key("foo")
            .build();

    final ProductDraft parentDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(parentProductKey))
            .slug(ofEnglish(parentProductKey))
            .masterVariant(
                ProductVariantDraftBuilder.of().sku(parentProductKey).key(parentProductKey).build())
            .key(parentProductKey)
            .build();

    final AtomicInteger requestInvocationCounter = new AtomicInteger(0);
    final ProjectApiRoot spyClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("custom-object") && method == ApiHttpMethod.GET) {
                    if (requestInvocationCounter.getAndIncrement() == 0) {
                      return CompletableFutureUtils.exceptionallyCompletedFuture(
                          createBadGatewayException());
                    }
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());

    syncOptions =
        ProductSyncOptionsBuilder.of(spyClient)
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
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(CompletionException.class)
        .hasRootCauseExactlyInstanceOf(BadGatewayException.class);
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();

    final UnresolvedReferencesService<WaitingToBeResolvedProducts> unresolvedReferencesService =
        new UnresolvedReferencesServiceImpl<>(syncOptions);

    final Set<WaitingToBeResolvedProducts> waitingDrafts =
        unresolvedReferencesService
            .fetch(
                Set.of(childDraft1.getKey()),
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    assertThat(waitingDrafts)
        .containsExactly(new WaitingToBeResolvedProducts(childDraft1, singleton(parentProductKey)));
  }

  @Nonnull
  private static ObjectNode createReferenceObjectNode(
      @Nonnull final String id, @Nonnull final String typeId) {
    final ObjectNode reference = JsonNodeFactory.instance.objectNode();
    reference.put(REFERENCE_TYPE_ID_FIELD, typeId);
    reference.put(REFERENCE_ID_FIELD, id);
    return reference;
  }
}
