package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.withnestedattributes;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.sdk2.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithNoReferencesTest {
  private VariantReferenceResolver referenceResolver;

  static final String RES_ROOT =
      "com/commercetools/sync/products/helpers/variantReferenceResolver/withnestedattributes/";
  private static final String RES_SUB_ROOT = "withnoreferences/";
  private static final String NESTED_ATTRIBUTE_WITH_TEXT_ATTRIBUTES =
      RES_ROOT + RES_SUB_ROOT + "with-text-attributes.json";
  private static final String NESTED_ATTRIBUTE_WITH_SET_OF_TEXT_ATTRIBUTES =
      RES_ROOT + RES_SUB_ROOT + "with-set-of-text-attributes.json";

  @BeforeEach
  void setup() {
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    referenceResolver =
        new VariantReferenceResolver(
            syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(ProductService.class),
            mock(ProductTypeService.class),
            mock(CategoryService.class),
            mock(CustomObjectService.class),
            mock(StateService.class),
            mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithNestedTextAttributes_ShouldReturnEqualDraft() {
    // preparation
    final ProductVariantDraft withNestedTextAttributes =
        createObjectFromResource(NESTED_ATTRIBUTE_WITH_TEXT_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(withNestedTextAttributes).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(withNestedTextAttributes);
  }

  @Test
  void resolveReferences_WithNestedSetOfTextAttributes_ShouldReturnEqualDraft() {
    // preparation
    final ProductVariantDraft withNestedSetOfTextAttributes =
        createObjectFromResource(
            NESTED_ATTRIBUTE_WITH_SET_OF_TEXT_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withNestedSetOfTextAttributes)
            .toCompletableFuture()
            .join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(withNestedSetOfTextAttributes);
  }
}
