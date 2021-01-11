package com.commercetools.sync.products.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncStatisticsTest {
  private ProductSyncStatistics productSyncStatistics;

  @BeforeEach
  void setup() {
    productSyncStatistics = new ProductSyncStatistics();
  }

  @Test
  void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
    productSyncStatistics.incrementCreated(1);
    productSyncStatistics.incrementFailed(1);
    productSyncStatistics.incrementUpdated(1);
    productSyncStatistics.incrementProcessed(3);

    assertThat(productSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 3 product(s) were processed in total "
                + "(1 created, 1 updated, 1 failed to sync and 0 product(s) with missing reference(s)).");
  }

  @Test
  void getNumberOfProductsWithMissingParents_WithEmptyMap_ShouldReturn0() {
    final int result = productSyncStatistics.getNumberOfProductsWithMissingParents();

    assertThat(result).isZero();
  }

  @Test
  void
      getNumberOfCategoriesWithMissingParents_WithNonEmptyNonDuplicateKeyMap_ShouldReturnCorrectValue() {
    // preparation
    productSyncStatistics.addMissingDependency("parent1", "key1");
    productSyncStatistics.addMissingDependency("parent1", "key2");

    productSyncStatistics.addMissingDependency("parent1", "key3");
    productSyncStatistics.addMissingDependency("parent2", "key4");

    // test
    final int result = productSyncStatistics.getNumberOfProductsWithMissingParents();

    // assert
    assertThat(result).isEqualTo(4);
  }

  @Test
  void
      getNumberOfCategoriesWithMissingParents_WithNonEmptyDuplicateKeyMap_ShouldReturnCorrectValue() {
    // preparation
    productSyncStatistics.addMissingDependency("parent1", "key1");
    productSyncStatistics.addMissingDependency("parent1", "key2");

    productSyncStatistics.addMissingDependency("parent1", "key3");
    productSyncStatistics.addMissingDependency("parent2", "key1");

    // test
    final int result = productSyncStatistics.getNumberOfProductsWithMissingParents();

    // assert
    assertThat(result).isEqualTo(3);
  }

  @Test
  void addMissingDependency_WithEmptyParentsAndChildren_ShouldAddKeys() {
    // preparation
    productSyncStatistics.addMissingDependency("", "");
    productSyncStatistics.addMissingDependency("foo", "");
    productSyncStatistics.addMissingDependency("", "");

    // test
    final int result = productSyncStatistics.getNumberOfProductsWithMissingParents();

    // assert
    assertThat(result).isOne();
  }

  @Test
  void removeAndGetReferencingKeys_WithEmptyMap_ShouldGetNull() {
    // test
    final Set<String> result = productSyncStatistics.removeAndGetReferencingKeys("foo");

    // assert
    assertThat(result).isNull();
  }

  @Test
  void removeAndGetReferencingKeys_WithNonExistingKey_ShouldGetAndRemoveNoKey() {
    // preparation
    productSyncStatistics.addMissingDependency("bar", "a");
    productSyncStatistics.addMissingDependency("foo", "b");

    // test
    final Set<String> result = productSyncStatistics.removeAndGetReferencingKeys("x");

    // assert
    assertThat(result).isNull();
    assertThat(productSyncStatistics.getNumberOfProductsWithMissingParents()).isEqualTo(2);
  }

  @Test
  void removeAndGetReferencingKeys_WithExistingKey_ShouldGetAndRemoveKey() {
    // preparation
    productSyncStatistics.addMissingDependency("bar", "a");
    productSyncStatistics.addMissingDependency("foo", "b");

    // test
    final Set<String> result = productSyncStatistics.removeAndGetReferencingKeys("foo");

    // assert
    assertThat(result).containsExactly("b");
    assertThat(productSyncStatistics.getNumberOfProductsWithMissingParents()).isEqualTo(1);
  }

  @Test
  void removeAndGetReferencingKeys_WithExistingKey_ShouldGetAndRemoveAllKeys() {
    // preparation
    productSyncStatistics.addMissingDependency("bar", "a");
    productSyncStatistics.addMissingDependency("foo", "b");
    productSyncStatistics.addMissingDependency("foo", "c");

    // test
    final Set<String> result = productSyncStatistics.removeAndGetReferencingKeys("foo");

    // assert
    assertThat(result).containsExactly("b", "c");
    assertThat(productSyncStatistics.getNumberOfProductsWithMissingParents()).isEqualTo(1);
  }
}
