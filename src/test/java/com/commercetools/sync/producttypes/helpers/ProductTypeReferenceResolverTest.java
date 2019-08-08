package com.commercetools.sync.producttypes.helpers;

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
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.ProductTypeDraftDsl;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductTypeReferenceResolverTest {

    @Test
    void resolveReferences_WithNullAttributes_ShouldNotResolveReferences() {
        // preparation
        final ProductTypeDraft productTypeDraft =
            ProductTypeDraftBuilder.of("foo", "foo", "desc", null)
                                   .build();

        final ProductTypeSyncOptions syncOptions =
            ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

        final ProductTypeReferenceResolver productTypeReferenceResolver = new ProductTypeReferenceResolver(syncOptions,
            productTypeService);


        final ProductTypeDraftDsl expectedResolvedProductTypeDraft =
            ProductTypeDraftBuilder.of(productTypeDraft)
                                   .build();

        // test and assertion
        assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
            .isCompletedWithValue(expectedResolvedProductTypeDraft);
    }

    @Test
    void resolveReferences_WithNoAttributes_ShouldNotResolveReferences() {
        // preparation
        final ProductTypeDraft productTypeDraft =
            ProductTypeDraftBuilder.of("foo", "foo", "desc", emptyList())
                                   .build();

        final ProductTypeSyncOptions syncOptions =
            ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

        final ProductTypeReferenceResolver productTypeReferenceResolver = new ProductTypeReferenceResolver(syncOptions,
            productTypeService);


        final ProductTypeDraftDsl expectedResolvedProductTypeDraft =
            ProductTypeDraftBuilder.of(productTypeDraft)
                                   .attributes(emptyList())
                                   .build();

        // test and assertion
        assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
            .isCompletedWithValue(expectedResolvedProductTypeDraft);
    }

    @Test
    void resolveReferences_WithNoNestedTypeReferences_ShouldResolveReferences() {
        // preparation
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(StringAttributeType.of(), "string attr", ofEnglish("string attr label"), true)
            .build();

        final ProductTypeDraft productTypeDraft =
            ProductTypeDraftBuilder.of("foo", "foo", "desc", singletonList(attributeDefinitionDraft))
                                   .build();

        final ProductTypeSyncOptions syncOptions =
            ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

        final ProductTypeReferenceResolver productTypeReferenceResolver = new ProductTypeReferenceResolver(syncOptions,
            productTypeService);


        final ProductTypeDraftDsl expectedResolvedProductTypeDraft =
            ProductTypeDraftBuilder.of(productTypeDraft)
                                   .attributes(singletonList(attributeDefinitionDraft))
                                   .build();

        // test and assertion
        assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
            .isCompletedWithValue(expectedResolvedProductTypeDraft);
    }

    @Test
    void resolveReferences_WithOneNestedTypeWithExistingProductTypeReference_ShouldResolveReferences() {
        // preparation
        final NestedAttributeType nestedAttributeType = NestedAttributeType.of(ProductType.reference("x"));
        final AttributeDefinitionDraft attributeDefinitionDraft =
            AttributeDefinitionDraftBuilder.of(nestedAttributeType, "foo", ofEnglish("foo"), true)
                                           .build();

        final ProductTypeDraft productTypeDraft =
            ProductTypeDraftBuilder.of("foo", "foo", "desc",
                singletonList(attributeDefinitionDraft))
                                   .build();

        final ProductTypeSyncOptions syncOptions =
            ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

        final ProductTypeReferenceResolver productTypeReferenceResolver = new ProductTypeReferenceResolver(syncOptions,
            productTypeService);


        final NestedAttributeType expectedResolvedNestedAttributeType =
            NestedAttributeType.of(ProductType.referenceOfId("foo"));
        final AttributeDefinitionDraft expectedResolvedAttrDef =
            AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft)
                                           .attributeType(expectedResolvedNestedAttributeType)
                                           .build();
        final ProductTypeDraftDsl expectedResolvedProductTypeDraft =
            ProductTypeDraftBuilder.of(productTypeDraft)
                                   .attributes(singletonList(expectedResolvedAttrDef))
                                   .build();

        // test and assertion
        assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
            .isCompletedWithValue(expectedResolvedProductTypeDraft);
    }

    @Test
    void resolveReferences_WithManyNestedTypeWithExistingProductTypeReference_ShouldResolveReferences() {
        // preparation
        final NestedAttributeType nestedAttributeType = NestedAttributeType.of(ProductType.reference("x"));
        final AttributeDefinitionDraft attributeDefinitionDraft =
            AttributeDefinitionDraftBuilder.of(nestedAttributeType, "foo", ofEnglish("foo"), true)
                                           .build();

        final ProductTypeDraft productTypeDraft =
            ProductTypeDraftBuilder.of("foo", "foo", "desc",
                asList(attributeDefinitionDraft, attributeDefinitionDraft))
                                   .build();

        final ProductTypeSyncOptions syncOptions =
            ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

        final ProductTypeReferenceResolver productTypeReferenceResolver = new ProductTypeReferenceResolver(syncOptions,
            productTypeService);


        final NestedAttributeType expectedResolvedNestedAttributeType =
            NestedAttributeType.of(ProductType.referenceOfId("foo"));
        final AttributeDefinitionDraft expectedResolvedAttrDef =
            AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft)
                                           .attributeType(expectedResolvedNestedAttributeType)
                                           .build();
        final ProductTypeDraftDsl expectedResolvedProductTypeDraft =
            ProductTypeDraftBuilder.of(productTypeDraft)
                                   .attributes(asList(expectedResolvedAttrDef, expectedResolvedAttrDef))
                                   .build();

        // test and assertion
        assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
            .isCompletedWithValue(expectedResolvedProductTypeDraft);
    }

    @Test
    void resolveReferences_WithOneSetOfNestedTypeWithExistingProductTypeReference_ShouldResolveReferences() {
        // preparation
        final NestedAttributeType nestedAttributeType = NestedAttributeType.of(ProductType.reference("x"));
        final SetAttributeType setAttributeType = SetAttributeType.of(nestedAttributeType);
        final AttributeDefinitionDraft attributeDefinitionDraft =
            AttributeDefinitionDraftBuilder.of(setAttributeType, "foo", ofEnglish("foo"), true)
                                           .build();

        final ProductTypeDraft productTypeDraft =
            ProductTypeDraftBuilder.of("foo", "foo", "desc",
                singletonList(attributeDefinitionDraft))
                                   .build();

        final ProductTypeSyncOptions syncOptions =
            ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

        final ProductTypeReferenceResolver productTypeReferenceResolver = new ProductTypeReferenceResolver(syncOptions,
            productTypeService);


        final NestedAttributeType expectedResolvedNestedAttributeType =
            NestedAttributeType.of(ProductType.referenceOfId("foo"));
        final SetAttributeType expectedSetAttributeType =
            SetAttributeType.of(expectedResolvedNestedAttributeType);

        final AttributeDefinitionDraft expectedResolvedAttrDef =
            AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft)
                                           .attributeType(expectedSetAttributeType)
                                           .build();
        final ProductTypeDraftDsl expectedResolvedProductTypeDraft =
            ProductTypeDraftBuilder.of(productTypeDraft)
                                   .attributes(singletonList(expectedResolvedAttrDef))
                                   .build();

        // test and assertion
        assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
            .isCompletedWithValue(expectedResolvedProductTypeDraft);
    }

    @Test
    void resolveReferences_WithNestedAndSetOfNestedTypeWithExistingProductTypeReference_ShouldResolveReferences() {
        // preparation
        final NestedAttributeType nestedAttributeType = NestedAttributeType.of(ProductType.reference("x"));
        final SetAttributeType setAttributeType = SetAttributeType.of(nestedAttributeType);
        final AttributeDefinitionDraft setAttributeDefinitionDraft =
            AttributeDefinitionDraftBuilder.of(setAttributeType, "foo", ofEnglish("foo"), true)
                                           .build();
        final AttributeDefinitionDraft nestedAttributeDefinitionDraft =
            AttributeDefinitionDraftBuilder.of(nestedAttributeType, "foo", ofEnglish("foo"), true)
                                           .build();

        final ProductTypeDraft productTypeDraft =
            ProductTypeDraftBuilder.of("foo", "foo", "desc",
                asList(setAttributeDefinitionDraft, nestedAttributeDefinitionDraft))
                                   .build();

        final ProductTypeSyncOptions syncOptions =
            ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class)).build();

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("foo")));

        final ProductTypeReferenceResolver productTypeReferenceResolver = new ProductTypeReferenceResolver(syncOptions,
            productTypeService);


        final NestedAttributeType expectedResolvedNestedAttributeType =
            NestedAttributeType.of(ProductType.referenceOfId("foo"));
        final SetAttributeType expectedResolvedSetAttributeType =
            SetAttributeType.of(expectedResolvedNestedAttributeType);

        final AttributeDefinitionDraft expectedResolvedSetAttrDef =
            AttributeDefinitionDraftBuilder.of(setAttributeDefinitionDraft)
                                           .attributeType(expectedResolvedSetAttributeType)
                                           .build();
        final AttributeDefinitionDraft expectedResolvedNestedAttrDef =
            AttributeDefinitionDraftBuilder.of(nestedAttributeDefinitionDraft)
                                           .attributeType(expectedResolvedNestedAttributeType)
                                           .build();

        final ProductTypeDraftDsl expectedResolvedProductTypeDraft =
            ProductTypeDraftBuilder.of(productTypeDraft)
                                   .attributes(asList(expectedResolvedSetAttrDef, expectedResolvedNestedAttrDef))
                                   .build();

        // test and assertion
        assertThat(productTypeReferenceResolver.resolveReferences(productTypeDraft))
            .isCompletedWithValue(expectedResolvedProductTypeDraft);
    }
}