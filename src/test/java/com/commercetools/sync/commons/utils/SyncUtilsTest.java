package com.commercetools.sync.commons.utils;


import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.models.Reference;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.commons.utils.SyncUtils.replaceReferenceIdWithKey;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SyncUtilsTest {

    @Test
    public void batchElements_WithValidSize_ShouldReturnCorrectBatches() {
        final int numberOfCategoryDrafts = 160;
        final int batchSize = 10;
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        for (int i = 0; i < numberOfCategoryDrafts; i++) {
            categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key" + i, "parentKey",
                "customTypeId", new HashMap<>()));
        }
        final List<List<CategoryDraft>> batches = batchElements(categoryDrafts, 10);
        assertThat(batches.size()).isEqualTo(numberOfCategoryDrafts / batchSize);
    }

    @Test
    public void batchElements_WithUniformSeparation_ShouldReturnCorrectBatches() {
        batchStringElementsAndAssertAfterBatching(100, 10);
        batchStringElementsAndAssertAfterBatching(3, 1);
    }

    @Test
    public void batchElements_WithNonUniformSeparation_ShouldReturnCorrectBatches() {
        batchStringElementsAndAssertAfterBatching(100, 9);
        batchStringElementsAndAssertAfterBatching(3, 2);
    }

    private void batchStringElementsAndAssertAfterBatching(final int numberOfElements, final int batchSize) {
        final List<String> elements = getPrefixedStrings(numberOfElements, "element");

        final List<List<String>> batches = batchElements(elements, batchSize);

        final int expectedNumberOfBatches = getExpectedNumberOfBatches(numberOfElements, batchSize);
        assertThat(batches.size()).isEqualTo(expectedNumberOfBatches);

        // Assert correct size of elements after batching
        final Integer numberOfElementsAfterBatching = batches.stream()
                                                             .map(List::size)
                                                             .reduce(0,
                                                                 (element1, element2) -> element1 + element2);

        assertThat(numberOfElementsAfterBatching).isEqualTo(numberOfElements);


        // Assert correct splitting of batches
        final int remainder = numberOfElements % batchSize;
        if (remainder == 0) {
            final int numberOfDistinctBatchSizes = batches.stream().collect(Collectors.groupingBy(List::size)).size();
            assertThat(numberOfDistinctBatchSizes).isEqualTo(1);
        } else {
            final List<String> lastBatch = batches.get(batches.size() - 1);
            assertThat(lastBatch).hasSize(remainder);
        }

        // Assert that all elements have been batched in correct order
        final List<String> flatBatches = batches.stream()
                                                .flatMap(Collection::stream).collect(Collectors.toList());
        IntStream.range(0, flatBatches.size())
                 .forEach(index -> assertThat(flatBatches.get(index)).isEqualTo(format("element#%s", index + 1)));
    }

    @Nonnull
    private List<String> getPrefixedStrings(final int numberOfElements, @Nonnull final String prefix) {
        return IntStream.range(1, numberOfElements + 1)
                        .mapToObj(i -> format("%s#%s", prefix, i))
                        .collect(Collectors.toList());
    }

    private int getExpectedNumberOfBatches(int numberOfElements, int batchSize) {
        return (int) (Math.ceil((double)numberOfElements / batchSize));
    }

    @Test
    public void batchElements_WithEmptyListAndAnySize_ShouldReturnNoBatches() {
        final List<List<CategoryDraft>> batches = batchElements(new ArrayList<>(), 100);
        assertThat(batches.size()).isEqualTo(0);
    }

    @Test
    public void batchElements_WithNegativeSize_ShouldReturnNoBatches() {
        final int numberOfCategoryDrafts = 160;
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        for (int i = 0; i < numberOfCategoryDrafts; i++) {
            categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key" + i, "parentKey",
                "customTypeId", new HashMap<>()));
        }
        final List<List<CategoryDraft>> batches = batchElements(categoryDrafts, -100);
        assertThat(batches.size()).isEqualTo(0);
    }

    @Test
    public void replaceReferenceIdWithKey_WithNullReference_ShouldReturnNullReference() {
        final Reference<Object> keyReplacedReference = replaceReferenceIdWithKey(null,
            () -> Reference.of(Category.referenceTypeId(), "id"));
        assertThat(keyReplacedReference).isNull();
    }

    @Test
    public void replaceReferenceIdWithKey_WithExpandedCategoryReference_ShouldReturnCategoryReferenceWithKey() {
        final String categoryKey = "categoryKey";
        final Category mockCategory = mock(Category.class);
        when(mockCategory.getKey()).thenReturn(categoryKey);

        final Reference<Category> categoryReference = Reference.ofResourceTypeIdAndObj(Category.referenceTypeId(),
            mockCategory);

        final Reference<Category> keyReplacedReference = replaceReferenceIdWithKey(categoryReference,
            () -> Category.referenceOfId(categoryReference.getObj().getKey()));
        assertThat(keyReplacedReference).isNotNull();
        assertThat(keyReplacedReference.getId()).isEqualTo(categoryKey);
    }

    @Test
    public void replaceReferenceIdWithKey_WithNonExpandedCategoryReference_ShouldReturnReferenceWithoutReplacedKey() {
        final String categoryUuid = UUID.randomUUID().toString();
        final Reference<Category> categoryReference = Reference.ofResourceTypeIdAndId(Category.referenceTypeId(),
            categoryUuid);

        final Reference<Category> keyReplacedReference = replaceReferenceIdWithKey(categoryReference,
            () -> Category.referenceOfId(categoryReference.getObj().getKey()));
        assertThat(keyReplacedReference).isNotNull();
        assertThat(keyReplacedReference.getId()).isEqualTo(categoryUuid);
    }
}
