package com.commercetools.sync.products.helpers;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceWithId;
import static com.commercetools.sync.products.ProductSyncMockUtils.getReferenceSetAttributeDraft;
import static com.commercetools.sync.products.helpers.ProductBatchProcessor.PRODUCT_DRAFT_IS_NULL;
import static com.commercetools.sync.products.helpers.ProductBatchProcessor.PRODUCT_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.products.helpers.ProductBatchProcessor.PRODUCT_VARIANT_DRAFT_IS_NULL;
import static com.commercetools.sync.products.helpers.ProductBatchProcessor.PRODUCT_VARIANT_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.products.helpers.ProductBatchProcessor.PRODUCT_VARIANT_DRAFT_SKU_NOT_SET;
import static com.commercetools.sync.products.helpers.ProductBatchProcessor.getProductDraftErrorsAndAcceptConsumer;
import static com.commercetools.sync.products.helpers.ProductBatchProcessor.getReferencedProductKeys;
import static com.commercetools.sync.products.helpers.ProductBatchProcessor.getVariantDraftErrors;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductBatchProcessorTest {
    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private ProductSync productSync;

    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((errorMessage, exception) -> {
                errorCallBackMessages.add(errorMessage);
                errorCallBackExceptions.add(exception);
            })
            .build();
        productSync = new ProductSync(syncOptions);
    }

    @Test
    void getVariantDraftErrors_WithNullVariant_ShouldHaveValidationErrors() {
        final int variantPosition = 0;
        final String productDraftKey = "key";
        final List<String> validationErrors = getVariantDraftErrors(null, variantPosition, productDraftKey);

        assertThat(validationErrors).hasSize(1);
        assertThat(validationErrors.get(0))
            .isEqualTo(format(PRODUCT_VARIANT_DRAFT_IS_NULL, variantPosition, productDraftKey));
    }

    @Test
    void getVariantDraftErrors_WithNokeyAndSku_ShouldHaveValidationErrors() {
        final int variantPosition = 0;
        final String productDraftKey = "key";
        final List<String> validationErrors =
            getVariantDraftErrors(mock(ProductVariantDraft.class), variantPosition, productDraftKey);

        assertThat(validationErrors).hasSize(2);
        assertThat(validationErrors).containsExactlyInAnyOrder(
            format(PRODUCT_VARIANT_DRAFT_SKU_NOT_SET, variantPosition, productDraftKey),
            format(PRODUCT_VARIANT_DRAFT_KEY_NOT_SET, variantPosition, productDraftKey));
    }

    @Test
    void getVariantDraftErrors_WithNoKey_ShouldHaveKeyValidationError() {
        final int variantPosition = 0;
        final String productDraftKey = "key";
        final ProductVariantDraft productVariantDraft = mock(ProductVariantDraft.class);
        when(productVariantDraft.getSku()).thenReturn("sku");

        final List<String> validationErrors =
            getVariantDraftErrors(productVariantDraft, variantPosition, productDraftKey);

        assertThat(validationErrors).hasSize(1);
        assertThat(validationErrors.get(0)).isEqualTo(
            format(PRODUCT_VARIANT_DRAFT_KEY_NOT_SET, variantPosition, productDraftKey));
    }

    @Test
    void getVariantDraftErrors_WithNoSku_ShouldHaveSkuValidationError() {
        final int variantPosition = 0;
        final String productDraftKey = "key";
        final ProductVariantDraft productVariantDraft = mock(ProductVariantDraft.class);
        when(productVariantDraft.getKey()).thenReturn("key");

        final List<String> validationErrors =
            getVariantDraftErrors(productVariantDraft, variantPosition, productDraftKey);

        assertThat(validationErrors).hasSize(1);
        assertThat(validationErrors.get(0)).isEqualTo(
            format(PRODUCT_VARIANT_DRAFT_SKU_NOT_SET, variantPosition, productDraftKey));
    }

    @Test
    void getVariantDraftErrors_WithSkuAndKey_ShouldHaveNoValidationErrors() {
        final String productDraftKey = "key";
        final ProductVariantDraft productVariantDraft = mock(ProductVariantDraft.class);
        when(productVariantDraft.getKey()).thenReturn("key");
        when(productVariantDraft.getSku()).thenReturn("sku");

        final List<String> validationErrors =
            getVariantDraftErrors(productVariantDraft, 0, productDraftKey);

        assertThat(validationErrors).isEmpty();
    }

    @Test
    void getProductDraftErrorsAndAcceptConsumer_WithNullMvAndNullVariants_ShouldNotAcceptConsumerAndHaveErrors() {
        final AtomicBoolean isConsumerAccepted = new AtomicBoolean(false);
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("key");
        when(productDraft.getVariants()).thenReturn(null);

        final List<String> validationErrors =
            getProductDraftErrorsAndAcceptConsumer(productDraft, variantDraft -> isConsumerAccepted.set(true));

        assertThat(validationErrors).hasSize(1);
        assertThat(validationErrors.get(0))
            .isEqualTo(format(PRODUCT_VARIANT_DRAFT_IS_NULL, 0, productDraft.getKey()));
        assertThat(isConsumerAccepted.get()).isFalse();
    }

    @Test
    void getProductDraftErrorsAndAcceptConsumer_WithNullMvAndNoVariants_ShouldNotAcceptConsumerAndHaveErrors() {
        final AtomicBoolean isConsumerAccepted = new AtomicBoolean(false);
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("key");

        final List<String> validationErrors =
            getProductDraftErrorsAndAcceptConsumer(productDraft, variantDraft -> isConsumerAccepted.set(true));

        assertThat(validationErrors).hasSize(1);
        assertThat(validationErrors.get(0))
            .isEqualTo(format(PRODUCT_VARIANT_DRAFT_IS_NULL, 0, productDraft.getKey()));
        assertThat(isConsumerAccepted.get()).isFalse();
    }

    @Test
    void getProductDraftErrorsAndAcceptConsumer_WithNullMvAndValidVariants_ShouldNotAcceptConsumerAndHaveErrors() {
        final AtomicBoolean isConsumerAccepted = new AtomicBoolean(false);

        final ProductVariantDraft productVariantDraft = mock(ProductVariantDraft.class);
        when(productVariantDraft.getKey()).thenReturn("key");
        when(productVariantDraft.getSku()).thenReturn("sku");

        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("key");
        when(productDraft.getVariants()).thenReturn(singletonList(productVariantDraft));

        final List<String> validationErrors =
            getProductDraftErrorsAndAcceptConsumer(productDraft, variantDraft -> isConsumerAccepted.set(true));

        assertThat(validationErrors).hasSize(1);
        assertThat(validationErrors.get(0))
            .isEqualTo(format(PRODUCT_VARIANT_DRAFT_IS_NULL, 0, productDraft.getKey()));
        assertThat(isConsumerAccepted.get()).isFalse();
    }

    @Test
    void getProductDraftErrorsAndAcceptConsumer_WithInValidMvAndValidVariants_ShouldNotAcceptConsumerAndHaveErrors() {
        final AtomicBoolean isConsumerAccepted = new AtomicBoolean(false);

        final ProductVariantDraft productVariantDraft = mock(ProductVariantDraft.class);
        when(productVariantDraft.getKey()).thenReturn("key");
        when(productVariantDraft.getSku()).thenReturn("sku");

        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("key");
        when(productDraft.getMasterVariant()).thenReturn(mock(ProductVariantDraft.class));
        when(productDraft.getVariants()).thenReturn(singletonList(productVariantDraft));

        final List<String> validationErrors =
            getProductDraftErrorsAndAcceptConsumer(productDraft, variantDraft -> isConsumerAccepted.set(true));

        assertThat(validationErrors).hasSize(2);
        assertThat(validationErrors).containsExactlyInAnyOrder(
            format(PRODUCT_VARIANT_DRAFT_SKU_NOT_SET, 0, productDraft.getKey()),
            format(PRODUCT_VARIANT_DRAFT_KEY_NOT_SET, 0, productDraft.getKey()));
        assertThat(isConsumerAccepted.get()).isFalse();
    }

    @Test
    void getProductDraftErrorsAndAcceptConsumer_WithValidMvAndValidVariants_ShouldAcceptConsumerAndNoErrors() {
        final AtomicBoolean isConsumerAccepted = new AtomicBoolean(false);

        final ProductVariantDraft productVariantDraft = mock(ProductVariantDraft.class);
        when(productVariantDraft.getKey()).thenReturn("key");
        when(productVariantDraft.getSku()).thenReturn("sku");

        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("key");
        when(productDraft.getMasterVariant()).thenReturn(productVariantDraft);
        when(productDraft.getVariants()).thenReturn(singletonList(productVariantDraft));

        final List<String> validationErrors =
            getProductDraftErrorsAndAcceptConsumer(productDraft, variantDraft -> isConsumerAccepted.set(true));

        assertThat(validationErrors).isEmpty();
        assertThat(isConsumerAccepted.get()).isTrue();
    }

    @Test
    void getReferencedProductKeys_WithNullAttributes_ShouldReturnEmptySet() {
        assertThat(getReferencedProductKeys(ProductVariantDraftBuilder.of().build())).isEmpty();
    }

    @Test
    void getReferencedProductKeys_WithNoAttributes_ShouldReturnEmptySet() {
        assertThat(getReferencedProductKeys(ProductVariantDraftBuilder.of().attributes().build())).isEmpty();
    }

    @Test
    void getReferencedProductKeys_WithANullAttribute_ShouldReturnEmptySet() {
        assertThat(getReferencedProductKeys(ProductVariantDraftBuilder.of().attributes(singletonList(null)).build()))
            .isEmpty();
    }

    @Test
    void getReferencedProductKeys_WithAProductRefAttribute_ShouldReturnEmptySet() {
        final AttributeDraft productReferenceSetAttribute =
            getReferenceSetAttributeDraft("foo", getProductReferenceWithId("foo"),
                getProductReferenceWithId("bar"));
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("foo", getProductReferenceWithId("foo"));

        final List<AttributeDraft> attributes = asList(null, productReferenceAttribute, productReferenceSetAttribute);

        final ProductVariantDraft variantDraft = ProductVariantDraftBuilder.of()
                                                                           .attributes(attributes)
                                                                           .build();
        assertThat(getReferencedProductKeys(variantDraft)).containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    void getReferencedProductKeys_WithANullAttrValue_ShouldReturnEmptySet() {
        final AttributeDraft attributeDraft = AttributeDraft.of("foo", null);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(attributeDraft)
            .build();

        final Set<String> result = getReferencedProductKeys(productVariantDraft);

        assertThat(result).isEmpty();
    }

    @Test
    void getReferencedProductKeys_WithSetAsValue_ShouldReturnSetKeys() {
        final AttributeDraft productReferenceSetAttribute =
            getReferenceSetAttributeDraft("foo", getProductReferenceWithId("foo"),
                getProductReferenceWithId("bar"));

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceSetAttribute)
            .build();

        final Set<String> result = getReferencedProductKeys(productVariantDraft);

        assertThat(result).containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    void getReferencedProductKeys_WithProductRefAsValue_ShouldReturnKeyInSet() {
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("foo", getProductReferenceWithId("foo"));

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceAttribute)
            .build();

        final Set<String> result = getReferencedProductKeys(productVariantDraft);

        assertThat(result).containsExactly("foo");
    }

    @Test
    void getProductKeyFromReference_WithNullJsonNode_ShouldReturnEmptyOpt() {
        final NullNode nullNode = JsonNodeFactory.instance.nullNode();
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("foo", nullNode);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceAttribute)
            .build();

        final Set<String> result = getReferencedProductKeys(productVariantDraft);

        assertThat(result).isEmpty();
    }

    @Test
    void getProductKeyFromReference_WithoutAProductReference_ShouldReturnEmptyOpt() {
        final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("key", "value");
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("foo", objectNode);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceAttribute)
            .build();

        final Set<String> result = getReferencedProductKeys(productVariantDraft);

        assertThat(result).isEmpty();
    }

    @Disabled("Fails due to bug on https://github.com/FasterXML/jackson-databind/issues/2442")
    @Test
    void getReferencedProductKeysFromSet_WithOnlyNullRefsInSet_ShouldReturnEmptySet() {
        final AttributeDraft productReferenceSetAttribute =
            getReferenceSetAttributeDraft("foo", null, null);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceSetAttribute)
            .build();

        final Set<String> result = getReferencedProductKeys(productVariantDraft);

        assertThat(result).isEmpty();
    }

    @Test
    void getReferencedProductKeysFromSet_WithNullAndOtherRefsInSet_ShouldReturnSetOfNonNullIds() {
        final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("key", "value");

        final AttributeDraft productReferenceSetAttribute =
            getReferenceSetAttributeDraft("foo", getProductReferenceWithId("foo"),
                getProductReferenceWithId("bar"), objectNode);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceSetAttribute)
            .build();

        final Set<String> result = getReferencedProductKeys(productVariantDraft);

        assertThat(result).containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    void validateBatch_WithEmptyBatch_ShouldHaveEmptyResults() {
        final ProductBatchProcessor batchProcessor = new ProductBatchProcessor(emptyList(), productSync);

        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    void validateBatch_WithANullDraft_ShouldResultInAnError() {
        final ProductBatchProcessor batchProcessor = new ProductBatchProcessor(singletonList(null), productSync);

        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages).containsExactly(PRODUCT_DRAFT_IS_NULL);
    }

    @Test
    void validateBatch_WithADraftWithNullKey_ShouldResultInAnError() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        final ProductBatchProcessor batchProcessor =
            new ProductBatchProcessor(singletonList(productDraft), productSync);

        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages)
            .containsExactly(format(PRODUCT_DRAFT_KEY_NOT_SET, productDraft.getName()));
    }

    @Test
    void validateBatch_WithADraftWithEmptyKey_ShouldResultInAnError() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("");

        final ProductBatchProcessor batchProcessor =
            new ProductBatchProcessor(singletonList(productDraft), productSync);
        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages)
            .containsExactly(format(PRODUCT_DRAFT_KEY_NOT_SET, productDraft.getName()));
    }

    @Test
    void validateBatch_WithDrafts_ShouldValidateCorrectly() {
        final AttributeDraft productReferenceSetAttribute =
            getReferenceSetAttributeDraft("foo", getProductReferenceWithId("foo"),
                getProductReferenceWithId("bar"));
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("foo", getProductReferenceWithId("foo"));

        final List<AttributeDraft> attributes = asList(null, productReferenceAttribute, productReferenceSetAttribute);

        final ProductVariantDraft validVariantDraft = ProductVariantDraftBuilder.of()
                                                                                .key("variantKey")
                                                                                .sku("variantSku")
                                                                                .attributes(attributes)
                                                                                .build();
        final ProductVariantDraft invalidVariantDraft = ProductVariantDraftBuilder.of()
                                                                                  .key("invalidVariant")
                                                                                  .attributes(attributes)
                                                                                  .build();

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


        final List<ProductDraft> productDrafts = asList(null, mock(ProductDraft.class),
            validProductDraft, inValidProductDraft1, inValidProductDraft2);

        final ProductBatchProcessor batchProcessor = new ProductBatchProcessor(productDrafts, productSync);
        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).hasSize(3);
        assertThat(batchProcessor.getKeysToCache()).containsExactlyInAnyOrder("validProductDraft", "foo", "bar");
        assertThat(batchProcessor.getValidDrafts()).hasSize(1);
        assertThat(batchProcessor.getValidDrafts()).containsExactly(validProductDraft);

        assertThat(errorCallBackMessages).hasSize(5);
        assertThat(errorCallBackMessages).containsExactlyInAnyOrder(
            PRODUCT_DRAFT_IS_NULL,
            format(PRODUCT_DRAFT_KEY_NOT_SET, "null"),
            format(PRODUCT_VARIANT_DRAFT_IS_NULL, 0, inValidProductDraft1.getKey()),
            format(PRODUCT_VARIANT_DRAFT_SKU_NOT_SET, 0, inValidProductDraft2.getKey()),
            format(PRODUCT_VARIANT_DRAFT_SKU_NOT_SET, 1, inValidProductDraft2.getKey()));
    }

}
