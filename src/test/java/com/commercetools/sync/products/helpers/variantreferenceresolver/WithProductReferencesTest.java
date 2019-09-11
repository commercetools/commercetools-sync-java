package com.commercetools.sync.products.helpers.variantreferenceresolver;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceWithId;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceWithRandomId;
import static com.commercetools.sync.products.ProductSyncMockUtils.getReferenceSetAttributeDraft;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WithProductReferencesTest {
    private ProductService productService;
    private static final String PRODUCT_ID = UUID.randomUUID().toString();
    private VariantReferenceResolver referenceResolver;

    @BeforeEach
    void setup() {
        productService = getMockProductService(PRODUCT_ID);
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new VariantReferenceResolver(syncOptions, mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            productService,
            mock(ProductTypeService.class),
            mock(CategoryService.class));
    }

    @Test
    void resolveReferences_WithNonExistingProductReferenceAttribute_ShouldNotResolveReferences() {
        // preparation
        when(productService.getIdFromCacheOrFetch(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode attributeValue = getProductReferenceWithRandomId();
        final AttributeDraft attributeDraft = AttributeDraft.of("attributeName", attributeValue);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(attributeDraft)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithNullIdFieldInProductReferenceAttribute_ShouldNotResolveReferences() {
        // preparation
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, Product.referenceTypeId());
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithNullNodeIdFieldInProductReferenceAttribute_ShouldNotResolveReferences() {
        // preparation
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, Product.referenceTypeId());
        attributeValue.set(REFERENCE_ID_FIELD, JsonNodeFactory.instance.nullNode());

        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("attributeName", attributeValue);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithExistingProductReferenceAttribute_ShouldResolveReferences() {
        // preparation
        final ObjectNode attributeValue = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();
        final AttributeDraft resolvedAttribute = resolvedAttributeDraft.getAttributes().get(0);
        assertThat(resolvedAttribute).isNotNull();
        assertThat(resolvedAttribute.getValue().get(REFERENCE_ID_FIELD).asText()).isEqualTo(PRODUCT_ID);
        assertThat(resolvedAttribute.getValue().get(REFERENCE_TYPE_ID_FIELD).asText())
            .isEqualTo(Product.referenceTypeId());
    }

    @Test
    void resolveReferences_WithProductReferenceSetAttribute_ShouldResolveReferences() {
        final AttributeDraft productReferenceSetAttributeDraft = getReferenceSetAttributeDraft("foo",
                getProductReferenceWithRandomId(), getProductReferenceWithRandomId());

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceSetAttributeDraft)
            .build();

        // test
        final ProductVariantDraft resolvedProductVariantDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture().join();

        // assertions
        assertThat(resolvedProductVariantDraft).isNotNull();
        assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

        final AttributeDraft resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);
        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final List<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                        .collect(Collectors.toList());
        assertThat(resolvedSet).isNotEmpty();
        final ObjectNode resolvedReference = JsonNodeFactory.instance.objectNode();
        resolvedReference.put(REFERENCE_TYPE_ID_FIELD, Product.referenceTypeId());
        resolvedReference.put(REFERENCE_ID_FIELD, PRODUCT_ID);
        assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
    }

    @Test
    void resolveReferences_WithNonExistingProductReferenceSetAttribute_ShouldNotResolveReferences() {
        // preparation
        when(productService.getIdFromCacheOrFetch(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode productReference = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceAttribute =
            getReferenceSetAttributeDraft("foo", productReference);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedProductVariantDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture().join();

        // assertions
        assertThat(resolvedProductVariantDraft).isNotNull();
        assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

        final AttributeDraft resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());
        assertThat(resolvedSet).containsExactly(productReference);
    }

    @Test
    void resolveReferences_WithSomeExistingProductReferenceSetAttribute_ShouldResolveExistingReferences() {
        // preparation
        when(productService.getIdFromCacheOrFetch("existingKey"))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("existingId")));
        when(productService.getIdFromCacheOrFetch("randomKey"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode productReference1 = getProductReferenceWithId("existingKey");
        final ObjectNode productReference2 = getProductReferenceWithId("randomKey");

        final AttributeDraft productReferenceAttribute =
            getReferenceSetAttributeDraft("foo", productReference1, productReference2);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedProductVariantDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture().join();

        // assertions
        assertThat(resolvedProductVariantDraft).isNotNull();
        assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

        final AttributeDraft resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());

        final ObjectNode resolvedReference1 = getProductReferenceWithId("existingId");
        final ObjectNode resolvedReference2 = getProductReferenceWithId("randomKey");
        assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference1, resolvedReference2);
    }
}
