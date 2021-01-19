package com.commercetools.sync.products.helpers;

import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceWithId;
import static com.commercetools.sync.products.ProductSyncMockUtils.getReferenceSetAttributeDraft;
import static com.commercetools.sync.products.helpers.ProductBatchValidator.PRODUCT_DRAFT_IS_NULL;
import static com.commercetools.sync.products.helpers.ProductBatchValidator.PRODUCT_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.products.helpers.ProductBatchValidator.PRODUCT_VARIANT_DRAFT_IS_NULL;
import static com.commercetools.sync.products.helpers.ProductBatchValidator.PRODUCT_VARIANT_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.products.helpers.ProductBatchValidator.PRODUCT_VARIANT_DRAFT_SKU_NOT_SET;
import static com.commercetools.sync.products.helpers.ProductBatchValidator.getReferencedProductKeys;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.models.DefaultCurrencyUnits;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.states.State;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.MoneyImpl;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductBatchValidatorTest {
  private List<String> errorCallBackMessages;
  private ProductSyncOptions syncOptions;
  private ProductSyncStatistics syncStatistics;

  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    final SphereClient ctpClient = mock(SphereClient.class);
    syncOptions =
        ProductSyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                })
            .build();
    syncStatistics = mock(ProductSyncStatistics.class);
  }

  @Test
  void getReferencedProductKeys_WithNullAttributes_ShouldReturnEmptySet() {
    assertThat(getReferencedProductKeys(ProductVariantDraftBuilder.of().build())).isEmpty();
  }

  @Test
  void getReferencedProductKeys_WithNoAttributes_ShouldReturnEmptySet() {
    assertThat(getReferencedProductKeys(ProductVariantDraftBuilder.of().attributes().build()))
        .isEmpty();
  }

  @Test
  void getReferencedProductKeys_WithANullAttribute_ShouldReturnEmptySet() {
    assertThat(
            getReferencedProductKeys(
                ProductVariantDraftBuilder.of().attributes(singletonList(null)).build()))
        .isEmpty();
  }

  @Test
  void getReferencedProductKeys_WithAProductRefAttribute_ShouldReturnEmptySet() {
    final AttributeDraft productReferenceSetAttribute =
        getReferenceSetAttributeDraft(
            "foo", getProductReferenceWithId("foo"), getProductReferenceWithId("bar"));
    final AttributeDraft productReferenceAttribute =
        AttributeDraft.of("foo", getProductReferenceWithId("foo"));

    final List<AttributeDraft> attributes =
        asList(null, productReferenceAttribute, productReferenceSetAttribute);

    final ProductVariantDraft variantDraft =
        ProductVariantDraftBuilder.of().attributes(attributes).build();
    assertThat(getReferencedProductKeys(variantDraft)).containsExactlyInAnyOrder("foo", "bar");
  }

  @Test
  void getReferencedProductKeys_WithANullAttrValue_ShouldReturnEmptySet() {
    final AttributeDraft attributeDraft = AttributeDraft.of("foo", null);
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeDraft).build();

    final Set<String> result = getReferencedProductKeys(productVariantDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void getReferencedProductKeys_WithSetAsValue_ShouldReturnSetKeys() {
    final AttributeDraft productReferenceSetAttribute =
        getReferenceSetAttributeDraft(
            "foo", getProductReferenceWithId("foo"), getProductReferenceWithId("bar"));

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceSetAttribute).build();

    final Set<String> result = getReferencedProductKeys(productVariantDraft);

    assertThat(result).containsExactlyInAnyOrder("foo", "bar");
  }

  @Test
  void getReferencedProductKeys_WithProductRefAsValue_ShouldReturnKeyInSet() {
    final AttributeDraft productReferenceAttribute =
        AttributeDraft.of("foo", getProductReferenceWithId("foo"));

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceAttribute).build();

    final Set<String> result = getReferencedProductKeys(productVariantDraft);

    assertThat(result).containsExactly("foo");
  }

  @Test
  void getProductKeyFromReference_WithNullJsonNode_ShouldReturnEmptyOpt() {
    final NullNode nullNode = JsonNodeFactory.instance.nullNode();
    final AttributeDraft productReferenceAttribute = AttributeDraft.of("foo", nullNode);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceAttribute).build();

    final Set<String> result = getReferencedProductKeys(productVariantDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void getProductKeyFromReference_WithoutAProductReference_ShouldReturnEmptyOpt() {
    final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
    objectNode.put("key", "value");
    final AttributeDraft productReferenceAttribute = AttributeDraft.of("foo", objectNode);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceAttribute).build();

    final Set<String> result = getReferencedProductKeys(productVariantDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void getReferencedProductKeysFromSet_WithOnlyNullRefsInSet_ShouldReturnEmptySet() {
    final AttributeDraft productReferenceSetAttribute =
        getReferenceSetAttributeDraft("foo", null, null);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceSetAttribute).build();

    final Set<String> result = getReferencedProductKeys(productVariantDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void getReferencedProductKeysFromSet_WithNullAndOtherRefsInSet_ShouldReturnSetOfNonNullIds() {
    final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
    objectNode.put("key", "value");

    final AttributeDraft productReferenceSetAttribute =
        getReferenceSetAttributeDraft(
            "foo", getProductReferenceWithId("foo"), getProductReferenceWithId("bar"), objectNode);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceSetAttribute).build();

    final Set<String> result = getReferencedProductKeys(productVariantDraft);

    assertThat(result).containsExactlyInAnyOrder("foo", "bar");
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
    final Set<ProductDraft> validDrafts = getValidDrafts(Collections.emptyList());

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithNullProductDraft_ShouldHaveValidationErrorAndEmptyResult() {
    final Set<ProductDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0)).isEqualTo(PRODUCT_DRAFT_IS_NULL);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithProductDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
    final ProductDraft productDraft = mock(ProductDraft.class);
    final Set<ProductDraft> validDrafts = getValidDrafts(Collections.singletonList(productDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format(PRODUCT_DRAFT_KEY_NOT_SET, productDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithCategoryDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
    final ProductDraft productDraft = mock(ProductDraft.class);
    when(productDraft.getKey()).thenReturn(EMPTY);
    final Set<ProductDraft> validDrafts = getValidDrafts(Collections.singletonList(productDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format(PRODUCT_DRAFT_KEY_NOT_SET, productDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void validateAndCollectReferencedKeys_WithNullVariant_ShouldHaveValidationErrors() {
    final String productDraftKey = "key";
    final int variantPosition = 0;

    final ProductDraft productDraft = mock(ProductDraft.class);
    when(productDraft.getKey()).thenReturn(productDraftKey);

    final Set<ProductDraft> validDrafts = getValidDrafts(Collections.singletonList(productDraft));

    assertThat(validDrafts).isEmpty();
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format(PRODUCT_VARIANT_DRAFT_IS_NULL, variantPosition, productDraftKey));
  }

  @Test
  void validateAndCollectReferencedKeys_WithVariantButNoKeyAndSku_ShouldHaveValidationErrors() {
    final int variantPosition = 0;
    final String productDraftKey = "key";

    final ProductDraft productDraft = mock(ProductDraft.class);
    when(productDraft.getKey()).thenReturn(productDraftKey);
    final ProductVariantDraft masterVariant = mock(ProductVariantDraft.class);
    when(productDraft.getMasterVariant()).thenReturn(masterVariant);

    final Set<ProductDraft> validDrafts = getValidDrafts(Collections.singletonList(productDraft));

    assertThat(validDrafts).isEmpty();
    assertThat(errorCallBackMessages).hasSize(2);
    assertThat(errorCallBackMessages)
        .containsExactlyInAnyOrder(
            format(PRODUCT_VARIANT_DRAFT_SKU_NOT_SET, variantPosition, productDraftKey),
            format(PRODUCT_VARIANT_DRAFT_KEY_NOT_SET, variantPosition, productDraftKey));
  }

  @Test
  void validateAndCollectReferencedKeys_WithNoVariantKey_ShouldHaveKeyValidationError() {
    final int variantPosition = 0;
    final String productDraftKey = "key";

    final ProductDraft productDraft = mock(ProductDraft.class);
    when(productDraft.getKey()).thenReturn(productDraftKey);

    final ProductVariantDraft masterVariant = mock(ProductVariantDraft.class);
    when(masterVariant.getSku()).thenReturn("sku");
    when(productDraft.getMasterVariant()).thenReturn(masterVariant);

    final Set<ProductDraft> validDrafts = getValidDrafts(Collections.singletonList(productDraft));

    assertThat(validDrafts).isEmpty();
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format(PRODUCT_VARIANT_DRAFT_KEY_NOT_SET, variantPosition, productDraftKey));
  }

  @Test
  void validateAndCollectReferencedKeys_WithNoVariantSku_ShouldHaveSkuValidationError() {
    final int variantPosition = 0;
    final String productDraftKey = "key";
    final ProductDraft productDraft = mock(ProductDraft.class);
    when(productDraft.getKey()).thenReturn(productDraftKey);

    final ProductVariantDraft masterVariant = mock(ProductVariantDraft.class);
    when(masterVariant.getKey()).thenReturn("key");
    when(productDraft.getMasterVariant()).thenReturn(masterVariant);

    final Set<ProductDraft> validDrafts = getValidDrafts(Collections.singletonList(productDraft));

    assertThat(validDrafts).isEmpty();
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format(PRODUCT_VARIANT_DRAFT_SKU_NOT_SET, variantPosition, productDraftKey));
  }

  @Test
  void validateAndCollectReferencedKeys_WithSkuAndKey_ShouldHaveNoValidationErrors() {
    final String productDraftKey = "key";
    final ProductDraft productDraft = mock(ProductDraft.class);
    when(productDraft.getKey()).thenReturn(productDraftKey);

    final ProductVariantDraft masterVariant = mock(ProductVariantDraft.class);
    when(masterVariant.getKey()).thenReturn("key");
    when(masterVariant.getSku()).thenReturn("sku");
    when(productDraft.getMasterVariant()).thenReturn(masterVariant);

    final Set<ProductDraft> validDrafts = getValidDrafts(Collections.singletonList(productDraft));

    assertThat(validDrafts).hasSize(1);
  }

  @Test
  void validateAndCollectReferencedKeys_WithDrafts_ShouldValidateCorrectly() {
    final AttributeDraft productReferenceSetAttribute =
        getReferenceSetAttributeDraft(
            "foo", getProductReferenceWithId("foo"), getProductReferenceWithId("bar"));
    final AttributeDraft productReferenceAttribute =
        AttributeDraft.of("foo", getProductReferenceWithId("foo"));

    final List<AttributeDraft> attributes =
        asList(null, productReferenceAttribute, productReferenceSetAttribute);

    final ProductVariantDraft validVariantDraft =
        ProductVariantDraftBuilder.of()
            .key("variantKey")
            .sku("variantSku")
            .attributes(attributes)
            .build();
    final ProductVariantDraft invalidVariantDraft =
        ProductVariantDraftBuilder.of().key("invalidVariant").attributes(attributes).build();

    final ProductDraft validProductDraft = mock(ProductDraft.class);
    when(validProductDraft.getKey()).thenReturn("validProductDraft");
    when(validProductDraft.getMasterVariant()).thenReturn(validVariantDraft);
    when(validProductDraft.getVariants()).thenReturn(singletonList(validVariantDraft));

    final ProductDraft inValidProductDraft1 = mock(ProductDraft.class);
    when(inValidProductDraft1.getKey()).thenReturn("invalidProductDraft1");
    when(inValidProductDraft1.getMasterVariant()).thenReturn(null);
    when(inValidProductDraft1.getVariants()).thenReturn(singletonList(validVariantDraft));

    final ProductDraft inValidProductDraft2 = mock(ProductDraft.class);
    when(inValidProductDraft2.getKey()).thenReturn("invalidProductDraft2");
    when(inValidProductDraft2.getMasterVariant()).thenReturn(invalidVariantDraft);
    when(inValidProductDraft2.getVariants()).thenReturn(singletonList(invalidVariantDraft));

    final List<ProductDraft> productDrafts =
        asList(
            null,
            mock(ProductDraft.class),
            validProductDraft,
            inValidProductDraft1,
            inValidProductDraft2);

    final ProductBatchValidator productBatchValidator =
        new ProductBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ProductDraft>, ProductBatchValidator.ReferencedKeys> pair =
        productBatchValidator.validateAndCollectReferencedKeys(productDrafts);

    assertThat(pair.getLeft()).hasSize(1);
    assertThat(pair.getLeft()).containsExactly(validProductDraft);
    assertThat(pair.getRight().getProductKeys()).hasSize(2);
    assertThat(pair.getRight().getProductKeys()).containsExactlyInAnyOrder("foo", "bar");

    assertThat(errorCallBackMessages).hasSize(5);
    assertThat(errorCallBackMessages)
        .containsExactlyInAnyOrder(
            PRODUCT_DRAFT_IS_NULL,
            format(PRODUCT_DRAFT_KEY_NOT_SET, "null"),
            format(PRODUCT_VARIANT_DRAFT_IS_NULL, 0, inValidProductDraft1.getKey()),
            format(PRODUCT_VARIANT_DRAFT_SKU_NOT_SET, 0, inValidProductDraft2.getKey()),
            format(PRODUCT_VARIANT_DRAFT_SKU_NOT_SET, 1, inValidProductDraft2.getKey()));
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithCustomerGroupRefsInPrices_ShouldCollectReferencesCorrectly() {
    final PriceDraft priceDraft =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .custom(CustomFieldsDraft.ofTypeKeyAndJson("customTypeKey", new HashMap<>()))
            .customerGroup(CustomerGroup.referenceOfId("customerGroupKey"))
            .channel(ResourceIdentifier.ofKey("channelKey"))
            .build();

    final PriceDraft priceDraftWithBlankCustomerGroup =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(CustomerGroup.referenceOfId(" "))
            .build();

    final PriceDraft priceDraftWithNullCustomerGroupId =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(CustomerGroup.referenceOfId(null))
            .build();

    final PriceDraft priceDraftWithNullCustomerGroup =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(null)
            .build();

    final ProductVariantDraft validVariantDraft =
        ProductVariantDraftBuilder.of()
            .key("variantKey")
            .sku("variantSku")
            .prices(
                asList(
                    priceDraft,
                    priceDraftWithBlankCustomerGroup,
                    priceDraftWithNullCustomerGroup,
                    priceDraftWithNullCustomerGroupId))
            .build();

    final ProductDraft validProductDraft = mock(ProductDraft.class);
    when(validProductDraft.getKey()).thenReturn("validProductDraft");
    when(validProductDraft.getMasterVariant()).thenReturn(validVariantDraft);
    when(validProductDraft.getVariants()).thenReturn(singletonList(validVariantDraft));

    final List<ProductDraft> productDrafts = singletonList(validProductDraft);

    final ProductBatchValidator productBatchValidator =
        new ProductBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ProductDraft>, ProductBatchValidator.ReferencedKeys> pair =
        productBatchValidator.validateAndCollectReferencedKeys(productDrafts);

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(pair.getLeft()).hasSize(1);
    assertThat(pair.getLeft()).containsExactly(validProductDraft);
    assertThat(pair.getRight().getTypeKeys()).containsExactly("customTypeKey");
    assertThat(pair.getRight().getChannelKeys()).containsExactly("channelKey");
    assertThat(pair.getRight().getCustomerGroupKeys()).containsExactly("customerGroupKey");
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyKeys_ShouldNotCollectKeys() {
    final ProductDraft productDraft = mock(ProductDraft.class);
    when(productDraft.getKey()).thenReturn("key");
    when(productDraft.getTaxCategory()).thenReturn(ResourceIdentifier.ofKey(EMPTY));
    when(productDraft.getState()).thenReturn(State.referenceOfId(EMPTY));
    final ProductVariantDraft masterVariant = mock(ProductVariantDraft.class);
    when(masterVariant.getKey()).thenReturn("key");
    when(masterVariant.getSku()).thenReturn("sku");
    when(productDraft.getMasterVariant()).thenReturn(masterVariant);

    final ProductBatchValidator productBatchValidator =
        new ProductBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ProductDraft>, ProductBatchValidator.ReferencedKeys> pair =
        productBatchValidator.validateAndCollectReferencedKeys(singletonList(productDraft));

    assertThat(pair.getLeft()).contains(productDraft);
    assertThat(pair.getRight().getTaxCategoryKeys()).isEmpty();
    assertThat(pair.getRight().getStateKeys()).isEmpty();
    assertThat(errorCallBackMessages).hasSize(0);
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithKeyValueDocumentAttDrafts_ShouldNotHaveKeyValidationError() {
    final String uuid = UUID.randomUUID().toString();
    final String validIdentifier = "container|key";
    final String invalidIdentifier = "container-key";

    final AttributeDraft referenceSetAttributeDraft =
        getReferenceSetAttributeDraft(
            "foo",
            createReferenceObject(validIdentifier, CustomObject.referenceTypeId()),
            createReferenceObject(uuid, CustomObject.referenceTypeId()),
            createReferenceObject(invalidIdentifier, CustomObject.referenceTypeId()));

    final List<AttributeDraft> attributes = asList(referenceSetAttributeDraft);

    final ProductVariantDraft validVariantDraft =
        ProductVariantDraftBuilder.of()
            .key("variantKey")
            .sku("variantSku")
            .attributes(attributes)
            .build();

    final ProductDraft validProductDraft = mock(ProductDraft.class);
    when(validProductDraft.getKey()).thenReturn("validProductDraft");
    when(validProductDraft.getMasterVariant()).thenReturn(validVariantDraft);

    final ProductBatchValidator productBatchValidator =
        new ProductBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ProductDraft>, ProductBatchValidator.ReferencedKeys> pair =
        productBatchValidator.validateAndCollectReferencedKeys(singletonList(validProductDraft));

    assertThat(pair.getLeft()).hasSize(1);
    assertThat(pair.getLeft()).containsExactly(validProductDraft);

    Set<CustomObjectCompositeIdentifier> identifiers =
        pair.getRight().getCustomObjectCompositeIdentifiers();
    assertThat(identifiers)
        .containsExactlyInAnyOrder(CustomObjectCompositeIdentifier.of(validIdentifier));

    assertThat(errorCallBackMessages).hasSize(0);
  }

  @Nonnull
  private Set<ProductDraft> getValidDrafts(@Nonnull final List<ProductDraft> productDrafts) {
    final ProductBatchValidator productBatchValidator =
        new ProductBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ProductDraft>, ProductBatchValidator.ReferencedKeys> pair =
        productBatchValidator.validateAndCollectReferencedKeys(productDrafts);
    return pair.getLeft();
  }
}
