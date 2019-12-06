package com.commercetools.sync.commons.utils.completablefutureutils;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

class MapStreamValuesToFutureOfCompletedValuesTest {
    /**
     * Stream to List : empty values.
     * Stream to Set : empty values.
     **/

    @Test
    void empty_StreamToList_ReturnsFutureOfEmptyList() {
        final CompletableFuture<List<String>> futureList = mapValuesToFutureOfCompletedValues(
            Stream.<String>empty(), CompletableFuture::completedFuture, toList());
        assertThat(futureList.join()).isEqualTo(emptyList());
    }

    @Test
    void empty_StreamToSet_ReturnsFutureOfEmptySet() {
        final CompletableFuture<Set<String>> future = mapValuesToFutureOfCompletedValues(Stream.<String>empty(),
            CompletableFuture::completedFuture, toSet());
        assertThat(future.join()).isEqualTo(emptySet());
    }


    /**
     * Stream to List : single null value.
     * Stream to Set : single null value.
     */

    @Test
    void singleNull_StreamToList_ReturnsFutureOfEmptyList() {
        final String nullString = null;
        final CompletableFuture<List<String>> futureList = mapValuesToFutureOfCompletedValues(
            Stream.of(nullString), CompletableFuture::completedFuture, toList());
        assertThat(futureList.join()).isEqualTo(emptyList());
    }

    @Test
    void singleNull_StreamToSet_ReturnsFutureOfEmptySet() {
        final String nullString = null;
        final CompletableFuture<Set<String>> futureSet = mapValuesToFutureOfCompletedValues(
            Stream.of(nullString), CompletableFuture::completedFuture, toSet());
        assertThat(futureSet.join()).isEqualTo(emptySet());
    }

    /**
     * Stream to List : multiple null value.
     * Stream to Set : multiple null value.
     */

    @Test
    void multipleNull_StreamToList_ReturnsFutureOfEmptyList() {
        final String nullString = null;
        final CompletableFuture<List<String>> futureList = mapValuesToFutureOfCompletedValues(
            Stream.of(nullString, nullString), CompletableFuture::completedFuture, toList());
        assertThat(futureList.join()).isEqualTo(emptyList());
    }

    @Test
    void multipleNull_StreamToSet_ReturnsFutureOfEmptySet() {
        final String nullString = null;
        final CompletableFuture<Set<String>> futureSet = mapValuesToFutureOfCompletedValues(
            Stream.of(nullString, nullString), CompletableFuture::completedFuture, toSet());
        assertThat(futureSet.join()).isEqualTo(emptySet());
    }

    /**
     * * Stream to List : Same Type Mapping : Single Value.
     * * Stream to Set : Same Type Mapping : Single Value.
     * * Stream to List : Diff Type Mapping : Single Value.
     * * Stream to Set : Diff Type Mapping : Single Value.
     */

    @Test
    void single_StreamToListWithSameTypeMapping_ReturnsFutureOfListOfMappedValue() {
        final CompletableFuture<List<String>> future = mapValuesToFutureOfCompletedValues(
            Stream.of("foo"), element -> completedFuture(element.concat("POSTFIX")), toList());
        final List<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    void single_StreamToSetWithSameTypeMapping_ReturnsFutureOfSetOfMappedValue() {
        final CompletableFuture<Set<String>> future = mapValuesToFutureOfCompletedValues(
            Stream.of("foo"), element -> completedFuture(element.concat("POSTFIX")), toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    void single_StreamToListWithDiffTypeMapping_ReturnsFutureOfListOfMappedValue() {
        final CompletableFuture<List<Integer>> future = mapValuesToFutureOfCompletedValues(
            Stream.of("foo"), element -> completedFuture(element.length()), toList());
        final List<Integer> result = future.join();
        assertThat(result).containsExactly(3);
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    void single_StreamToSetWithDiffTypeMapping_ReturnsFutureOfSetOfMappedValue() {
        final CompletableFuture<Set<Integer>> future = mapValuesToFutureOfCompletedValues(
            Stream.of("foo"), element -> completedFuture(element.length()), toSet());
        final Set<Integer> result = future.join();
        assertThat(result).containsExactly(3);
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    /**
     * * Stream to List : Same Type Mapping : Multiple Values : no duplicates.
     * * Stream to Set : Same Type Mapping : Multiple Values : no duplicates.
     * * Stream to List : Diff Type Mapping : Multiple Values : no duplicates.
     * * Stream to Set : Diff Type Mapping : Multiple Values : no duplicates.
     */

    @Test
    void multiple_StreamToListWithSameTypeMappingNoDuplicates_ReturnsFutureOfListOfMappedValues() {
        final CompletableFuture<List<String>> future = mapValuesToFutureOfCompletedValues(
            Stream.of("foo", "bar"), element -> completedFuture(element.concat("POSTFIX")), toList());
        final List<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX", "barPOSTFIX");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    void multiple_StreamToSetWithSameTypeMappingNoDuplicates_ReturnsFutureOfSetOfMappedValues() {
        final CompletableFuture<Set<String>> future = mapValuesToFutureOfCompletedValues(Stream.of("foo", "bar"),
            element -> completedFuture(element.concat("POSTFIX")), toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactlyInAnyOrder("fooPOSTFIX", "barPOSTFIX");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    void multiple_StreamToListWithDiffTypeMappingNoDuplicates_ReturnsFutureOfListOfMappedValues() {
        final CompletableFuture<List<Integer>> future = mapValuesToFutureOfCompletedValues(
            Stream.of("john", "smith"), element -> completedFuture(element.length()), toList());
        final List<Integer> result = future.join();
        assertThat(result).containsExactly(4, 5);
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    void multiple_StreamToSetWithDiffTypeMappingNoDuplicates_ReturnsFutureOfSetOfMappedValues() {
        final CompletableFuture<Set<Integer>> future = mapValuesToFutureOfCompletedValues(Stream.of("john", "smith"),
            element -> completedFuture(element.length()), toSet());
        final Set<Integer> result = future.join();
        assertThat(result).containsExactlyInAnyOrder(4, 5);
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    /**
     * * Stream to List : Same Type Mapping : Multiple Values : duplicates.
     * * Stream to Set : Same Type Mapping : Multiple Values : duplicates.
     * * Stream to List : Diff Type Mapping : Multiple Values : duplicates.
     * * Stream to Set : Diff Type Mapping : Multiple Values : duplicates.
     */

    @Test
    void multiple_StreamToListWithSameTypeMappingDuplicates_ReturnsFutureOfListOfMappedValuesWithDuplicates() {
        final CompletableFuture<List<String>> future = mapValuesToFutureOfCompletedValues(
            Stream.of("foo", "foo"), element -> completedFuture(element.concat("POSTFIX")), toList());
        final List<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX", "fooPOSTFIX");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    void multiple_StreamToSetWithSameTypeMappingDuplicates_ReturnsFutureOfSetOfMappedValuesNoDuplicates() {
        final CompletableFuture<Set<String>> future = mapValuesToFutureOfCompletedValues(Stream.of("foo", "foo"),
            element -> completedFuture(element.concat("POSTFIX")), toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    void multiple_StreamToListWithDiffTypeMappingDuplicates_ReturnsFutureOfListOfMappedValuesWithDuplicates() {
        final CompletableFuture<List<Integer>> future = mapValuesToFutureOfCompletedValues(
            Stream.of("john", "john"), element -> completedFuture(element.length()), toList());
        final List<Integer> result = future.join();
        assertThat(result).containsExactly(4, 4);
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    void multiple_StreamToSetWithDiffTypeMappingDuplicates_ReturnsFutureOfSetOfMappedValuesNoDuplicates() {
        final CompletableFuture<Set<Integer>> future = mapValuesToFutureOfCompletedValues(Stream.of("john", "john"),
            element -> completedFuture(element.length()), toSet());
        final Set<Integer> result = future.join();
        assertThat(result).containsExactly(4);
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }
}
