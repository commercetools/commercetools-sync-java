package com.commercetools.sync.commons.utils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CollectionUtilsTest {

  @Test
  void filterCollection_emptyCases() {
    Assertions.assertThat(CollectionUtils.filterCollection(null, i -> true)).isEmpty();
    Assertions.assertThat(CollectionUtils.filterCollection(Collections.emptyList(), i -> true))
        .isEmpty();
  }

  @Test
  void filterCollection_normalCases() {
    List<String> abcd = asList("a", "b", "c", "d");
    Assertions.assertThat(CollectionUtils.filterCollection(abcd, s -> true))
        .containsExactlyInAnyOrder("a", "b", "c", "d");
    Assertions.assertThat(CollectionUtils.filterCollection(abcd, s -> false)).isEmpty();
    Assertions.assertThat(CollectionUtils.filterCollection(abcd, s -> s.charAt(0) > 'b'))
        .containsExactlyInAnyOrder("c", "d");
  }

  @Test
  void filterCollection_duplicates() {
    List<String> abcd = asList("a", "b", "c", "d", "d", "c", "g", "a");
    Assertions.assertThat(CollectionUtils.filterCollection(abcd, s -> true))
        .containsExactlyInAnyOrder("a", "a", "b", "c", "c", "d", "d", "g");
    Assertions.assertThat(CollectionUtils.filterCollection(abcd, s -> false)).isEmpty();
    Assertions.assertThat(CollectionUtils.filterCollection(abcd, s -> s.charAt(0) > 'b'))
        .containsExactlyInAnyOrder("c", "c", "d", "d", "g");
  }

  @Test
  void collectionToMap_emptyCases() {
    Assertions.assertThat(CollectionUtils.collectionToMap(null, k -> k)).isEmpty();
    Assertions.assertThat(CollectionUtils.collectionToMap(null, k -> k, v -> v)).isEmpty();
    Assertions.assertThat(CollectionUtils.collectionToMap(Collections.emptyList(), k -> k))
        .isEmpty();
    Assertions.assertThat(CollectionUtils.collectionToMap(Collections.emptyList(), k -> k, v -> v))
        .isEmpty();
  }

  @Test
  void collectionToMap_normalCases() {
    List<Pair<String, Integer>> abcd =
        asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4));

    Assertions.assertThat(CollectionUtils.collectionToMap(abcd, Pair::getKey, Pair::getValue))
        .containsOnly(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4));

    Assertions.assertThat(CollectionUtils.collectionToMap(abcd, Pair::getValue, Pair::getKey))
        .containsOnly(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c"), Pair.of(4, "d"));

    Assertions.assertThat(
            CollectionUtils.collectionToMap(abcd, Pair::getKey)) // with default value mapper
        .containsOnly(
            Pair.of("a", Pair.of("a", 1)),
            Pair.of("b", Pair.of("b", 2)),
            Pair.of("c", Pair.of("c", 3)),
            Pair.of("d", Pair.of("d", 4)));

    Assertions.assertThat(
            CollectionUtils.collectionToMap(abcd, Pair::getValue)) // with default value mapper
        .containsOnly(
            Pair.of(1, Pair.of("a", 1)),
            Pair.of(2, Pair.of("b", 2)),
            Pair.of(3, Pair.of("c", 3)),
            Pair.of(4, Pair.of("d", 4)));
  }

  @Test
  void collectionToMap_duplicates() {
    List<Pair<String, Integer>> abcd =
        asList(
            Pair.of("a", 1),
            Pair.of("b", 2),
            Pair.of("c", 3),
            Pair.of("d", 4),
            Pair.of("d", 5),
            Pair.of("b", 6));

    // 2 duplicate keys should be suppressed
    Assertions.assertThat(CollectionUtils.collectionToMap(abcd, Pair::getKey, Pair::getValue))
        .containsOnlyKeys("a", "b", "c", "d");
    Assertions.assertThat(
            CollectionUtils.collectionToMap(abcd, Pair::getKey)) // with default value mapper
        .containsOnlyKeys("a", "b", "c", "d");

    // no duplicate values - nothing should be suppressed
    Assertions.assertThat(CollectionUtils.collectionToMap(abcd, Pair::getValue, Pair::getKey))
        .containsOnly(
            Pair.of(1, "a"),
            Pair.of(2, "b"),
            Pair.of(3, "c"),
            Pair.of(4, "d"),
            Pair.of(5, "d"),
            Pair.of(6, "b"));

    Assertions.assertThat(
            CollectionUtils.collectionToMap(abcd, Pair::getValue)) // with default value mapper
        .containsOnly(
            Pair.of(1, Pair.of("a", 1)),
            Pair.of(2, Pair.of("b", 2)),
            Pair.of(3, Pair.of("c", 3)),
            Pair.of(4, Pair.of("d", 4)),
            Pair.of(5, Pair.of("d", 5)),
            Pair.of(6, Pair.of("b", 6)));
  }

  @Test
  void emptyIfNull_withArrayList_returnsSameInstance() {
    ArrayList<String> arrayListCollection = new ArrayList<>();
    arrayListCollection.add("a");
    arrayListCollection.add("b");

    List<String> actualCollection = CollectionUtils.emptyIfNull(arrayListCollection);
    assertThat(actualCollection).isSameAs(arrayListCollection);
    assertThat(actualCollection).containsExactly("a", "b"); // verify ArrayList is not mutated
  }

  @Test
  void emptyIfNull_withListInstances_returnsNonNullList() {
    List<String> originalListToTest = asList("a", "b");

    List<String> actualListToTest = CollectionUtils.emptyIfNull(originalListToTest);
    assertThat(actualListToTest).isSameAs(originalListToTest);
    assertThat(actualListToTest).containsExactly("a", "b"); // verify list is not mutated
  }
}
