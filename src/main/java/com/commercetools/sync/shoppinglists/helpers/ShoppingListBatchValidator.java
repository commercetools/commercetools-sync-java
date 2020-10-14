package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ShoppingListBatchValidator
    extends BaseBatchValidator<ShoppingListDraft, ShoppingListSyncOptions, ShoppingListSyncStatistics> {

    static final String SHOPPING_LIST_DRAFT_KEY_NOT_SET = "ShoppingListDraft with name: %s doesn't have a key. "
        + "Please make sure all shopping list drafts have keys.";
    static final String SHOPPING_LIST_DRAFT_IS_NULL = "ShoppingListDraft is null.";

    public ShoppingListBatchValidator(@Nonnull final ShoppingListSyncOptions syncOptions,
                                      @Nonnull final ShoppingListSyncStatistics syncStatistics) {
        super(syncOptions, syncStatistics);
    }

    /**
     * Given the {@link List}&lt;{@link ShoppingListDraft}&gt; of drafts this method attempts to validate
     * drafts and collect referenced type keys from the draft and return an {@link ImmutablePair}&lt;{@link Set}&lt;
     * {@link ShoppingListDraft}&gt;,{@link Set}&lt;
     * {@link String}&gt;&gt; which contains the {@link Set} of valid drafts and referenced type keys.
     *
     * <p>A valid shopping list draft is one which satisfies the following conditions:
     * <ol>
     * <li>It is not null</li>
     * <li>It has a key which is not blank (null/empty)</li>
     * </ol>
     *
     * @param shoppingListDrafts the shopping list drafts to validate and collect referenced type keys.
     * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link ShoppingListDraft}&gt;,
     *      {@link Set}&lt;{@link String}&gt;&gt; which contains the {@link Set} of valid drafts and
     *      referenced type keys.
     */
    @Override
    public ImmutablePair<Set<ShoppingListDraft>, Set<String>> validateAndCollectReferencedKeys(
        @Nonnull final List<ShoppingListDraft> shoppingListDrafts) {
        final Set<String> typeKeys = new HashSet<>();
        final Set<ShoppingListDraft> validDrafts = shoppingListDrafts
            .stream()
            .filter(this::isValidShoppingListDraft)
            .peek(shoppingListDraft ->
                collectReferencedKeyFromCustomFieldsDraft(shoppingListDraft.getCustom(), typeKeys::add))
            .collect(Collectors.toSet());

        return ImmutablePair.of(validDrafts, typeKeys);
    }

    private boolean isValidShoppingListDraft(
        @Nullable final ShoppingListDraft shoppingListDraft) {

        if (shoppingListDraft == null) {
            handleError(SHOPPING_LIST_DRAFT_IS_NULL);
        } else if (isBlank(shoppingListDraft.getKey())) {
            handleError(format(SHOPPING_LIST_DRAFT_KEY_NOT_SET, shoppingListDraft.getName()));
        } else {
            return true;
        }

        return false;
    }
}
