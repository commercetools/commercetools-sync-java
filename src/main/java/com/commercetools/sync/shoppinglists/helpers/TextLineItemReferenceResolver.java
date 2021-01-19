package com.commercetools.sync.shoppinglists.helpers;

import static java.lang.String.format;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class TextLineItemReferenceResolver
    extends CustomReferenceResolver<
        TextLineItemDraft, TextLineItemDraftBuilder, ShoppingListSyncOptions> {

  static final String FAILED_TO_RESOLVE_CUSTOM_TYPE =
      "Failed to resolve custom type reference on " + "TextLineItemDraft with name: '%s'.";

  /**
   * Takes a {@link ShoppingListSyncOptions} instance, a {@link TypeService} to instantiate a {@link
   * TextLineItemReferenceResolver} instance that could be used to resolve the text line-item drafts
   * in the CTP project specified in the injected {@link ShoppingListSyncOptions} instance.
   *
   * @param shoppingListSyncOptions the container of all the options of the sync process including
   *     the CTP project client and/or configuration and other sync-specific options.
   * @param typeService the service to fetch the types for reference resolution.
   */
  public TextLineItemReferenceResolver(
      @Nonnull final ShoppingListSyncOptions shoppingListSyncOptions,
      @Nonnull final TypeService typeService) {

    super(shoppingListSyncOptions, typeService);
  }

  /**
   * Given a {@link TextLineItemDraft} this method attempts to resolve the attribute definition
   * references to return a {@link CompletionStage} which contains a new instance of the draft with
   * the resolved references.
   *
   * @param textLineItemDraft the textLineItemDraft to resolve its references.
   * @return a {@link CompletionStage} that contains as a result a new textLineItemDraft instance
   *     with resolved references or, in case an error occurs during reference resolution, a {@link
   *     ReferenceResolutionException}.
   */
  @Override
  @Nonnull
  public CompletionStage<TextLineItemDraft> resolveReferences(
      @Nonnull final TextLineItemDraft textLineItemDraft) {
    return resolveCustomTypeReference(TextLineItemDraftBuilder.of(textLineItemDraft))
        .thenApply(TextLineItemDraftBuilder::build);
  }

  @Nonnull
  protected CompletionStage<TextLineItemDraftBuilder> resolveCustomTypeReference(
      @Nonnull final TextLineItemDraftBuilder draftBuilder) {

    return resolveCustomTypeReference(
        draftBuilder,
        TextLineItemDraftBuilder::getCustom,
        TextLineItemDraftBuilder::custom,
        format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getName()));
  }
}
