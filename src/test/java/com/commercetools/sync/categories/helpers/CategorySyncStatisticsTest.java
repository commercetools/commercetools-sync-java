package com.commercetools.sync.categories.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    assertThat(categorySyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 3 categories were processed in total "
                + "(1 created, 1 updated, 1 failed to sync and 0 categories with a missing parent).");
  }

  @Test
  void getNumberOfProductsWithMissingParents_WithEmptyMap_ShouldReturn0() {
    final int result = categorySyncStatistics.getNumberOfCategoriesWithMissingParents();

    assertThat(result).isZero();
  }

  @Test
  void
      getNumberOfCategoriesWithMissingParents_WithNonEmptyNonDuplicateKeyMap_ShouldReturnCorrectValue() {
    // preparation
    categorySyncStatistics.addMissingDependency("parent1", "key1");
    categorySyncStatistics.addMissingDependency("parent1", "key2");

    categorySyncStatistics.addMissingDependency("parent1", "key3");
    categorySyncStatistics.addMissingDependency("parent2", "key4");

    // test
    final int result = categorySyncStatistics.getNumberOfCategoriesWithMissingParents();

    // assert
    assertThat(result).isEqualTo(4);
  }

  @Test
  void
      getNumberOfCategoriesWithMissingParents_WithNonEmptyDuplicateKeyMap_ShouldReturnCorrectValue() {
    // preparation
    categorySyncStatistics.addMissingDependency("parent1", "key1");
    categorySyncStatistics.addMissingDependency("parent1", "key2");

    categorySyncStatistics.addMissingDependency("parent1", "key3");
    categorySyncStatistics.addMissingDependency("parent2", "key1");

    // test
    final int result = categorySyncStatistics.getNumberOfCategoriesWithMissingParents();

    // assert
    assertThat(result).isEqualTo(3);
  }

  @Test
  void addMissingDependency_WithEmptyParentsAndChildren_ShouldAddKeys() {
    // preparation
    categorySyncStatistics.addMissingDependency("", "");
    categorySyncStatistics.addMissingDependency("foo", "");
    categorySyncStatistics.addMissingDependency("", "");

    // test
    final int result = categorySyncStatistics.getNumberOfCategoriesWithMissingParents();

    // assert
    assertThat(result).isOne();
  }

  @Test
  void removeChildCategoryKeyFromMissingParentsMap_WithExistingKey_ShouldRemoveKey() {
    // preparation
    categorySyncStatistics.addMissingDependency("foo", "a");
    categorySyncStatistics.addMissingDependency("foo", "b");

    // test
    categorySyncStatistics.removeChildCategoryKeyFromMissingParentsMap("foo", "a");

    // assert
    assertThat(categorySyncStatistics.getNumberOfCategoriesWithMissingParents()).isEqualTo(1);
  }

  @Test
  void removeChildCategoryKeyFromMissingParentsMap_WithExistingKey_ShouldRemoveParentMap() {
    // preparation
    categorySyncStatistics.addMissingDependency("foo", "a");

    // test
    categorySyncStatistics.removeChildCategoryKeyFromMissingParentsMap("foo", "a");

    // assert
    assertThat(categorySyncStatistics.getNumberOfCategoriesWithMissingParents()).isEqualTo(0);
  }
}
