package com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.sync.commons.utils.TestUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
            Mockito.mock(TypeService.class),
            Mockito.mock(ChannelService.class),
            Mockito.mock(CustomerGroupService.class),
            Mockito.mock(ProductService.class),
            Mockito.mock(ProductTypeService.class),
            Mockito.mock(CategoryService.class),
            Mockito.mock(CustomObjectService.class),
            Mockito.mock(StateService.class),
            Mockito.mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithNestedTextAttributes_ShouldReturnEqualDraft() {
    // preparation
    final ProductVariantDraft withNestedTextAttributes =
        TestUtils.readObjectFromResource(
            NESTED_ATTRIBUTE_WITH_TEXT_ATTRIBUTES, ProductVariantDraft.class);

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
        TestUtils.readObjectFromResource(
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
