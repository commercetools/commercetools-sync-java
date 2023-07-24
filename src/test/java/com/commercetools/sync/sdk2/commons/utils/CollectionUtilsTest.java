package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.sdk2.commons.utils.CollectionUtils.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

class CollectionUtilsTest {

  @Test
  void filterCollection_emptyCases() {
    assertThat(filterCollection(null, i -> true)).isEmpty();
    assertThat(filterCollection(Collections.emptyList(), i -> true)).isEmpty();
  }

  @Test
  void filterCollection_normalCases() {
    List<String> abcd = asList("a", "b", "c", "d");
    assertThat(filterCollection(abcd, s -> true)).containsExactlyInAnyOrder("a", "b", "c", "d");
    assertThat(filterCollection(abcd, s -> false)).isEmpty();
    assertThat(filterCollection(abcd, s -> s.charAt(0) > 'b')).containsExactlyInAnyOrder("c", "d");
  }

  @Test
  void filterCollection_duplicates() {
    List<String> abcd = asList("a", "b", "c", "d", "d", "c", "g", "a");
    assertThat(filterCollection(abcd, s -> true))
        .containsExactlyInAnyOrder("a", "a", "b", "c", "c", "d", "d", "g");
    assertThat(filterCollection(abcd, s -> false)).isEmpty();
    assertThat(filterCollection(abcd, s -> s.charAt(0) > 'b'))
        .containsExactlyInAnyOrder("c", "c", "d", "d", "g");
  }

  @Test
  void collectionToMap_emptyCases() {
    assertThat(collectionToMap(null, k -> k)).isEmpty();
    assertThat(collectionToMap(null, k -> k, v -> v)).isEmpty();
    assertThat(collectionToMap(Collections.emptyList(), k -> k)).isEmpty();
    assertThat(collectionToMap(Collections.emptyList(), k -> k, v -> v)).isEmpty();
  }

  @Test
  void collectionToMap_normalCases() {
    List<Pair<String, Integer>> abcd =
        asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4));

    assertThat(collectionToMap(abcd, Pair::getKey, Pair::getValue))
        .containsOnly(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4));

    assertThat(collectionToMap(abcd, Pair::getValue, Pair::getKey))
        .containsOnly(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c"), Pair.of(4, "d"));

    assertThat(collectionToMap(abcd, Pair::getKey)) // with default value mapper
        .containsOnly(
            Pair.of("a", Pair.of("a", 1)),
            Pair.of("b", Pair.of("b", 2)),
            Pair.of("c", Pair.of("c", 3)),
            Pair.of("d", Pair.of("d", 4)));

    assertThat(collectionToMap(abcd, Pair::getValue)) // with default value mapper
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
    assertThat(collectionToMap(abcd, Pair::getKey, Pair::getValue))
        .containsOnlyKeys("a", "b", "c", "d");
    assertThat(collectionToMap(abcd, Pair::getKey)) // with default value mapper
        .containsOnlyKeys("a", "b", "c", "d");

    // no duplicate values - nothing should be suppressed
    assertThat(collectionToMap(abcd, Pair::getValue, Pair::getKey))
        .containsOnly(
            Pair.of(1, "a"),
            Pair.of(2, "b"),
            Pair.of(3, "c"),
            Pair.of(4, "d"),
            Pair.of(5, "d"),
            Pair.of(6, "b"));

    assertThat(collectionToMap(abcd, Pair::getValue)) // with default value mapper
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

    List<String> actualCollection = emptyIfNull(arrayListCollection);
    assertThat(actualCollection).isSameAs(arrayListCollection);
    assertThat(actualCollection).containsExactly("a", "b"); // verify ArrayList is not mutated
  }

  @Test
  void emptyIfNull_withListInstances_returnsNonNullList() {
    List<String> originalListToTest = asList("a", "b");

    List<String> actualListToTest = emptyIfNull(originalListToTest);
    assertThat(actualListToTest).isSameAs(originalListToTest);
    assertThat(actualListToTest).containsExactly("a", "b"); // verify list is not mutated
  }
}
