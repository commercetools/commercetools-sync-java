package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.sphere.sdk.types.CustomFieldsDraft.ofTypeIdAndJson;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;


public final class LineItemReferenceResolver
    extends BaseReferenceResolver<LineItemDraft, ShoppingListSyncOptions> {

    private final TypeService typeService;

    private static final String TYPE_DOES_NOT_EXIST = "Type with key '%s' doesn't exist.";
    private static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
            + "LineItemDraft with key:'%s'. Reason: %s";

    /**
     * Takes a {@link ShoppingListSyncOptions} instance, a {@link TypeService} to instantiate a
     * {@link LineItemReferenceResolver} instance that could be used to resolve the line-item drafts in the
     * CTP project specified in the injected {@link ShoppingListSyncOptions} instance.
     *
     * @param shoppingListSyncOptions   the container of all the options of the sync process including the CTP project client
     *                                  and/or configuration and other sync-specific options.
     * @param typeService       the service to fetch the states for reference resolution.
     */
    public LineItemReferenceResolver(@Nonnull final ShoppingListSyncOptions shoppingListSyncOptions,

                                     @Nonnull final TypeService typeService) {
        super(shoppingListSyncOptions);
        this.typeService = typeService;
    }

    /**
     * Given a {@link ShoppingListDraft} this method attempts to resolve the attribute definition references to return
     * a {@link CompletionStage} which contains a new instance of the draft with the resolved references.
     *
     * @param lineItemDraft the shoppingListDraft to resolve its references.
     * @return a {@link CompletionStage} that contains as a result a new shoppingListDraft instance with resolved
     *         references or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Override
    @Nonnull
    public CompletionStage<LineItemDraft> resolveReferences(@Nonnull final LineItemDraft lineItemDraft) {
        return resolveCustomTypeReference(LineItemDraftBuilder.of(lineItemDraft))
                .thenApply(LineItemDraftBuilder::build);
    }

    @Nonnull
    private CompletionStage<LineItemDraftBuilder> resolveCustomTypeReference(
            @Nonnull final LineItemDraftBuilder draftBuilder) {
        final Function<LineItemDraftBuilder, CustomFieldsDraft> customGetter = LineItemDraftBuilder::getCustom;
        final BiFunction<
                LineItemDraftBuilder,
                CustomFieldsDraft,
                LineItemDraftBuilder> customSetter = LineItemDraftBuilder::custom;
        final String errorMessage  = format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getSku());

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
    private CompletionStage<LineItemDraftBuilder> fetchAndResolveTypeReference(
            @Nonnull final LineItemDraftBuilder draftBuilder,
            @Nonnull final BiFunction<
                    LineItemDraftBuilder,
                    CustomFieldsDraft,
                    LineItemDraftBuilder> customSetter,
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
