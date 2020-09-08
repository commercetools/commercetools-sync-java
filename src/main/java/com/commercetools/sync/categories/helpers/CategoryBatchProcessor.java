package com.commercetools.sync.categories.helpers;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.models.ResourceIdentifier;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CategoryBatchProcessor {
    static final String CATEGORY_DRAFT_KEY_NOT_SET = "CategoryDraft with name: %s doesn't have a key. "
        + "Please make sure all category drafts have keys.";
    static final String CATEGORY_DRAFT_IS_NULL = "CategoryDraft is null.";
    static final String PARENT_CATEGORY_IS_NULL = "Parent category reference of CategoryDraft "
        + "with key '%s' is null.";
    static final String PARENT_CATEGORY_KEY_NOT_SET = "Parent category reference of "
        + "CategoryDraft with key '%s' has no key set. Please make sure all variants have keys.";

    private final List<CategoryDraft> categoryDrafts;
    private final Set<CategoryDraft> validDrafts = new HashSet<>();
    private final Set<String> keysToCache = new HashSet<>();

    public CategoryBatchProcessor(@Nonnull final List<CategoryDraft> categoryDrafts) {
        this.categoryDrafts = categoryDrafts;
    }

    /**
     * This method validates the batch of drafts, and only for valid drafts it adds the valid draft
     * to {@code validDrafts} set, and adds the keys of all referenced categories to
     * {@code keysToCache}.
     *
     *
     * <p>A valid category draft is one which satisfies the following conditions:
     * <ol>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has parent category valid</li>
     * <li>A parent is valid if it satisfies the following conditions:
     * <ol>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has an id which is not blank (null/empty)</li>
     * </ol>
     * </li>
     * </ol>
     * @return a list of errors if validation fails else empty list
     */
    public List<String> validateBatch() {
        List<String> validationErrors = new ArrayList<>();
        for (CategoryDraft categoryDraft : categoryDrafts) {
            validationErrors = validateCategoryDraft(categoryDraft);
            if (validationErrors.isEmpty()) {
                validationErrors = validateParentCategory(categoryDraft.getParent(), categoryDraft.getKey());
                if (validationErrors.isEmpty()) {
                    keysToCache.add(categoryDraft.getParent().getKey());
                    keysToCache.add(categoryDraft.getKey());
                    validDrafts.add(categoryDraft);
                }
            }
        }
        return validationErrors;
    }

    private List<String> validateCategoryDraft(CategoryDraft categoryDraft) {
        final List<String> errorMessages = new ArrayList<>();
        if (categoryDraft != null) {
            if (isBlank(categoryDraft.getKey())) {
                errorMessages.add(format(CATEGORY_DRAFT_KEY_NOT_SET, categoryDraft.getName()));
            }


        } else {
            errorMessages.add(CATEGORY_DRAFT_IS_NULL);
        }
        return errorMessages;
    }

    private List<String> validateParentCategory(ResourceIdentifier<Category> parent, String categoryDraftKey) {
        List<String> errorMessages = new ArrayList<>();
        if (isNull(parent)) {
            errorMessages.add(format(PARENT_CATEGORY_IS_NULL, categoryDraftKey));
        } else {
            if (isBlank(parent.getKey())) {
                errorMessages.add(format(PARENT_CATEGORY_KEY_NOT_SET, categoryDraftKey));
            }
        }
        return errorMessages;

    }


    public Set<CategoryDraft> getValidDrafts() {
        return validDrafts;
    }

    public Set<String> getKeysToCache() {
        return keysToCache;
    }
}
