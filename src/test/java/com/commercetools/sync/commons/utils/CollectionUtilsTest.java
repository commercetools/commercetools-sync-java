package com.commercetools.sync.commons.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToMap;
import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToSet;
import static com.commercetools.sync.commons.utils.CollectionUtils.emptyIfNull;
import static com.commercetools.sync.commons.utils.CollectionUtils.filterCollection;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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
        assertThat(filterCollection(abcd, s -> true)).containsExactlyInAnyOrder("a", "a", "b", "c", "c", "d", "d", "g");
        assertThat(filterCollection(abcd, s -> false)).isEmpty();
        assertThat(filterCollection(abcd, s -> s.charAt(0) > 'b')).containsExactlyInAnyOrder("c", "c", "d", "d", "g");
    }

    @Test
    void collectionToSet_emptyCases() {
        assertThat(collectionToSet(null, i -> i)).isEmpty();
        assertThat(collectionToSet(Collections.emptyList(), i -> i)).isEmpty();
    }

    @Test
    void collectionToSet_normalCases() {
        List<Pair<String, Integer>> abcd = asList(
                Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4));
        assertThat(collectionToSet(abcd, Pair::getKey)).containsExactlyInAnyOrder("a", "b", "c", "d");
        assertThat(collectionToSet(abcd, Pair::getValue)).containsExactlyInAnyOrder(1, 2, 3, 4);
    }

    @Test
    void collectionToSet_duplicates() {
        List<Pair<String, Integer>> abcd = asList(
                Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4), Pair.of("d", 5), Pair.of("b", 6));
        // 2 duplicate keys should be suppressed
        assertThat(collectionToSet(abcd, Pair::getKey)).containsExactlyInAnyOrder("a", "b", "c", "d");
        // no duplicate values - nothing should be suppressed
        assertThat(collectionToSet(abcd, Pair::getValue)).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);
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
        List<Pair<String, Integer>> abcd = asList(
                Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4));

        assertThat(collectionToMap(abcd, Pair::getKey, Pair::getValue))
                .containsOnly(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4));

        assertThat(collectionToMap(abcd, Pair::getValue, Pair::getKey))
                .containsOnly(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c"), Pair.of(4, "d"));

        assertThat(collectionToMap(abcd, Pair::getKey)) // with default value mapper
                .containsOnly(Pair.of("a", Pair.of("a", 1)), Pair.of("b", Pair.of("b", 2)),
                        Pair.of("c", Pair.of("c", 3)), Pair.of("d", Pair.of("d", 4)));

        assertThat(collectionToMap(abcd, Pair::getValue)) // with default value mapper
                .containsOnly(Pair.of(1, Pair.of("a", 1)), Pair.of(2, Pair.of("b", 2)), Pair.of(3, Pair.of("c", 3)),
                        Pair.of(4, Pair.of("d", 4)));
    }

    @Test
    void collectionToMap_duplicates() {
        List<Pair<String, Integer>> abcd = asList(
                Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4), Pair.of("d", 5), Pair.of("b", 6));

        // 2 duplicate keys should be suppressed
        assertThat(collectionToMap(abcd, Pair::getKey, Pair::getValue)).containsOnlyKeys("a", "b", "c", "d");
        assertThat(collectionToMap(abcd, Pair::getKey)) // with default value mapper
                .containsOnlyKeys("a", "b", "c", "d");

        // no duplicate values - nothing should be suppressed
        assertThat(collectionToMap(abcd, Pair::getValue, Pair::getKey))
                .containsOnly(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c"), Pair.of(4, "d"),
                        Pair.of(5, "d"), Pair.of(6, "b"));

        assertThat(collectionToMap(abcd, Pair::getValue)) // with default value mapper
                .containsOnly(Pair.of(1, Pair.of("a", 1)), Pair.of(2, Pair.of("b", 2)), Pair.of(3, Pair.of("c", 3)),
                        Pair.of(4, Pair.of("d", 4)), Pair.of(5, Pair.of("d", 5)), Pair.of(6, Pair.of("b", 6)));
    }

    @Test
    void emptyIfNull_withNullInstances_returnsNonNullInstances() {
        Collection<String> nonNullEmptyCollection = emptyIfNull((Collection<String>)null);
        assertThat(nonNullEmptyCollection).isNotNull();
        assertThat(nonNullEmptyCollection).isEmpty();

        Set<Exception> nonNullEmptySet = emptyIfNull((Set<Exception>)null);
        assertThat(nonNullEmptySet).isNotNull();
        assertThat(nonNullEmptySet).isEmpty();

        List<Integer> nonNullEmptyList = emptyIfNull((List<Integer>)null);
        assertThat(nonNullEmptyList).isNotNull();
        assertThat(nonNullEmptyList).isEmpty();

        Map<Double, CharSequence> nonNullEmptyMap = emptyIfNull((Map<Double, CharSequence>)null);
        assertThat(nonNullEmptyMap).isNotNull();
        assertThat(nonNullEmptyMap).isEmpty();
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
    void emptyIfNull_withHashSet_returnsSameInstance() {
        HashSet<String> arrayListCollection = new HashSet<>();
        arrayListCollection.add("woot");
        arrayListCollection.add("hack");

        Set<String> actualCollection = emptyIfNull(arrayListCollection);
        assertThat(actualCollection).isSameAs(arrayListCollection);
        assertThat(actualCollection).containsExactlyInAnyOrder("woot", "hack"); // verify HashSet is not mutated
    }

    @Test
    void emptyIfNull_withListInstances_returnsNonNullList() {
        List<String> originalListToTest = asList("a", "b");

        List<String> actualListToTest = emptyIfNull(originalListToTest);
        assertThat(actualListToTest).isSameAs(originalListToTest);
        assertThat(actualListToTest).containsExactly("a", "b"); // verify list is not mutated
    }

    @Test
    void emptyIfNull_withMapInstances_returnsNonNullMap() {
        Map<String, Integer> mapToTest = new HashMap<>();
        mapToTest.put("X", 10);
        mapToTest.put("Y", 11);

        assertThat(emptyIfNull(mapToTest)).isSameAs(mapToTest);
        assertThat(mapToTest).containsExactly(entry("X", 10), entry("Y", 11)); // verify map is not mutated
    }
}