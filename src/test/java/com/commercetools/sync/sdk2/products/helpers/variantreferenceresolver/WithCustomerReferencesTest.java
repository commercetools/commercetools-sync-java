package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
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
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.sdk2.services.*;
import java.util.List;
import java.util.Spliterator;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithCustomerReferencesTest {
  private VariantReferenceResolver referenceResolver;
  private static final String CUSTOMER_ID = UUID.randomUUID().toString();
  private CustomerService customerService;

  @BeforeEach
  void setup() {
    customerService = getMockCustomerService(CUSTOMER_ID);
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
        getReferenceSetAttributeDraft(
            "foo",
            createReferenceObject(UUID.randomUUID().toString(), CustomerReference.CUSTOMER),
            createReferenceObject(UUID.randomUUID().toString(), CustomerReference.CUSTOMER));

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
    final List<CustomerReference> references = (List) resolvedAttributeDraft.getValue();
    final Spliterator<CustomerReference> attributeReferencesIterator = references.spliterator();
    assertThat(attributeReferencesIterator).isNotNull();
    final List<CustomerReference> resolvedSet =
        StreamSupport.stream(attributeReferencesIterator, false).collect(Collectors.toList());
    assertThat(resolvedSet).isNotEmpty();
    final CustomerReference resolvedReference =
        CustomerReferenceBuilder.of().id(CUSTOMER_ID).build();
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
  }
}
