package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

public final class ShoppingListReferenceResolver
    extends CustomReferenceResolver<ShoppingListDraft, ShoppingListDraftBuilder, ShoppingListSyncOptions> {

    static final String FAILED_TO_RESOLVE_CUSTOMER_REFERENCE = "Failed to resolve customer resource identifier on "
        + "ShoppingListDraft with key:'%s'. Reason: %s";
    static final String CUSTOMER_DOES_NOT_EXIST = "Customer with key '%s' doesn't exist.";
    static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
        + "ShoppingListDraft with key:'%s'. ";

    private final CustomerService customerService;
    private final TypeService typeService;
    private final LineItemReferenceResolver lineItemReferenceResolver;
    private final TextLineItemReferenceResolver textLineItemReferenceResolver;

    /**
     * Takes a {@link ShoppingListSyncOptions} instance, a {@link CustomerService} and {@link TypeService} to
     * instantiate a {@link ShoppingListReferenceResolver} instance that could be used to resolve the shopping list
     * drafts in the CTP project specified in the injected {@link ShoppingListSyncOptions} instance.
     *
     * @param shoppingListSyncOptions the container of all the options of the sync process including the CTP project
     *                                client and/or configuration and other sync-specific options.
     * @param customerService         the service to fetch the customers for reference resolution.
     * @param typeService             the service to fetch the types for reference resolution.
     */
    public ShoppingListReferenceResolver(@Nonnull final ShoppingListSyncOptions shoppingListSyncOptions,
                                         @Nonnull final CustomerService customerService,
                                         @Nonnull final TypeService typeService) {

        super(shoppingListSyncOptions, typeService);
        this.lineItemReferenceResolver = new LineItemReferenceResolver(shoppingListSyncOptions, typeService);
        this.textLineItemReferenceResolver = new TextLineItemReferenceResolver(shoppingListSyncOptions, typeService);
        this.customerService = customerService;
        this.typeService = typeService;
    }

    /**
     * Given a {@link ShoppingListDraft} this method attempts to resolve the customer and custom type references to
     * return a {@link CompletionStage} which contains a new instance of the draft with the resolved references.
     *
     * @param shoppingListDraft the shoppingListDraft to resolve its references.
     * @return a {@link CompletionStage} that contains as a result a new shoppingListDraft instance with resolved
     *         references or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<ShoppingListDraft> resolveReferences(@Nonnull final ShoppingListDraft shoppingListDraft) {
        return resolveCustomerReference(ShoppingListDraftBuilder.of(shoppingListDraft))
            .thenCompose(this::resolveCustomTypeReference)
            .thenCompose(this::resolveLineItemReferences)
            .thenCompose(this::resolveTextLineItemReferences)
            .thenApply(ShoppingListDraftBuilder::build);
    }

    @Nonnull
    protected CompletionStage<ShoppingListDraftBuilder> resolveCustomerReference(
        @Nonnull final ShoppingListDraftBuilder draftBuilder) {

        final ResourceIdentifier<Customer> customerResourceIdentifier = draftBuilder.getCustomer();
        if (customerResourceIdentifier != null && customerResourceIdentifier.getId() == null) {
            String customerKey;
            try {
                customerKey = getKeyFromResourceIdentifier(customerResourceIdentifier);
            } catch (ReferenceResolutionException referenceResolutionException) {
                return exceptionallyCompletedFuture(new ReferenceResolutionException(
                    format(FAILED_TO_RESOLVE_CUSTOMER_REFERENCE, draftBuilder.getKey(),
                        referenceResolutionException.getMessage())));
            }

            return fetchAndResolveCustomerReference(draftBuilder, customerKey);
        }
        return completedFuture(draftBuilder);
    }

    @Nonnull
    private CompletionStage<ShoppingListDraftBuilder> fetchAndResolveCustomerReference(
        @Nonnull final ShoppingListDraftBuilder draftBuilder,
        @Nonnull final String customerKey) {

        return customerService
            .fetchCachedCustomerId(customerKey)
            .thenCompose(resolvedCustomerIdOptional -> resolvedCustomerIdOptional
                .map(resolvedCustomerId ->
                    completedFuture(draftBuilder.customer(
                        Customer.referenceOfId(resolvedCustomerId).toResourceIdentifier())))
                .orElseGet(() -> {
                    final String errorMessage = format(CUSTOMER_DOES_NOT_EXIST, customerKey);
                    return exceptionallyCompletedFuture(new ReferenceResolutionException(
                        format(FAILED_TO_RESOLVE_CUSTOMER_REFERENCE, draftBuilder.getKey(), errorMessage)));
                }));
    }

    @Nonnull
    protected CompletionStage<ShoppingListDraftBuilder> resolveCustomTypeReference(
        @Nonnull final ShoppingListDraftBuilder draftBuilder) {

        return resolveCustomTypeReference(
            draftBuilder,
            ShoppingListDraftBuilder::getCustom,
            ShoppingListDraftBuilder::custom,
            format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getKey()));
    }

    @Nonnull
    private CompletionStage<ShoppingListDraftBuilder> resolveLineItemReferences(
        @Nonnull final ShoppingListDraftBuilder draftBuilder) {

        if (draftBuilder.getLineItems() != null) {
            return mapValuesToFutureOfCompletedValues(
                draftBuilder.getLineItems(), lineItemReferenceResolver::resolveReferences, toList())
                .thenApply(draftBuilder::lineItems);
        }

        return completedFuture(draftBuilder);
    }

    @Nonnull
    private CompletionStage<ShoppingListDraftBuilder> resolveTextLineItemReferences(
        @Nonnull final ShoppingListDraftBuilder draftBuilder) {

        if (draftBuilder.getTextLineItems() != null) {
            return mapValuesToFutureOfCompletedValues(
                draftBuilder.getTextLineItems(), textLineItemReferenceResolver::resolveReferences, toList())
                .thenApply(draftBuilder::textLineItems);
        }

        return completedFuture(draftBuilder);
    }

    /**
     * Calls the {@code cacheKeysToIds} service methods to fetch all the referenced keys
     * (i.e custom type, customer) from the CTP to populate caches for the reference resolution.
     *
     * <p>Note: This method is only to be used internally by the library to improve performance.
     *
     * @param referencedKeys a wrapper for the custom type and customer references to fetch the keys, and store the
     *                       corresponding keys -&gt; ids into cached maps.
     * @return {@link CompletionStage}&lt;{@link Map}&lt;{@link String}&gt;{@link String}&gt;&gt; in which the results
     *     of its completions contains a map of requested references keys -&gt; ids of customer references.
     */
    @Nonnull
    public CompletableFuture<List<Map<String, String>>> populateKeyToIdCachesForReferencedKeys(
        @Nonnull final ShoppingListBatchValidator.ReferencedKeys referencedKeys) {

        final List<CompletionStage<Map<String, String>>> futures = new ArrayList<>();

        final Set<String> typeKeys = referencedKeys.getTypeKeys();
        if (!typeKeys.isEmpty()) {
            futures.add(typeService.cacheKeysToIds(typeKeys));
        }

        final Set<String> customerKeys = referencedKeys.getCustomerKeys();
        if (!customerKeys.isEmpty()) {
            futures.add(customerService.cacheKeysToIds(customerKeys));
        }

        return collectionOfFuturesToFutureOfCollection(futures, toList());
    }
}
