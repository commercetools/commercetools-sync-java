package com.commercetools.sync.producttypes.helpers;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.sync.commons.exceptions.InvalidReferenceException;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductTypeBatchValidatorTest {
  private List<String> errorCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private ProductTypeSyncOptions syncOptions;
  private ProductTypeSyncStatistics syncStatistics;

  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    syncOptions =
        ProductTypeSyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception);
                })
            .build();
    syncStatistics = new ProductTypeSyncStatistics();
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
    final Set<ProductTypeDraft> validDrafts = getValidDrafts(emptyList());

    assertThat(validDrafts).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithNullProductTypeDraft_ShouldHaveValidationErrorAndEmptyResult() {
    final Set<ProductTypeDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(ProductTypeBatchValidator.PRODUCT_TYPE_DRAFT_IS_NULL);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithProductTypeDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
    final ProductTypeDraft productTypeDraft = mock(ProductTypeDraft.class);
    final Set<ProductTypeDraft> validDrafts =
        getValidDrafts(Collections.singletonList(productTypeDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                ProductTypeBatchValidator.PRODUCT_TYPE_DRAFT_KEY_NOT_SET,
                productTypeDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithProductTypeDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
    final ProductTypeDraft productTypeDraft = mock(ProductTypeDraft.class);
    when(productTypeDraft.getKey()).thenReturn(EMPTY);
    final Set<ProductTypeDraft> validDrafts =
        getValidDrafts(Collections.singletonList(productTypeDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                ProductTypeBatchValidator.PRODUCT_TYPE_DRAFT_KEY_NOT_SET,
                productTypeDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithADraftWithAValidNestedReference_ShouldNotResultInAnError() {
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(AttributeTypeBuilder::textBuilder)
            .name("foo")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final AttributeDefinitionDraft nestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(
                            productTypeReferenceBuilder -> productTypeReferenceBuilder.id("x")))
            .name("nested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final List<AttributeDefinitionDraft> attributes = new ArrayList<>();
    attributes.add(attributeDefinitionDraft);
    attributes.add(nestedTypeAttrDefDraft);
    attributes.add(null);

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("mainProductType")
            .name("foo")
            .description("foo")
            .attributes(attributes)
            .build();

    final ProductTypeBatchValidator batchValidator =
        new ProductTypeBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ProductTypeDraft>, Set<String>> pair =
        batchValidator.validateAndCollectReferencedKeys(singletonList(productTypeDraft));

    assertThat(pair.getLeft()).containsExactly(productTypeDraft);
    assertThat(pair.getRight()).containsExactlyInAnyOrder("x");
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithADraftWithAnInvalidNestedReference_ShouldResultInAnError() {
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(AttributeTypeBuilder::textBuilder)
            .name("foo")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final AttributeDefinitionDraft invalidNestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(
                            productTypeReferenceBuilder -> productTypeReferenceBuilder.id("")))
            .name("invalidNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final List<AttributeDefinitionDraft> attributes = new ArrayList<>();
    attributes.add(attributeDefinitionDraft);
    attributes.add(invalidNestedTypeAttrDefDraft);

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("foo")
            .description("foo")
            .attributes(attributes)
            .build();

    final ProductTypeBatchValidator batchValidator =
        new ProductTypeBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ProductTypeDraft>, Set<String>> pair =
        batchValidator.validateAndCollectReferencedKeys(singletonList(productTypeDraft));

    assertThat(pair.getLeft()).isEmpty();
    assertThat(pair.getRight()).isEmpty();

    final String expectedExceptionMessage =
        String.format(
            ProductTypeBatchValidator.PRODUCT_TYPE_HAS_INVALID_REFERENCES,
            productTypeDraft.getKey(),
            "[invalidNested]");
    assertThat(errorCallBackMessages).containsExactly(expectedExceptionMessage);
    assertThat(errorCallBackExceptions)
        .singleElement()
        .matches(
            throwable -> {
              assertThat(throwable).isInstanceOf(SyncException.class);
              assertThat(throwable.getMessage()).isEqualTo(expectedExceptionMessage);
              assertThat(throwable.getCause()).isInstanceOf(InvalidReferenceException.class);
              assertThat(throwable.getCause().getMessage())
                  .isEqualTo(BaseReferenceResolver.BLANK_ID_VALUE_ON_REFERENCE);
              return true;
            });
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithADraftWithMultipleInvalidNestedReferences_ShouldResultInAnError() {
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(AttributeTypeBuilder::textBuilder)
            .name("foo")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final AttributeDefinitionDraft nestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(
                            productTypeReferenceBuilder -> productTypeReferenceBuilder.id("foo")))
            .name("validNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final AttributeDefinitionDraft setOfNestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(
                                        productTypeReferenceBuilder ->
                                            productTypeReferenceBuilder.id("foo"))))
            .name("setOfNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final AttributeDefinitionDraft invalidNestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(
                            productTypeReferenceBuilder -> productTypeReferenceBuilder.id("")))
            .name("invalidNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final AttributeDefinitionDraft setOfInvalidNestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(
                                        productTypeReferenceBuilder ->
                                            productTypeReferenceBuilder.id(""))))
            .name("setOfInvalidNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final List<AttributeDefinitionDraft> attributes = new ArrayList<>();
    attributes.add(attributeDefinitionDraft);
    attributes.add(nestedTypeAttrDefDraft);
    attributes.add(setOfNestedTypeAttrDefDraft);
    attributes.add(invalidNestedTypeAttrDefDraft);
    attributes.add(setOfInvalidNestedTypeAttrDefDraft);
    attributes.add(null);

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("foo")
            .description("foo")
            .attributes(attributes)
            .build();

    final ProductTypeBatchValidator batchValidator =
        new ProductTypeBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ProductTypeDraft>, Set<String>> pair =
        batchValidator.validateAndCollectReferencedKeys(singletonList(productTypeDraft));

    assertThat(pair.getLeft()).isEmpty();
    assertThat(pair.getRight()).isEmpty();

    final String expectedExceptionMessage =
        String.format(
            ProductTypeBatchValidator.PRODUCT_TYPE_HAS_INVALID_REFERENCES,
            productTypeDraft.getKey(),
            "[invalidNested, setOfInvalidNested]");
    assertThat(errorCallBackMessages).containsExactly(expectedExceptionMessage);
    assertThat(errorCallBackExceptions)
        .singleElement()
        .matches(
            throwable -> {
              assertThat(throwable).isInstanceOf(SyncException.class);
              assertThat(throwable.getMessage()).isEqualTo(expectedExceptionMessage);
              assertThat(throwable.getCause()).isInstanceOf(InvalidReferenceException.class);
              assertThat(throwable.getCause().getMessage())
                  .isEqualTo(BaseReferenceResolver.BLANK_ID_VALUE_ON_REFERENCE);
              return true;
            });
  }

  @Test
  void validateAndCollectReferencedKeys_WithMixOfValidAndInvalidDrafts_ShouldValidateCorrectly() {
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(AttributeTypeBuilder::textBuilder)
            .name("foo")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final AttributeDefinitionDraft nestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(
                            productTypeReferenceBuilder -> productTypeReferenceBuilder.id("x")))
            .name("validNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final AttributeDefinitionDraft setOfNestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(
                                        productTypeReferenceBuilder ->
                                            productTypeReferenceBuilder.id("y"))))
            .name("setOfNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final AttributeDefinitionDraft invalidNestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(
                            productTypeReferenceBuilder -> productTypeReferenceBuilder.id("")))
            .name("invalidNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final AttributeDefinitionDraft setOfInvalidNestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(
                                        productTypeReferenceBuilder ->
                                            productTypeReferenceBuilder.id(""))))
            .name("setOfInvalidNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final List<AttributeDefinitionDraft> attributes = new ArrayList<>();
    attributes.add(attributeDefinitionDraft);
    attributes.add(nestedTypeAttrDefDraft);
    attributes.add(setOfNestedTypeAttrDefDraft);
    attributes.add(invalidNestedTypeAttrDefDraft);
    attributes.add(setOfInvalidNestedTypeAttrDefDraft);

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("foo")
            .description("foo")
            .attributes(attributes)
            .build();

    final ProductTypeDraft productTypeDraftWithEmptyKey =
        ProductTypeDraftBuilder.of()
            .key("")
            .name("foo")
            .description("foo")
            .attributes(attributes)
            .build();

    final ProductTypeDraft validProductTypeDraftWithReferences =
        ProductTypeDraftBuilder.of()
            .key("bar")
            .name("bar")
            .description("bar")
            .attributes(
                asList(
                    attributeDefinitionDraft, nestedTypeAttrDefDraft, setOfNestedTypeAttrDefDraft))
            .build();

    final ProductTypeDraft draftWithEmptyAttributes =
        ProductTypeDraftBuilder.of()
            .key("bar")
            .name("bar")
            .description("bar")
            .attributes(emptyList())
            .build();

    final ProductTypeDraft draftWithNullAttributes =
        ProductTypeDraftBuilder.of()
            .key("bar")
            .name("bar")
            .description("bar")
            .attributes((AttributeDefinitionDraft) null)
            .build();

    final List<ProductTypeDraft> productTypeDrafts = new ArrayList<>();
    productTypeDrafts.add(productTypeDraft);
    productTypeDrafts.add(null);
    productTypeDrafts.add(productTypeDraftWithEmptyKey);
    productTypeDrafts.add(validProductTypeDraftWithReferences);
    productTypeDrafts.add(draftWithEmptyAttributes);
    productTypeDrafts.add(draftWithNullAttributes);

    final ProductTypeBatchValidator batchValidator =
        new ProductTypeBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ProductTypeDraft>, Set<String>> pair =
        batchValidator.validateAndCollectReferencedKeys(productTypeDrafts);

    assertThat(pair.getLeft())
        .containsExactlyInAnyOrder(
            validProductTypeDraftWithReferences, draftWithEmptyAttributes, draftWithNullAttributes);
    assertThat(pair.getRight()).containsExactlyInAnyOrder("x", "y");

    final String expectedExceptionMessage =
        String.format(
            ProductTypeBatchValidator.PRODUCT_TYPE_HAS_INVALID_REFERENCES,
            productTypeDraft.getKey(),
            "[invalidNested, setOfInvalidNested]");
    assertThat(errorCallBackMessages)
        .containsExactlyInAnyOrderElementsOf(
            asList(
                expectedExceptionMessage,
                ProductTypeBatchValidator.PRODUCT_TYPE_DRAFT_IS_NULL,
                String.format(
                    ProductTypeBatchValidator.PRODUCT_TYPE_DRAFT_KEY_NOT_SET,
                    productTypeDraftWithEmptyKey.getName())));

    final Predicate<Throwable> invalidReferencePredicate =
        throwable ->
            expectedExceptionMessage.equals(throwable.getMessage())
                && BaseReferenceResolver.BLANK_ID_VALUE_ON_REFERENCE.equals(
                    throwable.getCause().getMessage());

    final Condition<Throwable> invalidReferenceCondition =
        new Condition<>(
            invalidReferencePredicate,
            "ReferenceResolutionException: "
                + "ProductTypeDraft with Key 'foo' has invalid references on attributeDraft with name 'nested'.");

    final Predicate<Throwable> nullDraftPredicate =
        throwable ->
            ProductTypeBatchValidator.PRODUCT_TYPE_DRAFT_IS_NULL.equals(throwable.getMessage())
                && throwable instanceof SyncException;

    final Condition<Throwable> nullDraftCondition =
        new Condition<>(nullDraftPredicate, "SyncException: ProductTypeDraft is null.");

    final Predicate<Throwable> blankProductTypeKeyPredicate =
        throwable ->
            String.format(
                        ProductTypeBatchValidator.PRODUCT_TYPE_DRAFT_KEY_NOT_SET,
                        productTypeDraftWithEmptyKey.getName())
                    .equals(throwable.getMessage())
                && throwable instanceof SyncException;

    final Condition<Throwable> blankKeyCondition =
        new Condition<>(
            blankProductTypeKeyPredicate, "SyncException: ProductTypeDraft has blank key.");

    assertThat(errorCallBackExceptions)
        .hasSize(3)
        .haveExactly(1, invalidReferenceCondition)
        .haveExactly(1, nullDraftCondition)
        .haveExactly(1, blankKeyCondition);
  }

  @Nonnull
  private Set<ProductTypeDraft> getValidDrafts(
      @Nonnull final List<ProductTypeDraft> productTypeDrafts) {
    final ProductTypeBatchValidator batchValidator =
        new ProductTypeBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ProductTypeDraft>, Set<String>> pair =
        batchValidator.validateAndCollectReferencedKeys(productTypeDrafts);
    return pair.getLeft();
  }
}
