package com.commercetools.sync.categories.helpers;


import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class CategorySyncStatisticsTest {

    private CategorySyncStatistics categorySyncStatistics;

    @Before
    public void setup() {
        categorySyncStatistics = new CategorySyncStatistics();
    }

    @Test
    public void getUpdated_WithNoUpdated_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getUpdated()).hasValue(0);
    }

    @Test
    public void incrementUpdated_ShouldIncrementUpdatedValue() {
        categorySyncStatistics.incrementUpdated();
        assertThat(categorySyncStatistics.getUpdated()).hasValue(1);
    }

    @Test
    public void getCreated_WithNoCreated_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getCreated()).hasValue(0);
    }

    @Test
    public void incrementCreated_ShouldIncrementCreatedValue() {
        categorySyncStatistics.incrementCreated();
        assertThat(categorySyncStatistics.getCreated()).hasValue(1);
    }

    @Test
    public void getProcessed_WithNoProcessed_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getProcessed()).hasValue(0);
    }

    @Test
    public void incrementProcessed_ShouldIncrementProcessedValue() {
        categorySyncStatistics.incrementProcessed();
        assertThat(categorySyncStatistics.getProcessed()).hasValue(1);
    }

    @Test
    public void getFailed_WithNoFailed_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getFailed()).hasValue(0);
    }

    @Test
    public void incrementFailed_ShouldIncrementFailedValue() {
        categorySyncStatistics.incrementFailed();
        assertThat(categorySyncStatistics.getFailed()).hasValue(1);
    }

    @Test
    public void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
        categorySyncStatistics.incrementCreated(1);
        categorySyncStatistics.incrementFailed(1);
        categorySyncStatistics.incrementUpdated(1);
        categorySyncStatistics.incrementProcessed(3);

        assertThat(categorySyncStatistics.getReportMessage()).isEqualTo("Summary: 3 categories were processed in total "
            + "(1 created, 1 updated, 1 failed to sync and 0 categories with a missing parent).");
    }

    @Test
    public void setAndGetCategoryKeysWithMissingParents_WithAnyMap_ShouldSetAndGetMapCorrectly() {
        final HashMap<String, List<String>> catKeysWithMissingParents = new HashMap<>();
        categorySyncStatistics.setCategoryKeysWithMissingParents(catKeysWithMissingParents);

        assertThat(categorySyncStatistics.getCategoryKeysWithMissingParents()).isSameAs(catKeysWithMissingParents);
    }

    @Test
    public void getNumberOfCategoriesWithMissingParents_WithEmptyMap_ShouldReturn0() {
        final HashMap<String, List<String>> catKeysWithMissingParents = new HashMap<>();
        categorySyncStatistics.setCategoryKeysWithMissingParents(catKeysWithMissingParents);

        assertThat(categorySyncStatistics.getNumberOfCategoriesWithMissingParents()).isZero();
    }

    @Test
    public void getNumberOfCategoriesWithMissingParents_WithEmptyAndNullValues_ShouldReturn0() {
        final Map<String, List<String>> categoryKeysWithMissingParents = new HashMap<>();
        categoryKeysWithMissingParents.put("parent1", null);
        categoryKeysWithMissingParents.put("parent2", emptyList());

        categorySyncStatistics.setCategoryKeysWithMissingParents(categoryKeysWithMissingParents);
        assertThat(categorySyncStatistics.getNumberOfCategoriesWithMissingParents()).isEqualTo(0);
    }

    @Test
    public void getNumberOfCategoriesWithMissingParents_WithNonEmptyMap_ShouldReturnCorrectNumberOfChildren() {
        final Map<String, List<String>> categoryKeysWithMissingParents = new HashMap<>();
        final ArrayList<String> firstMissingParentChildrenKeys = new ArrayList<>();
        firstMissingParentChildrenKeys.add("key1");
        firstMissingParentChildrenKeys.add("key2");

        final ArrayList<String> secondMissingParentChildrenKeys = new ArrayList<>();
        secondMissingParentChildrenKeys.add("key3");
        secondMissingParentChildrenKeys.add("key4");

        categoryKeysWithMissingParents.put("parent1", firstMissingParentChildrenKeys);
        categoryKeysWithMissingParents.put("parent2", secondMissingParentChildrenKeys);


        categorySyncStatistics.setCategoryKeysWithMissingParents(categoryKeysWithMissingParents);

        assertThat(categorySyncStatistics.getNumberOfCategoriesWithMissingParents()).isEqualTo(4);
    }
}
