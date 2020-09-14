package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.exceptions.SyncException;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.models.ResourceIdentifier;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CategoryBatchValidator {
    static final String CATEGORY_DRAFT_KEY_NOT_SET = "CategoryDraft with name: %s doesn't have a key. "
            + "Please make sure all category drafts have keys.";
    static final String CATEGORY_DRAFT_IS_NULL = "CategoryDraft is null.";
    static final String PARENT_CATEGORY_KEY_NOT_SET = "Parent category reference of "
            + "CategoryDraft with key '%s' has no key set. Please make sure parent category has a key.";

    final CategorySyncOptions syncOptions;
    final CategorySyncStatistics statistics;

    public CategoryBatchValidator(@Nonnull final CategorySyncOptions syncOptions,
                                  @Nonnull final CategorySyncStatistics statistics) {
        this.syncOptions = syncOptions;
        this.statistics = statistics;
    }

    /**
     * This method validates a list of category drafts.
     *
     * <p>A valid category draft is one which satisfies the following conditions:
     * <ol>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has parent category valid</li>
     * <li>A parent is valid if it satisfies the following conditions:
     * <ol>
     * <li>It has a key which is not blank (null/empty)</li>
     * </ol>
     * </li>
     * </ol>
     *
     * @param categoryDrafts - a list of category drafts considered in this batch
     *
     * @return a tuple of valid category drafts and valid keys
     */

    public ImmutablePair<Set<CategoryDraft>, Set<String>> validateAndCollectValidDraftsAndKeys(
            final List<CategoryDraft> categoryDrafts) {
        Set<CategoryDraft> validDrafts = new HashSet<>();
        Set<String> validKeys = new HashSet<>();
        for (CategoryDraft categoryDraft : categoryDrafts) {
            if (validateCategoryDraft(categoryDraft)) {
                ResourceIdentifier<Category> parent = categoryDraft.getParent();
                if (parent == null) {
                    validKeys.add(categoryDraft.getKey());
                    validDrafts.add(categoryDraft);
                } else if (validateParentCategory(parent, categoryDraft.getKey())) {
                    validDrafts.add(categoryDraft);
                    validKeys.add(categoryDraft.getKey());
                    validKeys.add(parent.getKey());
                }
            }
        }

        return ImmutablePair.of(validDrafts, validKeys);
    }

    private boolean validateCategoryDraft(final CategoryDraft categoryDraft) {
        String errorMessage = EMPTY;
        if (categoryDraft != null) {
            if (isBlank(categoryDraft.getKey())) {
                errorMessage = format(CATEGORY_DRAFT_KEY_NOT_SET, categoryDraft.getName());
            }
        } else {
            errorMessage = CATEGORY_DRAFT_IS_NULL;
        }

        if (!isBlank(errorMessage)) {
            handleError(errorMessage);
            return false;
        }

        return true;
    }

    private boolean validateParentCategory(final ResourceIdentifier<Category> parent, final String categoryDraftKey) {
        if (isBlank(parent.getKey())) {
            handleError(new SyncException(new ReferenceResolutionException(format(PARENT_CATEGORY_KEY_NOT_SET, categoryDraftKey))));
            return false;
        }

        return true;
    }

    private void handleError(@Nonnull final SyncException syncException) {
        this.syncOptions.applyErrorCallback(syncException);
        this.statistics.incrementFailed();
    }

    /**
     * Given a {@link String} {@code errorMessage}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed categories to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     */
    private void handleError(@Nonnull final String errorMessage) {
        this.syncOptions.applyErrorCallback(errorMessage);
        this.statistics.incrementFailed();
    }

}
