package com.commercetools.sync.commons.utils.completablefutureutils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class MapValuesToFutureOfCompletedValuesTest {
    /**
     * List to List : empty values
     * List to Set : empty values
     * Set to Set : empty values
     * Set to List : empty values
     **/

    @Test
    public void empty_ListToList_ReturnsFutureOfEmptyList() {
        final CompletableFuture<List<String>> futureList = mapValuesToFutureOfCompletedValues(
            new ArrayList<String>(), CompletableFuture::completedFuture, toList());
        assertThat(futureList.join()).isEqualTo(emptyList());
    }

    @Test
    public void empty_ListToSet_ReturnsFutureOfEmptySet() {
        final CompletableFuture<Set<String>> futureSet = mapValuesToFutureOfCompletedValues(
            new ArrayList<String>(), CompletableFuture::completedFuture, toSet());
        assertThat(futureSet.join()).isEqualTo(emptySet());
    }

    @Test
    public void empty_SetToSet_ReturnsFutureOfEmptyList() {
        final CompletableFuture<List<String>> futureList = mapValuesToFutureOfCompletedValues(
            new HashSet<String>(), CompletableFuture::completedFuture, toList());
        assertThat(futureList.join()).isEqualTo(emptyList());
    }

    @Test
    public void empty_SetToList_ReturnsFutureOfEmptySet() {
        final CompletableFuture<Set<String>> futureSet = mapValuesToFutureOfCompletedValues(
            new HashSet<String>(), CompletableFuture::completedFuture, toSet());
        assertThat(futureSet.join()).isEqualTo(emptySet());
    }


    /**
     * List to List : single null value
     * List to Set : single null value
     * Set to Set : single null value
     * Set to List : single null value
     */

    @Test
    public void singleNull_ListToList_ReturnsFutureOfEmptyList() {
        final String nullString = null;
        final CompletableFuture<List<String>> futureList = mapValuesToFutureOfCompletedValues(
            singletonList(nullString), CompletableFuture::completedFuture, toList());
        assertThat(futureList.join()).isEqualTo(emptyList());
    }

    @Test
    public void singleNull_ListToSet_ReturnsFutureOfEmptySet() {
        final String nullString = null;
        final CompletableFuture<Set<String>> futureSet = mapValuesToFutureOfCompletedValues(
            singletonList(nullString), CompletableFuture::completedFuture, toSet());
        assertThat(futureSet.join()).isEqualTo(emptySet());
    }

    @Test
    public void singleNull_SetToSet_ReturnsFutureOfEmptyList() {
        final String nullString = null;
        final CompletableFuture<List<String>> futureList = mapValuesToFutureOfCompletedValues(
            singleton(nullString), CompletableFuture::completedFuture, toList());
        assertThat(futureList.join()).isEqualTo(emptyList());
    }

    @Test
    public void singleNull_SetToList_ReturnsFutureOfEmptySet() {
        final String nullString = null;
        final CompletableFuture<Set<String>> futureSet = mapValuesToFutureOfCompletedValues(
            singleton(nullString), CompletableFuture::completedFuture, toSet());
        assertThat(futureSet.join()).isEqualTo(emptySet());
    }

    /**
     * List to List : multiple null value
     * List to Set : multiple null value
     */

    @Test
    public void multipleNull_ListToList_ReturnsFutureOfEmptyList() {
        final String nullString = null;
        final CompletableFuture<List<String>> futureList = mapValuesToFutureOfCompletedValues(
            asList(nullString, nullString), CompletableFuture::completedFuture, toList());
        assertThat(futureList.join()).isEqualTo(emptyList());
    }

    @Test
    public void multipleNull_ListToSet_ReturnsFutureOfEmptySet() {
        final String nullString = null;
        final CompletableFuture<Set<String>> futureSet = mapValuesToFutureOfCompletedValues(
            asList(nullString, nullString), CompletableFuture::completedFuture, toSet());
        assertThat(futureSet.join()).isEqualTo(emptySet());
    }

    /**
     * * List to List : Same Type Mapping : Single Value
     * * List to Set : Same Type Mapping : Single Value
     * * Set to Set : Same Type Mapping : Single Value
     * * Set to List : Same Type Mapping : Single Value
     *
     * * List to List : Diff Type Mapping : Single Value
     * * List to Set : Diff Type Mapping : Single Value
     * * Set to Set : Diff Type Mapping : Single Value
     * * Set to List : Diff Type Mapping : Single Value
     */

    @Test
    public void single_ListToListWithSameTypeMapping_ReturnsFutureOfListOfMappedValue() {
        final CompletableFuture<List<String>> future = mapValuesToFutureOfCompletedValues(
            singletonList("foo"), element -> completedFuture(element.concat("POSTFIX")), toList());
        final List<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    public void single_ListToSetWithSameTypeMapping_ReturnsFutureOfSetOfMappedValue() {
        final CompletableFuture<Set<String>> future = mapValuesToFutureOfCompletedValues(
            singletonList("foo"), element -> completedFuture(element.concat("POSTFIX")), toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void single_SetToSetWithSameTypeMapping_ReturnsFutureOfSetOfMappedValue() {
        final CompletableFuture<Set<String>> future = mapValuesToFutureOfCompletedValues(
            singleton("foo"), element -> completedFuture(element.concat("POSTFIX")), toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void single_SetToListWithSameTypeMapping_ReturnsFutureOfListOfMappedValue() {
        final CompletableFuture<List<String>> future = mapValuesToFutureOfCompletedValues(
            singleton("foo"), element -> completedFuture(element.concat("POSTFIX")), toList());
        final List<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    public void single_ListToListWithDiffTypeMapping_ReturnsFutureOfListOfMappedValue() {
        final CompletableFuture<List<Integer>> future = mapValuesToFutureOfCompletedValues(
            singletonList("foo"), element -> completedFuture(element.length()), toList());
        final List<Integer> result = future.join();
        assertThat(result).containsExactly(3);
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    public void single_ListToSetWithDiffTypeMapping_ReturnsFutureOfSetOfMappedValue() {
        final CompletableFuture<Set<Integer>> future = mapValuesToFutureOfCompletedValues(
            singletonList("foo"), element -> completedFuture(element.length()), toSet());
        final Set<Integer> result = future.join();
        assertThat(result).containsExactly(3);
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void single_SetToSetWithDiffTypeMapping_ReturnsFutureOfSetOfMappedValue() {
        final CompletableFuture<Set<Integer>> future = mapValuesToFutureOfCompletedValues(
            singleton("foo"), element -> completedFuture(element.length()), toSet());
        final Set<Integer> result = future.join();
        assertThat(result).containsExactly(3);
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void single_SetToListWithDiffTypeMapping_ReturnsFutureOfListOfMappedValue() {
        final CompletableFuture<List<Integer>> future = mapValuesToFutureOfCompletedValues(
            singleton("foo"), element -> completedFuture(element.length()), toList());
        final List<Integer> result = future.join();
        assertThat(result).containsExactly(3);
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    /**
     * * List to List : Same Type Mapping : Multiple Values : no duplicates
     * * List to Set : Same Type Mapping : Multiple Values : no duplicates
     * * Set to Set : Same Type Mapping : Multiple Values : no duplicates
     * * Set to List : Same Type Mapping : Multiple Values : no duplicates
     *
     * * List to List : Diff Type Mapping : Multiple Values : no duplicates
     * * List to Set : Diff Type Mapping : Multiple Values : no duplicates
     * * Set to Set : Diff Type Mapping : Multiple Values : no duplicates
     * * Set to List : Diff Type Mapping : Multiple Values : no duplicates
     */

    @Test
    public void multiple_ListToListWithSameTypeMappingNoDuplicates_ReturnsFutureOfListOfMappedValues() {
        final CompletableFuture<List<String>> future = mapValuesToFutureOfCompletedValues(
            asList("foo", "bar"), element -> completedFuture(element.concat("POSTFIX")), toList());
        final List<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX", "barPOSTFIX");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    public void multiple_ListToSetWithSameTypeMappingNoDuplicates_ReturnsFutureOfSetOfMappedValues() {
        final CompletableFuture<Set<String>> future = mapValuesToFutureOfCompletedValues(asList("foo", "bar"),
            element -> completedFuture(element.concat("POSTFIX")), toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactlyInAnyOrder("fooPOSTFIX", "barPOSTFIX");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToSetWithSameTypeMappingNoDuplicates_ReturnsFutureOfSetOfMappedValues() {
        final Set<String> set = new HashSet<>();
        set.add("foo");
        set.add("bar");

        final CompletableFuture<Set<String>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture(element.concat("POSTFIX")), toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactlyInAnyOrder("fooPOSTFIX", "barPOSTFIX");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToListWithSameTypeMappingNoDuplicates_ReturnsFutureOfListOfMappedValues() {
        final Set<String> set = new HashSet<>();
        set.add("foo");
        set.add("bar");

        final CompletableFuture<List<String>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture(element.concat("POSTFIX")), toList());
        final List<String> result = future.join();
        assertThat(result).containsExactlyInAnyOrder("fooPOSTFIX", "barPOSTFIX");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    public void multiple_ListToListWithDiffTypeMappingNoDuplicates_ReturnsFutureOfListOfMappedValues() {
        final CompletableFuture<List<Integer>> future = mapValuesToFutureOfCompletedValues(
            asList("john", "smith"), element -> completedFuture(element.length()), toList());
        final List<Integer> result = future.join();
        assertThat(result).containsExactly(4, 5);
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    public void multiple_ListToSetWithDiffTypeMappingNoDuplicates_ReturnsFutureOfSetOfMappedValues() {
        final CompletableFuture<Set<Integer>> future = mapValuesToFutureOfCompletedValues(asList("john", "smith"),
            element -> completedFuture(element.length()), toSet());
        final Set<Integer> result = future.join();
        assertThat(result).containsExactlyInAnyOrder(4, 5);
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToSetWithDiffTypeMappingNoDuplicates_ReturnsFutureOfSetOfMappedValues() {
        final Set<String> set = new HashSet<>();
        set.add("john");
        set.add("smith");

        final CompletableFuture<Set<Integer>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture(element.length()), toSet());
        final Set<Integer> result = future.join();
        assertThat(result).containsExactlyInAnyOrder(4, 5);
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToListWithDiffTypeMappingNoDuplicates_ReturnsFutureOfListOfMappedValues() {
        final Set<String> set = new HashSet<>();
        set.add("john");
        set.add("smith");

        final CompletableFuture<List<Integer>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture(element.length()), toList());
        final List<Integer> result = future.join();
        assertThat(result).containsExactlyInAnyOrder(4, 5);
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    /**
     * * List to List : Same Type Mapping : Multiple Values : duplicates
     * * List to Set : Same Type Mapping : Multiple Values : duplicates
     * * Set to Set : Same Type Mapping : Multiple Values : duplicates
     * * Set to List : Same Type Mapping : Multiple Values : duplicates
     *
     * * List to List : Diff Type Mapping : Multiple Values : duplicates
     * * List to Set : Diff Type Mapping : Multiple Values : duplicates
     * * Set to Set : Diff Type Mapping : Multiple Values : duplicates
     * * Set to List : Diff Type Mapping : Multiple Values : duplicates
     */

    @Test
    public void multiple_ListToListWithSameTypeMappingDuplicates_ReturnsFutureOfListOfMappedValuesWithDuplicates() {
        final CompletableFuture<List<String>> future = mapValuesToFutureOfCompletedValues(
            asList("foo", "foo"), element -> completedFuture(element.concat("POSTFIX")), toList());
        final List<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX", "fooPOSTFIX");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    public void multiple_ListToSetWithSameTypeMappingDuplicates_ReturnsFutureOfSetOfMappedValuesNoDuplicates() {
        final CompletableFuture<Set<String>> future = mapValuesToFutureOfCompletedValues(asList("foo", "foo"),
            element -> completedFuture(element.concat("POSTFIX")), toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactly("fooPOSTFIX");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToSetWithSameTypeMappingDuplicates_ReturnsFutureOfSetOfMappedValuesNoDuplicates() {
        final Set<String> set = new HashSet<>();
        set.add("foo");
        set.add("bar");

        final CompletableFuture<Set<String>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture("constantResult"), toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactly("constantResult");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToListWithSameTypeMappingDuplicates_ReturnsFutureOfListOfMappedValuesWithDuplicates() {
        final Set<String> set = new HashSet<>();
        set.add("foo");
        set.add("bar");

        final CompletableFuture<List<String>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture("constantResult"), toList());
        final List<String> result = future.join();
        assertThat(result).containsExactly("constantResult", "constantResult");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    public void multiple_ListToListWithDiffTypeMappingDuplicates_ReturnsFutureOfListOfMappedValuesWithDuplicates() {
        final CompletableFuture<List<Integer>> future = mapValuesToFutureOfCompletedValues(
            asList("john", "john"), element -> completedFuture(element.length()), toList());
        final List<Integer> result = future.join();
        assertThat(result).containsExactly(4, 4);
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    public void multiple_ListToSetWithDiffTypeMappingDuplicates_ReturnsFutureOfSetOfMappedValuesNoDuplicates() {
        final CompletableFuture<Set<Integer>> future = mapValuesToFutureOfCompletedValues(asList("john", "john"),
            element -> completedFuture(element.length()), toSet());
        final Set<Integer> result = future.join();
        assertThat(result).containsExactly(4);
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToSetWithDiffTypeMappingDuplicates_ReturnsFutureOfSetOfMappedValuesNoDuplicates() {
        final Set<String> set = new HashSet<>();
        set.add("foo");
        set.add("bar");

        final CompletableFuture<Set<Integer>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture(element.length()), toSet());
        final Set<Integer> result = future.join();
        assertThat(result).containsExactly(3);
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToListWithDiffTypeMappingDuplicates_ReturnsFutureOfListOfMappedValuesWithDuplicates() {
        final Set<String> set = new HashSet<>();
        set.add("foo");
        set.add("bar");

        final CompletableFuture<List<Integer>> future = mapValuesToFutureOfCompletedValues(set,
            element -> completedFuture(element.length()), toList());
        final List<Integer> result = future.join();
        assertThat(result).containsExactly(3, 3);
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }
}
