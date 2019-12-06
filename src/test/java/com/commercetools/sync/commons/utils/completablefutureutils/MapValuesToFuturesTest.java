package com.commercetools.sync.commons.utils.completablefutureutils;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutures;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

class MapValuesToFuturesTest {
    /**
     * Stream to List : empty values.
     * Stream to Set : empty values.
     **/

    @Test
    void empty_StreamToList_ReturnsEmptyList() {
        final List<CompletableFuture<String>> futures = mapValuesToFutures(Stream.<String>empty(),
            CompletableFuture::completedFuture, toList());
        assertThat(futures).isEmpty();
    }

    @Test
    void empty_StreamToSet_ReturnsEmptySet() {
        final Set<CompletableFuture<String>> futures = mapValuesToFutures(Stream.<String>empty(),
            CompletableFuture::completedFuture, toSet());
        assertThat(futures).isEmpty();
    }


    /**
     * Stream to List : single null value.
     * Stream to Set : single null value.
     */

    @Test
    void singleNull_StreamToList_ReturnsEmptyList() {
        final String nullString = null;
        final List<CompletableFuture<String>> futures = mapValuesToFutures(Stream.of(nullString),
            CompletableFuture::completedFuture, toList());
        assertThat(futures).isEmpty();
    }

    @Test
    void singleNull_StreamToSet_ReturnsEmptySet() {
        final String nullString = null;
        final Set<CompletableFuture<String>> futures = mapValuesToFutures(Stream.of(nullString),
            CompletableFuture::completedFuture, toSet());
        assertThat(futures).isEmpty();
    }

    /**
     * Stream to List : multiple null value.
     * Stream to Set : multiple null value.
     */
    @Test
    void multipleNull_StreamToList_ReturnsEmptyList() {
        final String nullString = null;
        final List<CompletableFuture<String>> futures = mapValuesToFutures(Stream.of(nullString, nullString),
            CompletableFuture::completedFuture, toList());
        assertThat(futures).isEmpty();
    }

    @Test
    void multipleNull_StreamToSet_ReturnsEmptySet() {
        final String nullString = null;
        final Set<CompletableFuture<String>> futures = mapValuesToFutures(Stream.of(nullString, nullString),
            CompletableFuture::completedFuture, toSet());
        assertThat(futures).isEmpty();
    }

    /**
     * * Stream to List : Same Type Mapping : Single Value.
     * * Stream to Set : Same Type Mapping : Single Value.
     * * Stream to List : Diff Type Mapping : Single Value.
     * * Stream to Set : Diff Type Mapping : Single Value.
     */

    @Test
    void single_StreamToListWithSameTypeMapping_ReturnsListOfMappedValue() {
        final List<CompletableFuture<String>> futures = mapValuesToFutures(Stream.of("foo"),
            element -> completedFuture(element.concat("POSTFIX")), toList());

        assertThat(futures).hasSize(1);
        assertThat(futures).isExactlyInstanceOf(ArrayList.class);
        assertThat(futures.get(0).join()).isEqualTo("fooPOSTFIX");
    }

    @Test
    void single_StreamToSetWithSameTypeMapping_ReturnsSetOfMappedValue() {
        final Set<CompletableFuture<String>> futures = mapValuesToFutures(Stream.of("foo"),
            element -> completedFuture(element.concat("POSTFIX")), toSet());

        assertThat(futures).hasSize(1);
        assertThat(futures).isExactlyInstanceOf(HashSet.class);
        assertThat(futures.iterator().next().join()).isEqualTo("fooPOSTFIX");
    }

    @Test
    void single_StreamToListWithDiffTypeMapping_ReturnsListOfMappedValue() {
        final List<CompletableFuture<Integer>> futures = mapValuesToFutures(Stream.of("foo"),
            element -> completedFuture(element.length()), toList());

        assertThat(futures).hasSize(1);
        assertThat(futures).isExactlyInstanceOf(ArrayList.class);
        assertThat(futures.iterator().next().join()).isEqualTo(3);
    }

    @Test
    void single_StreamToSetWithDiffTypeMapping_ReturnsSetOfMappedValue() {
        final Set<CompletableFuture<Integer>> futures = mapValuesToFutures(Stream.of("foo"),
            element -> completedFuture(element.length()), toSet());

        assertThat(futures).hasSize(1);
        assertThat(futures).isExactlyInstanceOf(HashSet.class);
        assertThat(futures.iterator().next().join()).isEqualTo(3);
    }

    /**
     * * Stream to List : Same Type Mapping : Multiple Values : no duplicates.
     * * Stream to Set : Same Type Mapping : Multiple Values : no duplicates.
     * * Stream to List : Diff Type Mapping : Multiple Values : no duplicates.
     * * Stream to Set : Diff Type Mapping : Multiple Values : no duplicates.
     */

    @Test
    void multiple_StreamToListWithSameTypeMappingNoDuplicates_ReturnsListOfMappedValues() {
        final List<CompletableFuture<String>> futures = mapValuesToFutures(Stream.of("foo", "bar"),
            element -> completedFuture(element.concat("POSTFIX")), toList());

        assertThat(futures).hasSize(2);
        assertThat(futures).isExactlyInstanceOf(ArrayList.class);

        final List<String> results = futures.stream()
                                            .map(CompletableFuture::join).collect(toList());

        assertThat(results).containsExactly("fooPOSTFIX", "barPOSTFIX");
    }

    @Test
    void multiple_StreamToSetWithSameTypeMappingNoDuplicates_ReturnsSetOfMappedValues() {
        final Set<CompletableFuture<String>> futures = mapValuesToFutures(Stream.of("foo", "bar"),
            element -> completedFuture(element.concat("POSTFIX")), toSet());

        assertThat(futures).hasSize(2);
        assertThat(futures).isExactlyInstanceOf(HashSet.class);

        final List<String> results = futures.stream()
                                             .map(CompletableFuture::join)
                                             .collect(toList());

        assertThat(results).containsExactlyInAnyOrder("fooPOSTFIX", "barPOSTFIX");

    }

    @Test
    void multiple_StreamToListWithDiffTypeMappingNoDuplicates_ReturnsListOfMappedValues() {
        final List<CompletableFuture<Integer>> futures = mapValuesToFutures(Stream.of("john", "smith"),
            element -> completedFuture(element.length()), toList());

        assertThat(futures).hasSize(2);
        assertThat(futures).isExactlyInstanceOf(ArrayList.class);

        final List<Integer> results = futures.stream()
                                             .map(CompletableFuture::join)
                                             .collect(toList());


        assertThat(results).containsExactly(4, 5);
    }

    @Test
    void multiple_StreamToSetWithDiffTypeMappingNoDuplicates_ReturnsSetOfMappedValues() {
        final Set<CompletableFuture<Integer>> futures = mapValuesToFutures(Stream.of("john", "smith"),
            element -> completedFuture(element.length()), toSet());

        assertThat(futures).hasSize(2);
        assertThat(futures).isExactlyInstanceOf(HashSet.class);

        final List<Integer> results = futures.stream()
                                            .map(CompletableFuture::join)
                                            .collect(toList());


        assertThat(results).containsExactlyInAnyOrder(4, 5);

    }

    /**
     * * Stream to List : Same Type Mapping : Multiple Values : duplicates.
     * * Stream to Set : Same Type Mapping : Multiple Values : duplicates.
     * * Stream to List : Diff Type Mapping : Multiple Values : duplicates.
     * * Stream to Set : Diff Type Mapping : Multiple Values : duplicates.
     */

    @Test
    void multiple_StreamToListWithSameTypeMappingDuplicates_ReturnsListOfMappedValuesWithDuplicates() {
        final List<CompletableFuture<String>> futures = mapValuesToFutures(Stream.of("john", "john"),
            CompletableFuture::completedFuture, toList());

        assertThat(futures).hasSize(2);
        assertThat(futures).isExactlyInstanceOf(ArrayList.class);

        final List<String> results = futures.stream()
                                            .map(CompletableFuture::join)
                                            .collect(toList());


        assertThat(results).containsExactly("john", "john");
    }

    @Test
    void multiple_StreamToSetWithSameTypeMappingDuplicates_ReturnsSetOfMappedValuesDuplicates() {
        final Set<CompletableFuture<String>> futures = mapValuesToFutures(Stream.of("john", "john"),
            CompletableFuture::completedFuture, toSet());

        assertThat(futures).hasSize(2);
        assertThat(futures).isExactlyInstanceOf(HashSet.class);

        final List<String> results = futures.stream()
                                             .map(CompletableFuture::join)
                                             .collect(toList());


        assertThat(results).containsExactly("john", "john");
    }

    @Test
    void multiple_StreamToListWithDiffTypeMappingDuplicates_ReturnsListOfMappedValuesWithDuplicates() {
        final List<CompletableFuture<Integer>> futures = mapValuesToFutures(Stream.of("john", "john"),
            element -> completedFuture(element.length()), toList());

        assertThat(futures).hasSize(2);
        assertThat(futures).isExactlyInstanceOf(ArrayList.class);

        final List<Integer> results = futures.stream()
                                            .map(CompletableFuture::join)
                                            .collect(toList());


        assertThat(results).containsExactly(4, 4);
    }

    @Test
    void multiple_StreamToSetWithDiffTypeMappingDuplicates_ReturnsSetOfMappedValuesWithDuplicates() {
        final Set<CompletableFuture<Integer>> futures = mapValuesToFutures(Stream.of("john", "john"),
            element -> completedFuture(element.length()), toSet());

        assertThat(futures).hasSize(2);
        assertThat(futures).isExactlyInstanceOf(HashSet.class);

        final List<Integer> results = futures.stream()
                                             .map(CompletableFuture::join)
                                             .collect(toList());


        assertThat(results).containsExactly(4, 4);
    }
}
