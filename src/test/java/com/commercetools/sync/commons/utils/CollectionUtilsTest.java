package com.commercetools.sync.commons.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToSet;
import static com.commercetools.sync.commons.utils.CollectionUtils.filterCollection;
import static org.assertj.core.api.Assertions.assertThat;

public class CollectionUtilsTest {
    @Test
    public void filterCollection_emptyCases() throws Exception {
        assertThat(filterCollection(null, i -> true)).isEmpty();
        assertThat(filterCollection(Collections.emptyList(), i -> true)).isEmpty();
    }

    @Test
    public void filterCollection_normalCases() throws Exception {
        List<String> abcd = Arrays.asList("a", "b", "c", "d");
        assertThat(filterCollection(abcd, s -> true)).containsExactlyInAnyOrder("a", "b", "c", "d");
        assertThat(filterCollection(abcd, s -> false)).isEmpty();
        assertThat(filterCollection(abcd, s -> s.charAt(0) > 'b')).containsExactlyInAnyOrder("c", "d");
    }

    @Test
    public void filterCollection_duplicates() throws Exception {
        List<String> abcd = Arrays.asList("a", "b", "c", "d", "d", "c", "g", "a");
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
        List<Pair<String, Integer>> abcd = Arrays.asList(
                Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4));
        assertThat(collectionToSet(abcd, Pair::getKey)).containsExactlyInAnyOrder("a", "b", "c", "d");
        assertThat(collectionToSet(abcd, Pair::getValue)).containsExactlyInAnyOrder(1, 2, 3, 4);
    }

    @Test
    public void collectionToSet_duplicates() throws Exception {
        List<Pair<String, Integer>> abcd = Arrays.asList(
                Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3), Pair.of("d", 4), Pair.of("d", 5), Pair.of("b", 6));
        // 2 duplicate keys should be suppressed
        assertThat(collectionToSet(abcd, Pair::getKey)).containsExactlyInAnyOrder("a", "b", "c", "d");
        // no duplicate values - nothing should be suppressed
        assertThat(collectionToSet(abcd, Pair::getValue)).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);
    }
}