package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.*;

import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.Locale;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.MockUtils.getMockCustomerService;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_REFERENCE;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListReferenceResolver.CUSTOMER_DOES_NOT_EXIST;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListReferenceResolver.FAILED_TO_RESOLVE_REFERENCE;
import static java.lang.String.format;
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

        // test
        final ShoppingListDraftBuilder resolvedDraft = referenceResolver
                .resolveCustomTypeReference(draftBuilder)
                .toCompletableFuture().join();

        // assertion
        assertThat(resolvedDraft.getCustom()).isNotNull();
        assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo("typeId");

    }

    @Test
    void resolveCustomTypeReference_WithNonNullKeyOnCustomTypeResId_ShouldResolveCustomTypeReference() {
        final String customTypeId = "customTypeId";
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
                .ofTypeIdAndJson(customTypeId, new HashMap<>());

        final ShoppingListDraftBuilder draftBuilder =
                ShoppingListDraftBuilder
                        .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                        .custom(customFieldsDraft)
                        .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        // test
        final ShoppingListDraftBuilder resolvedDraft = referenceResolver
                .resolveCustomTypeReference(draftBuilder)
                .toCompletableFuture().join();

        // assertion
        assertThat(resolvedDraft.getCustom()).isNotNull();
        assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo(customTypeId);

    }

    @Test
    void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
        // preparation
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

        // test
        final CompletionStage<ShoppingListDraftBuilder> resolvedDraftCompletionStage = referenceResolver
                .resolveCustomTypeReference(draftBuilder);

        // assertion
        assertThat(resolvedDraftCompletionStage)
                .isCompletedExceptionally()
                .withFailMessage("CTP error on fetch");
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
                format("%s Reason: %s", expectedExceptionMessage, format(TYPE_DOES_NOT_EXIST, customTypeKey));;
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

        // assertion
        assertThat(referenceResolver.resolveCustomTypeReference(draftBuilder))
                .hasFailedWithThrowableThat()
                .isExactlyInstanceOf(ReferenceResolutionException.class)
                .hasMessage(format("Failed to resolve custom type reference on ShoppingListDraft"
                        + " with key:'null'.  Reason: %s", BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));

    }

    @Test
    void resolveCustomerReference_WithNonNullKeyOnCustomerKey_ShouldResolveCustomerReference() {
        final Reference<Customer> customerReference = Reference.of(Customer.referenceTypeId(), "customerKey");

        final ShoppingListDraftBuilder draftBuilder =
                ShoppingListDraftBuilder
                        .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                        .customer(customerReference)
                        .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        // test
        final ShoppingListDraftBuilder resolvedDraft = referenceResolver
                .resolveCustomTypeReference(draftBuilder)
                .toCompletableFuture().join();

        // assertion
        assertThat(resolvedDraft.getCustomer()).isNotNull();
        assertThat(resolvedDraft.getCustomer().getId()).isEqualTo("customerKey");

    }

    @Test
    void resolveCustomerReferences_WithNullCustomerReferences_ShouldNotResolveCustomerReferences() {
        // preparation
        final ShoppingListDraftBuilder draftBuilder =
                ShoppingListDraftBuilder
                        .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                        .customer(null)
                        .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        // test
        final ShoppingListDraftBuilder referencesResolvedDraft =
                referenceResolver.resolveCustomerReference(draftBuilder).toCompletableFuture().join();

        // assertion
        assertThat(referencesResolvedDraft.getCustom()).isNull();

    }

    @Test
    void resolveCustomerReference_WithNullCustomerKey_ShouldNotResolveCustomerReference() {
        // preparation
        final String customerKey = null;
        final Reference<Customer> customerReference = Reference.of(Customer.referenceTypeId(), customerKey);

        final ShoppingListDraftBuilder draftBuilder =
                ShoppingListDraftBuilder
                        .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                        .customer(customerReference)
                        .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        // test and assertion
        assertThat(referenceResolver.resolveCustomerReference(draftBuilder))
                .hasFailedWithThrowableThat()
                .isExactlyInstanceOf(ReferenceResolutionException.class)
                .hasMessage(format(FAILED_TO_RESOLVE_REFERENCE,
                        draftBuilder.getKey(), BLANK_ID_VALUE_ON_REFERENCE));
    }

    @Test
    void resolveCustomerReference_WithEmptyCustomerKey_ShouldNotResolveCustomerReference() {
        // preparation
        final String customerKey = "";
        final Reference<Customer> customerReference = Reference.of(Customer.referenceTypeId(), customerKey);

        final ShoppingListDraftBuilder draftBuilder =
                ShoppingListDraftBuilder
                        .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                        .customer(customerReference)
                        .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        // test and assertion
        assertThat(referenceResolver.resolveCustomerReference(draftBuilder))
                .hasFailedWithThrowableThat()
                .isExactlyInstanceOf(ReferenceResolutionException.class)
                .hasMessage(format(FAILED_TO_RESOLVE_REFERENCE,
                        draftBuilder.getKey(), BLANK_ID_VALUE_ON_REFERENCE));
    }

    @Test
    void resolveCustomerReference_WithNonExistingCustomerId_ShouldNotResolveCustomerReference() {
        // preparation
        when(customerService.fetchCachedCustomerId(anyString()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final String customerKey = "non-existing-customer-key";
        final Reference<Customer> customerReference = Reference.of(Customer.referenceTypeId(), customerKey);

        final ShoppingListDraftBuilder draftBuilder =
                ShoppingListDraftBuilder
                        .of(LocalizedString.of(Locale.ENGLISH, "NAME"))
                        .customer(customerReference)
                        .description(LocalizedString.of(Locale.ENGLISH, "DESCRIPTION"));

        // test and assertion
        assertThat(referenceResolver.resolveCustomerReference(draftBuilder))
                .hasFailedWithThrowableThat()
                .isExactlyInstanceOf(ReferenceResolutionException.class)
                .hasMessage(format(ShoppingListReferenceResolver.FAILED_TO_RESOLVE_REFERENCE,
                        draftBuilder.getKey(), format(CUSTOMER_DOES_NOT_EXIST, customerKey)));
    }
}
