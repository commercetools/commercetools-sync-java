package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import io.sphere.sdk.categories.CategoryDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CategoryBatchValidator
    extends BaseBatchValidator<CategoryDraft, CategorySyncOptions, CategorySyncStatistics> {

    static final String CATEGORY_DRAFT_KEY_NOT_SET = "CategoryDraft with name: %s doesn't have a key. "
        + "Please make sure all category drafts have keys.";
    static final String CATEGORY_DRAFT_IS_NULL = "CategoryDraft is null.";

    public CategoryBatchValidator(
        @Nonnull final CategorySyncOptions syncOptions,
        @Nonnull final CategorySyncStatistics syncStatistics) {

        super(syncOptions, syncStatistics);
    }

    @Override
    public ImmutablePair<Set<CategoryDraft>, ReferencedKeys> validateAndCollectReferencedKeys(
        @Nonnull final List<CategoryDraft> categoryDrafts) {
        final ReferencedKeys referencedKeys = new ReferencedKeys();
        final Set<CategoryDraft> validDrafts = categoryDrafts
            .stream()
            .filter(this::isValidCategoryDraft)
            .peek(categoryDraft -> {
                referencedKeys.categoryKeys.add(categoryDraft.getKey());
                collectReferencedKeyFromResourceIdentifier(categoryDraft.getParent(),
                    referencedKeys.categoryKeys::add);
                collectReferencedKeyFromCustomFieldsDraft(categoryDraft.getCustom(),
                    referencedKeys.typeKeys::add);
                collectReferencedKeysFromAssetDrafts(categoryDraft.getAssets(),
                    referencedKeys.typeKeys::add);
            })
            .collect(Collectors.toSet());

        return ImmutablePair.of(validDrafts, referencedKeys);
    }

    private boolean isValidCategoryDraft(@Nullable final CategoryDraft categoryDraft) {
        if (categoryDraft == null) {
            handleError(CATEGORY_DRAFT_IS_NULL);
        } else if (isBlank(categoryDraft.getKey())) {
            handleError(format(CATEGORY_DRAFT_KEY_NOT_SET, categoryDraft.getName()));
        } else {
            return true;
        }

        return false;
    }

    public static class ReferencedKeys {
        private final Set<String> categoryKeys = new HashSet<>();
        private final Set<String> typeKeys = new HashSet<>();

        public Set<String> getCategoryKeys() {
            return categoryKeys;
        }

        public Set<String> getTypeKeys() {
            return typeKeys;
        }
    }
}
