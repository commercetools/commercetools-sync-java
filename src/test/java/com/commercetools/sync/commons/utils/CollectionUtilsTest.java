package com.commercetools.sync.commons.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToMap;
import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToSet;
import static com.commercetools.sync.commons.utils.CollectionUtils.emptyIfNull;
import static com.commercetools.sync.commons.utils.CollectionUtils.filterCollection;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class CollectionUtilsTest {

    @Test
    public void filterCollection_emptyCases() throws Exception {
        assertThat(filterCollection(null, i -> true)).isEmpty();
        assertThat(filterCollection(Collections.emptyList(), i -> true)).isEmpty();
    }

    @Test
    public void filterCollection_normalCases() throws Exception {
        List<String> abcd = asList("a", "b", "c", "d");
        assertThat(filterCollection(abcd, s -> true)).containsExactlyInAnyOrder("a", "b", "c", "d");
        assertThat(filterCollection(abcd, s -> false)).isEmpty();
        assertThat(filterCollection(abcd, s -> s.charAt(0) > 'b')).containsExactlyInAnyOrder("c", "d");
    }

    @Test
    public void filterCollection_duplicates() throws Exception {
        List<String> abcd = asList("a", "b", "c", "d", "d", "c", "g", "a");
        assertThat(filterCollection(abcd, s -> true)).containsExactlyInAnyOrder("a", "a", "b", "c", "c", "d", "d", "g");
        assertThat(filterCollection(abcd, s -> false)).isEmpty();
        assertThat(filterCollection(abcd, s -> s.charAt(0) > 'b')).containsExactlyInAnyOrder("c", "c", "d", "d", "g");
    }

    @Test
    public void collectionToSet_emptyCases() throws Exception {
        assertThat(collectionToSet(null, i -> i)).isEmpty();
        assertThat(collectionToSet(Collections.emptyList(), i -> i)).isEmpty();
    }

    @Test
    public void collectionToSet_normalCases() throws Exception {
        List<Pair<String, Integer>> abcd = asList(
                Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4));
        assertThat(collectionToSet(abcd, Pair::getKey)).containsExactlyInAnyOrder("a", "b", "c", "d");
        assertThat(collectionToSet(abcd, Pair::getValue)).containsExactlyInAnyOrder(1, 2, 3, 4);
    }

    @Test
    public void collectionToSet_duplicates() throws Exception {
        List<Pair<String, Integer>> abcd = asList(
                Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4), Pair.of("d", 5), Pair.of("b", 6));
        // 2 duplicate keys should be suppressed
        assertThat(collectionToSet(abcd, Pair::getKey)).containsExactlyInAnyOrder("a", "b", "c", "d");
        // no duplicate values - nothing should be suppressed
        assertThat(collectionToSet(abcd, Pair::getValue)).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);
    }

    @Test
    public void collectionToMap_emptyCases() throws Exception {
        assertThat(collectionToMap(null, k -> k)).isEmpty();
        assertThat(collectionToMap(null, k -> k, v -> v)).isEmpty();
        assertThat(collectionToMap(Collections.emptyList(), k -> k)).isEmpty();
        assertThat(collectionToMap(Collections.emptyList(), k -> k, v -> v)).isEmpty();
    }

    @Test
    public void collectionToMap_normalCases() throws Exception {
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
    public void collectionToMap_duplicates() throws Exception {
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
    public void emptyIfNull_withListInstances_returnsNonNull() {
        List<String> nonNullEmptyList = emptyIfNull((List<String>)null);
        assertThat(nonNullEmptyList).isNotNull();
        assertThat(nonNullEmptyList).isEmpty();

        List<String> listToTest = asList("a", "b");
        assertThat(emptyIfNull(listToTest)).isSameAs(listToTest);
        assertThat(listToTest).containsExactly("a", "b"); // verify list is not mutated
    }

    @Test
    public void emptyIfNull_withMapInstances_returnsNonNull() {
        Map<String, String> nonNullEmptyMap = emptyIfNull((Map<String, String>)null);
        assertThat(nonNullEmptyMap).isNotNull();
        assertThat(nonNullEmptyMap).isEmpty();

        Map<String, Integer> mapToTest = new HashMap<>();
        mapToTest.put("X", 10);
        mapToTest.put("Y", 11);

        assertThat(emptyIfNull(mapToTest)).isSameAs(mapToTest);
        assertThat(mapToTest).containsExactly(entry("X", 10), entry("Y", 11)); // verify map is not mutated
    }
}