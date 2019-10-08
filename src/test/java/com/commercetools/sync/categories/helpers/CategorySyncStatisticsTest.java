package com.commercetools.sync.categories.helpers;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategorySyncStatisticsTest {

    private CategorySyncStatistics categorySyncStatistics;

    @BeforeEach
    void setup() {
        categorySyncStatistics = new CategorySyncStatistics();
    }

    @Test
    void getUpdated_WithNoUpdated_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getUpdated()).hasValue(0);
    }

    @Test
    void incrementUpdated_ShouldIncrementUpdatedValue() {
        categorySyncStatistics.incrementUpdated();
        assertThat(categorySyncStatistics.getUpdated()).hasValue(1);
    }

    @Test
    void getCreated_WithNoCreated_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getCreated()).hasValue(0);
    }

    @Test
    void incrementCreated_ShouldIncrementCreatedValue() {
        categorySyncStatistics.incrementCreated();
        assertThat(categorySyncStatistics.getCreated()).hasValue(1);
    }

    @Test
    void getProcessed_WithNoProcessed_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getProcessed()).hasValue(0);
    }

    @Test
    void incrementProcessed_ShouldIncrementProcessedValue() {
        categorySyncStatistics.incrementProcessed();
        assertThat(categorySyncStatistics.getProcessed()).hasValue(1);
    }

    @Test
    void getFailed_WithNoFailed_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getFailed()).hasValue(0);
    }

    @Test
    void incrementFailed_ShouldIncrementFailedValue() {
        categorySyncStatistics.incrementFailed();
        assertThat(categorySyncStatistics.getFailed()).hasValue(1);
    }

    @Test
    void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
        categorySyncStatistics.incrementCreated(1);
        categorySyncStatistics.incrementFailed(1);
        categorySyncStatistics.incrementUpdated(1);
        categorySyncStatistics.incrementProcessed(3);

        assertThat(categorySyncStatistics.getReportMessage()).isEqualTo("Summary: 3 categories were processed in total "
            + "(1 created, 1 updated, 1 failed to sync and 0 categories with a missing parent).");
    }

    @Test
    void getNumberOfCategoriesWithMissingParents_WithEmptyMap_ShouldReturn0() {
        final ConcurrentHashMap<String, Set<String>> catKeysWithMissingParents = new ConcurrentHashMap<>();
        categorySyncStatistics.setCategoryKeysWithMissingParents(catKeysWithMissingParents);

        assertThat(categorySyncStatistics.getNumberOfCategoriesWithMissingParents()).isZero();
    }

    @Test
    void getNumberOfCategoriesWithMissingParents_WithEmptyValue_ShouldReturn0() {
        final ConcurrentHashMap<String, Set<String>> categoryKeysWithMissingParents = new ConcurrentHashMap<>();
        categoryKeysWithMissingParents.put("parent2", emptySet());

        categorySyncStatistics.setCategoryKeysWithMissingParents(categoryKeysWithMissingParents);
        assertThat(categorySyncStatistics.getNumberOfCategoriesWithMissingParents()).isZero();
    }

    @Test
    void getNumberOfCategoriesWithMissingParents_WithNonEmptyMap_ShouldReturnCorrectNumberOfChildren() {
        final ConcurrentHashMap<String, Set<String>> categoryKeysWithMissingParents = new ConcurrentHashMap<>();
        final Set<String> firstMissingParentChildrenKeys = new HashSet<>();
        firstMissingParentChildrenKeys.add("key1");
        firstMissingParentChildrenKeys.add("key2");

        final Set<String> secondMissingParentChildrenKeys = new HashSet<>();
        secondMissingParentChildrenKeys.add("key3");
        secondMissingParentChildrenKeys.add("key4");

        categoryKeysWithMissingParents.put("parent1", firstMissingParentChildrenKeys);
        categoryKeysWithMissingParents.put("parent2", secondMissingParentChildrenKeys);


        categorySyncStatistics.setCategoryKeysWithMissingParents(categoryKeysWithMissingParents);

        assertThat(categorySyncStatistics.getNumberOfCategoriesWithMissingParents()).isEqualTo(4);
    }

    @Test
    void getCategoryKeysWithMissingParents_WithAnyMap_ShouldGetUnmodifiableMapCorrectly() {
        final ConcurrentHashMap<String, Set<String>> catKeysWithMissingParents = new ConcurrentHashMap<>();
        categorySyncStatistics.setCategoryKeysWithMissingParents(catKeysWithMissingParents);

        final Map<String, Set<String>> fetchedMap = categorySyncStatistics.getCategoryKeysWithMissingParents();

        assertThatThrownBy(() -> fetchedMap.put("e", emptySet()))
            .isExactlyInstanceOf(UnsupportedOperationException.class);

        assertThat(fetchedMap).isEqualTo(catKeysWithMissingParents);
    }

    @Test
    void putMissingParentCategoryChildKey_OnAnEmptyList_ShouldAddNewParentEntryInTheMap() {
        final String parentKey = "parentKey";
        final String childKey = "childKey";
        categorySyncStatistics.putMissingParentCategoryChildKey(parentKey, childKey);

        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).isNotEmpty();
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).hasSize(1);
        final Set<String> childrenKeySet = categorySyncStatistics.getCategoryKeysWithMissingParents().get(parentKey);
        assertThat(childrenKeySet).isNotNull();
        assertThat(childrenKeySet).hasSize(1);
        assertThat(childrenKeySet.iterator().next()).isEqualTo(childKey);
    }

    @Test
    void putMissingParentCategoryChildKey_OnANonEmptyListWithNewParent_ShouldAddNewParentEntryInTheMap() {
        final ConcurrentHashMap<String, Set<String>> categoryKeysWithMissingParents = new ConcurrentHashMap<>();
        final Set<String> existingChildrenKeys = new HashSet<>();
        existingChildrenKeys.add("key1");
        existingChildrenKeys.add("key2");

        final String existingParentKey = "existingParent";
        categoryKeysWithMissingParents.put(existingParentKey, existingChildrenKeys);
        categorySyncStatistics.setCategoryKeysWithMissingParents(categoryKeysWithMissingParents);

        final String newParentKey = "parentKey";
        final String newChildKey = "childKey";
        categorySyncStatistics.putMissingParentCategoryChildKey(newParentKey, newChildKey);

        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).isNotEmpty();
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).hasSize(2);
        final Set<String> newChildrenKeySet = categorySyncStatistics.getCategoryKeysWithMissingParents()
                                                                    .get(newParentKey);
        assertThat(newChildrenKeySet).isNotNull();
        assertThat(newChildrenKeySet).hasSize(1);
        assertThat(newChildrenKeySet.iterator().next()).isEqualTo(newChildKey);

        final Set<String> existingChildrenKeySet = categorySyncStatistics.getCategoryKeysWithMissingParents()
                                                                         .get(existingParentKey);
        assertThat(existingChildrenKeySet).isNotNull();
        assertThat(existingChildrenKeySet).hasSize(2);
        assertThat(existingChildrenKeySet).isEqualTo(existingChildrenKeys);
    }

    @Test
    void putMissingParentCategoryChildKey_OnNonEmptyListWithExistingParent_ShouldAddParentToExistingEntry() {
        final ConcurrentHashMap<String, Set<String>> categoryKeysWithMissingParents = new ConcurrentHashMap<>();
        final Set<String> existingChildrenKeys = new HashSet<>();
        existingChildrenKeys.add("key1");
        existingChildrenKeys.add("key2");

        final String existingParentKey = "existingParent";
        categoryKeysWithMissingParents.put(existingParentKey, existingChildrenKeys);
        categorySyncStatistics.setCategoryKeysWithMissingParents(categoryKeysWithMissingParents);

        final String newChildKey = "childKey";
        categorySyncStatistics.putMissingParentCategoryChildKey(existingParentKey, newChildKey);

        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).isNotEmpty();
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).hasSize(1);
        final Set<String> newChildrenKeySet = categorySyncStatistics.getCategoryKeysWithMissingParents()
                                                                    .get(existingParentKey);
        assertThat(newChildrenKeySet).isNotNull();
        assertThat(newChildrenKeySet).hasSize(3);
        assertThat(newChildrenKeySet).containsOnly(newChildKey, "key1", "key2");
    }

    @Test
    void getMissingParentKey_OAnEmptyList_ShouldReturnAnEmptyOptional() {
        assertThat(categorySyncStatistics.getMissingParentKey("foo")).isEmpty();
    }

    @Test
    void getMissingParentKey_OnANonEmptyList_ShouldReturnCorrectParentKeyInOptional() {
        final ConcurrentHashMap<String, Set<String>> categoryKeysWithMissingParents = new ConcurrentHashMap<>();
        final Set<String> existingChildrenKeys = new HashSet<>();
        existingChildrenKeys.add("key1");
        existingChildrenKeys.add("key2");

        final String existingParentKey = "existingParent";
        categoryKeysWithMissingParents.put(existingParentKey, existingChildrenKeys);
        categorySyncStatistics.setCategoryKeysWithMissingParents(categoryKeysWithMissingParents);

        assertThat(categorySyncStatistics.getMissingParentKey("key1")).contains(existingParentKey);
        assertThat(categorySyncStatistics.getMissingParentKey("key2")).contains(existingParentKey);
    }

    @Test
    void removeChildCategoryKeyFromMissingParentsMap_OnAEmptyList_ShouldNotAffectTheMap() {
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).isEmpty();
        categorySyncStatistics.removeChildCategoryKeyFromMissingParentsMap("foo");
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).isEmpty();
    }

    @Test
    void removeChildCategoryKeyFromMissingParentsMap_OnANonEmptyListButNonExistingKey_ShouldNotAffectTheMap() {
        final ConcurrentHashMap<String, Set<String>> categoryKeysWithMissingParents = new ConcurrentHashMap<>();
        final Set<String> existingChildrenKeys = new HashSet<>();
        existingChildrenKeys.add("key1");
        existingChildrenKeys.add("key2");

        final String existingParentKey = "existingParent";
        categoryKeysWithMissingParents.put(existingParentKey, existingChildrenKeys);
        categorySyncStatistics.setCategoryKeysWithMissingParents(categoryKeysWithMissingParents);

        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).hasSize(1);
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents().get(existingParentKey)).hasSize(2);
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents().get(existingParentKey))
            .containsOnly("key2", "key1");

        categorySyncStatistics.removeChildCategoryKeyFromMissingParentsMap("foo");

        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).hasSize(1);
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents().get(existingParentKey)).hasSize(2);
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents().get(existingParentKey))
            .containsOnly("key2", "key1");
    }

    @Test
    void removeChildCategoryKeyFromMissingParentsMap_OnANonEmptyListWithExistingKey_ShouldNotAffectTheMap() {
        final ConcurrentHashMap<String, Set<String>> categoryKeysWithMissingParents = new ConcurrentHashMap<>();
        final Set<String> existingChildrenKeys = new HashSet<>();
        existingChildrenKeys.add("key1");
        existingChildrenKeys.add("key2");

        final String existingParentKey = "existingParent";
        categoryKeysWithMissingParents.put(existingParentKey, existingChildrenKeys);
        categorySyncStatistics.setCategoryKeysWithMissingParents(categoryKeysWithMissingParents);

        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).hasSize(1);
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents().get(existingParentKey)).hasSize(2);
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents().get(existingParentKey))
            .isEqualTo(existingChildrenKeys);
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents().get(existingParentKey))
            .containsOnly("key2", "key1");

        categorySyncStatistics.removeChildCategoryKeyFromMissingParentsMap("key1");

        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).hasSize(1);
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents().get(existingParentKey)).hasSize(1);
        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents().get(existingParentKey))
            .doesNotContain("key1").containsOnly("key2");
    }
}
