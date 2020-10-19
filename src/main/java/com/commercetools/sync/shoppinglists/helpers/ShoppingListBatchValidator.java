package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ShoppingListBatchValidator
    extends BaseBatchValidator<ShoppingListDraft, ShoppingListSyncOptions, ShoppingListSyncStatistics> {

    static final String SHOPPING_LIST_DRAFT_KEY_NOT_SET = "ShoppingListDraft with name: %s doesn't have a key. "
        + "Please make sure all shopping list drafts have keys.";
    static final String SHOPPING_LIST_DRAFT_IS_NULL = "ShoppingListDraft is null.";
    static final String SHOPPING_LIST_DRAFT_NAME_NOT_SET = "ShoppingListDraft with key: %s doesn't have a name. "
        + "Please make sure all shopping list drafts have names.";

    public ShoppingListBatchValidator(@Nonnull final ShoppingListSyncOptions syncOptions,
                                      @Nonnull final ShoppingListSyncStatistics syncStatistics) {
        super(syncOptions, syncStatistics);
    }

    /**
     * Given the {@link List}&lt;{@link ShoppingListDraft}&gt; of drafts this method attempts to validate
     * drafts and collect referenced type keys from the draft and return an {@link ImmutablePair}&lt;{@link Set}&lt;
     * {@link ShoppingListDraft}&gt;,{@link ReferencedKeys}&gt; which contains the {@link Set} of valid drafts and
     * referenced keys.
     *
     * <p>A valid shopping list draft is one which satisfies the following conditions:
     * <ol>
     * <li>It is not null</li>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has a name which is not null</li>
     * </ol>
     *
     * @param shoppingListDrafts the shopping list drafts to validate and collect referenced type keys.
     * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link ShoppingListDraft}&gt;,
     * {@link ReferencedKeys}&gt; which contains the {@link Set} of valid drafts and
     *      referenced keys within a wrapper.
     */
    @Override
    public ImmutablePair<Set<ShoppingListDraft>, ReferencedKeys> validateAndCollectReferencedKeys(
        @Nonnull final List<ShoppingListDraft> shoppingListDrafts) {
        final ReferencedKeys referencedKeys = new ReferencedKeys();
        final Set<ShoppingListDraft> validDrafts = shoppingListDrafts
            .stream()
            .filter(this::isValidShoppingListDraft)
            .peek(shoppingListDraft ->
                collectReferencedKeys(referencedKeys, shoppingListDraft))
            .collect(Collectors.toSet());

        return ImmutablePair.of(validDrafts, referencedKeys);
    }

    private boolean isValidShoppingListDraft(
        @Nullable final ShoppingListDraft shoppingListDraft) {

        if (shoppingListDraft == null) {
            handleError(SHOPPING_LIST_DRAFT_IS_NULL);
        } else if (isBlank(shoppingListDraft.getKey())) {
            handleError(format(SHOPPING_LIST_DRAFT_KEY_NOT_SET, shoppingListDraft.getName()));
        } else if (shoppingListDraft.getName() == null || shoppingListDraft.getName().getLocales().isEmpty()) {
            handleError(format(SHOPPING_LIST_DRAFT_NAME_NOT_SET, shoppingListDraft.getKey()));
        } else {
            return true;
        }

        return false;
    }

    private void collectReferencedKeys(
        @Nonnull final ReferencedKeys referencedKeys,
        @Nonnull final ShoppingListDraft shoppingListDraft) {

        collectReferencedKeyFromReference(shoppingListDraft.getCustomer(),
            referencedKeys.customerKeys::add);
        collectReferencedKeyFromCustomFieldsDraft(shoppingListDraft.getCustom(),
            referencedKeys.typeKeys::add);
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
            .stream()
            .filter(Objects::nonNull)
            .forEach(lineItemDraft -> {
                collectReferencedKeyFromCustomFieldsDraft(lineItemDraft.getCustom(),
                    referencedKeys.typeKeys::add);
            });
    }

    private void collectReferencedKeysInTextLineItems(
        @Nonnull final ReferencedKeys referencedKeys,
        @Nonnull final ShoppingListDraft shoppingListDraft) {

        if (shoppingListDraft.getTextLineItems() == null) {
            return;
        }

        shoppingListDraft
            .getTextLineItems()
            .stream()
            .filter(Objects::nonNull)
            .forEach(textLineItemDraft -> {
                collectReferencedKeyFromCustomFieldsDraft(textLineItemDraft.getCustom(),
                    referencedKeys.typeKeys::add);
            });
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
