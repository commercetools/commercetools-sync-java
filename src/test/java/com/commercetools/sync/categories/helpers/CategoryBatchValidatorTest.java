package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.ResourceIdentifier;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.categories.helpers.CategoryBatchValidator.*;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryBatchValidatorTest {

    private CategoryBatchValidator categoryBatchValidator;
    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);
        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder.of(ctpClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                })
                .build();
        CategorySyncStatistics statistics = mock(CategorySyncStatistics.class);

        categoryBatchValidator = new CategoryBatchValidator(syncOptions, statistics);
    }

    @Test
    void validateBatch_WithEmptyBatch_ShouldHaveEmptyResult() {
        final ImmutablePair<Set<CategoryDraft>, Set<String>> draftsAndKeys = categoryBatchValidator.validateAndCollectValidDraftsAndKeys(Collections.emptyList());

        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(draftsAndKeys.left).isEmpty();
        assertThat(draftsAndKeys.right).isEmpty();
    }

    @Test
    void validateBatch_WithNullCategoryDraft_ShouldHaveValidationErrorAndEmptyResult() {
        final ImmutablePair<Set<CategoryDraft>, Set<String>> draftsAndKeys = categoryBatchValidator.validateAndCollectValidDraftsAndKeys(Collections.singletonList(null));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(CATEGORY_DRAFT_IS_NULL);
        assertThat(draftsAndKeys.left).isEmpty();
        assertThat(draftsAndKeys.right).isEmpty();
    }

    @Test
    void validateBatch_WithCategoryDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        final ImmutablePair<Set<CategoryDraft>, Set<String>> draftsAndKeys = categoryBatchValidator.validateAndCollectValidDraftsAndKeys(Collections.singletonList(categoryDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format(CATEGORY_DRAFT_KEY_NOT_SET, categoryDraft.getName()));
        assertThat(draftsAndKeys.left).isEmpty();
        assertThat(draftsAndKeys.right).isEmpty();
    }

    @Test
    void validateBatch_WithCategoryDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        when(categoryDraft.getKey()).thenReturn(EMPTY);
        final ImmutablePair<Set<CategoryDraft>, Set<String>> draftsAndKeys = categoryBatchValidator.validateAndCollectValidDraftsAndKeys(Collections.singletonList(categoryDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format(CATEGORY_DRAFT_KEY_NOT_SET, categoryDraft.getName()));
        assertThat(draftsAndKeys.left).isEmpty();
        assertThat(draftsAndKeys.right).isEmpty();
    }

    @Test
    void validateBatch_WithCategoryDraftWithParentHasNullKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        when(categoryDraft.getKey()).thenReturn("key");
        when(categoryDraft.getParent()).thenReturn(ResourceIdentifier.ofKey(null));
        final ImmutablePair<Set<CategoryDraft>, Set<String>> draftsAndKeys = categoryBatchValidator.validateAndCollectValidDraftsAndKeys(Collections.singletonList(categoryDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format(PARENT_CATEGORY_KEY_NOT_SET, categoryDraft.getKey()));
        assertThat(draftsAndKeys.left).isEmpty();
        assertThat(draftsAndKeys.right).isEmpty();
    }

    @Test
    void validateBatch_WithCategoryDraftWithParentHasEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        when(categoryDraft.getKey()).thenReturn("key");
        when(categoryDraft.getParent()).thenReturn(ResourceIdentifier.ofKey(""));
        final ImmutablePair<Set<CategoryDraft>, Set<String>> draftsAndKeys = categoryBatchValidator.validateAndCollectValidDraftsAndKeys(Collections.singletonList(categoryDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format(PARENT_CATEGORY_KEY_NOT_SET, categoryDraft.getKey()));
        assertThat(draftsAndKeys.left).isEmpty();
        assertThat(draftsAndKeys.right).isEmpty();
    }

    @Test
    void validateBatch_WithValidDrafts_ShouldReturnResult() {
        final CategoryDraft validCategoryDraft = mock(CategoryDraft.class);
        when(validCategoryDraft.getKey()).thenReturn("validDraftKey");
        when(validCategoryDraft.getParent()).thenReturn(ResourceIdentifier.ofKey("validParentKey"));

        final CategoryDraft validMainCategoryDraft = mock(CategoryDraft.class);
        when(validMainCategoryDraft.getKey()).thenReturn("validDraftKey1");

        final CategoryDraft invalidCategoryDraft = mock(CategoryDraft.class);
        when(invalidCategoryDraft.getParent()).thenReturn(ResourceIdentifier.ofKey("key"));

        final ImmutablePair<Set<CategoryDraft>, Set<String>> draftsAndKeys = categoryBatchValidator.validateAndCollectValidDraftsAndKeys(Arrays.asList(validCategoryDraft, invalidCategoryDraft, validMainCategoryDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format(CATEGORY_DRAFT_KEY_NOT_SET,invalidCategoryDraft.getName()));
        assertThat(draftsAndKeys.right).hasSize(3);
        assertThat(draftsAndKeys.right).containsExactlyInAnyOrder("validDraftKey", "validParentKey", "validDraftKey1");
        assertThat(draftsAndKeys.left).hasSize(2);
        assertThat(draftsAndKeys.left).containsExactlyInAnyOrder(validCategoryDraft, validMainCategoryDraft);
    }



}
