package com.commercetools.sync.products.helpers.variantreferenceresolver.withsetofnestedattributes;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.services.*;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.products.ProductVariantDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WithNoReferencesTest {
    private VariantReferenceResolver referenceResolver;

    static final String RES_ROOT =
        "com/commercetools/sync/products/helpers/variantReferenceResolver/withsetofnestedattributes/";
    private static final String RES_SUB_ROOT = "withnoreferences/";
    private static final String SET_OF_NESTED_ATTRIBUTE_WITH_TEXT_ATTRIBUTES =
        RES_ROOT + RES_SUB_ROOT + "with-text-attributes.json";
    private static final String SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_TEXT_ATTRIBUTES =
        RES_ROOT + RES_SUB_ROOT + "with-set-of-text-attributes.json";

    /**
     * Sets up the services and the options needed for reference resolution.
     */
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
    void resolveReferences_WithSetOfNestedTextAttributes_ShouldReturnEqualDraft() {
        // preparation
        final ProductVariantDraft withSetOfNestedTextAttributes = SphereJsonUtils
            .readObjectFromResource(SET_OF_NESTED_ATTRIBUTE_WITH_TEXT_ATTRIBUTES, ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withSetOfNestedTextAttributes)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(withSetOfNestedTextAttributes);
    }

    @Test
    void resolveReferences_WithSetOfNestedSetOfTextAttributes_ShouldReturnEqualDraft() {
        // preparation
        final ProductVariantDraft withSetOfNestedSetOfTextAttributes = SphereJsonUtils
            .readObjectFromResource(SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_TEXT_ATTRIBUTES, ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withSetOfNestedSetOfTextAttributes)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(withSetOfNestedSetOfTextAttributes);
    }
}
