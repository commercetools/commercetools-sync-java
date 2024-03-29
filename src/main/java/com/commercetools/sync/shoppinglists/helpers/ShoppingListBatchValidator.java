package com.commercetools.sync.shoppinglists.helpers;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class ShoppingListBatchValidator
    extends BaseBatchValidator<
        ShoppingListDraft, ShoppingListSyncOptions, ShoppingListSyncStatistics> {

  public static final String SHOPPING_LIST_DRAFT_KEY_NOT_SET =
      "ShoppingListDraft with name: %s doesn't have a key. "
          + "Please make sure all shopping list drafts have keys.";
  public static final String SHOPPING_LIST_DRAFT_IS_NULL = "ShoppingListDraft is null.";
  public static final String SHOPPING_LIST_DRAFT_NAME_NOT_SET =
      "ShoppingListDraft with key: %s doesn't have a name. "
          + "Please make sure all shopping list drafts have names.";
  static final String LINE_ITEM_DRAFT_IS_NULL =
      "LineItemDraft at position '%d' of ShoppingListDraft " + "with key '%s' is null.";
  static final String LINE_ITEM_DRAFT_SKU_NOT_SET =
      "LineItemDraft at position '%d' of "
          + "ShoppingListDraft with key '%s' has no SKU set. Please make sure all lineItems have SKUs.";
  static final String TEXT_LINE_ITEM_DRAFT_IS_NULL =
      "TextLineItemDraft at position '%d' of ShoppingListDraft " + "with key '%s' is null.";
  static final String TEXT_LINE_ITEM_DRAFT_NAME_NOT_SET =
      "TextLineItemDraft at position '%d' of "
          + "ShoppingListDraft with key '%s' has no name set. Please make sure all textLineItems have names.";

  public ShoppingListBatchValidator(
      @Nonnull final ShoppingListSyncOptions syncOptions,
      @Nonnull final ShoppingListSyncStatistics syncStatistics) {
    super(syncOptions, syncStatistics);
  }

  /**
   * Given the {@link java.util.List}&lt;{@link ShoppingListDraft}&gt; of drafts this method
   * attempts to validate drafts and collect referenced type keys from the draft and return an
   * {@link org.apache.commons.lang3.tuple.ImmutablePair}&lt;{@link java.util.Set}&lt; {@link
   * ShoppingListDraft}&gt;,{@link ShoppingListBatchValidator.ReferencedKeys}&gt; which contains the
   * {@link java.util.Set} of valid drafts and referenced keys.
   *
   * <p>A valid shopping list draft is one which satisfies the following conditions:
   *
   * <ol>
   *   <li>It is not null
   *   <li>It has a key which is not blank (null/empty)
   *   <li>It has a name which is not null
   *   <li>It has all lineItems AND textLineItems valid
   *   <li>A lineItem is valid if it satisfies the following conditions:
   *       <ol>
   *         <li>It has a SKU which is not blank (null/empty)
   *       </ol>
   *   <li>A textLineItem is valid if it satisfies the following conditions:
   *       <ol>
   *         <li>It has a name which is not blank (null/empty)
   *       </ol>
   * </ol>
   *
   * @param shoppingListDrafts the shopping list drafts to validate and collect referenced keys.
   * @return {@link org.apache.commons.lang3.tuple.ImmutablePair}&lt;{@link java.util.Set}&lt;{@link
   *     ShoppingListDraft}&gt;, {@link ShoppingListBatchValidator.ReferencedKeys}&gt; which
   *     contains the {@link java.util.Set} of valid drafts and referenced keys within a wrapper.
   */
  @Override
  public ImmutablePair<Set<ShoppingListDraft>, ReferencedKeys> validateAndCollectReferencedKeys(
      @Nonnull final List<ShoppingListDraft> shoppingListDrafts) {
    final ReferencedKeys referencedKeys = new ReferencedKeys();
    final Set<ShoppingListDraft> validDrafts =
        shoppingListDrafts.stream()
            .filter(this::isValidShoppingListDraft)
            .peek(shoppingListDraft -> collectReferencedKeys(referencedKeys, shoppingListDraft))
            .collect(toSet());

    return ImmutablePair.of(validDrafts, referencedKeys);
  }

  private boolean isValidShoppingListDraft(@Nullable final ShoppingListDraft shoppingListDraft) {

    if (shoppingListDraft == null) {
      handleError(SHOPPING_LIST_DRAFT_IS_NULL);
    } else if (isBlank(shoppingListDraft.getKey())) {
      handleError(format(SHOPPING_LIST_DRAFT_KEY_NOT_SET, shoppingListDraft.getName()));
    } else if (isNullOrEmptyLocalizedString(shoppingListDraft.getName())) {
      handleError(format(SHOPPING_LIST_DRAFT_NAME_NOT_SET, shoppingListDraft.getKey()));
    } else {
      final List<String> draftErrors = getErrorsInAllLineItemsAndTextLineItems(shoppingListDraft);
      if (!draftErrors.isEmpty()) {
        final String concatenatedErrors = String.join(",", draftErrors);
        this.handleError(concatenatedErrors);
      } else {
        return true;
      }
    }

    return false;
  }

  @Nonnull
  private List<String> getErrorsInAllLineItemsAndTextLineItems(
      @Nonnull final ShoppingListDraft shoppingListDraft) {
    final List<String> errorMessages = new ArrayList<>();

    if (shoppingListDraft.getLineItems() != null) {
      final List<ShoppingListLineItemDraft> lineItemDrafts = shoppingListDraft.getLineItems();
      for (int i = 0; i < lineItemDrafts.size(); i++) {
        errorMessages.addAll(
            getLineItemDraftErrorsInAllLineItems(
                lineItemDrafts.get(i), i, shoppingListDraft.getKey()));
      }
    }

    if (shoppingListDraft.getTextLineItems() != null) {
      final List<TextLineItemDraft> textLineItems = shoppingListDraft.getTextLineItems();
      for (int i = 0; i < textLineItems.size(); i++) {
        errorMessages.addAll(
            getTextLineItemDraftErrorsInAllTextLineItems(
                textLineItems.get(i), i, shoppingListDraft.getKey()));
      }
    }

    return errorMessages;
  }

  @Nonnull
  private List<String> getLineItemDraftErrorsInAllLineItems(
      @Nullable final ShoppingListLineItemDraft lineItemDraft,
      final int itemPosition,
      @Nonnull final String shoppingListDraftKey) {
    final List<String> errorMessages = new ArrayList<>();
    if (lineItemDraft != null) {
      if (isBlank(lineItemDraft.getSku())) {
        errorMessages.add(format(LINE_ITEM_DRAFT_SKU_NOT_SET, itemPosition, shoppingListDraftKey));
      }
    } else {
      errorMessages.add(format(LINE_ITEM_DRAFT_IS_NULL, itemPosition, shoppingListDraftKey));
    }
    return errorMessages;
  }

  @Nonnull
  private List<String> getTextLineItemDraftErrorsInAllTextLineItems(
      @Nullable final TextLineItemDraft textLineItemDraft,
      final int itemPosition,
      @Nonnull final String shoppingListDraftKey) {
    final List<String> errorMessages = new ArrayList<>();
    if (textLineItemDraft != null) {
      if (isNullOrEmptyLocalizedString(textLineItemDraft.getName())) {
        errorMessages.add(
            format(TEXT_LINE_ITEM_DRAFT_NAME_NOT_SET, itemPosition, shoppingListDraftKey));
      }
    } else {
      errorMessages.add(format(TEXT_LINE_ITEM_DRAFT_IS_NULL, itemPosition, shoppingListDraftKey));
    }
    return errorMessages;
  }

  private boolean isNullOrEmptyLocalizedString(@Nullable final LocalizedString localizedString) {
    return localizedString == null || localizedString.getLocales().isEmpty();
  }

  private void collectReferencedKeys(
      @Nonnull final ReferencedKeys referencedKeys,
      @Nonnull final ShoppingListDraft shoppingListDraft) {

    collectReferencedKeyFromResourceIdentifier(
        shoppingListDraft.getCustomer(), referencedKeys.customerKeys::add);
    collectReferencedKeyFromCustomFieldsDraft(
        shoppingListDraft.getCustom(), referencedKeys.typeKeys::add);
    collectReferencedKeysInLineItems(referencedKeys, shoppingListDraft);
    collectReferencedKeysInTextLineItems(referencedKeys, shoppingListDraft);
  }

  private void collectReferencedKeysInLineItems(
      @Nonnull final ReferencedKeys referencedKeys,
      @Nonnull final ShoppingListDraft shoppingListDraft) {

    if (shoppingListDraft.getLineItems() == null) {
      return;
    }

    shoppingListDraft
        .getLineItems()
        .forEach(
            lineItemDraft ->
                collectReferencedKeyFromCustomFieldsDraft(
                    lineItemDraft.getCustom(), referencedKeys.typeKeys::add));
  }

  private void collectReferencedKeysInTextLineItems(
      @Nonnull final ReferencedKeys referencedKeys,
      @Nonnull final ShoppingListDraft shoppingListDraft) {

    if (shoppingListDraft.getTextLineItems() == null) {
      return;
    }

    shoppingListDraft
        .getTextLineItems()
        .forEach(
            textLineItemDraft ->
                collectReferencedKeyFromCustomFieldsDraft(
                    textLineItemDraft.getCustom(), referencedKeys.typeKeys::add));
  }

  public static class ReferencedKeys {
    private final Set<String> customerKeys = new HashSet<>();
    private final Set<String> typeKeys = new HashSet<>();

    public Set<String> getTypeKeys() {
      return typeKeys;
    }

    public Set<String> getCustomerKeys() {
      return customerKeys;
    }
  }
}
