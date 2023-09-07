package com.commercetools.sync.shoppinglists.helpers;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.customer.CustomerResourceIdentifier;
import com.commercetools.api.models.customer.CustomerResourceIdentifierBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraftBuilder;
import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.api.models.shopping_list.TextLineItemDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.sync.commons.ExceptionUtils;
import com.commercetools.sync.commons.MockUtils;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShoppingListReferenceResolverTest {

  private TypeService typeService;
  private CustomerService customerService;

  private ShoppingListReferenceResolver referenceResolver;

  /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    typeService = MockUtils.getMockTypeService();
    customerService = MockUtils.getMockCustomerService();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    referenceResolver =
        new ShoppingListReferenceResolver(syncOptions, customerService, typeService);
  }

  @Test
  void
      resolveCustomTypeReference_WithNonNullIdOnCustomTypeResId_ShouldResolveCustomTypeReference() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(
                typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key("customTypeKey"))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();

    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("NAME"))
            .custom(customFieldsDraft)
            .description(ofEnglish("DESCRIPTION"));

    final ShoppingListDraftBuilder resolvedDraft =
        referenceResolver.resolveCustomTypeReference(draftBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getCustom()).isNotNull();
    assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo("typeId");
  }

  @Test
  void resolveCustomTypeReference_WithCustomTypeId_ShouldNotResolveCustomTypeReferenceWithKey() {
    final String customTypeId = "customTypeId";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(customTypeId))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();

    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("NAME"))
            .custom(customFieldsDraft)
            .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

    final ShoppingListDraftBuilder resolvedDraft =
        referenceResolver.resolveCustomTypeReference(draftBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getCustom()).isNotNull();
    assertThat(resolvedDraft.getCustom()).isEqualTo(customFieldsDraft);
  }

  @Test
  void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFutureUtils.failed(ExceptionUtils.createBadGatewayException()));

    final String customTypeKey = "customTypeKey";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(customTypeKey))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();
    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("NAME"))
            .custom(customFieldsDraft)
            .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

    final CompletionStage<ShoppingListDraftBuilder> resolvedDraftCompletionStage =
        referenceResolver.resolveCustomTypeReference(draftBuilder);

    assertThat(resolvedDraftCompletionStage)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class)
        .withMessageContaining("test");
  }

  @Test
  void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
    final String customTypeKey = "customTypeKey";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(customTypeKey))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();

    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("NAME"))
            .custom(customFieldsDraft)
            .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    // test
    final CompletionStage<ShoppingListDraftBuilder> resolvedDraftCompletionStage =
        referenceResolver.resolveCustomTypeReference(draftBuilder);

    // assertion
    final String expectedExceptionMessage =
        String.format(
            ShoppingListReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getKey());

    final String expectedMessageWithCause =
        format(
            "%s Reason: %s",
            expectedExceptionMessage,
            String.format(CustomReferenceResolver.TYPE_DOES_NOT_EXIST, customTypeKey));
    ;
    assertThat(resolvedDraftCompletionStage)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(expectedMessageWithCause);
  }

  @Test
  void resolveCustomTypeReference_WithEmptyKeyOnCustomTypeResId_ShouldCompleteExceptionally() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(""))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();

    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("NAME"))
            .custom(customFieldsDraft)
            .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

    assertThat(referenceResolver.resolveCustomTypeReference(draftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve custom type reference on ShoppingListDraft"
                    + " with key:'null'.  Reason: %s",
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomerReference_WithCustomerKey_ShouldResolveCustomerReference() {
    when(customerService.fetchCachedCustomerId("customerKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("customerId")));

    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("NAME"))
            .customer(CustomerResourceIdentifierBuilder.of().key("customerKey").build())
            .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

    final ShoppingListDraftBuilder resolvedDraft =
        referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture().join();

    assertThat(resolvedDraft.getCustomer()).isNotNull();
    assertThat(resolvedDraft.getCustomer().getId()).isEqualTo("customerId");
  }

  @Test
  void resolveCustomerReference_WithNullCustomerReference_ShouldNotResolveCustomerReference() {
    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("NAME"))
            .customer((CustomerResourceIdentifier) null)
            .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

    final ShoppingListDraftBuilder referencesResolvedDraft =
        referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture().join();

    assertThat(referencesResolvedDraft.getCustom()).isNull();
  }

  @Test
  void resolveCustomerReference_WithExceptionOnCustomerGroupFetch_ShouldNotResolveReference() {
    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("NAME"))
            .customer(CustomerResourceIdentifierBuilder.of().key("anyKey").build())
            .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

    final CompletableFuture<Optional<String>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(ExceptionUtils.createBadGatewayException());
    when(customerService.fetchCachedCustomerId(anyString()))
        .thenReturn(futureThrowingSphereException);

    assertThat(referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class)
        .withMessageContaining("test");
  }

  @Test
  void resolveCustomerReference_WithNullCustomerKey_ShouldNotResolveCustomerReference() {
    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("NAME"))
            .customer(CustomerResourceIdentifierBuilder.of().key(null).build())
            .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

    assertThat(referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                ShoppingListReferenceResolver.FAILED_TO_RESOLVE_CUSTOMER_REFERENCE,
                draftBuilder.getKey(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomerReference_WithEmptyCustomerKey_ShouldNotResolveCustomerReference() {
    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("NAME"))
            .customer(CustomerResourceIdentifierBuilder.of().key(" ").build())
            .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

    assertThat(referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                ShoppingListReferenceResolver.FAILED_TO_RESOLVE_CUSTOMER_REFERENCE,
                draftBuilder.getKey(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomerReference_WithNonExistingCustomerKey_ShouldNotResolveCustomerReference() {
    when(customerService.fetchCachedCustomerId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final String customerKey = "non-existing-customer-key";
    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "NAME"))
            .customer(CustomerResourceIdentifierBuilder.of().key(customerKey).build())
            .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

    assertThat(referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                ShoppingListReferenceResolver.FAILED_TO_RESOLVE_CUSTOMER_REFERENCE,
                draftBuilder.getKey(),
                String.format(ShoppingListReferenceResolver.CUSTOMER_DOES_NOT_EXIST, customerKey)));
  }

  @Test
  void resolveCustomerReference_WithIdOnCustomerReference_ShouldNotResolveReference() {
    final ShoppingListDraftBuilder draftBuilder =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("NAME"))
            .customer(CustomerResourceIdentifierBuilder.of().id("existingId").build())
            .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

    assertThat(referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture())
        .isCompleted()
        .isCompletedWithValueMatching(
            resolvedDraft ->
                Objects.equals(resolvedDraft.getCustomer(), draftBuilder.getCustomer()));
  }

  @Test
  void resolveReferences_WithoutReferences_ShouldNotResolveReferences() {
    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of().name(ofEnglish("name")).key("shoppingList-key").build();

    final ShoppingListDraft resolvedDraft =
        referenceResolver.resolveReferences(shoppingListDraft).toCompletableFuture().join();

    assertThat(resolvedDraft).isEqualTo(shoppingListDraft);
  }

  @Test
  void resolveReferences_WithAllValidFieldsAndReferences_ShouldResolveReferences() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key("typeKey"))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();

    final ZonedDateTime addedAt = ZonedDateTime.now();
    final ShoppingListLineItemDraft lineItemDraft =
        ShoppingListLineItemDraftBuilder.of()
            .sku("variant-sku")
            .quantity(20L)
            .custom(customFieldsDraft)
            .addedAt(addedAt)
            .build();

    final TextLineItemDraft textLineItemDraft =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("textLineItemName"))
            .quantity(10L)
            .description(LocalizedString.ofEnglish("desc"))
            .custom(customFieldsDraft)
            .addedAt(addedAt)
            .build();

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("name"))
            .key("shoppingList-key")
            .description(LocalizedString.ofEnglish("desc"))
            .slug(LocalizedString.ofEnglish("slug"))
            .deleteDaysAfterLastModification(0L)
            .anonymousId("anonymousId")
            .lineItems(asList(null, lineItemDraft))
            .textLineItems(asList(null, textLineItemDraft))
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        typeResourceIdentifierBuilder ->
                            typeResourceIdentifierBuilder.key("typeKey"))
                    .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                    .build())
            .customer(CustomerResourceIdentifierBuilder.of().key("customerKey").build())
            .build();

    final ShoppingListDraft resolvedDraft =
        referenceResolver.resolveReferences(shoppingListDraft).toCompletableFuture().join();

    final ShoppingListDraft expectedDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("name"))
            .key("shoppingList-key")
            .description(LocalizedString.ofEnglish("desc"))
            .slug(LocalizedString.ofEnglish("slug"))
            .deleteDaysAfterLastModification(0L)
            .anonymousId("anonymousId")
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("typeId"))
                    .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
                    .build())
            .customer(CustomerResourceIdentifierBuilder.of().id("customerId").build())
            .lineItems(
                singletonList(
                    ShoppingListLineItemDraftBuilder.of()
                        .sku("variant-sku")
                        .quantity(20L)
                        .custom(
                            CustomFieldsDraftBuilder.of()
                                .type(
                                    typeResourceIdentifierBuilder ->
                                        typeResourceIdentifierBuilder.id("typeId"))
                                .fields(
                                    fieldContainerBuilder ->
                                        fieldContainerBuilder.values(new HashMap<>()))
                                .build())
                        .addedAt(addedAt)
                        .build()))
            .textLineItems(
                singletonList(
                    TextLineItemDraftBuilder.of()
                        .name(ofEnglish("textLineItemName"))
                        .quantity(10L)
                        .description(LocalizedString.ofEnglish("desc"))
                        .custom(
                            CustomFieldsDraftBuilder.of()
                                .type(
                                    typeResourceIdentifierBuilder ->
                                        typeResourceIdentifierBuilder.id("typeId"))
                                .fields(
                                    fieldContainerBuilder ->
                                        fieldContainerBuilder.values(new HashMap<>()))
                                .build())
                        .addedAt(addedAt)
                        .build()))
            .build();

    assertThat(resolvedDraft).isEqualTo(expectedDraft);
  }
}
