package com.commercetools.sync.products.helpers.variantreferenceresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceImpl;
import com.commercetools.api.models.customer.CustomerReference;
import com.commercetools.api.models.customer.CustomerReferenceBuilder;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.sync.commons.utils.TestUtils;
import com.commercetools.sync.products.ProductSyncMockUtils;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WithCustomerReferencesTest {
  private VariantReferenceResolver referenceResolver;
  private static final String CUSTOMER_ID = UUID.randomUUID().toString();
  private CustomerService customerService;

  @BeforeEach
  void setup() {
    customerService = ProductSyncMockUtils.getMockCustomerService(CUSTOMER_ID);
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
            customerService);
  }

  @Test
  void resolveReferences_WithNullIdFieldInCustomerReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    final Reference attributeValue = new ReferenceImpl();
    final Attribute customerReferenceAttribute =
        AttributeBuilder.of().name("attributeName").value(attributeValue).build();
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(customerReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithCustomerReferenceSetAttribute_ShouldResolveReferences() {
    final Attribute customerReferenceSetAttributeDraft =
        ProductSyncMockUtils.getReferenceSetAttributeDraft(
            "foo",
            ProductSyncMockUtils.createReferenceObject(
                UUID.randomUUID().toString(), CustomerReference.CUSTOMER),
            ProductSyncMockUtils.createReferenceObject(
                UUID.randomUUID().toString(), CustomerReference.CUSTOMER));

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(customerReferenceSetAttributeDraft).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);
    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();
    final List<CustomerReference> references =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), CustomerReference.typeReference());
    assertThat(references).isNotEmpty();
    final CustomerReference resolvedReference =
        CustomerReferenceBuilder.of().id(CUSTOMER_ID).build();
    assertThat(references).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
  }
}
