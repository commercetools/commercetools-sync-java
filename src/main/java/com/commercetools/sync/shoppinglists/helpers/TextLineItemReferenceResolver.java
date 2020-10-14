package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.states.helpers.StateReferenceResolver;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
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


public final class TextLineItemReferenceResolver
    extends BaseReferenceResolver<TextLineItemDraft, ShoppingListSyncOptions> {

    private final TypeService typeService;

    private static final String TYPE_DOES_NOT_EXIST = "Type with key '%s' doesn't exist.";
    private static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
            + "TextLineItemDraft with key:'%s'. Reason: %s";

    /**
     * Takes a {@link ShoppingListSyncOptions} instance, a {@link StateService} to instantiate a
     * {@link StateReferenceResolver} instance that could be used to resolve the category drafts in the
     * CTP project specified in the injected {@link ShoppingListSyncOptions} instance.
     *
     * @param shoppingListSyncOptions   the container of all the options of the sync process including the CTP project client
     *                                  and/or configuration and other sync-specific options.
     * @param typeService       the service to fetch the states for reference resolution.
     */
    public TextLineItemReferenceResolver(@Nonnull final ShoppingListSyncOptions shoppingListSyncOptions,
                                         @Nonnull final TypeService typeService) {
        super(shoppingListSyncOptions);
        this.typeService = typeService;
    }

    /**
     * Given a {@link TextLineItemDraft} this method attempts to resolve the attribute definition references to return
     * a {@link CompletionStage} which contains a new instance of the draft with the resolved references.
     *
     * @param textLineItemDraft the textLineItemDraft to resolve its references.
     * @return a {@link CompletionStage} that contains as a result a new textLineItemDraft instance with resolved
     *         references or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Override
    @Nonnull
    public CompletionStage<TextLineItemDraft> resolveReferences(@Nonnull final TextLineItemDraft textLineItemDraft) {
        return resolveCustomTypeReference(TextLineItemDraftBuilder.of(textLineItemDraft))
                .thenApply(TextLineItemDraftBuilder::build);
    }

    @Nonnull
    private CompletionStage<TextLineItemDraftBuilder> resolveCustomTypeReference(
            @Nonnull final TextLineItemDraftBuilder draftBuilder) {
        final Function<TextLineItemDraftBuilder, CustomFieldsDraft> customGetter = TextLineItemDraftBuilder::getCustom;
        final BiFunction<
                TextLineItemDraftBuilder,
                CustomFieldsDraft,
                TextLineItemDraftBuilder> customSetter = TextLineItemDraftBuilder::custom;
        final String errorMessage  = format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getName());

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
    private CompletionStage<TextLineItemDraftBuilder> fetchAndResolveTypeReference(
            @Nonnull final TextLineItemDraftBuilder draftBuilder,
            @Nonnull final BiFunction<
                    TextLineItemDraftBuilder,
                    CustomFieldsDraft,
                    TextLineItemDraftBuilder> customSetter,
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
