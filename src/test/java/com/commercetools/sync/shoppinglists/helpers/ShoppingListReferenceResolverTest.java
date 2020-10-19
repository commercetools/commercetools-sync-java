package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.MockUtils.getMockCustomerService;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListReferenceResolver.CUSTOMER_DOES_NOT_EXIST;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListReferenceResolver.FAILED_TO_RESOLVE_CUSTOMER_REFERENCE;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShoppingListReferenceResolverTest {

    private TypeService typeService;
    private CustomerService customerService;

    private ShoppingListReferenceResolver referenceResolver;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @BeforeEach
    void setup() {
        typeService = getMockTypeService();
        customerService = getMockCustomerService();

        final ShoppingListSyncOptions syncOptions = ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new ShoppingListReferenceResolver(syncOptions, customerService, typeService);
    }

    @Test
    void resolveCustomTypeReference_WithNonNullIdOnCustomTypeResId_ShouldResolveCustomTypeReference() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeKeyAndJson("customTypeKey", new HashMap<>());

        final ShoppingListDraftBuilder draftBuilder =
            ShoppingListDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                .custom(customFieldsDraft)
                .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        final ShoppingListDraftBuilder resolvedDraft = referenceResolver
            .resolveCustomTypeReference(draftBuilder)
            .toCompletableFuture().join();

        assertThat(resolvedDraft.getCustom()).isNotNull();
        assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo("typeId");

    }

    @Test
    void resolveCustomTypeReference_WithCustomTypeId_ShouldNotResolveCustomTypeReferenceWithKey() {
        final String customTypeId = "customTypeId";
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeIdAndJson(customTypeId, new HashMap<>());

        final ShoppingListDraftBuilder draftBuilder =
            ShoppingListDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                .custom(customFieldsDraft)
                .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        final ShoppingListDraftBuilder resolvedDraft = referenceResolver
            .resolveCustomTypeReference(draftBuilder)
            .toCompletableFuture().join();

        assertThat(resolvedDraft.getCustom()).isNotNull();
        assertThat(resolvedDraft.getCustom()).isEqualTo(customFieldsDraft);
    }

    @Test
    void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFutureUtils.failed(new SphereException("CTP error on fetch")));

        final String customTypeKey = "customTypeKey";
        final CustomFieldsDraft customFieldsDraft =
            CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, new HashMap<>());
        final ShoppingListDraftBuilder draftBuilder =
            ShoppingListDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                .custom(customFieldsDraft)
                .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        final CompletionStage<ShoppingListDraftBuilder> resolvedDraftCompletionStage = referenceResolver
            .resolveCustomTypeReference(draftBuilder);

        assertThat(resolvedDraftCompletionStage)
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
        final String customTypeKey = "customTypeKey";
        final CustomFieldsDraft customFieldsDraft =
            CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, new HashMap<>());

        final ShoppingListDraftBuilder draftBuilder =
            ShoppingListDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                .custom(customFieldsDraft)
                .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        // test
        final CompletionStage<ShoppingListDraftBuilder> resolvedDraftCompletionStage = referenceResolver
            .resolveCustomTypeReference(draftBuilder);

        // assertion
        final String expectedExceptionMessage = format(FAILED_TO_RESOLVE_CUSTOM_TYPE,
            draftBuilder.getKey());

        final String expectedMessageWithCause =
            format("%s Reason: %s", expectedExceptionMessage, format(TYPE_DOES_NOT_EXIST, customTypeKey));
        ;
        assertThat(resolvedDraftCompletionStage)
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(expectedMessageWithCause);
    }

    @Test
    void resolveCustomTypeReference_WithEmptyKeyOnCustomTypeResId_ShouldCompleteExceptionally() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeKeyAndJson("", new HashMap<>());

        final ShoppingListDraftBuilder draftBuilder =
            ShoppingListDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                .custom(customFieldsDraft)
                .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        assertThat(referenceResolver.resolveCustomTypeReference(draftBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve custom type reference on ShoppingListDraft"
                + " with key:'null'.  Reason: %s", BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    void resolveCustomerReference_WithCustomerKey_ShouldResolveCustomerReference() {
        when(customerService.fetchCachedCustomerId("customerKey"))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("customerId")));

        final ShoppingListDraftBuilder draftBuilder =
                ShoppingListDraftBuilder
                        .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                        .customer(ResourceIdentifier.ofKey("customerKey"))
                        .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        final ShoppingListDraftBuilder resolvedDraft = referenceResolver
                .resolveCustomerReference(draftBuilder)
                .toCompletableFuture()
                .join();

        assertThat(resolvedDraft.getCustomer()).isNotNull();
        assertThat(resolvedDraft.getCustomer().getId()).isEqualTo("customerId");
    }

    @Test
    void resolveCustomerReference_WithNullCustomerReference_ShouldNotResolveCustomerReference() {
        final ShoppingListDraftBuilder draftBuilder =
            ShoppingListDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                .customer((ResourceIdentifier<Customer>) null)
                .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        final ShoppingListDraftBuilder referencesResolvedDraft =
            referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture().join();

        assertThat(referencesResolvedDraft.getCustom()).isNull();
    }

    @Test
    void resolveCustomerReference_WithExceptionOnCustomerGroupFetch_ShouldNotResolveReference() {
        final ShoppingListDraftBuilder draftBuilder =
            ShoppingListDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                .customer(ResourceIdentifier.ofKey("anyKey"))
                .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(customerService.fetchCachedCustomerId(anyString())).thenReturn(futureThrowingSphereException);

        assertThat(referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture())
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    void resolveCustomerReference_WithNullCustomerKey_ShouldNotResolveCustomerReference() {
        final ShoppingListDraftBuilder draftBuilder =
            ShoppingListDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                .customer(ResourceIdentifier.ofKey(null))
                .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        assertThat(referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture())
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format(FAILED_TO_RESOLVE_CUSTOMER_REFERENCE, draftBuilder.getKey(),
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    void resolveCustomerReference_WithEmptyCustomerKey_ShouldNotResolveCustomerReference() {
        final ShoppingListDraftBuilder draftBuilder =
            ShoppingListDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                .customer(ResourceIdentifier.ofKey(" "))
                .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        assertThat(referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture())
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format(FAILED_TO_RESOLVE_CUSTOMER_REFERENCE, draftBuilder.getKey(),
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    void resolveCustomerReference_WithNonExistingCustomerKey_ShouldNotResolveCustomerReference() {
        when(customerService.fetchCachedCustomerId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final String customerKey = "non-existing-customer-key";
        final ShoppingListDraftBuilder draftBuilder =
            ShoppingListDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                .customer(ResourceIdentifier.ofKey(customerKey))
                .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        assertThat(referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture())
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format(ShoppingListReferenceResolver.FAILED_TO_RESOLVE_CUSTOMER_REFERENCE,
                draftBuilder.getKey(), format(CUSTOMER_DOES_NOT_EXIST, customerKey)));
    }

    @Test
    void resolveCustomerReference_WithIdOnCustomerReference_ShouldNotResolveReference() {
        final ShoppingListDraftBuilder draftBuilder =
            ShoppingListDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                .customer(ResourceIdentifier.ofId("existingId"))
                .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        assertThat(referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft ->
                Objects.equals(resolvedDraft.getCustomer(), draftBuilder.getCustomer()));
    }

    @Test
    void resolveReferences_WithoutReferences_ShouldNotResolveReferences() {
        final ShoppingListDraft shoppingListDraft = ShoppingListDraftBuilder
            .of(LocalizedString.ofEnglish("name"))
            .key("shoppingList-key")
            .build();

        final ShoppingListDraft resolvedDraft = referenceResolver
            .resolveReferences(shoppingListDraft)
            .toCompletableFuture()
            .join();

        assertThat(resolvedDraft).isEqualTo(shoppingListDraft);
    }

    @Test
    void resolveReferences_WithAllValidFieldsAndReferences_ShouldResolveReferences() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeKeyAndJson("typeKey", new HashMap<>());

        final ZonedDateTime addedAt = ZonedDateTime.now();
        final LineItemDraft lineItemDraft =
            LineItemDraftBuilder.ofSku("variant-sku", 20L)
                                .custom(customFieldsDraft)
                                .addedAt(addedAt)
                                .build();

        final TextLineItemDraft textLineItemDraft =
            TextLineItemDraftBuilder
                .of(LocalizedString.ofEnglish("textLineItemName"), 10L)
                .description(LocalizedString.ofEnglish("desc"))
                .custom(customFieldsDraft)
                .addedAt(addedAt)
                .build();

        final ShoppingListDraft shoppingListDraft = ShoppingListDraftBuilder
            .of(LocalizedString.ofEnglish("name"))
            .key("shoppingList-key")
            .description(LocalizedString.ofEnglish("desc"))
            .slug(LocalizedString.ofEnglish("slug"))
            .deleteDaysAfterLastModification(0)
            .anonymousId("anonymousId")
            .lineItems(asList(null, lineItemDraft))
            .textLineItems(asList(null, textLineItemDraft))
            .custom(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", emptyMap()))
            .customer(ResourceIdentifier.ofKey("customerKey"))
            .build();

        final ShoppingListDraft resolvedDraft = referenceResolver
            .resolveReferences(shoppingListDraft)
            .toCompletableFuture()
            .join();

        final ShoppingListDraft expectedDraft = ShoppingListDraftBuilder
            .of(LocalizedString.ofEnglish("name"))
            .key("shoppingList-key")
            .description(LocalizedString.ofEnglish("desc"))
            .slug(LocalizedString.ofEnglish("slug"))
            .deleteDaysAfterLastModification(0)
            .anonymousId("anonymousId")
            .custom(CustomFieldsDraft.ofTypeIdAndJson("typeId", new HashMap<>()))
            .customer(Customer.referenceOfId("customerId").toResourceIdentifier())
            .lineItems(singletonList(LineItemDraftBuilder
                .ofSku("variant-sku", 20L)
                .custom(CustomFieldsDraft.ofTypeIdAndJson("typeId", new HashMap<>()))
                .addedAt(addedAt)
                .build()))
            .textLineItems(singletonList(TextLineItemDraftBuilder
                .of(LocalizedString.ofEnglish("textLineItemName"), 10L)
                .description(LocalizedString.ofEnglish("desc"))
                .custom(CustomFieldsDraft.ofTypeIdAndJson("typeId", new HashMap<>()))
                .addedAt(addedAt)
                .build()))
            .build();

        assertThat(resolvedDraft).isEqualTo(expectedDraft);
    }
}
