package com.commercetools.sync.products.helpers.variantreferenceresolver.withsetofnestedattributes;

import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

class WithCustomerReferencesTest {

  private VariantReferenceResolver referenceResolver;

  private static final String RES_SUB_ROOT = "withcustomerreferences/";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_CUSTOMER_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-reference.json";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_CUSTOMER_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";

  @BeforeEach
  void setup() {
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    referenceResolver =
        new VariantReferenceResolver(
            syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(ProductService.class),
            mock(ProductTypeService.class),
            mock(CategoryService.class),
            mock(CustomObjectService.class));
  }

  @Test
  void resolveReferences_WithSetOfNestedCustomerReferenceAttributes_ShouldNotResolveReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedCustomerReferenceAttributes =
        readObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_CUSTOMER_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedCustomerReferenceAttributes)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(withSetOfNestedCustomerReferenceAttributes).isEqualTo(resolvedAttributeDraft);
  }

  @Test
  void
      resolveReferences_WithSetOfNestedSetOfCustomerReferenceAttributes_ShouldNotResolveReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedSetOfCustomerReferenceAttributes =
        readObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_CUSTOMER_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedSetOfCustomerReferenceAttributes)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(withSetOfNestedSetOfCustomerReferenceAttributes).isEqualTo(resolvedAttributeDraft);
  }
}
