package com.commercetools.sync.categories.helpers;

import static com.commercetools.sync.categories.helpers.CategoryBatchValidator.CATEGORY_DRAFT_IS_NULL;
import static com.commercetools.sync.categories.helpers.CategoryBatchValidator.CATEGORY_DRAFT_KEY_NOT_SET;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFieldsDraft;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryBatchValidatorTest {

  private CategorySyncOptions syncOptions;
  private CategorySyncStatistics syncStatistics;
  private List<String> errorCallBackMessages;

  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    final SphereClient ctpClient = mock(SphereClient.class);

    syncOptions =
        CategorySyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errorCallBackMessages.add(exception.getMessage()))
            .build();
    syncStatistics = mock(CategorySyncStatistics.class);
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
    final Set<CategoryDraft> validDrafts = getValidDrafts(Collections.emptyList());

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithNullCategoryDraft_ShouldHaveValidationErrorAndEmptyResult() {
    final Set<CategoryDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0)).isEqualTo(CATEGORY_DRAFT_IS_NULL);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithCategoryDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
    final CategoryDraft categoryDraft = mock(CategoryDraft.class);
    final Set<CategoryDraft> validDrafts = getValidDrafts(Collections.singletonList(categoryDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format(CATEGORY_DRAFT_KEY_NOT_SET, categoryDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithCategoryDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
    final CategoryDraft categoryDraft = mock(CategoryDraft.class);
    when(categoryDraft.getKey()).thenReturn(EMPTY);
    final Set<CategoryDraft> validDrafts = getValidDrafts(Collections.singletonList(categoryDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format(CATEGORY_DRAFT_KEY_NOT_SET, categoryDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void validateAndCollectReferencedKeys_WithValidDrafts_ShouldReturnCorrectResults() {
    final CategoryDraft validCategoryDraft = mock(CategoryDraft.class);
    when(validCategoryDraft.getKey()).thenReturn("validDraftKey");
    when(validCategoryDraft.getParent()).thenReturn(ResourceIdentifier.ofKey("validParentKey"));
    when(validCategoryDraft.getCustom())
        .thenReturn(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", Collections.emptyMap()));

    final CategoryDraft validMainCategoryDraft = mock(CategoryDraft.class);
    when(validMainCategoryDraft.getKey()).thenReturn("validDraftKey1");

    final CategoryDraft invalidCategoryDraft = mock(CategoryDraft.class);
    when(invalidCategoryDraft.getParent()).thenReturn(ResourceIdentifier.ofKey("key"));

    final CategoryBatchValidator categoryBatchValidator =
        new CategoryBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<CategoryDraft>, CategoryBatchValidator.ReferencedKeys> pair =
        categoryBatchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(validCategoryDraft, invalidCategoryDraft, validMainCategoryDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format(CATEGORY_DRAFT_KEY_NOT_SET, invalidCategoryDraft.getName()));
    assertThat(pair.getLeft())
        .containsExactlyInAnyOrder(validCategoryDraft, validMainCategoryDraft);
    assertThat(pair.getRight().getCategoryKeys())
        .containsExactlyInAnyOrder("validDraftKey", "validParentKey", "validDraftKey1");
    assertThat(pair.getRight().getTypeKeys()).containsExactlyInAnyOrder("typeKey");
  }

  @Nonnull
  private Set<CategoryDraft> getValidDrafts(@Nonnull final List<CategoryDraft> categoryDrafts) {
    final CategoryBatchValidator categoryBatchValidator =
        new CategoryBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<CategoryDraft>, CategoryBatchValidator.ReferencedKeys> pair =
        categoryBatchValidator.validateAndCollectReferencedKeys(categoryDrafts);
    return pair.getLeft();
  }
}
