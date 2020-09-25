package com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.ProductVariantDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes.WithNoReferencesTest.RES_ROOT;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WithCustomerReferencesTest {
    private VariantReferenceResolver referenceResolver;

    private static final String RES_SUB_ROOT = "withcustomerreferences/";
    private static final String NESTED_ATTRIBUTE_WITH_CUSTOMER_REFERENCE_ATTRIBUTES =
        RES_ROOT + RES_SUB_ROOT + "with-reference.json";
    private static final String NESTED_ATTRIBUTE_WITH_SET_OF_CUSTOMER_REFERENCE_ATTRIBUTES =
        RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";

    @BeforeEach
    void setup() {
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new VariantReferenceResolver(syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(ProductService.class),
            mock(ProductTypeService.class),
            mock(CategoryService.class),
            mock(CustomObjectService.class));
    }

    @Test
    void resolveReferences_WithNestedCustomerReferenceAttributes_ShouldNotResolveReferences() {
        // preparation
        final ProductVariantDraft withNestedCustomerReferenceAttributes =
            readObjectFromResource(NESTED_ATTRIBUTE_WITH_CUSTOMER_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withNestedCustomerReferenceAttributes)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(withNestedCustomerReferenceAttributes).isEqualTo(resolvedAttributeDraft);
    }

    @Test
    void resolveReferences_WithNestedSetOfCustomerReferenceAttributes_ShouldNotResolveReferences() {
        // preparation
        final ProductVariantDraft withNestedSetOfCustomerReferenceAttributes =
            readObjectFromResource(NESTED_ATTRIBUTE_WITH_SET_OF_CUSTOMER_REFERENCE_ATTRIBUTES,
                ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withNestedSetOfCustomerReferenceAttributes)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(withNestedSetOfCustomerReferenceAttributes).isEqualTo(resolvedAttributeDraft);
    }
}
