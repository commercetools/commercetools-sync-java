package com.commercetools.sync.states.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateSyncStatisticsTest {

  private StateSyncStatistics stateSyncStatistics;

  @BeforeEach
  void setup() {
    stateSyncStatistics = new StateSyncStatistics();
  }

  @Test
  void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
    stateSyncStatistics.incrementCreated(1);
    stateSyncStatistics.incrementFailed(1);
    stateSyncStatistics.incrementUpdated(1);
    stateSyncStatistics.incrementProcessed(3);

    assertThat(stateSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 3 state(s) were processed in total "
                + "(1 created, 1 updated, 1 failed to sync and 0 state(s) with missing transition(s)).");
  }

  @Test
  void getNumberOfStatesWithMissingParents_WithEmptyMap_ShouldReturn0() {
    final int result = stateSyncStatistics.getNumberOfStatesWithMissingParents();

    assertThat(result).isZero();
  }

  @Test
  void
      getNumberOfCategoriesWithMissingParents_WithNonEmptyNonDuplicateKeyMap_ShouldReturnCorrectValue() {
    // preparation
    stateSyncStatistics.addMissingDependency("parent1", "key1");
    stateSyncStatistics.addMissingDependency("parent1", "key2");

    stateSyncStatistics.addMissingDependency("parent1", "key3");
    stateSyncStatistics.addMissingDependency("parent2", "key4");

    // test
    final int result = stateSyncStatistics.getNumberOfStatesWithMissingParents();

    // assert
    assertThat(result).isEqualTo(4);
  }

  @Test
  void
      getNumberOfCategoriesWithMissingParents_WithNonEmptyDuplicateKeyMap_ShouldReturnCorrectValue() {
    // preparation
    stateSyncStatistics.addMissingDependency("parent1", "key1");
    stateSyncStatistics.addMissingDependency("parent1", "key2");

    stateSyncStatistics.addMissingDependency("parent1", "key3");
    stateSyncStatistics.addMissingDependency("parent2", "key1");

    // test
    final int result = stateSyncStatistics.getNumberOfStatesWithMissingParents();

    // assert
    assertThat(result).isEqualTo(3);
  }

  @Test
  void addMissingDependency_WithEmptyParentsAndChildren_ShouldAddKeys() {
    // preparation
    stateSyncStatistics.addMissingDependency("", "");
    stateSyncStatistics.addMissingDependency("foo", "");
    stateSyncStatistics.addMissingDependency("", "");

    // test
    final int result = stateSyncStatistics.getNumberOfStatesWithMissingParents();

    // assert
    assertThat(result).isOne();
  }

  @Test
  void removeAndGetReferencingKeys_WithEmptyMap_ShouldGetNull() {
    // test
    final Set<String> result = stateSyncStatistics.removeAndGetReferencingKeys("foo");

    // assert
    assertThat(result).isNull();
  }

  @Test
  void removeAndGetReferencingKeys_WithNonExistingKey_ShouldGetAndRemoveNoKey() {
    // preparation
    stateSyncStatistics.addMissingDependency("bar", "a");
    stateSyncStatistics.addMissingDependency("foo", "b");

    // test
    final Set<String> result = stateSyncStatistics.removeAndGetReferencingKeys("x");

    // assert
    assertThat(result).isNull();
    assertThat(stateSyncStatistics.getNumberOfStatesWithMissingParents()).isEqualTo(2);
  }

  @Test
  void removeAndGetReferencingKeys_WithExistingKey_ShouldGetAndRemoveKey() {
    // preparation
    stateSyncStatistics.addMissingDependency("bar", "a");
    stateSyncStatistics.addMissingDependency("foo", "b");

    // test
    final Set<String> result = stateSyncStatistics.removeAndGetReferencingKeys("foo");

    // assert
    assertThat(result).containsExactly("b");
    assertThat(stateSyncStatistics.getNumberOfStatesWithMissingParents()).isEqualTo(1);
  }

  @Test
  void removeAndGetReferencingKeys_WithExistingKey_ShouldGetAndRemoveAllKeys() {
    // preparation
    stateSyncStatistics.addMissingDependency("bar", "a");
    stateSyncStatistics.addMissingDependency("foo", "b");
    stateSyncStatistics.addMissingDependency("foo", "c");

    // test
    final Set<String> result = stateSyncStatistics.removeAndGetReferencingKeys("foo");

    // assert
    assertThat(result).containsExactly("b", "c");
    assertThat(stateSyncStatistics.getNumberOfStatesWithMissingParents()).isEqualTo(1);
  }
}
