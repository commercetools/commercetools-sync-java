package com.commercetools.sync.customers.helpers;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.store.StoreResourceIdentifierBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifier;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TypeService;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
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
        CustomerSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    typeService = getMockTypeService();

    customerGroupService =
        getMockCustomerGroupService(getMockCustomerGroup(CUSTOMER_GROUP_ID, CUSTOMER_GROUP_KEY));

    referenceResolver =
        new CustomerReferenceResolver(syncOptions, typeService, customerGroupService);
  }

  private static TypeService getMockTypeService() {
    final TypeService typeService = mock(TypeService.class);
    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(completedFuture(Optional.of("typeId")));
    when(typeService.cacheKeysToIds(anySet()))
        .thenReturn(completedFuture(Collections.singletonMap("typeKey", "typeId")));
    return typeService;
  }

  private static CustomerGroup getMockCustomerGroup(final String id, final String key) {
    final CustomerGroup customerGroup = mock(CustomerGroup.class);
    when(customerGroup.getId()).thenReturn(id);
    when(customerGroup.getKey()).thenReturn(key);
    return customerGroup;
  }

  private static CustomerGroupService getMockCustomerGroupService(
      @Nonnull final CustomerGroup customerGroup) {
    final String customerGroupId = customerGroup.getId();

    final CustomerGroupService customerGroupService = mock(CustomerGroupService.class);
    when(customerGroupService.fetchCachedCustomerGroupId(anyString()))
        .thenReturn(completedFuture(Optional.of(customerGroupId)));
    return customerGroupService;
  }

  @Test
  void resolveReferences_WithoutReferences_ShouldNotResolveReferences() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email@example.com").password("secret123").build();

    final CustomerDraft resolvedDraft =
        referenceResolver.resolveReferences(customerDraft).toCompletableFuture().join();

    assertThat(resolvedDraft).isEqualTo(customerDraft);
  }

  @Test
  void
      resolveCustomTypeReference_WithNullKeyOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
    final CustomFieldsDraft newCustomFieldsDraft = mock(CustomFieldsDraft.class);
    when(newCustomFieldsDraft.getType()).thenReturn(TypeResourceIdentifier.of());

    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .key("customer-key")
            .custom(newCustomFieldsDraft);

    assertThat(
            referenceResolver
                .resolveCustomTypeReference(customerDraftBuilder)
                .toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve custom type reference on CustomerDraft with "
                    + "key:'customer-key'. Reason: %s",
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void
      resolveCustomTypeReference_WithEmptyKeyOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .key("customer-key")
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key("").build())
                    .build());

    assertThat(
            referenceResolver
                .resolveCustomTypeReference(customerDraftBuilder)
                .toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve custom type reference on CustomerDraft with "
                    + "key:'customer-key'. Reason: %s",
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
    final String customTypeKey = "nonExistingKey";
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .key("customer-key")
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key(customTypeKey).build())
                    .build());

    when(typeService.fetchCachedTypeId(anyString())).thenReturn(completedFuture(Optional.empty()));

    final String expectedExceptionMessage =
        String.format(
            CustomerReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE, customerDraftBuilder.getKey());
    final String expectedMessageWithCause =
        format(
            "%s Reason: %s",
            expectedExceptionMessage,
            String.format(CustomReferenceResolver.TYPE_DOES_NOT_EXIST, customTypeKey));
    assertThat(referenceResolver.resolveCustomTypeReference(customerDraftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(expectedMessageWithCause);
  }

  @Test
  void resolveReferences_WithAllValidReferences_ShouldResolveReferences() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .key("customer-key")
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key("typeKey").build())
                    .build())
            .build();

    final CustomerDraft resolvedDraft =
        referenceResolver.resolveReferences(customerDraft).toCompletableFuture().join();

    final CustomerDraft expectedDraft =
        CustomerDraftBuilder.of(customerDraft)
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().id("typeId").build())
                    .build())
            .build();

    assertThat(resolvedDraft).isEqualTo(expectedDraft);
  }

  @Test
  void resolveCustomerGroupReference_WithKeys_ShouldResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .customerGroup(
                CustomerGroupResourceIdentifierBuilder.of().key("customer-group-key").build());

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
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .customerGroup(CustomerGroupResourceIdentifierBuilder.of().key("anyKey").build())
            .key("dummyKey");

    when(customerGroupService.fetchCachedCustomerGroupId(anyString()))
        .thenReturn(completedFuture(Optional.empty()));

    final String expectedMessageWithCause =
        String.format(
            CustomerReferenceResolver.FAILED_TO_RESOLVE_CUSTOMER_GROUP_REFERENCE,
            "dummyKey",
            String.format(CustomerReferenceResolver.CUSTOMER_GROUP_DOES_NOT_EXIST, "anyKey"));

    assertThat(referenceResolver.resolveCustomerGroupReference(customerDraftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(expectedMessageWithCause);
  }

  @Test
  void
      resolveCustomerGroupReference_WithNullKeyOnCustomerGroupReference_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .customerGroup(CustomerGroupResourceIdentifierBuilder.of().key(null).build())
            .key("dummyKey");

    assertThat(
            referenceResolver
                .resolveCustomerGroupReference(customerDraftBuilder)
                .toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                CustomerReferenceResolver.FAILED_TO_RESOLVE_CUSTOMER_GROUP_REFERENCE,
                "dummyKey",
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void
      resolveCustomerGroupReference_WithEmptyKeyOnCustomerGroupReference_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .customerGroup(CustomerGroupResourceIdentifierBuilder.of().key("").build())
            .key("dummyKey");

    assertThat(
            referenceResolver
                .resolveCustomerGroupReference(customerDraftBuilder)
                .toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                CustomerReferenceResolver.FAILED_TO_RESOLVE_CUSTOMER_GROUP_REFERENCE,
                "dummyKey",
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomerGroupReference_WithExceptionOnCustomerGroupFetch_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .customerGroup(CustomerGroupResourceIdentifierBuilder.of().key("anyKey").build())
            .key("dummyKey");

    final CompletableFuture<Optional<String>> futureThrowingException = new CompletableFuture<>();
    futureThrowingException.completeExceptionally(new RuntimeException("CTP error on fetch"));
    when(customerGroupService.fetchCachedCustomerGroupId(anyString()))
        .thenReturn(futureThrowingException);

    assertThat(referenceResolver.resolveCustomerGroupReference(customerDraftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(RuntimeException.class)
        .withMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveCustomerGroupReference_WithIdOnCustomerGroupReference_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .customerGroup(CustomerGroupResourceIdentifierBuilder.of().id("existingId").build())
            .key("dummyKey");

    assertThat(
            referenceResolver
                .resolveCustomerGroupReference(customerDraftBuilder)
                .toCompletableFuture())
        .isCompletedWithValueMatching(
            resolvedDraft ->
                Objects.equals(
                    resolvedDraft.getCustomerGroup(), customerDraftBuilder.getCustomerGroup()));
  }

  @Test
  void resolveStoreReferences_WithNullStoreReferences_ShouldNotResolveReferences() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of().email("email@example.com").password("secret123").key("dummyKey");

    final CustomerDraftBuilder resolvedDraft =
        referenceResolver.resolveStoreReferences(customerDraftBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getStores()).isNull();
  }

  @Test
  void resolveStoreReferences_WithEmptyStoreReferences_ShouldNotResolveReferences() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .stores(emptyList())
            .key("dummyKey");

    final CustomerDraftBuilder resolvedDraft =
        referenceResolver.resolveStoreReferences(customerDraftBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getStores()).isEmpty();
  }

  @Test
  void resolveStoreReferences_WithValidStores_ShouldResolveReferences() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .stores(
                StoreResourceIdentifierBuilder.of().key("store-key1").build(),
                StoreResourceIdentifierBuilder.of().key("store-key2").build(),
                StoreResourceIdentifierBuilder.of().id("store-id3").build())
            .key("dummyKey");

    final CustomerDraftBuilder resolvedDraft =
        referenceResolver.resolveStoreReferences(customerDraftBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getStores())
        .containsExactly(
            StoreResourceIdentifierBuilder.of().key("store-key1").build(),
            StoreResourceIdentifierBuilder.of().key("store-key2").build(),
            StoreResourceIdentifierBuilder.of().id("store-id3").build());
  }

  @Test
  void resolveStoreReferences_WithNullStore_ShouldResolveReferences() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .stores(singletonList(null))
            .key("dummyKey");

    final CustomerDraftBuilder resolvedDraft =
        referenceResolver.resolveStoreReferences(customerDraftBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getStores()).isEmpty();
  }

  @Test
  void resolveStoreReferences_WithNullKeyOnStoreReference_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .stores(singletonList(StoreResourceIdentifierBuilder.of().key(null).build()))
            .key("dummyKey");

    assertThat(referenceResolver.resolveStoreReferences(customerDraftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                CustomerReferenceResolver.FAILED_TO_RESOLVE_STORE_REFERENCE,
                "dummyKey",
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveStoreReferences_WithBlankKeyOnStoreReference_ShouldNotResolveReference() {
    final CustomerDraftBuilder customerDraftBuilder =
        CustomerDraftBuilder.of()
            .email("email@example.com")
            .password("secret123")
            .stores(singletonList(StoreResourceIdentifierBuilder.of().key(" ").build()))
            .key("dummyKey");

    assertThat(referenceResolver.resolveStoreReferences(customerDraftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                CustomerReferenceResolver.FAILED_TO_RESOLVE_STORE_REFERENCE,
                "dummyKey",
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }
}
