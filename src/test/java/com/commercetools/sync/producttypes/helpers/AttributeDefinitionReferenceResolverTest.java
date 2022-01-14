package com.commercetools.sync.producttypes.helpers;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AttributeDefinitionReferenceResolverTest {

  @Test
  void resolveReferences_WithNoNestedTypeReferences_ShouldNotResolveReferences() {
    // preparation
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(StringAttributeType.of(), "foo", ofEnglish("foo"), true)
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
        new AttributeDefinitionReferenceResolver(syncOptions, mock(ProductTypeService.class));

    // test and assertion
    assertThat(attributeDefinitionReferenceResolver.resolveReferences(attributeDefinitionDraft))
        .isCompletedWithValue(attributeDefinitionDraft);
  }

  @Test
  void
      resolveReferences_WithOneNestedTypeWithExistingProductTypeReference_ShouldResolveReferences() {
    // preparation
    final NestedAttributeType nestedAttributeType =
        NestedAttributeType.of(ProductType.reference("x"));
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(nestedAttributeType, "foo", ofEnglish("foo"), true)
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
        new AttributeDefinitionReferenceResolver(syncOptions, productTypeService);

    final NestedAttributeType expectedResolvedNestedAttributeType =
        NestedAttributeType.of(ProductType.referenceOfId("foo"));
    final AttributeDefinitionDraft expectedResolvedAttrDef =
        AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft)
            .attributeType(expectedResolvedNestedAttributeType)
            .build();

    // test and assertion
    assertThat(attributeDefinitionReferenceResolver.resolveReferences(attributeDefinitionDraft))
        .isCompletedWithValue(expectedResolvedAttrDef);
  }

  @Test
  void
      resolveReferences_WithOneNestedTypeWithNonExistingProductTypeReference_ShouldNotResolveReferences() {
    // preparation
    final NestedAttributeType nestedAttributeType =
        NestedAttributeType.of(ProductType.reference("x"));
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(nestedAttributeType, "foo", ofEnglish("foo"), true)
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

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
    final NestedAttributeType nestedAttributeType =
        NestedAttributeType.of(ProductType.reference(""));
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(nestedAttributeType, "foo", ofEnglish("foo"), true)
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

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
    final SetAttributeType setAttributeType = SetAttributeType.of(StringAttributeType.of());

    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(setAttributeType, "foo", ofEnglish("foo"), true).build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

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
    final NestedAttributeType nestedAttributeType =
        NestedAttributeType.of(ProductType.reference("x"));
    final SetAttributeType setAttributeType = SetAttributeType.of(nestedAttributeType);

    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(setAttributeType, "foo", ofEnglish("foo"), true).build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

    final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
        new AttributeDefinitionReferenceResolver(syncOptions, productTypeService);

    final NestedAttributeType expectedResolvedNestedAttributeType =
        NestedAttributeType.of(ProductType.referenceOfId("foo"));
    final SetAttributeType expectedResolvedSetAttributeType =
        SetAttributeType.of(expectedResolvedNestedAttributeType);
    final AttributeDefinitionDraft expectedResolvedAttrDef =
        AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft)
            .attributeType(expectedResolvedSetAttributeType)
            .build();

    // test and assertion
    assertThat(attributeDefinitionReferenceResolver.resolveReferences(attributeDefinitionDraft))
        .isCompletedWithValue(expectedResolvedAttrDef);
  }

  @Test
  void
      resolveReferences_WithSetOfNestedTypeWithInvalidProductTypeReference_ShouldNotResolveReferences() {
    // preparation
    final NestedAttributeType nestedAttributeType =
        NestedAttributeType.of(ProductType.reference(""));
    final SetAttributeType setAttributeType = SetAttributeType.of(nestedAttributeType);
    final AttributeDefinitionDraft attributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(setAttributeType, "foo", ofEnglish("foo"), true).build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

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
