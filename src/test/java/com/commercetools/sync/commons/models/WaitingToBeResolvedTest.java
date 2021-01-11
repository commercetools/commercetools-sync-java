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

import io.sphere.sdk.products.ProductDraft;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class WaitingToBeResolvedTest {

  @Test
  void setProductDraft_WithNonNullProductDraft_ShouldSetProductDraft() {
    // preparation
    final ProductDraft productDraft = mock(ProductDraft.class);
    final WaitingToBeResolved waitingToBeResolved = new WaitingToBeResolved();

    // test
    waitingToBeResolved.setProductDraft(productDraft);

    // assertions
    assertThat(waitingToBeResolved.getProductDraft()).isEqualTo(productDraft);
  }

  @Test
  void setMissingReferencedProductKeys_WithNonNullSet_ShouldSetTheSet() {
    // preparation
    final WaitingToBeResolved waitingToBeResolved = new WaitingToBeResolved();

    // test
    waitingToBeResolved.setMissingReferencedProductKeys(emptySet());

    // assertions
    assertThat(waitingToBeResolved.getMissingReferencedProductKeys()).isEqualTo(emptySet());
  }

  @Test
  void equals_WithSameRef_ShouldReturnTrue() {
    // preparation
    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolved(mock(ProductDraft.class), new HashSet<>());
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
        new WaitingToBeResolved(mock(ProductDraft.class), new HashSet<>());
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
        new WaitingToBeResolved(mock(ProductDraft.class), new HashSet<>());
    final WaitingToBeResolved other =
        new WaitingToBeResolved(mock(ProductDraft.class), new HashSet<>());

    // test
    boolean result = waitingToBeResolved.equals(other);

    // assertions
    assertTrue(result);
  }

  @Test
  void equals_WithDifferentMissingRefKeys_ShouldReturnFalse() {
    // preparation
    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolved(mock(ProductDraft.class), new HashSet<>());
    final WaitingToBeResolved other =
        new WaitingToBeResolved(mock(ProductDraft.class), singleton("foo"));

    // test
    boolean result = waitingToBeResolved.equals(other);

    // assertions
    assertFalse(result);
  }

  @Test
  void equals_WithDifferentProductDraft_ShouldReturnFalse() {
    // preparation
    final ProductDraft productDraft = mock(ProductDraft.class);
    final ProductDraft productDraft1 = mock(ProductDraft.class);
    when(productDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolved(productDraft, new HashSet<>());
    final WaitingToBeResolved other = new WaitingToBeResolved(productDraft1, new HashSet<>());

    // test
    boolean result = waitingToBeResolved.equals(other);

    // assertions
    assertFalse(result);
  }

  @Test
  void equals_WithCompletelyDifferentFieldValues_ShouldReturnFalse() {
    // preparation
    final ProductDraft productDraft = mock(ProductDraft.class);
    final ProductDraft productDraft1 = mock(ProductDraft.class);
    when(productDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolved(productDraft, singleton("foo"));
    final WaitingToBeResolved other = new WaitingToBeResolved(productDraft1, singleton("bar"));

    // test
    boolean result = waitingToBeResolved.equals(other);

    // assertions
    assertFalse(result);
  }

  @Test
  void hashCode_withSameInstances_ShouldBeEquals() {
    // preparation
    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolved(mock(ProductDraft.class), new HashSet<>());
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
        new WaitingToBeResolved(mock(ProductDraft.class), new HashSet<>());
    final WaitingToBeResolved other =
        new WaitingToBeResolved(mock(ProductDraft.class), new HashSet<>());

    // test
    final int hash1 = waitingToBeResolved.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertEquals(hash1, hash2);
  }

  @Test
  void hashCode_withDifferentProductKeyAndSameRefSet_ShouldNotBeEquals() {
    // preparation
    final ProductDraft productDraft = mock(ProductDraft.class);
    final ProductDraft productDraft1 = mock(ProductDraft.class);
    when(productDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolved(productDraft, new HashSet<>());
    final WaitingToBeResolved other = new WaitingToBeResolved(productDraft1, new HashSet<>());

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
        new WaitingToBeResolved(mock(ProductDraft.class), new HashSet<>());
    final WaitingToBeResolved other =
        new WaitingToBeResolved(mock(ProductDraft.class), singleton("foo"));

    // test
    final int hash1 = waitingToBeResolved.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertNotEquals(hash1, hash2);
  }

  @Test
  void hashCode_withCompletelyDifferentFields_ShouldNotBeEquals() {
    // preparation
    final ProductDraft productDraft = mock(ProductDraft.class);
    final ProductDraft productDraft1 = mock(ProductDraft.class);
    when(productDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolved waitingToBeResolved =
        new WaitingToBeResolved(productDraft, singleton("foo"));
    final WaitingToBeResolved other = new WaitingToBeResolved(productDraft1, singleton("bar"));

    // test
    final int hash1 = waitingToBeResolved.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertNotEquals(hash1, hash2);
  }
}
