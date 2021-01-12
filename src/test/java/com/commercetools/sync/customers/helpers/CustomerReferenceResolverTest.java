package com.commercetools.sync.customers.helpers;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static com.commercetools.sync.customers.helpers.CustomerReferenceResolver.CUSTOMER_GROUP_DOES_NOT_EXIST;
import static com.commercetools.sync.customers.helpers.CustomerReferenceResolver.FAILED_TO_RESOLVE_CUSTOMER_GROUP_REFERENCE;
import static com.commercetools.sync.customers.helpers.CustomerReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE;
import static com.commercetools.sync.customers.helpers.CustomerReferenceResolver.FAILED_TO_RESOLVE_STORE_REFERENCE;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerGroup;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerGroupService;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.CustomFieldsDraft;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerReferenceResolverTest {

  private CustomerReferenceResolver referenceResolver;
  private TypeService typeService;
  private CustomerGroupService customerGroupService;

  private static final String CUSTOMER_GROUP_KEY = "customer-group-key";
  private static final String CUSTOMER_GROUP_ID = UUID.randomUUID().toString();

  @BeforeEach
  void setup() {
    final CustomerSyncOptions syncOptions =
        CustomerSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    typeService = getMockTypeService();

    customerGroupService =
        getMockCustomerGroupService(getMockCustomerGroup(CUSTOMER_GROUP_ID, CUSTOMER_GROUP_KEY));

    referenceResolver =
        new CustomerReferenceResolver(syncOptions, typeService, customerGroupService);
  }

  @Test
  void resolveReferences_WithoutReferences_ShouldNotResolveReferences() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of("email@example.com", "secret123").build();

    final CustomerDraft resolvedDraft =
        referenceResolver.resolveReferences(customerDraft).toCompletableFuture().join();

    assertThat(resolvedDraft).isEqualTo(customerDraft);
  }

  @Test
  void
      resolveCustomTypeReference_WithNullKeyOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
    final CustomFieldsDraft newCustomFieldsDraft = mock(CustomFieldsDraft.class);
    when(newCustomFieldsDraft.getType()).thenReturn(ResourceIdentifier.ofId(null));

    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .key("customer-key")
            .custom(newCustomFieldsDraft);

    assertThat(
            referenceResolver
                .resolveCustomTypeReference(customerDraftBuilder)
                .toCompletableFuture())
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                "Failed to resolve custom type reference on CustomerDraft with "
                    + "key:'customer-key'. Reason: %s",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void
      resolveCustomTypeReference_WithEmptyKeyOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .key("customer-key")
            .custom(CustomFieldsDraft.ofTypeKeyAndJson("", emptyMap()));

    assertThat(
            referenceResolver
                .resolveCustomTypeReference(customerDraftBuilder)
                .toCompletableFuture())
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                "Failed to resolve custom type reference on CustomerDraft with "
                    + "key:'customer-key'. Reason: %s",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
    final String customTypeKey = "nonExistingKey";
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .key("customer-key")
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, emptyMap()));

    when(typeService.fetchCachedTypeId(anyString())).thenReturn(completedFuture(Optional.empty()));

    final String expectedExceptionMessage =
        format(FAILED_TO_RESOLVE_CUSTOM_TYPE, customerDraftBuilder.getKey());
    final String expectedMessageWithCause =
        format(
            "%s Reason: %s", expectedExceptionMessage, format(TYPE_DOES_NOT_EXIST, customTypeKey));
    assertThat(referenceResolver.resolveCustomTypeReference(customerDraftBuilder))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(expectedMessageWithCause);
  }

  @Test
  void resolveReferences_WithAllValidReferences_ShouldResolveReferences() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .key("customer-key")
            .custom(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", emptyMap()))
            .build();

    final CustomerDraft resolvedDraft =
        referenceResolver.resolveReferences(customerDraft).toCompletableFuture().join();

    final CustomerDraft expectedDraft =
        CustomerDraftBuilder.of(customerDraft)
            .custom(CustomFieldsDraft.ofTypeIdAndJson("typeId", new HashMap<>()))
            .build();

    assertThat(resolvedDraft).isEqualTo(expectedDraft);
  }

  @Test
  void resolveCustomerGroupReference_WithKeys_ShouldResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .customerGroup(ResourceIdentifier.ofKey("customer-group-key"));

    final CustomerDraftBuilder resolvedDraft =
        referenceResolver
            .resolveCustomerGroupReference(customerDraftBuilder)
            .toCompletableFuture()
            .join();

    assertThat(resolvedDraft.getCustomerGroup()).isNotNull();
    assertThat(resolvedDraft.getCustomerGroup().getId()).isEqualTo(CUSTOMER_GROUP_ID);
  }

  @Test
  void resolveCustomerGroupReference_WithNonExistentCustomerGroup_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .customerGroup(ResourceIdentifier.ofKey("anyKey"))
            .key("dummyKey");

    when(customerGroupService.fetchCachedCustomerGroupId(anyString()))
        .thenReturn(completedFuture(Optional.empty()));

    final String expectedMessageWithCause =
        format(
            FAILED_TO_RESOLVE_CUSTOMER_GROUP_REFERENCE,
            "dummyKey",
            format(CUSTOMER_GROUP_DOES_NOT_EXIST, "anyKey"));

    assertThat(referenceResolver.resolveCustomerGroupReference(customerDraftBuilder))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(expectedMessageWithCause);
  }

  @Test
  void
      resolveCustomerGroupReference_WithNullKeyOnCustomerGroupReference_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .customerGroup(ResourceIdentifier.ofKey(null))
            .key("dummyKey");

    assertThat(
            referenceResolver
                .resolveCustomerGroupReference(customerDraftBuilder)
                .toCompletableFuture())
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                FAILED_TO_RESOLVE_CUSTOMER_GROUP_REFERENCE,
                "dummyKey",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void
      resolveCustomerGroupReference_WithEmptyKeyOnCustomerGroupReference_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .customerGroup(ResourceIdentifier.ofKey(""))
            .key("dummyKey");

    assertThat(
            referenceResolver
                .resolveCustomerGroupReference(customerDraftBuilder)
                .toCompletableFuture())
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                FAILED_TO_RESOLVE_CUSTOMER_GROUP_REFERENCE,
                "dummyKey",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomerGroupReference_WithExceptionOnCustomerGroupFetch_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .customerGroup(ResourceIdentifier.ofKey("anyKey"))
            .key("dummyKey");

    final CompletableFuture<Optional<String>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
    when(customerGroupService.fetchCachedCustomerGroupId(anyString()))
        .thenReturn(futureThrowingSphereException);

    assertThat(referenceResolver.resolveCustomerGroupReference(customerDraftBuilder))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(SphereException.class)
        .hasMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveCustomerGroupReference_WithIdOnCustomerGroupReference_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .customerGroup(ResourceIdentifier.ofId("existingId"))
            .key("dummyKey");

    assertThat(
            referenceResolver
                .resolveCustomerGroupReference(customerDraftBuilder)
                .toCompletableFuture())
        .hasNotFailed()
        .isCompletedWithValueMatching(
            resolvedDraft ->
                Objects.equals(
                    resolvedDraft.getCustomerGroup(), customerDraftBuilder.getCustomerGroup()));
  }

  @Test
  void resolveStoreReferences_WithNullStoreReferences_ShouldNotResolveReferences() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123").stores(null).key("dummyKey");

    final CustomerDraftBuilder resolvedDraft =
        referenceResolver.resolveStoreReferences(customerDraftBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getStores()).isNull();
  }

  @Test
  void resolveStoreReferences_WithEmptyStoreReferences_ShouldNotResolveReferences() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .stores(emptyList())
            .key("dummyKey");

    final CustomerDraftBuilder resolvedDraft =
        referenceResolver.resolveStoreReferences(customerDraftBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getStores()).isEmpty();
  }

  @Test
  void resolveStoreReferences_WithValidStores_ShouldResolveReferences() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .stores(
                asList(
                    ResourceIdentifier.ofKey("store-key1"),
                    ResourceIdentifier.ofKey("store-key2"),
                    ResourceIdentifier.ofId("store-id-3")))
            .key("dummyKey");

    final CustomerDraftBuilder resolvedDraft =
        referenceResolver.resolveStoreReferences(customerDraftBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getStores())
        .containsExactly(
            ResourceIdentifier.ofKey("store-key1"),
            ResourceIdentifier.ofKey("store-key2"),
            ResourceIdentifier.ofId("store-id-3"));
  }

  @Test
  void resolveStoreReferences_WithNullStore_ShouldResolveReferences() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .stores(singletonList(null))
            .key("dummyKey");

    final CustomerDraftBuilder resolvedDraft =
        referenceResolver.resolveStoreReferences(customerDraftBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getStores()).isEmpty();
  }

  @Test
  void resolveStoreReferences_WithNullKeyOnStoreReference_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .stores(singletonList(ResourceIdentifier.ofKey(null)))
            .key("dummyKey");

    assertThat(referenceResolver.resolveStoreReferences(customerDraftBuilder))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                FAILED_TO_RESOLVE_STORE_REFERENCE,
                "dummyKey",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveStoreReferences_WithBlankKeyOnStoreReference_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of("email@example.com", "secret123")
            .stores(singletonList(ResourceIdentifier.ofKey(" ")))
            .key("dummyKey");

    assertThat(referenceResolver.resolveStoreReferences(customerDraftBuilder))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                FAILED_TO_RESOLVE_STORE_REFERENCE,
                "dummyKey",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }
}
