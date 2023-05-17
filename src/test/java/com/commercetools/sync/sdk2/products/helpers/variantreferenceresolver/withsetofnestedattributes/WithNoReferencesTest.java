package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.withsetofnestedattributes;

import static com.commercetools.sync.sdk2.commons.utils.TestUtils.readObjectFromResource;
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
      "com/commercetools/sync/products/helpers/variantReferenceResolver/withsetofnestedattributes/";
  private static final String RES_SUB_ROOT = "withnoreferences/";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_TEXT_ATTRIBUTES =
      RES_ROOT + RES_SUB_ROOT + "with-text-attributes.json";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_TEXT_ATTRIBUTES =
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
  void resolveReferences_WithSetOfNestedTextAttributes_ShouldReturnEqualDraft() {
    // preparation
    final ProductVariantDraft withSetOfNestedTextAttributes =
        readObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_TEXT_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedTextAttributes)
            .toCompletableFuture()
            .join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(withSetOfNestedTextAttributes);
  }

  @Test
  void resolveReferences_WithSetOfNestedSetOfTextAttributes_ShouldReturnEqualDraft() {
    // preparation
    final ProductVariantDraft withSetOfNestedSetOfTextAttributes =
        readObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_TEXT_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedSetOfTextAttributes)
            .toCompletableFuture()
            .join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(withSetOfNestedSetOfTextAttributes);
  }
}
