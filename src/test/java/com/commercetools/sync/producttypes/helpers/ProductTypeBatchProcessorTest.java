package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.exceptions.InvalidReferenceException;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.producttypes.helpers.ProductTypeBatchProcessor.PRODUCT_TYPE_DRAFT_IS_NULL;
import static com.commercetools.sync.producttypes.helpers.ProductTypeBatchProcessor.PRODUCT_TYPE_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.producttypes.helpers.ProductTypeBatchProcessor.PRODUCT_TYPE_HAS_INVALID_REFERENCES;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductTypeBatchProcessorTest {
    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private ProductTypeSync productTypeSync;

    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);
        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorCallBackMessages.add(exception.getMessage());
                errorCallBackExceptions.add(exception);
            })
            .build();
        productTypeSync = new ProductTypeSync(syncOptions);
    }

    @Test
    void validateBatch_WithEmptyBatch_ShouldResultInNoValidDraftsAndNoCachedKeys() {
        final ProductTypeBatchProcessor batchProcessor = new ProductTypeBatchProcessor(emptyList(), productTypeSync);
        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    void validateBatch_WithANullDraft_ShouldResultInAnError() {
        final ProductTypeBatchProcessor batchProcessor =
            new ProductTypeBatchProcessor(singletonList(null), productTypeSync);
        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        assertThat(errorCallBackMessages).containsExactly(PRODUCT_TYPE_DRAFT_IS_NULL);
        assertThat(errorCallBackExceptions).hasOnlyOneElementSatisfying(throwable -> {
            assertThat(throwable).isInstanceOf(SyncException.class);
            assertThat(throwable).hasMessage(PRODUCT_TYPE_DRAFT_IS_NULL);
        });
    }

    @Test
    void validateBatch_WithADraftWithNullKey_ShouldResultInAnError() {
        final ProductTypeDraft productTypeDraft = mock(ProductTypeDraft.class);
        when(productTypeDraft.getKey()).thenReturn(null);

        final ProductTypeBatchProcessor batchProcessor =
            new ProductTypeBatchProcessor(singletonList(productTypeDraft), productTypeSync);
        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        final String errorMessage = format(PRODUCT_TYPE_DRAFT_KEY_NOT_SET, productTypeDraft.getName());
        assertThat(errorCallBackMessages).containsExactly(errorMessage);
        assertThat(errorCallBackExceptions).hasOnlyOneElementSatisfying(throwable -> {
            assertThat(throwable).isInstanceOf(SyncException.class);
            assertThat(throwable).hasMessage(errorMessage);
        });
    }

    @Test
    void validateBatch_WithADraftWithEmptyKey_ShouldResultInAnError() {
        final ProductTypeDraft productTypeDraft = mock(ProductTypeDraft.class);
        when(productTypeDraft.getKey()).thenReturn("");

        final ProductTypeBatchProcessor batchProcessor =
            new ProductTypeBatchProcessor(singletonList(productTypeDraft), productTypeSync);
        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        final String errorMessage = format(PRODUCT_TYPE_DRAFT_KEY_NOT_SET, productTypeDraft.getName());
        assertThat(errorCallBackMessages).containsExactly(errorMessage);
        assertThat(errorCallBackExceptions).hasOnlyOneElementSatisfying(throwable -> {
            assertThat(throwable).isInstanceOf(SyncException.class);
            assertThat(throwable).hasMessage(errorMessage);
        });
    }

    @Test
    void validateBatch_WithADraftWithAValidNestedReference_ShouldNotResultInAnError() {
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(StringAttributeType.of(), "foo", ofEnglish("koko"), true)
            .build();

        final AttributeDefinitionDraft nestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(NestedAttributeType.of(ProductType.referenceOfId("x")), "nested", ofEnglish("koko"), true)
            .build();

        final List<AttributeDefinitionDraft> attributes = new ArrayList<>();
        attributes.add(attributeDefinitionDraft);
        attributes.add(nestedTypeAttrDefDraft);
        attributes.add(null);

        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("mainProductType", "foo", "foo", attributes)
            .build();

        final ProductTypeBatchProcessor batchProcessor =
            new ProductTypeBatchProcessor(singletonList(productTypeDraft), productTypeSync);
        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).containsExactlyInAnyOrder("x");
        assertThat(batchProcessor.getValidDrafts()).containsExactly(productTypeDraft);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    void validateBatch_WithADraftWithAnInvalidNestedReference_ShouldResultInAnError() {
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(StringAttributeType.of(), "foo", ofEnglish("koko"), true)
            .build();

        final AttributeDefinitionDraft invalidNestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(NestedAttributeType.of(ProductType.referenceOfId("")), "invalidNested", ofEnglish("koko"), true)
            .build();

        final List<AttributeDefinitionDraft> attributes = new ArrayList<>();
        attributes.add(attributeDefinitionDraft);
        attributes.add(invalidNestedTypeAttrDefDraft);

        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("foo", "foo", "foo", attributes)
            .build();

        final ProductTypeBatchProcessor batchProcessor =
            new ProductTypeBatchProcessor(singletonList(productTypeDraft), productTypeSync);
        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();

        final String expectedExceptionMessage =
            format(PRODUCT_TYPE_HAS_INVALID_REFERENCES, productTypeDraft.getKey(), "[invalidNested]");
        assertThat(errorCallBackMessages).containsExactly(expectedExceptionMessage);
        assertThat(errorCallBackExceptions)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isInstanceOf(SyncException.class);
                assertThat(throwable.getMessage()).isEqualTo(expectedExceptionMessage);
                assertThat(throwable.getCause()).isInstanceOf(InvalidReferenceException.class);
                assertThat(throwable.getCause().getMessage()).isEqualTo(BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER);
            });
    }

    @Test
    void validateBatch_WithADraftWithMultipleInvalidNestedReferences_ShouldResultInAnError() {
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(StringAttributeType.of(), "foo", ofEnglish("koko"), true)
            .build();

        final AttributeDefinitionDraft nestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(NestedAttributeType.of(ProductType.referenceOfId("foo")), "validNested", ofEnglish("koko"), true)
            .build();

        final AttributeDefinitionDraft setOfNestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(NestedAttributeType.of(ProductType.referenceOfId("foo"))), "setOfNested",
                ofEnglish("koko"), true)
            .build();

        final AttributeDefinitionDraft invalidNestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(NestedAttributeType.of(ProductType.referenceOfId("")), "invalidNested", ofEnglish("koko"), true)
            .build();

        final AttributeDefinitionDraft setOfInvalidNestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(NestedAttributeType.of(ProductType.referenceOfId(null))), "setOfInvalidNested",
                ofEnglish("koko"), true)
            .build();

        final List<AttributeDefinitionDraft> attributes = new ArrayList<>();
        attributes.add(attributeDefinitionDraft);
        attributes.add(nestedTypeAttrDefDraft);
        attributes.add(setOfNestedTypeAttrDefDraft);
        attributes.add(invalidNestedTypeAttrDefDraft);
        attributes.add(setOfInvalidNestedTypeAttrDefDraft);
        attributes.add(null);

        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("foo", "foo", "foo", attributes)
            .build();

        final ProductTypeBatchProcessor batchProcessor =
            new ProductTypeBatchProcessor(singletonList(productTypeDraft), productTypeSync);
        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).isEmpty();
        assertThat(batchProcessor.getValidDrafts()).isEmpty();
        final String expectedExceptionMessage = format(PRODUCT_TYPE_HAS_INVALID_REFERENCES,
            productTypeDraft.getKey(), "[invalidNested, setOfInvalidNested]");
        assertThat(errorCallBackMessages).containsExactly(expectedExceptionMessage);
        assertThat(errorCallBackExceptions)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isInstanceOf(SyncException.class);
                assertThat(throwable.getMessage()).isEqualTo(expectedExceptionMessage);
                assertThat(throwable.getCause()).isInstanceOf(InvalidReferenceException.class);
                assertThat(throwable.getCause().getMessage()).isEqualTo(BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER);
            });
    }

    @Test
    void validateBatch_WithMixOfValidAndInvalidDrafts_ShouldValidateCorrectly() {
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(StringAttributeType.of(), "foo", ofEnglish("koko"), true)
            .build();

        final AttributeDefinitionDraft nestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(NestedAttributeType.of(ProductType.referenceOfId("x")), "validNested", ofEnglish("koko"), true)
            .build();

        final AttributeDefinitionDraft setOfNestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(NestedAttributeType.of(ProductType.referenceOfId("y"))), "setOfNested",
                ofEnglish("koko"), true)
            .build();

        final AttributeDefinitionDraft invalidNestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(NestedAttributeType.of(ProductType.referenceOfId("")), "invalidNested", ofEnglish("koko"), true)
            .build();

        final AttributeDefinitionDraft setOfInvalidNestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(NestedAttributeType.of(ProductType.referenceOfId(""))), "setOfInvalidNested",
                ofEnglish("koko"), true)
            .build();

        final List<AttributeDefinitionDraft> attributes = new ArrayList<>();
        attributes.add(attributeDefinitionDraft);
        attributes.add(nestedTypeAttrDefDraft);
        attributes.add(setOfNestedTypeAttrDefDraft);
        attributes.add(invalidNestedTypeAttrDefDraft);
        attributes.add(setOfInvalidNestedTypeAttrDefDraft);

        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("foo", "foo", "foo", attributes)
            .build();

        final ProductTypeDraft productTypeDraftWithEmptyKey = ProductTypeDraftBuilder
            .of("", "foo", "foo", attributes)
            .build();

        final ProductTypeDraft validProductTypeDraftWithReferences = ProductTypeDraftBuilder
            .of("bar", "bar", "bar",
                asList(attributeDefinitionDraft, nestedTypeAttrDefDraft, setOfNestedTypeAttrDefDraft))
            .build();

        final ProductTypeDraft draftWithEmptyAttributes = ProductTypeDraftBuilder
            .of("bar", "bar", "bar", emptyList())
            .build();

        final ProductTypeDraft draftWithNullAttributes = ProductTypeDraftBuilder
            .of("bar", "bar", "bar", null)
            .build();

        final List<ProductTypeDraft> productTypeDrafts = new ArrayList<>();
        productTypeDrafts.add(productTypeDraft);
        productTypeDrafts.add(null);
        productTypeDrafts.add(productTypeDraftWithEmptyKey);
        productTypeDrafts.add(validProductTypeDraftWithReferences);
        productTypeDrafts.add(draftWithEmptyAttributes);
        productTypeDrafts.add(draftWithNullAttributes);

        final ProductTypeBatchProcessor batchProcessor =
            new ProductTypeBatchProcessor(productTypeDrafts, productTypeSync);
        batchProcessor.validateBatch();

        assertThat(batchProcessor.getKeysToCache()).containsExactlyInAnyOrder("x", "y");
        assertThat(batchProcessor.getValidDrafts()).containsExactlyInAnyOrder(validProductTypeDraftWithReferences,
            draftWithEmptyAttributes, draftWithNullAttributes);

        final String expectedExceptionMessage = format(PRODUCT_TYPE_HAS_INVALID_REFERENCES,
            productTypeDraft.getKey(), "[invalidNested, setOfInvalidNested]");
        assertThat(errorCallBackMessages).containsExactlyInAnyOrderElementsOf(
            asList(expectedExceptionMessage, PRODUCT_TYPE_DRAFT_IS_NULL,
                format(PRODUCT_TYPE_DRAFT_KEY_NOT_SET, productTypeDraftWithEmptyKey.getName())));

        final Predicate<Throwable> invalidReferencePredicate = throwable ->
            expectedExceptionMessage.equals(throwable.getMessage())
                && BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER.equals(throwable.getCause().getMessage());

        final Condition<Throwable> invalidReferenceCondition = new Condition<>(invalidReferencePredicate,
            "ReferenceResolutionException: "
                + "ProductTypeDraft with Key 'foo' has invalid references on attributeDraft with name 'nested'.");

        final Predicate<Throwable> nullDraftPredicate = throwable ->
            PRODUCT_TYPE_DRAFT_IS_NULL.equals(throwable.getMessage())
                && throwable instanceof SyncException;

        final Condition<Throwable> nullDraftCondition = new Condition<>(nullDraftPredicate,
            "SyncException: ProductTypeDraft is null.");

        final Predicate<Throwable> blankProductTypeKeyPredicate = throwable ->
            format(PRODUCT_TYPE_DRAFT_KEY_NOT_SET, productTypeDraftWithEmptyKey.getName())
                .equals(throwable.getMessage()) && throwable instanceof SyncException;

        final Condition<Throwable> blankKeyCondition = new Condition<>(blankProductTypeKeyPredicate,
            "SyncException: ProductTypeDraft has blank key.");


        assertThat(errorCallBackExceptions)
            .hasSize(3)
            .haveExactly(1, invalidReferenceCondition)
            .haveExactly(1, nullDraftCondition)
            .haveExactly(1, blankKeyCondition);
    }
}
