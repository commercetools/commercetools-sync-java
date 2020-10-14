package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;

import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.states.helpers.StateReferenceResolver;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.shoppinglists.*;
import io.sphere.sdk.states.State;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;

import static io.sphere.sdk.types.CustomFieldsDraft.ofTypeIdAndJson;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

public final class ShoppingListReferenceResolver
    extends BaseReferenceResolver<ShoppingListDraft, ShoppingListSyncOptions> {

    private final CustomerService customerService;
    private final TypeService typeService;
    private final LineItemReferenceResolver lineItemReferenceResolver;
    private final TextLineItemReferenceResolver textLineItemReferenceResolver;

    private static final String FAILED_TO_RESOLVE_REFERENCE = "Failed to resolve 'transition' reference on "
            + "ShoppingListDraft with key:'%s'. Reason: %s";
    private static final String CUSTOMER_DOES_NOT_EXIST = "Customer with key '%s' doesn't exist.";
    private static final String TYPE_DOES_NOT_EXIST = "Type with key '%s' doesn't exist.";
    private static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
            + "ShoppingListDraft with key:'%s'. Reason: %s";

    /**
     * Takes a {@link ShoppingListSyncOptions} instance, a {@link StateService} to instantiate a
     * {@link StateReferenceResolver} instance that could be used to resolve the category drafts in the
     * CTP project specified in the injected {@link ShoppingListSyncOptions} instance.
     *
     * @param shoppingListSyncOptions   the container of all the options of the sync process including the CTP project client
     *                                  and/or configuration and other sync-specific options.
     * @param customerService       the service to fetch the states for reference resolution.
     */
    public ShoppingListReferenceResolver(@Nonnull final ShoppingListSyncOptions shoppingListSyncOptions,

                                        @Nonnull final CustomerService customerService,
                                        @Nonnull final TypeService typeService) {
        super(shoppingListSyncOptions);
        this.lineItemReferenceResolver = new LineItemReferenceResolver(shoppingListSyncOptions, typeService);
        this.textLineItemReferenceResolver = new TextLineItemReferenceResolver(shoppingListSyncOptions, typeService);
        this.customerService = customerService;
        this.typeService = typeService;
    }

    /**
     * Given a {@link ShoppingListDraft} this method attempts to resolve the attribute definition references to return
     * a {@link CompletionStage} which contains a new instance of the draft with the resolved references.
     *
     * @param shoppingListDraft the shoppingListDraft to resolve its references.
     * @return a {@link CompletionStage} that contains as a result a new shoppingListDraft instance with resolved
     *         references or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<ShoppingListDraft> resolveReferences(@Nonnull final ShoppingListDraft shoppingListDraft) {
        return resolveCustomerReferences(ShoppingListDraftBuilder.of(shoppingListDraft))
                    .thenCompose(this::resolveCustomTypeReference)
                    .thenCompose(this::resolveLineItemReferences)
                    .thenCompose(this::resolveTextLineItemReferences)
                    .thenApply(ShoppingListDraftBuilder::build);
    }

    @Nonnull
    private CompletionStage<ShoppingListDraftBuilder> resolveCustomerReferences(
            @Nonnull final ShoppingListDraftBuilder draftBuilder) {

        final Reference<Customer> customerReference = draftBuilder.getCustomer();
        if (customerReference != null) {
            String customerKey;
            try {
                customerKey = getIdFromReference(customerReference);
            } catch (ReferenceResolutionException referenceResolutionException) {
                return exceptionallyCompletedFuture(new ReferenceResolutionException(
                    format(FAILED_TO_RESOLVE_REFERENCE, State.referenceTypeId(), draftBuilder.getKey(),
                        referenceResolutionException.getMessage())));
            }

            return fetchAndResolveCustomerReference(draftBuilder, customerKey);
        }
        return completedFuture(draftBuilder);
    }

    @Nonnull
    private CompletionStage<ShoppingListDraftBuilder> resolveCustomTypeReference(
            @Nonnull final ShoppingListDraftBuilder draftBuilder) {
        final Function<ShoppingListDraftBuilder, CustomFieldsDraft> customGetter = ShoppingListDraftBuilder::getCustom;
        final BiFunction<
                ShoppingListDraftBuilder,
                CustomFieldsDraft,
                ShoppingListDraftBuilder> customSetter = ShoppingListDraftBuilder::custom;
        final String errorMessage  = format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getKey());

        final CustomFieldsDraft custom = customGetter.apply(draftBuilder);

        if (custom != null) {
            final ResourceIdentifier<Type> customType = custom.getType();

            if (customType.getId() == null) {
                String customTypeKey;

                try {
                    customTypeKey = getCustomTypeKey(customType, errorMessage);
                } catch (ReferenceResolutionException referenceResolutionException) {
                    return exceptionallyCompletedFuture(referenceResolutionException);
                }

                return fetchAndResolveTypeReference(draftBuilder, customSetter, custom.getFields(), customTypeKey,
                        errorMessage);
            }
        }
        return completedFuture(draftBuilder);
    }

    @Nonnull
    private CompletionStage<ShoppingListDraftBuilder> resolveLineItemReferences(
            @Nonnull final ShoppingListDraftBuilder draftBuilder) {

        final List<LineItemDraft> lineItemDrafts = draftBuilder.getLineItems();

        if (lineItemDrafts != null && !lineItemDrafts.isEmpty()) {
            return mapValuesToFutureOfCompletedValues(lineItemDrafts,
                    lineItemReferenceResolver::resolveReferences, toList())
                    .thenApply(draftBuilder::lineItems);
        }
        return completedFuture(draftBuilder);
    }

    @Nonnull
    private CompletionStage<ShoppingListDraftBuilder> resolveTextLineItemReferences(
            @Nonnull final ShoppingListDraftBuilder draftBuilder) {

        final List<TextLineItemDraft> textLineItemDrafts = draftBuilder.getTextLineItems();

        if (textLineItemDrafts != null && !textLineItemDrafts.isEmpty()) {
            return mapValuesToFutureOfCompletedValues(textLineItemDrafts,
                    textLineItemReferenceResolver::resolveReferences, toList())
                    .thenApply(draftBuilder::textLineItems);
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
                    completedFuture(draftBuilder.customer(Customer.referenceOfId(resolvedCustomerId))))
                .orElseGet(() -> {
                    final String errorMessage = format(CUSTOMER_DOES_NOT_EXIST, customerKey);
                    return exceptionallyCompletedFuture(new ReferenceResolutionException(
                        format(FAILED_TO_RESOLVE_REFERENCE, State.referenceTypeId(), draftBuilder.getKey(),
                                errorMessage)));
                }));
    }

    @Nonnull
    private String getCustomTypeKey(
            @Nonnull final ResourceIdentifier<Type> customType,
            @Nonnull final String referenceResolutionErrorMessage) throws ReferenceResolutionException {

        try {
            return getKeyFromResourceIdentifier(customType);
        } catch (ReferenceResolutionException exception) {
            final String errorMessage =
                    format("%s Reason: %s", referenceResolutionErrorMessage, exception.getMessage());
            throw new ReferenceResolutionException(errorMessage, exception);
        }
    }

    @Nonnull
    private CompletionStage<ShoppingListDraftBuilder> fetchAndResolveTypeReference(
            @Nonnull final ShoppingListDraftBuilder draftBuilder,
            @Nonnull final BiFunction<
                    ShoppingListDraftBuilder,
                    CustomFieldsDraft,
                    ShoppingListDraftBuilder> customSetter,
            @Nullable final Map<String, JsonNode> customFields,
            @Nonnull final String typeKey,
            @Nonnull final String referenceResolutionErrorMessage) {

        return typeService
                .fetchCachedTypeId(typeKey)
                .thenCompose(resolvedTypeIdOptional -> resolvedTypeIdOptional
                        .map(resolvedTypeId ->
                                completedFuture(
                                        customSetter.apply(draftBuilder, ofTypeIdAndJson(resolvedTypeId, customFields))))
                        .orElseGet(() -> {
                            final String errorMessage =
                                    format("%s Reason: %s",
                                            referenceResolutionErrorMessage,
                                            format(TYPE_DOES_NOT_EXIST, typeKey));
                            return exceptionallyCompletedFuture(new ReferenceResolutionException(errorMessage));
                        }));
    }


}
