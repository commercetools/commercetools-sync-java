package com.commercetools.sync.categories.helpers;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class CategoryBatchValidator
    extends BaseBatchValidator<CategoryDraft, CategorySyncOptions, CategorySyncStatistics> {

  public static final String CATEGORY_DRAFT_KEY_NOT_SET =
      "CategoryDraft with name: %s doesn't have a key. "
          + "Please make sure all category drafts have keys.";
  static final String CATEGORY_DRAFT_IS_NULL = "CategoryDraft is null.";

  public CategoryBatchValidator(
      @Nonnull final CategorySyncOptions syncOptions,
      @Nonnull final CategorySyncStatistics syncStatistics) {

    super(syncOptions, syncStatistics);
  }

  /**
   * Given the {@link java.util.List}&lt;{@link CategoryDraft}&gt; of drafts this method attempts to
   * validate drafts and collect referenced keys from the draft and return an {@link
   * org.apache.commons.lang3.tuple.ImmutablePair}&lt;{@link java.util.Set}&lt;{@link
   * CategoryDraft}&gt;,{@link CategoryBatchValidator.ReferencedKeys}&gt; which contains the {@link
   * java.util.Set} of valid drafts and referenced keys within a wrapper.
   *
   * <p>A valid category draft is one which satisfies the following conditions:
   *
   * <ol>
   *   <li>It is not null
   *   <li>It has a key which is not blank (null/empty)
   * </ol>
   *
   * @param categoryDrafts the category drafts to validate and collect referenced keys.
   * @return {@link org.apache.commons.lang3.tuple.ImmutablePair}&lt;{@link java.util.Set}&lt;{@link
   *     CategoryDraft}&gt;,{@link CategoryBatchValidator.ReferencedKeys}&gt; which contains the
   *     {@link java.util.Set} of valid drafts and referenced keys within a wrapper.
   */
  @Override
  public ImmutablePair<Set<CategoryDraft>, ReferencedKeys> validateAndCollectReferencedKeys(
      @Nonnull final List<CategoryDraft> categoryDrafts) {
    final ReferencedKeys referencedKeys = new ReferencedKeys();
    final Set<CategoryDraft> validDrafts =
        categoryDrafts.stream()
            .filter(this::isValidCategoryDraft)
            .peek(categoryDraft -> collectReferencedKeys(referencedKeys, categoryDraft))
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

  private void collectReferencedKeys(
      @Nonnull final ReferencedKeys referencedKeys, @Nonnull final CategoryDraft categoryDraft) {

    referencedKeys.categoryKeys.add(categoryDraft.getKey());
    collectReferencedKeyFromResourceIdentifier(
        categoryDraft.getParent(), referencedKeys.categoryKeys::add);
    collectReferencedKeyFromCustomFieldsDraft(
        categoryDraft.getCustom(), referencedKeys.typeKeys::add);
    collectReferencedKeysFromAssetDrafts(categoryDraft.getAssets(), referencedKeys.typeKeys::add);
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
