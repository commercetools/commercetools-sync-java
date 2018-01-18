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
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceSetAttributeDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceWithId;
import static com.commercetools.sync.products.helpers.BatchProcessor.PRODUCT_DRAFT_IS_NULL;
import static com.commercetools.sync.products.helpers.BatchProcessor.PRODUCT_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.products.helpers.BatchProcessor.PRODUCT_VARIANT_DRAFT_IS_NULL;
import static com.commercetools.sync.products.helpers.BatchProcessor.PRODUCT_VARIANT_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.products.helpers.BatchProcessor.PRODUCT_VARIANT_DRAFT_SKU_NOT_SET;
import static com.commercetools.sync.products.helpers.BatchProcessor.getProductDraftErrorsAndAcceptConsumer;
import static com.commercetools.sync.products.helpers.BatchProcessor.getProductKeyFromReference;
import static com.commercetools.sync.products.helpers.BatchProcessor.getReferencedProductKeys;
import static com.commercetools.sync.products.helpers.BatchProcessor.getReferencedProductKeysFromSet;
import static com.commercetools.sync.products.helpers.BatchProcessor.getVariantDraftErrors;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchProcessorTest {
    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private ProductSync productSync;

    @Before
    public void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(ctpClient)
                                                                        .errorCallback((errorMessage, exception) -> {
                                                                            errorCallBackMessages.add(errorMessage);
                                                                            errorCallBackExceptions.add(exception);
                                                                        })
                                                                        .build();
        productSync = new ProductSync(syncOptions);
    }

    @Test
    public void getVariantDraftErrors_WithNullVariant_ShouldHaveValidationErrors() {
        final int variantPosition = 0;
        final String productDraftKey = "key";
        final List<String> validationErrors = getVariantDraftErrors(null, variantPosition, productDraftKey);

        assertThat(validationErrors).hasSize(1);
        assertThat(validationErrors.get(0))
            .isEqualTo(format(PRODUCT_VARIANT_DRAFT_IS_NULL, variantPosition, productDraftKey));
    }

    @Test
    public void getVariantDraftErrors_WithNokeyAndSku_ShouldHaveValidationErrors() {
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
    public void getVariantDraftErrors_WithNokey_ShouldHaveKeyValidationError() {
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
    public void getVariantDraftErrors_WithNoSku_ShouldHaveSkuValidationError() {
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
    public void getVariantDraftErrors_WithSkuAndKey_ShouldHaveNoValidationErrors() {
        final String productDraftKey = "key";
        final ProductVariantDraft productVariantDraft = mock(ProductVariantDraft.class);
        when(productVariantDraft.getKey()).thenReturn("key");
        when(productVariantDraft.getSku()).thenReturn("sku");

        final List<String> validationErrors =
            getVariantDraftErrors(productVariantDraft, 0, productDraftKey);

        assertThat(validationErrors).isEmpty();
    }

    @Test
    public void
        getProductDraftErrorsAndAcceptConsumer_WithNullMvAndNullVariants_ShouldNotAcceptConsumerAndHaveErrors() {
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
    public void
        getProductDraftErrorsAndAcceptConsumer_WithNullMvAndNoVariants_ShouldNotAcceptConsumerAndHaveErrors() {
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
    public void
        getProductDraftErrorsAndAcceptConsumer_WithNullMvAndValidVariants_ShouldNotAcceptConsumerAndHaveErrors() {
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
    public void
        getProductDraftErrorsAndAcceptConsumer_WithInValidMvAndValidVariants_ShouldNotAcceptConsumerAndHaveErrors() {
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
    public void
        getProductDraftErrorsAndAcceptConsumer_WithValidMvAndValidVariants_ShouldNotAcceptConsumerAndHaveErrors() {
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
    public void getProductKeyFromReference_WithNullJsonNode_ShouldReturnEmptyOpt() {
        final NullNode nullNode = JsonNodeFactory.instance.nullNode();
        assertThat(getProductKeyFromReference(nullNode)).isEmpty();
    }

    @Test
    public void getProductKeyFromReference_WithoutAProductReference_ShouldReturnEmptyOpt() {
        final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("key", "value");
        assertThat(getProductKeyFromReference(objectNode)).isEmpty();
    }

    @Test
    public void getProductKeyFromReference_WithAProductReference_ShouldReturnOptWithRefText() {
        assertThat(getProductKeyFromReference(getProductReferenceWithId("foo"))).contains("foo");
    }

    @Test
    public void getReferencedProductKeysFromSet_WithOnlyNullRefsInSet_ShouldReturnEmptySet() {
        final AttributeDraft productReferenceSetAttribute =
            getProductReferenceSetAttributeDraft("foo", null, null);
        assertThat(getReferencedProductKeysFromSet(productReferenceSetAttribute.getValue())).isEmpty();
    }

    @Test
    public void getReferencedProductKeysFromSet_WithNullRefsInSet_ShouldReturnSetOfNonNullIds() {
        final AttributeDraft productReferenceSetAttribute =
            getProductReferenceSetAttributeDraft("foo", getProductReferenceWithId("foo"),
                getProductReferenceWithId("bar"));
        assertThat(getReferencedProductKeysFromSet(productReferenceSetAttribute.getValue()))
            .containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    public void getReferencedProductKeysFromSet_WithNullAndOtherRefsInSet_ShouldReturnSetOfNonNullIds() {
        final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("key", "value");

        final AttributeDraft productReferenceSetAttribute =
            getProductReferenceSetAttributeDraft("foo", getProductReferenceWithId("foo"),
                getProductReferenceWithId("bar"), objectNode);
        assertThat(getReferencedProductKeysFromSet(productReferenceSetAttribute.getValue()))
            .containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    public void getReferencedProductKeys_WithNullDraftValue_ShouldReturnEmptySet() {
        assertThat(getReferencedProductKeys(AttributeDraft.of("foo", null))).isEmpty();
    }

    @Test
    public void getReferencedProductKeys_WithSetAsValue_ShouldReturnSetKeys() {
        final AttributeDraft productReferenceSetAttribute =
            getProductReferenceSetAttributeDraft("foo", getProductReferenceWithId("foo"),
                getProductReferenceWithId("bar"));
        assertThat(getReferencedProductKeys(productReferenceSetAttribute)).containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    public void getReferencedProductKeys_WithProductRefAsValue_ShouldReturnKeyinSet() {
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("foo", getProductReferenceWithId("foo"));
        assertThat(getReferencedProductKeys(productReferenceAttribute)).containsExactly("foo");
    }

    @Test
    public void getReferencedProductKeys_WithNullAttributes_ShouldReturnEmptySet() {
        assertThat(getReferencedProductKeys(ProductVariantDraftBuilder.of().build())).isEmpty();
    }

    @Test
    public void getReferencedProductKeys_WithNoAttributes_ShouldReturnEmptySet() {
        assertThat(getReferencedProductKeys(ProductVariantDraftBuilder.of().attributes().build())).isEmpty();
    }

    @Test
    public void getReferencedProductKeys_WithANullAttribute_ShouldReturnEmptySet() {
        assertThat(getReferencedProductKeys(ProductVariantDraftBuilder.of().attributes(singletonList(null)).build()))
            .isEmpty();
    }

    @Test
    public void getReferencedProductKeys_WithAProductRefAttribute_ShouldReturnEmptySet() {
        final AttributeDraft productReferenceSetAttribute =
            getProductReferenceSetAttributeDraft("foo", getProductReferenceWithId("foo"),
                getProductReferenceWithId("bar"));
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("foo", getProductReferenceWithId("foo"));

        final List<AttributeDraft> attributes = asList(null, productReferenceAttribute, productReferenceSetAttribute);

        final ProductVariantDraft variantDraft = ProductVariantDraftBuilder.of()
                                                                           .attributes(attributes)
                                                                           .build();
        assertThat(getReferencedProductKeys(variantDraft)).containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    public void validateBatch_WithEmptyBatch_ShouldHaveEmptyResults() {
        final BatchProcessor batchProcessor = new BatchProcessor(emptyList(), productSync);

        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    public void validateBatch_WithANullDraft_ShouldResultInAnError() {
        final BatchProcessor batchProcessor = new BatchProcessor(singletonList(null), productSync);

        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages).containsExactly(PRODUCT_DRAFT_IS_NULL);
    }

    @Test
    public void validateBatch_WithADraftWithNullKey_ShouldResultInAnError() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        final BatchProcessor batchProcessor = new BatchProcessor(singletonList(productDraft), productSync);

        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages)
            .containsExactly(format(PRODUCT_DRAFT_KEY_NOT_SET, productDraft.getName()));
    }

    @Test
    public void validateBatch_WithADraftWithEmptyKey_ShouldResultInAnError() {
        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("");

        final BatchProcessor batchProcessor = new BatchProcessor(singletonList(productDraft), productSync);
        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages)
            .containsExactly(format(PRODUCT_DRAFT_KEY_NOT_SET, productDraft.getName()));
    }

    @Test
    public void validateBatch_WithDrafts_ShouldValidateCorrectly() {
        final AttributeDraft productReferenceSetAttribute =
            getProductReferenceSetAttributeDraft("foo", getProductReferenceWithId("foo"),
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

        final BatchProcessor batchProcessor = new BatchProcessor(productDrafts, productSync);
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
