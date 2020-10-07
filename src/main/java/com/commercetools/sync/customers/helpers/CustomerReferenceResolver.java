package com.commercetools.sync.customers.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.stores.Store;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

public final class CustomerReferenceResolver
    extends CustomReferenceResolver<CustomerDraft, CustomerDraftBuilder, CustomerSyncOptions> {

    public static final String FAILED_TO_RESOLVE_CUSTOMER_GROUP_REFERENCE =
        "Failed to resolve customer group resource identifier on CustomerDraft with key:'%s'. Reason: %s";
    public static final String FAILED_TO_RESOLVE_STORE_REFERENCE =
        "Failed to resolve store resource identifier on CustomerDraft with key:'%s'. Reason: %s";
    public static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
        + "CustomerDraft with key:'%s'.";
    public static final String CUSTOMER_GROUP_DOES_NOT_EXIST = "CustomerGroup with key '%s' doesn't exist.";

    private final CustomerGroupService customerGroupService;

    /**
     * Takes a {@link CustomerSyncOptions} instance, a {@link TypeService} and a {@link CustomerGroupService} to
     * instantiate a {@link CustomerReferenceResolver} instance that could be used to resolve the customer drafts in the
     * CTP project specified in the injected {@link CustomerSyncOptions} instance.
     *
     * @param options     the container of all the options of the sync process including the CTP project client
     *                    and/or configuration and other sync-specific options.
     * @param typeService the service to fetch the custom types for reference resolution.
     * @param customerGroupService the service to fetch the customer groups for reference resolution.
     */
    public CustomerReferenceResolver(
        @Nonnull final CustomerSyncOptions options,
        @Nonnull final TypeService typeService,
        @Nonnull final CustomerGroupService customerGroupService) {
        super(options, typeService);
        this.customerGroupService = customerGroupService;
    }

    /**
     * Given a {@link CustomerDraft} this method attempts to resolve the stores, customer group and custom type
     * references to return a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * references or, in case an error occurs during reference resolution, a {@link ReferenceResolutionException}.
     *
     * @param customerDraft the draft to resolve its references.
     * @return a {@link CompletionStage} that contains as a result a new CustomerDraft instance with resolved
     *          custom type reference or, in case an error occurs during reference resolution,
     *          a {@link ReferenceResolutionException}.
     */
    @Override
    @Nonnull
    public CompletionStage<CustomerDraft> resolveReferences(@Nonnull final CustomerDraft customerDraft) {
        return resolveCustomTypeReference(CustomerDraftBuilder.of(customerDraft))
            .thenCompose(this::resolveCustomerGroupReference)
            .thenCompose(this::resolveStoreReferences)
            .thenApply(CustomerDraftBuilder::build);
    }

    @Override
    protected CompletionStage<CustomerDraftBuilder> resolveCustomTypeReference(
        @Nonnull final CustomerDraftBuilder draftBuilder) {

        return resolveCustomTypeReference(draftBuilder,
            CustomerDraftBuilder::getCustom,
            CustomerDraftBuilder::custom,
            format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getKey()));
    }

    /**
     * Given a {@link CustomerDraftBuilder} this method attempts to resolve the customer group to return a
     * {@link CompletionStage} which contains a new instance of the builder with the resolved customer group reference.
     *
     * @param draftBuilder the customerDraft to resolve its customer group reference.
     * @return a {@link CompletionStage} that contains as a result a new builder instance with resolved customer group
     *         reference or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<CustomerDraftBuilder> resolveCustomerGroupReference(
        @Nonnull final CustomerDraftBuilder draftBuilder) {

        final ResourceIdentifier<CustomerGroup> customerGroupResourceIdentifier = draftBuilder.getCustomerGroup();
        if (customerGroupResourceIdentifier != null && customerGroupResourceIdentifier.getId() == null) {
            String customerGroupKey;
            try {
                customerGroupKey = getKeyFromResourceIdentifier(customerGroupResourceIdentifier);
            } catch (ReferenceResolutionException referenceResolutionException) {
                return exceptionallyCompletedFuture(new ReferenceResolutionException(
                    format(FAILED_TO_RESOLVE_CUSTOMER_GROUP_REFERENCE, draftBuilder.getKey(),
                        referenceResolutionException.getMessage())));
            }

            return fetchAndResolveCustomerGroupReference(draftBuilder, customerGroupKey);
        }
        return completedFuture(draftBuilder);
    }

    @Nonnull
    private CompletionStage<CustomerDraftBuilder> fetchAndResolveCustomerGroupReference(
        @Nonnull final CustomerDraftBuilder draftBuilder,
        @Nonnull final String customerGroupKey) {

        return customerGroupService
            .fetchCachedCustomerGroupId(customerGroupKey)
            .thenCompose(resolvedCustomerGroupIdOptional -> resolvedCustomerGroupIdOptional
                .map(resolvedCustomerGroupId ->
                    completedFuture(draftBuilder.customerGroup(
                        CustomerGroup.referenceOfId(resolvedCustomerGroupId).toResourceIdentifier())))
                .orElseGet(() -> {
                    final String errorMessage = format(CUSTOMER_GROUP_DOES_NOT_EXIST, customerGroupKey);
                    return exceptionallyCompletedFuture(new ReferenceResolutionException(
                        format(FAILED_TO_RESOLVE_CUSTOMER_GROUP_REFERENCE, draftBuilder.getKey(),
                            errorMessage)));
                }));
    }

    /**
     * Given a {@link CustomerDraftBuilder} this method attempts to resolve the stores and return
     * a {@link CompletionStage} which contains a new instance of the builder with the resolved references.
     *
     * @param draftBuilder the customer draft to resolve its store references.
     * @return a {@link CompletionStage} that contains as a result a new builder instance with resolved references or,
     *         in case an error occurs during reference resolution, a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<CustomerDraftBuilder> resolveStoreReferences(
        @Nonnull final CustomerDraftBuilder draftBuilder) {

        final List<ResourceIdentifier<Store>> storeResourceIdentifiers = draftBuilder.getStores();
        if (storeResourceIdentifiers == null || storeResourceIdentifiers.isEmpty()) {
            return completedFuture(draftBuilder);
        }
        final List<ResourceIdentifier<Store>> resolvedReferences = new ArrayList<>();
        for (ResourceIdentifier<Store> storeResourceIdentifier : storeResourceIdentifiers) {
            if (storeResourceIdentifier != null) {
                if (storeResourceIdentifier.getId() == null) {
                    try {
                        final String storeKey = getKeyFromResourceIdentifier(storeResourceIdentifier);
                        resolvedReferences.add(ResourceIdentifier.ofKey(storeKey));
                    } catch (ReferenceResolutionException referenceResolutionException) {
                        return exceptionallyCompletedFuture(
                            new ReferenceResolutionException(
                                format(FAILED_TO_RESOLVE_STORE_REFERENCE,
                                    draftBuilder.getKey(), referenceResolutionException.getMessage())));
                    }
                } else {
                    resolvedReferences.add(ResourceIdentifier.ofId(storeResourceIdentifier.getId()));
                }
            }
        }
        return completedFuture(draftBuilder.stores(resolvedReferences));
    }
}
