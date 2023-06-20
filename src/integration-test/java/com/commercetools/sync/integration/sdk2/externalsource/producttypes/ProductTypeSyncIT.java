package com.commercetools.sync.integration.sdk2.externalsource.producttypes;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductTypeITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionBuilder;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValueBuilder;
import com.commercetools.api.models.product_type.AttributePlainEnumValueBuilder;
import com.commercetools.api.models.product_type.AttributeReferenceTypeId;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeChangeAttributeOrderByNameActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.api.models.product_type.TextInputHint;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSync;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.sdk2.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.sdk2.producttypes.helpers.ResourceToDraftConverters;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductTypeSyncIT {

  private AtomicInteger concurrentModificationCounter;

  /**
   * Deletes product types from the target CTP project. Populates target CTP project with test data.
   */
  @BeforeEach
  void setup() {
    deleteProductTypes(CTP_TARGET_CLIENT);
    try {
      // The removal of the attributes is eventually consistent.
      // Here with one second break we are slowing down the ITs a little bit so CTP could remove the
      // attributes.
      // see: SUPPORT-8408
      Thread.sleep(1000);
    } catch (InterruptedException expected) {
    }
    populateTargetProject();
  }

  /**
   * Deletes all the test data from the {@code CTP_TARGET_CLIENT} project that were set up in this
   * test class.
   */
  @AfterAll
  static void tearDown() {
    deleteProductTypes(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_WithUpdatedProductType_ShouldUpdateProductType() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_2)
            .description(PRODUCT_TYPE_DESCRIPTION_2)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1)
            .build();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertion
    assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);

    final Optional<ProductType> oldProductTypeAfter =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

    assertThat(oldProductTypeAfter)
        .hasValueSatisfying(
            productType -> {
              assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_2);
              assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_2);
              assertAttributesAreEqual(
                  productType.getAttributes(), singletonList(ATTRIBUTE_DEFINITION_DRAFT_1));
            });
  }

  @Test
  void sync_WithNewProductType_ShouldCreateProductType() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_2)
            .name(PRODUCT_TYPE_NAME_2)
            .description(PRODUCT_TYPE_DESCRIPTION_2)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1)
            .build();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // tests
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(productTypeSyncStatistics).hasValues(1, 1, 0, 0, 0);

    final Optional<ProductType> oldProductTypeAfter =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_2);

    assertThat(oldProductTypeAfter)
        .hasValueSatisfying(
            productType -> {
              assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_2);
              assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_2);
              assertAttributesAreEqual(
                  productType.getAttributes(), singletonList(ATTRIBUTE_DEFINITION_DRAFT_1));
            });
  }

  @Test
  void sync_WithUpdatedProductType_WithNewAttribute_ShouldUpdateProductTypeAddingAttribute() {
    // preparation
    // Adding ATTRIBUTE_DEFINITION_DRAFT_3
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(
                ATTRIBUTE_DEFINITION_DRAFT_1,
                ATTRIBUTE_DEFINITION_DRAFT_2,
                ATTRIBUTE_DEFINITION_DRAFT_3)
            .build();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // tests
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);

    final Optional<ProductType> oldProductTypeAfter =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

    assertThat(oldProductTypeAfter)
        .hasValueSatisfying(
            productType ->
                assertAttributesAreEqual(
                    productType.getAttributes(),
                    asList(
                        ATTRIBUTE_DEFINITION_DRAFT_1,
                        ATTRIBUTE_DEFINITION_DRAFT_2,
                        ATTRIBUTE_DEFINITION_DRAFT_3)));
  }

  @Test
  void sync_WithUpdatedProductType_WithoutOldAttribute_ShouldUpdateProductTypeRemovingAttribute() {
    // Removing ATTRIBUTE_DEFINITION_DRAFT_2
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1)
            .build();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);

    final Optional<ProductType> oldProductTypeAfter =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

    assertThat(oldProductTypeAfter)
        .hasValueSatisfying(
            productType ->
                assertAttributesAreEqual(
                    productType.getAttributes(), singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)));
  }

  @Test
  void
      sync_WithUpdatedProductType_ChangingAttributeOrder_ShouldUpdateProductTypeChangingAttributeOrder() {
    // Changing order from ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2 to
    // ATTRIBUTE_DEFINITION_DRAFT_2, ATTRIBUTE_DEFINITION_DRAFT_1
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_2, ATTRIBUTE_DEFINITION_DRAFT_1)
            .build();

    final ArrayList<ProductTypeUpdateAction> builtUpdateActions = new ArrayList<>();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .beforeUpdateCallback(
                (actions, draft, oldProductType) -> {
                  builtUpdateActions.addAll(actions);
                  return actions;
                })
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);

    final Optional<ProductType> oldProductTypeAfter =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

    assertThat(oldProductTypeAfter)
        .hasValueSatisfying(
            productType ->
                assertAttributesAreEqual(
                    productType.getAttributes(),
                    asList(ATTRIBUTE_DEFINITION_DRAFT_2, ATTRIBUTE_DEFINITION_DRAFT_1)));

    assertThat(builtUpdateActions)
        .containsExactly(
            ProductTypeChangeAttributeOrderByNameActionBuilder.of()
                .attributeNames(
                    ATTRIBUTE_DEFINITION_DRAFT_2.getName(), ATTRIBUTE_DEFINITION_DRAFT_1.getName())
                .build());
  }

  @Test
  void sync_WithUpdatedAttributeDefinition_ShouldUpdateProductTypeUpdatingAttribute() {
    // Updating ATTRIBUTE_DEFINITION_1 (name = "attr_name_1") changing the label, attribute
    // constraint, input tip,
    // input hint, isSearchable fields.
    final AttributeDefinitionDraft attributeDefinitionDraftUpdated =
        AttributeDefinitionDraftBuilder.of()
            .type(attributeTypeBuilder -> attributeTypeBuilder.textBuilder())
            .name("attr_name_1")
            .label(ofEnglish("attr_label_updated"))
            .isRequired(true)
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputTip(ofEnglish("inputTip_updated"))
            .inputHint(TextInputHint.TextInputHintEnum.MULTI_LINE)
            .isSearchable(false)
            .build();

    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(attributeDefinitionDraftUpdated)
            .build();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);

    final Optional<ProductType> oldProductTypeAfter =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

    assertThat(oldProductTypeAfter)
        .hasValueSatisfying(
            productType ->
                assertAttributesAreEqual(
                    productType.getAttributes(), singletonList(attributeDefinitionDraftUpdated)));
  }

  @Test
  void sync_WithoutKey_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // Draft without key throws an error
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_1)
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    final String expectedErrorMessage =
        format(
            "ProductTypeDraft with name: %s doesn't have a key. "
                + "Please make sure all productType drafts have keys.",
            newProductTypeDraft.getName());
    // assertions
    assertThat(errorMessages).hasSize(1).singleElement(as(STRING)).isEqualTo(expectedErrorMessage);

    assertThat(exceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            throwable -> {
              assertThat(throwable).isInstanceOf(SyncException.class);
              assertThat(throwable.getMessage()).isEqualTo(expectedErrorMessage);
              return true;
            });

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
  }

  @Test
  void sync_WithNullDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final ProductTypeDraft newProductTypeDraft = null;
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo("ProductTypeDraft is null.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            throwable -> {
              assertThat(throwable).isInstanceOf(SyncException.class);
              assertThat(throwable.getMessage()).isEqualTo("ProductTypeDraft is null.");
              return true;
            });

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
  }

  @Test
  void sync_WithErrorCreatingTheProductType_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation

    // Invalid attribute definition due to having the same name as an already existing one but
    // different
    // type.
    final AttributeDefinitionDraft invalidAttrDefinition =
        AttributeDefinitionDraftBuilder.of(ATTRIBUTE_DEFINITION_DRAFT_1)
            .type(AttributeTypeBuilder::moneyBuilder)
            .attributeConstraint(AttributeConstraintEnum.COMBINATION_UNIQUE)
            .build();

    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_2)
            .name(PRODUCT_TYPE_NAME_2)
            .description(PRODUCT_TYPE_DESCRIPTION_2)
            .attributes(invalidAttrDefinition)
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to create draft with key: 'key_2'.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            throwable -> {
              assertThat(throwable).hasCauseExactlyInstanceOf(CompletionException.class);
              assertThat(throwable.getCause()).hasCauseExactlyInstanceOf(BadRequestException.class);
              assertThat(throwable).hasMessageContaining("AttributeDefinitionTypeConflict");
              return true;
            });

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
  }

  @Test
  void sync_WithErrorUpdatingTheProductType_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation

    // Invalid attribute definition due to having an invalid name.
    final AttributeDefinitionDraft invalidAttrDefinition =
        AttributeDefinitionDraftBuilder.of()
            .type(AttributeTypeBuilder::moneyBuilder)
            .name("*invalidName*")
            .label(ofEnglish("description"))
            .isRequired(true)
            .isSearchable(false)
            .build();

    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(invalidAttrDefinition)
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to update product type with key: 'key_1'.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            throwable -> {
              assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
              assertThat(throwable).hasCauseExactlyInstanceOf(BadRequestException.class);
              assertThat(throwable).hasMessageContaining("InvalidInput");
              return true;
            });

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
  }

  @Test
  void
      syncDrafts_WithConcurrentModificationException_ShouldRetryToUpdateNewProductTypeWithSuccess() {
    // Preparation
    final ProjectApiRoot spyClient = buildClientWithConcurrentModificationUpdate();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("key")
            .name("foo")
            .description("description")
            .attributes(emptyList())
            .build();

    CTP_TARGET_CLIENT.productTypes().create(productTypeDraft).executeBlocking();

    final String newProductTypeName = "bar";
    final ProductTypeDraft updatedDraft =
        ProductTypeDraftBuilder.of(productTypeDraft).name(newProductTypeName).build();

    final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder.of(spyClient).build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

    // Test
    final ProductTypeSyncStatistics statistics =
        productTypeSync.sync(singletonList(updatedDraft)).toCompletableFuture().join();

    // Assertion
    assertThat(statistics).hasValues(1, 0, 1, 0, 0);
    assertThat(concurrentModificationCounter.get()).isEqualTo(1);
  }

  @Test
  void syncDrafts_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // Preparation
    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("key")
            .name("foo")
            .description("description")
            .attributes(emptyList())
            .build();

    CTP_TARGET_CLIENT.productTypes().create(productTypeDraft).executeBlocking();

    final String newProductTypeName = "bar";
    final ProductTypeDraft updatedDraft =
        ProductTypeDraftBuilder.of(productTypeDraft).name(newProductTypeName).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> errors = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errors.add(exception.getCause());
                })
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

    // Test
    final ProductTypeSyncStatistics statistics =
        productTypeSync.sync(singletonList(updatedDraft)).toCompletableFuture().join();

    // Assertion
    assertThat(statistics).hasValues(1, 0, 0, 1, 0);

    assertThat(errorMessages).hasSize(1);
    assertThat(errors).hasSize(1);

    assertThat(errors.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
    assertThat(errorMessages.get(0))
        .contains(
            format(
                "Failed to update product type with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                    + "after concurrency modification.",
                productTypeDraft.getKey()));
  }

  @Test
  void
      syncDrafts_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // Preparation
    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("key")
            .name("foo")
            .description("description")
            .attributes(emptyList())
            .build();

    CTP_TARGET_CLIENT
        .productTypes()
        .create(productTypeDraft)
        .execute()
        .toCompletableFuture()
        .join();

    final String newProductTypeName = "bar";
    final ProductTypeDraft updatedDraft =
        ProductTypeDraftBuilder.of(productTypeDraft).name(newProductTypeName).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> errors = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errors.add(exception.getCause());
                })
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

    // Test
    final ProductTypeSyncStatistics statistics =
        productTypeSync.sync(singletonList(updatedDraft)).toCompletableFuture().join();

    // Assertion
    assertThat(statistics).hasValues(1, 0, 0, 1, 0);

    assertThat(errorMessages).hasSize(1);
    assertThat(errors).hasSize(1);
    assertThat(errorMessages.get(0))
        .contains(
            format(
                "Failed to update product type with key: '%s'. Reason: Not found when attempting to fetch while "
                    + "retrying after concurrency modification.",
                productTypeDraft.getKey()));
  }

  @Test
  void sync_WithSeveralBatches_ShouldReturnProperStatistics() {
    // Default batch size is 50 (check ProductTypeSyncOptionsBuilder) so we have 2 batches of 50
    final List<ProductTypeDraft> productTypeDrafts =
        IntStream.range(0, 100)
            .mapToObj(
                i ->
                    ProductTypeDraftBuilder.of()
                        .key("product_type_key_" + i)
                        .name("product_type_name_" + i)
                        .description("product_type_description_" + i)
                        .attributes(ATTRIBUTE_DEFINITION_DRAFT_1)
                        .build())
            .collect(Collectors.toList());

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(productTypeDrafts).toCompletableFuture().join();

    assertThat(productTypeSyncStatistics).hasValues(100, 100, 0, 0, 0);
  }

  @Test
  void sync_WithSetOfEnumsAndSetOfLenumsChanges_ShouldUpdateProductType() {
    // preparation
    final AttributeDefinitionDraft withSetOfEnumsOld =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .enumBuilder()
                                    .values(
                                        AttributePlainEnumValueBuilder.of()
                                            .key("d")
                                            .label("d")
                                            .build(),
                                        AttributePlainEnumValueBuilder.of()
                                            .key("b")
                                            .label("newB")
                                            .build(),
                                        AttributePlainEnumValueBuilder.of()
                                            .key("a")
                                            .label("a")
                                            .build(),
                                        AttributePlainEnumValueBuilder.of()
                                            .key("c")
                                            .label("c")
                                            .build())))
            .name("foo")
            .label(ofEnglish("foo"))
            .isRequired(false)
            .build();

    final AttributeDefinitionDraft withSetOfSetOfLEnumsOld =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .lenumBuilder()
                                    .values(
                                        AttributeLocalizedEnumValueBuilder.of()
                                            .key("d")
                                            .label(ofEnglish("d"))
                                            .build(),
                                        AttributeLocalizedEnumValueBuilder.of()
                                            .key("b")
                                            .label(ofEnglish("newB"))
                                            .build(),
                                        AttributeLocalizedEnumValueBuilder.of()
                                            .key("a")
                                            .label(ofEnglish("a"))
                                            .build(),
                                        AttributeLocalizedEnumValueBuilder.of()
                                            .key("c")
                                            .label(ofEnglish("c"))
                                            .build())))
            .name("bar")
            .label(ofEnglish("bar"))
            .isRequired(false)
            .build();

    final ProductTypeDraft oldDraft =
        ProductTypeDraftBuilder.of()
            .key("withSetOfEnums")
            .name("withSetOfEnums")
            .description("withSetOfEnums")
            .attributes(withSetOfEnumsOld, withSetOfSetOfLEnumsOld)
            .build();

    CTP_TARGET_CLIENT.productTypes().create(oldDraft).executeBlocking();

    final AttributeDefinitionDraft withSetOfEnumsNew =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .enumBuilder()
                                    .values(
                                        AttributePlainEnumValueBuilder.of()
                                            .key("a")
                                            .label("a")
                                            .build(),
                                        AttributePlainEnumValueBuilder.of()
                                            .key("b")
                                            .label("b")
                                            .build(),
                                        AttributePlainEnumValueBuilder.of()
                                            .key("c")
                                            .label("c")
                                            .build())))
            .name("foo")
            .label(ofEnglish("foo"))
            .isRequired(false)
            .build();

    final AttributeDefinitionDraft withSetOfSetOfLEnumsNew =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .lenumBuilder()
                                    .values(
                                        AttributeLocalizedEnumValueBuilder.of()
                                            .key("a")
                                            .label(ofEnglish("a"))
                                            .build(),
                                        AttributeLocalizedEnumValueBuilder.of()
                                            .key("b")
                                            .label(ofEnglish("newB"))
                                            .build(),
                                        AttributeLocalizedEnumValueBuilder.of()
                                            .key("c")
                                            .label(ofEnglish("c"))
                                            .build())))
            .name("bar")
            .label(ofEnglish("bar"))
            .isRequired(false)
            .build();

    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("withSetOfEnums")
            .name("withSetOfEnums")
            .description("withSetOfEnums")
            .attributes(withSetOfEnumsNew, withSetOfSetOfLEnumsNew)
            .build();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // tests
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);

    final Optional<ProductType> oldProductTypeAfter =
        getProductTypeByKey(CTP_TARGET_CLIENT, "withSetOfEnums");

    assertThat(oldProductTypeAfter)
        .hasValueSatisfying(
            productType ->
                assertAttributesAreEqual(
                    productType.getAttributes(),
                    asList(withSetOfEnumsNew, withSetOfSetOfLEnumsNew)));
  }

  @Test
  void sync_withProductTypeWithCategoryReference_ShouldAddNewAttributesToTheProductType() {
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final AttributeDefinition referenceTypeAttr =
        AttributeDefinitionBuilder.of()
            .name("referenceTypeAttr")
            .label(ofEnglish("referenceTypeAttr"))
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .referenceBuilder()
                                    .referenceTypeId(AttributeReferenceTypeId.CATEGORY)))
            .isRequired(false)
            .inputHint(TextInputHint.SINGLE_LINE)
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .isSearchable(false)
            .build();
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_3)
            .name(PRODUCT_TYPE_NAME_3)
            .description(PRODUCT_TYPE_DESCRIPTION_3)
            .attributes(
                ResourceToDraftConverters.toAttributeDefinitionDraftBuilder(referenceTypeAttr)
                    .build())
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);
    productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    final ProductTypeDraft updatedProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_3)
            .name(PRODUCT_TYPE_NAME_3)
            .description(PRODUCT_TYPE_DESCRIPTION_3)
            .attributes(
                ATTRIBUTE_DEFINITION_DRAFT_1,
                ResourceToDraftConverters.toAttributeDefinitionDraftBuilder(referenceTypeAttr)
                    .build())
            .build();

    productTypeSync.sync(singletonList(updatedProductTypeDraft)).toCompletableFuture().join();

    final Optional<ProductType> updatedProductType =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_3);
    assert updatedProductType.isPresent();

    final Optional<AttributeDefinition> newAttributeDefinition =
        updatedProductType.get().getAttributes().stream()
            .filter(
                attributeDefinition ->
                    attributeDefinition.getName().equals(ATTRIBUTE_DEFINITION_DRAFT_1.getName()))
            .findAny();

    assertThat(newAttributeDefinition).isPresent();
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdate() {
    // Helps to count invocation of a request and used to decide execution or mocking response
    concurrentModificationCounter = new AtomicInteger(0);

    return withTestClient(
        (uri, method) -> {
          if (uri.contains("product-types") && ApiHttpMethod.POST.equals(method)) {
            if (concurrentModificationCounter.get() == 0) {
              concurrentModificationCounter.incrementAndGet();
              return CompletableFutureUtils.exceptionallyCompletedFuture(
                  createConcurrentModificationException());
            }
          }
          return null;
        });
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {
    return withTestClient(
        (uri, method) -> {
          if (uri.contains("product-types/key=") && ApiHttpMethod.GET.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(createNotFoundException());
          }
          if (uri.contains("product-types/") && ApiHttpMethod.POST.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(
                createConcurrentModificationException());
          }
          return null;
        });
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {
    return withTestClient(
        (uri, method) -> {
          if (uri.contains("product-types/key=") && ApiHttpMethod.GET.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(createBadGatewayException());
          } else if (uri.contains("product-types/") && ApiHttpMethod.POST.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(
                createConcurrentModificationException());
          }
          return null;
        });
  }

  private ProjectApiRoot withTestClient(
      BiFunction<String, ApiHttpMethod, CompletableFuture<ApiHttpResponse<byte[]>>> fn) {
    return ApiRootBuilder.of(
            request -> {
              final String uri = request.getUri() != null ? request.getUri().toString() : "";
              final ApiHttpMethod method = request.getMethod();
              final CompletableFuture<ApiHttpResponse<byte[]>> exceptionResponse =
                  fn.apply(uri, method);
              if (exceptionResponse != null) {
                return exceptionResponse;
              }
              return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
            })
        .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
        .build(CTP_TARGET_CLIENT.getProjectKey());
  }
}
