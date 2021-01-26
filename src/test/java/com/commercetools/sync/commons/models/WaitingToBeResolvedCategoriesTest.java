package com.commercetools.sync.commons.models;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.categories.CategoryDraft;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class WaitingToBeResolvedCategoriesTest {

  @Test
  void setCategoryDraft_WithNonNullCategoryDraft_ShouldSetCategoryDraft() {
    // preparation
    final CategoryDraft CategoryDraft = mock(CategoryDraft.class);
    final WaitingToBeResolvedCategories waitingToBeResolved = new WaitingToBeResolvedCategories();

    // test
    waitingToBeResolved.setCategoryDraft(CategoryDraft);

    // assertions
    assertThat(waitingToBeResolved.getCategoryDraft()).isEqualTo(CategoryDraft);
  }

  @Test
  void setMissingReferencedProductKeys_WithNonNullSet_ShouldSetTheSet() {
    // preparation
    final WaitingToBeResolvedCategories waitingToBeResolved = new WaitingToBeResolvedCategories();

    // test
    waitingToBeResolved.setMissingReferencedCategoriesKeys(emptySet());

    // assertions
    assertThat(waitingToBeResolved.getMissingReferencedCategoriesKeys()).isEqualTo(emptySet());
  }

  @Test
  void equals_WithSameRef_ShouldReturnTrue() {
    // preparation
    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(mock(CategoryDraft.class), new HashSet<>());
    final WaitingToBeResolved other = waitingToBeResolved;

    // test
    boolean result = waitingToBeResolved.equals(other);

    // assertions
    assertTrue(result);
  }

  @Test
  void equals_WithDiffType_ShouldReturnFalse() {
    // preparation
    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(mock(CategoryDraft.class), new HashSet<>());
    final Object other = new Object();

    // test
    boolean result = waitingToBeResolved.equals(other);

    // assertions
    assertFalse(result);
  }

  @Test
  void equals_WithEqualObjects_ShouldReturnTrue() {
    // preparation
    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(mock(CategoryDraft.class), new HashSet<>());
    final WaitingToBeResolved other =
        new WaitingToBeResolvedCategories(mock(CategoryDraft.class), new HashSet<>());

    // test
    boolean result = waitingToBeResolved.equals(other);

    // assertions
    assertTrue(result);
  }

  @Test
  void equals_WithDifferentMissingRefKeys_ShouldReturnFalse() {
    // preparation
    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(mock(CategoryDraft.class), new HashSet<>());
    final WaitingToBeResolved other =
        new WaitingToBeResolvedCategories(mock(CategoryDraft.class), singleton("foo"));

    // test
    boolean result = waitingToBeResolved.equals(other);

    // assertions
    assertFalse(result);
  }

  @Test
  void equals_WithDifferentCategoryDraft_ShouldReturnFalse() {
    // preparation
    final CategoryDraft CategoryDraft = mock(CategoryDraft.class);
    final CategoryDraft CategoryDraft1 = mock(CategoryDraft.class);
    when(CategoryDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(CategoryDraft, new HashSet<>());
    final WaitingToBeResolved other =
        new WaitingToBeResolvedCategories(CategoryDraft1, new HashSet<>());

    // test
    boolean result = waitingToBeResolved.equals(other);

    // assertions
    assertFalse(result);
  }

  @Test
  void equals_WithCompletelyDifferentFieldValues_ShouldReturnFalse() {
    // preparation
    final CategoryDraft CategoryDraft = mock(CategoryDraft.class);
    final CategoryDraft CategoryDraft1 = mock(CategoryDraft.class);
    when(CategoryDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(CategoryDraft, singleton("foo"));
    final WaitingToBeResolved other =
        new WaitingToBeResolvedCategories(CategoryDraft1, singleton("bar"));

    // test
    boolean result = waitingToBeResolved.equals(other);

    // assertions
    assertFalse(result);
  }

  @Test
  void hashCode_withSameInstances_ShouldBeEquals() {
    // preparation
    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(mock(CategoryDraft.class), new HashSet<>());
    final WaitingToBeResolved other = waitingToBeResolved;

    // test
    final int hash1 = waitingToBeResolved.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertEquals(hash1, hash2);
  }

  @Test
  void hashCode_withSameProductKeyAndSameRefSet_ShouldBeEquals() {
    // preparation
    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(mock(CategoryDraft.class), new HashSet<>());
    final WaitingToBeResolved other =
        new WaitingToBeResolvedCategories(mock(CategoryDraft.class), new HashSet<>());

    // test
    final int hash1 = waitingToBeResolved.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertEquals(hash1, hash2);
  }

  @Test
  void hashCode_withDifferentProductKeyAndSameRefSet_ShouldNotBeEquals() {
    // preparation
    final CategoryDraft CategoryDraft = mock(CategoryDraft.class);
    final CategoryDraft CategoryDraft1 = mock(CategoryDraft.class);
    when(CategoryDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(CategoryDraft, new HashSet<>());
    final WaitingToBeResolved other =
        new WaitingToBeResolvedCategories(CategoryDraft1, new HashSet<>());

    // test
    final int hash1 = waitingToBeResolved.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertNotEquals(hash1, hash2);
  }

  @Test
  void hashCode_withSameProductKeyAndDiffRefSet_ShouldNotBeEquals() {
    // preparation
    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(mock(CategoryDraft.class), new HashSet<>());
    final WaitingToBeResolved other =
        new WaitingToBeResolvedCategories(mock(CategoryDraft.class), singleton("foo"));

    // test
    final int hash1 = waitingToBeResolved.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertNotEquals(hash1, hash2);
  }

  @Test
  void hashCode_withCompletelyDifferentFields_ShouldNotBeEquals() {
    // preparation
    final CategoryDraft CategoryDraft = mock(CategoryDraft.class);
    final CategoryDraft CategoryDraft1 = mock(CategoryDraft.class);
    when(CategoryDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(CategoryDraft, singleton("foo"));
    final WaitingToBeResolved other =
        new WaitingToBeResolvedCategories(CategoryDraft1, singleton("bar"));

    // test
    final int hash1 = waitingToBeResolved.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertNotEquals(hash1, hash2);
  }

  @Test
  void getKey_withValidCategoryDraftKey_ShouldReturnKey() {
    // preparation
    String key = "anyKey";
    final CategoryDraft CategoryDraft1 = mock(CategoryDraft.class);
    when(CategoryDraft1.getKey()).thenReturn(key);

    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolvedCategories(CategoryDraft1, singleton("bar"));

    // test
    final String resolvedKey = waitingToBeResolved.getKey();

    // assertions
    assertEquals(resolvedKey, key);
  }
}
