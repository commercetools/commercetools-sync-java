package com.commercetools.sync.producttypes.helpers;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.*;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.services.ProductTypeService;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AttributeDefinitionReferenceResolverTest {

  @Test
  void resolveReferences_WithNoNestedTypeReferences_ShouldNotResolveReferences() {
    // preparation
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(AttributeTextTypeBuilder.of().build())
            .name("foo")
            .label(ofEnglish("foo"))
            .isRequired(true)
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
        new AttributeDefinitionReferenceResolver(
            syncOptions, Mockito.mock(ProductTypeService.class));

    // test and assertion
    assertThat(attributeDefinitionReferenceResolver.resolveReferences(attributeDefinitionDraft))
        .isCompletedWithValue(attributeDefinitionDraft);
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

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
        new AttributeDefinitionReferenceResolver(syncOptions, productTypeService);

    final AttributeNestedType expectedResolvedNestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("foo").build())
            .build();
    final AttributeDefinitionDraft expectedResolvedAttrDef =
        AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft)
            .type(expectedResolvedNestedAttributeType)
            .build();

    // test and assertion
    assertThat(attributeDefinitionReferenceResolver.resolveReferences(attributeDefinitionDraft))
        .isCompletedWithValue(expectedResolvedAttrDef);
  }

  @Test
  void
      resolveReferences_WithOneNestedTypeWithNonExistingProductTypeReference_ShouldNotResolveReferences() {
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

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
        new AttributeDefinitionReferenceResolver(syncOptions, productTypeService);

    // test and assertion
    assertThat(attributeDefinitionReferenceResolver.resolveReferences(attributeDefinitionDraft))
        .isCompletedWithValue(attributeDefinitionDraft);
  }

  @Test
  void
      resolveReferences_WithOneNestedTypeWithInvalidProductTypeReference_ShouldNotResolveReferences() {
    // preparation
    final AttributeNestedType nestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("").build())
            .build();
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(nestedAttributeType)
            .name("foo")
            .label(ofEnglish("foo"))
            .isRequired(true)
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
        new AttributeDefinitionReferenceResolver(syncOptions, productTypeService);

    // test and assertion
    assertThat(attributeDefinitionReferenceResolver.resolveReferences(attributeDefinitionDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            "Failed to resolve references on attribute definition with name 'foo'");
  }

  @Test
  void resolveReferences_WithSetOfNonNestedType_ShouldResolveReferences() {
    // preparation
    final AttributeSetType setAttributeType =
        AttributeSetTypeBuilder.of().elementType(AttributeTextTypeBuilder.of().build()).build();

    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(setAttributeType)
            .name("foo")
            .label(ofEnglish("foo"))
            .isRequired(true)
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
        new AttributeDefinitionReferenceResolver(syncOptions, productTypeService);

    // test and assertion
    assertThat(attributeDefinitionReferenceResolver.resolveReferences(attributeDefinitionDraft))
        .isCompletedWithValue(attributeDefinitionDraft);
  }

  @Test
  void
      resolveReferences_WithSetOfNestedTypeWithExistingProductTypeReference_ShouldResolveReferences() {
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

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
        new AttributeDefinitionReferenceResolver(syncOptions, productTypeService);

    final AttributeNestedType expectedResolvedNestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("foo").build())
            .build();
    final AttributeSetType expectedResolvedSetAttributeType =
        AttributeSetTypeBuilder.of().elementType(expectedResolvedNestedAttributeType).build();
    final AttributeDefinitionDraft expectedResolvedAttrDef =
        AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft)
            .type(expectedResolvedSetAttributeType)
            .build();

    // test and assertion
    assertThat(attributeDefinitionReferenceResolver.resolveReferences(attributeDefinitionDraft))
        .isCompletedWithValue(expectedResolvedAttrDef);
  }

  @Test
  void
      resolveReferences_WithSetOfNestedTypeWithInvalidProductTypeReference_ShouldNotResolveReferences() {
    // preparation
    final AttributeNestedType nestedAttributeType =
        AttributeNestedTypeBuilder.of()
            .typeReference(ProductTypeReferenceBuilder.of().id("").build())
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

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
        new AttributeDefinitionReferenceResolver(syncOptions, productTypeService);

    // test and assertion
    assertThat(attributeDefinitionReferenceResolver.resolveReferences(attributeDefinitionDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            "Failed to resolve references on attribute definition with name 'foo'");
  }
}
