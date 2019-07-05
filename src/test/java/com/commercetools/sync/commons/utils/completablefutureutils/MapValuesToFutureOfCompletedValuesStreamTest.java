package com.commercetools.sync.commons.utils.completablefutureutils;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class MapValuesToFutureOfCompletedValuesStreamTest {
    /**
     * List to stream : empty values.
     * Set to stream : empty values.
     **/

    @Test
    void empty_ListToStream_ReturnsFutureOfEmptyStream() {
        final CompletableFuture<Stream<String>> future = mapValuesToFutureOfCompletedValues(
            new ArrayList<String>(), CompletableFuture::completedFuture);
        assertThat(future.join().collect(toList())).isEqualTo(emptyList());
    }

    @Test
    void empty_SetToStream_ReturnsFutureOfEmptyStream() {
        final CompletableFuture<Stream<String>> future = mapValuesToFutureOfCompletedValues(
            new HashSet<String>(), CompletableFuture::completedFuture);
        assertThat(future.join().collect(toList())).isEqualTo(emptyList());
    }


    /**
     * List to Stream : single null value.
     * Set to Stream : single null value.
     */

    @Test
    void singleNull_ListToStream_ReturnsFutureOfEmptyStream() {
        final String nullString = null;
        final CompletableFuture<Stream<String>> future = mapValuesToFutureOfCompletedValues(
            singletonList(nullString), CompletableFuture::completedFuture);
        assertThat(future.join().collect(toList())).isEqualTo(emptyList());
    }

    @Test
    void singleNull_SetToStream_ReturnsFutureOfEmptyStream() {
        final String nullString = null;
        final CompletableFuture<Stream<String>> future = mapValuesToFutureOfCompletedValues(
            singleton(nullString), CompletableFuture::completedFuture);
        assertThat(future.join().collect(toList())).isEqualTo(emptyList());
    }

    /**
     * List to Stream : multiple null value.
     */

    @Test
    void multipleNull_ListToStream_ReturnsFutureOfEmptyStream() {
        final String nullString = null;
        final CompletableFuture<Stream<String>> future = mapValuesToFutureOfCompletedValues(
            asList(nullString, nullString), CompletableFuture::completedFuture);
        assertThat(future.join().collect(toList())).isEqualTo(emptyList());
    }

    /**
     * * List to Stream : Same Type Mapping : Single Value.
     * * Set to Stream : Same Type Mapping : Single Value.
     * * List to Stream : Diff Type Mapping : Single Value.
     * * Set to Stream : Diff Type Mapping : Single Value.
     */

    @Test
    void single_ListWithSameTypeMapping_ReturnsFutureOfStreamOfMappedValue() {
        final CompletableFuture<Stream<String>> future = mapValuesToFutureOfCompletedValues(
            singletonList("foo"), element -> completedFuture(element.concat("POSTFIX")));
        final Stream<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX");
    }

    @Test
    void single_SetWithSameTypeMapping_ReturnsFutureOfStreamOfMappedValue() {
        final CompletableFuture<Stream<String>> future = mapValuesToFutureOfCompletedValues(
            singleton("foo"), element -> completedFuture(element.concat("POSTFIX")));
        final Stream<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX");
    }

    @Test
    void single_ListWithDiffTypeMapping_ReturnsFutureOfStreamOfMappedValue() {
        final CompletableFuture<Stream<Integer>> future = mapValuesToFutureOfCompletedValues(
            singletonList("foo"), element -> completedFuture(element.length()));
        final Stream<Integer> result = future.join();
        assertThat(result).containsExactly(3);
    }

    @Test
    void single_SetWithDiffTypeMapping_ReturnsFutureOfStreamOfMappedValue() {
        final CompletableFuture<Stream<Integer>> future = mapValuesToFutureOfCompletedValues(
            singleton("foo"), element -> completedFuture(element.length()));
        final Stream<Integer> result = future.join();
        assertThat(result).containsExactly(3);
    }


    /**
     * * List to Stream : Same Type Mapping : Multiple Values : no duplicates.
     * * Set to Stream : Same Type Mapping : Multiple Values : no duplicates.
     * * List to Stream : Diff Type Mapping : Multiple Values : no duplicates.
     * * Set to Stream : Diff Type Mapping : Multiple Values : no duplicates.
     */

    @Test
    void multiple_ListWithSameTypeMappingNoDuplicates_ReturnsFutureOfStreamOfMappedValues() {
        final CompletableFuture<Stream<String>> future = mapValuesToFutureOfCompletedValues(
            asList("foo", "bar"), element -> completedFuture(element.concat("POSTFIX")));
        final Stream<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX", "barPOSTFIX");
    }

    @Test
    void multiple_SetWithSameTypeMappingNoDuplicates_ReturnsFutureOfStreamOfMappedValues() {
        final Set<String> set = new HashSet<>();
        set.add("foo");
        set.add("bar");

        final CompletableFuture<Stream<String>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture(element.concat("POSTFIX")));
        final Stream<String> result = future.join();
        assertThat(result).containsExactlyInAnyOrder("fooPOSTFIX", "barPOSTFIX");
    }

    @Test
    void multiple_ListWithDiffTypeMappingNoDuplicates_ReturnsFutureOfStreamOfMappedValues() {
        final CompletableFuture<Stream<Integer>> future = mapValuesToFutureOfCompletedValues(
            asList("john", "smith"), element -> completedFuture(element.length()));
        final Stream<Integer> result = future.join();
        assertThat(result).containsExactly(4, 5);
    }

    @Test
    void multiple_SetWithDiffTypeMappingNoDuplicates_ReturnsFutureOfStreamOfMappedValues() {
        final Set<String> set = new HashSet<>();
        set.add("john");
        set.add("smith");

        final CompletableFuture<Stream<Integer>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture(element.length()));
        final Stream<Integer> result = future.join();
        assertThat(result).containsExactlyInAnyOrder(4, 5);
    }

    /**
     * * List to Stream : Same Type Mapping : Multiple Values : duplicates.
     * * Set to Stream : Same Type Mapping : Multiple Values : duplicates.
     * * List to Stream : Diff Type Mapping : Multiple Values : duplicates.
     * * Set to Stream : Diff Type Mapping : Multiple Values : duplicates.
     */

    @Test
    void multiple_ListWithSameTypeMappingDuplicates_ReturnsFutureOfStreamOfMappedValuesWithDuplicates() {
        final CompletableFuture<Stream<String>> future = mapValuesToFutureOfCompletedValues(
            asList("foo", "foo"), element -> completedFuture(element.concat("POSTFIX")));
        final Stream<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX", "fooPOSTFIX");
    }

    @Test
    void multiple_SetWithSameTypeMappingDuplicates_ReturnsFutureOfStreamOfMappedValuesWithDuplicates() {
        final Set<String> set = new HashSet<>();
        set.add("foo");
        set.add("bar");

        final CompletableFuture<Stream<String>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture("constantResult"));
        final Stream<String> result = future.join();
        assertThat(result).containsExactly("constantResult", "constantResult");
    }

    @Test
    void multiple_ListWithDiffTypeMappingDuplicates_ReturnsFutureOfStreamOfMappedValuesWithDuplicates() {
        final CompletableFuture<Stream<Integer>> future = mapValuesToFutureOfCompletedValues(
            asList("john", "john"), element -> completedFuture(element.length()));
        final Stream<Integer> result = future.join();
        assertThat(result).containsExactly(4, 4);
    }

    @Test
    void multiple_SetWithDiffTypeMappingDuplicates_ReturnsFutureOfStreamOfMappedValuesWithDuplicates() {
        final Set<String> set = new HashSet<>();
        set.add("foo");
        set.add("bar");

        final CompletableFuture<Stream<Integer>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture(element.length()));
        final Stream<Integer> result = future.join();
        assertThat(result).containsExactly(3, 3);
    }
}
