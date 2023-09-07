package com.commercetools.sync.producttypes.helpers;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeNestedType;
import com.commercetools.api.models.product_type.AttributeNestedTypeBuilder;
import com.commercetools.api.models.product_type.AttributeSetType;
import com.commercetools.api.models.product_type.AttributeSetTypeBuilder;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeReferenceBuilder;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.services.ProductTypeService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class ProductTypeReferenceResolverTest {

  @Test
  void resolveReferences_WithNullAttributes_ShouldNotResolveReferences() {
    // preparation
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("foo")
            .description("desc")
            .attributes((List<AttributeDefinitionDraft>) null)
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final ProductTypeReferenceResolver productTypeReferenceResolver =
        new ProductTypeReferenceResolver(syncOptions, productTypeService);

    final ProductTypeDraft expectedResolvedProductTypeDraft =
        ProductTypeDraftBuilder.of(productTypeDraft).build();

    // test and assertion
    assertThat(
            productTypeReferenceResolver
                .resolveReferences(productTypeDraft)
                .toCompletableFuture()
                .join())
        .isEqualTo(expectedResolvedProductTypeDraft);
  }

  @Test
  void resolveReferences_WithNoAttributes_ShouldNotResolveReferences() {
    // preparation
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("foo")
            .description("desc")
            .attributes(emptyList())
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final ProductTypeReferenceResolver productTypeReferenceResolver =
        new ProductTypeReferenceResolver(syncOptions, productTypeService);

    final ProductTypeDraft expectedResolvedProductTypeDraft =
        ProductTypeDraftBuilder.of(productTypeDraft).attributes(emptyList()).build();

    // test and assertion
    assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
        .isCompletedWithValue(expectedResolvedProductTypeDraft);
  }

  @Test
  void resolveReferences_WithNoNestedTypeReferences_ShouldResolveReferences() {
    // preparation
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(AttributeTypeBuilder::textBuilder)
            .name("string attr")
            .label(ofEnglish("string attr label"))
            .isRequired(true)
            .build();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("foo")
            .description("desc")
            .attributes(singletonList(attributeDefinitionDraft))
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final ProductTypeReferenceResolver productTypeReferenceResolver =
        new ProductTypeReferenceResolver(syncOptions, productTypeService);

    final ProductTypeDraft expectedResolvedProductTypeDraft =
        ProductTypeDraftBuilder.of(productTypeDraft)
            .attributes(singletonList(attributeDefinitionDraft))
            .build();

    // test and assertion
    assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
        .isCompletedWithValue(expectedResolvedProductTypeDraft);
  }

  @Test
  void
      resolveReferences_WithOneNestedTypeWithExistingProductTypeReference_ShouldResolveReferences() {
    // preparation
    final AttributeNestedType nestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("x").build())
            .build();
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(nestedAttributeType)
            .name("foo")
            .label(ofEnglish("foo"))
            .isRequired(true)
            .build();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("foo")
            .description("desc")
            .attributes(singletonList(attributeDefinitionDraft))
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final ProductTypeReferenceResolver productTypeReferenceResolver =
        new ProductTypeReferenceResolver(syncOptions, productTypeService);

    final AttributeNestedType expectedResolvedNestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("foo").build())
            .build();
    final AttributeDefinitionDraft expectedResolvedAttrDef =
        AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft)
            .type(expectedResolvedNestedAttributeType)
            .build();
    final ProductTypeDraft expectedResolvedProductTypeDraft =
        ProductTypeDraftBuilder.of(productTypeDraft)
            .attributes(singletonList(expectedResolvedAttrDef))
            .build();

    // test and assertion
    assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
        .isCompletedWithValue(expectedResolvedProductTypeDraft);
  }

  @Test
  void
      resolveReferences_WithManyNestedTypeWithExistingProductTypeReference_ShouldResolveReferences() {
    // preparation
    final AttributeNestedType nestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("x").build())
            .build();
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(nestedAttributeType)
            .name("foo")
            .label(ofEnglish("foo"))
            .isRequired(true)
            .build();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("foo")
            .description("desc")
            .attributes(asList(attributeDefinitionDraft, attributeDefinitionDraft))
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final ProductTypeReferenceResolver productTypeReferenceResolver =
        new ProductTypeReferenceResolver(syncOptions, productTypeService);

    final AttributeNestedType expectedResolvedNestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("foo").build())
            .build();
    final AttributeDefinitionDraft expectedResolvedAttrDef =
        AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft)
            .type(expectedResolvedNestedAttributeType)
            .build();
    final ProductTypeDraft expectedResolvedProductTypeDraft =
        ProductTypeDraftBuilder.of(productTypeDraft)
            .attributes(asList(expectedResolvedAttrDef, expectedResolvedAttrDef))
            .build();

    // test and assertion
    assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
        .isCompletedWithValue(expectedResolvedProductTypeDraft);
  }

  @Test
  void
      resolveReferences_WithOneSetOfNestedTypeWithExistingProductTypeReference_ShouldResolveReferences() {
    // preparation
    final AttributeNestedType nestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("x").build())
            .build();
    final AttributeSetType setAttributeType =
        AttributeSetTypeBuilder.of().elementType(nestedAttributeType).build();
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(setAttributeType)
            .name("foo")
            .label(ofEnglish("foo"))
            .isRequired(true)
            .build();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("foo")
            .description("desc")
            .attributes(singletonList(attributeDefinitionDraft))
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final ProductTypeReferenceResolver productTypeReferenceResolver =
        new ProductTypeReferenceResolver(syncOptions, productTypeService);

    final AttributeNestedType expectedResolvedNestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("foo").build())
            .build();
    final AttributeSetType expectedSetAttributeType =
        AttributeSetTypeBuilder.of().elementType(expectedResolvedNestedAttributeType).build();

    final AttributeDefinitionDraft expectedResolvedAttrDef =
        AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft)
            .type(expectedSetAttributeType)
            .build();
    final ProductTypeDraft expectedResolvedProductTypeDraft =
        ProductTypeDraftBuilder.of(productTypeDraft)
            .attributes(singletonList(expectedResolvedAttrDef))
            .build();

    // test and assertion
    assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
        .isCompletedWithValue(expectedResolvedProductTypeDraft);
  }

  @Test
  void
      resolveReferences_WithNestedAndSetOfNestedTypeWithExistingProductTypeReference_ShouldResolveReferences() {
    // preparation
    final AttributeNestedType nestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("x").build())
            .build();
    final AttributeSetType setAttributeType =
        AttributeSetTypeBuilder.of().elementType(nestedAttributeType).build();
    final AttributeDefinitionDraft setAttributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(setAttributeType)
            .name("foo")
            .label(ofEnglish("foo"))
            .isRequired(true)
            .build();
    final AttributeDefinitionDraft nestedAttributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(nestedAttributeType)
            .name("foo")
            .label(ofEnglish("foo"))
            .isRequired(true)
            .build();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("foo")
            .description("desc")
            .attributes(asList(setAttributeDefinitionDraft, nestedAttributeDefinitionDraft))
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final ProductTypeReferenceResolver productTypeReferenceResolver =
        new ProductTypeReferenceResolver(syncOptions, productTypeService);

    final AttributeNestedType expectedResolvedNestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("foo").build())
            .build();
    final AttributeSetType expectedResolvedSetAttributeType =
        AttributeSetTypeBuilder.of().elementType(expectedResolvedNestedAttributeType).build();

    final AttributeDefinitionDraft expectedResolvedSetAttrDef =
        AttributeDefinitionDraftBuilder.of(setAttributeDefinitionDraft)
            .type(expectedResolvedSetAttributeType)
            .build();
    final AttributeDefinitionDraft expectedResolvedNestedAttrDef =
        AttributeDefinitionDraftBuilder.of(nestedAttributeDefinitionDraft)
            .type(expectedResolvedNestedAttributeType)
            .build();

    final ProductTypeDraft expectedResolvedProductTypeDraft =
        ProductTypeDraftBuilder.of(productTypeDraft)
            .attributes(asList(expectedResolvedSetAttrDef, expectedResolvedNestedAttrDef))
            .build();

    // test and assertion
    assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
        .isCompletedWithValue(expectedResolvedProductTypeDraft);
  }
}
