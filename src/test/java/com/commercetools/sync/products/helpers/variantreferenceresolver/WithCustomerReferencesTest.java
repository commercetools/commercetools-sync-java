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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.products.ProductSyncMockUtils.getReferenceSetAttributeDraft;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WithCustomerReferencesTest {
    private VariantReferenceResolver referenceResolver;

    @BeforeEach
    void setup() {
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new VariantReferenceResolver(syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(ProductService.class),
            mock(ProductTypeService.class),
            mock(CategoryService.class));
    }


    @Test
    void resolveReferences_WithNullIdFieldInCustomerReferenceAttribute_ShouldNotResolveReferences() {
        // preparation
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, Customer.referenceTypeId());
        final AttributeDraft customerReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(customerReferenceAttribute)
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
    void resolveReferences_WithNullNodeIdFieldInCustomerReferenceAttribute_ShouldNotResolveReferences() {
        // preparation
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, Customer.referenceTypeId());
        attributeValue.set(REFERENCE_ID_FIELD, JsonNodeFactory.instance.nullNode());
        final AttributeDraft customerReferenceAttribute =
            AttributeDraft.of("attributeName", attributeValue);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(customerReferenceAttribute)
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
    void resolveReferences_WithCustomerReferenceSetAttribute_ShouldNotResolveReferences() {
        final AttributeDraft customerReferenceSetAttributeDraft = getReferenceSetAttributeDraft("foo",
            createReferenceObject(UUID.randomUUID().toString(), Customer.referenceTypeId()),
            createReferenceObject(UUID.randomUUID().toString(), Customer.referenceTypeId()));

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(customerReferenceSetAttributeDraft)
            .build();

        // test
        final ProductVariantDraft resolvedProductVariantDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture().join();

        // assertions
        assertThat(productVariantDraft).isEqualTo(resolvedProductVariantDraft);
    }
}
