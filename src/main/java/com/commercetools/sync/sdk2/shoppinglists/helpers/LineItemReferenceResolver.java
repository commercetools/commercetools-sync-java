package com.commercetools.sync.sdk2.shoppinglists.helpers;

import static java.lang.String.format;

import com.commercetools.api.models.cart.LineItemDraft;
import com.commercetools.api.models.cart.LineItemDraftBuilder;
import com.commercetools.sync.sdk2.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.sdk2.services.TypeService;
import com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptions;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class LineItemReferenceResolver
    extends CustomReferenceResolver<LineItemDraft, LineItemDraftBuilder, ShoppingListSyncOptions> {

  static final String FAILED_TO_RESOLVE_CUSTOM_TYPE =
      "Failed to resolve custom type reference on " + "LineItemDraft with SKU: '%s'.";

  /**
   * Takes a {@link ShoppingListSyncOptions} instance, a {@link TypeService} to instantiate a {@link
   * com.commercetools.sync.sdk2.shoppinglists.helpers.LineItemReferenceResolver} instance that
   * could be used to resolve the custom type references of the line item drafts in the CTP project
   * specified in the injected {@link ShoppingListSyncOptions} instance.
   *
   * @param shoppingListSyncOptions the container of all the options of the sync process including
   *     the CTP project client and/or configuration and other sync-specific options.
   * @param typeService the service to fetch the types for reference resolution.
   */
  public LineItemReferenceResolver(
      @Nonnull final ShoppingListSyncOptions shoppingListSyncOptions,
      @Nonnull final TypeService typeService) {

    super(shoppingListSyncOptions, typeService);
  }

  /**
   * Given a {@link com.commercetools.api.models.cart.LineItemDraft} this method attempts to resolve
   * the custom type reference to return a {@link java.util.concurrent.CompletionStage} which
   * contains a new instance of the draft with the resolved references.
   *
   * @param lineItemDraft the lineItemDraft to resolve its references.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new
   *     lineItemDraft instance with resolved references or, in case an error occurs during
   *     reference resolution, a {@link
   *     com.commercetools.sync.sdk2.commons.exceptions.ReferenceResolutionException}.
   */
  @Override
  @Nonnull
  public CompletionStage<LineItemDraft> resolveReferences(
      @Nonnull final LineItemDraft lineItemDraft) {
    return resolveCustomTypeReference(LineItemDraftBuilder.of(lineItemDraft))
        .thenApply(LineItemDraftBuilder::build);
  }

  @Nonnull
  protected CompletionStage<LineItemDraftBuilder> resolveCustomTypeReference(
      @Nonnull final LineItemDraftBuilder draftBuilder) {

    return resolveCustomTypeReference(
        draftBuilder,
        LineItemDraftBuilder::getCustom,
        LineItemDraftBuilder::custom,
        format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getSku()));
  }
}
