package com.commercetools.sync.commons.utils.completablefutureutils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class CollectionOfFuturesToFutureOfCollectionTest {

    /**
     * List to List : empty values.
     * List to Set : empty values.
     * Set to Set : empty values.
     * Set to List : empty values.
     **/

    @Test
    public void empty_ListToList_ReturnsFutureOfEmptyList() {
        final CompletableFuture<List<String>> future = collectionOfFuturesToFutureOfCollection(
            new ArrayList<CompletableFuture<String>>(), toList());
        assertThat(future.join()).isEqualTo(emptyList());
    }

    @Test
    public void empty_ListToSet_ReturnsFutureOfEmptySet() {
        final CompletableFuture<Set<String>> future = collectionOfFuturesToFutureOfCollection(
            new ArrayList<CompletableFuture<String>>(), toSet());
        assertThat(future.join()).isEqualTo(emptySet());
    }

    @Test
    public void empty_SetToSet_ReturnsFutureOfEmptySet() {
        final CompletableFuture<Set<String>> future = collectionOfFuturesToFutureOfCollection(new HashSet<>(), toSet());
        assertThat(future.join()).isEqualTo(emptySet());
    }

    @Test
    public void empty_SetToList_ReturnsFutureOfEmptySet() {
        final CompletableFuture<List<String>> future =
            collectionOfFuturesToFutureOfCollection(new HashSet<>(), toList());
        assertThat(future.join()).isEqualTo(emptyList());
    }


    /**
     * List to List : single null value.
     * List to Set : single null value.
     * Set to Set : single null value.
     * Set to List : single null value.
     */

    @Test
    public void singleNull_ListToList_ReturnsFutureOfEmptyList() {
        final CompletableFuture<String> nullFuture = null;
        final CompletableFuture<List<String>> future =
            collectionOfFuturesToFutureOfCollection(singletonList(nullFuture), toList());
        assertThat(future.join()).isEqualTo(emptyList());
    }

    @Test
    public void singleNull_ListToSet_ReturnsFutureOfEmptySet() {
        final CompletableFuture<String> nullFuture = null;
        final CompletableFuture<Set<String>> future =
            collectionOfFuturesToFutureOfCollection(singletonList(nullFuture), toSet());
        assertThat(future.join()).isEqualTo(emptySet());
    }

    @Test
    public void singleNull_SetToSet_ReturnsFutureOfEmptySet() {
        final CompletableFuture<String> nullFuture = null;
        final CompletableFuture<Set<String>> future =
            collectionOfFuturesToFutureOfCollection(singleton(nullFuture), toSet());
        assertThat(future.join()).isEqualTo(emptySet());
    }

    @Test
    public void singleNull_SetToList_ReturnsFutureOfEmptyList() {
        final CompletableFuture<String> nullFuture = null;
        final CompletableFuture<List<String>> future =
            collectionOfFuturesToFutureOfCollection(singleton(nullFuture), toList());
        assertThat(future.join()).isEqualTo(emptyList());
    }

    /**
     * List to List : multiple null value.
     * List to Set : multiple null value.
     */

    @Test
    public void multipleNull_ListToList_ReturnsFutureOfEmptyList() {
        final CompletableFuture<String> nullFuture = null;
        final CompletableFuture<List<String>> future =
            collectionOfFuturesToFutureOfCollection(asList(nullFuture, nullFuture), toList());
        assertThat(future.join()).isEqualTo(emptyList());
    }

    @Test
    public void multipleNull_ListToSet_ReturnsFutureOfEmptySet() {
        final CompletableFuture<String> nullFuture = null;
        final CompletableFuture<Set<String>> future = collectionOfFuturesToFutureOfCollection(
            asList(nullFuture, nullFuture), toSet());
        assertThat(future.join()).isEqualTo(emptySet());
    }

    /**
     * * List to List : Single Value.
     * * List to Set : Single Value.
     * * Set to Set : Single Value.
     * * Set to List : Single Value.
     */

    @Test
    public void single_ListToList_ReturnsFutureOfList() {
        final String result = "value1";
        final CompletableFuture<List<String>> future =
            collectionOfFuturesToFutureOfCollection(singletonList(completedFuture(result)), toList());
        assertThat(future.join()).containsExactly(result);
    }

    @Test
    public void single_ListToSet_ReturnsFutureOfSet() {
        final String result = "value1";
        final CompletableFuture<Set<String>> future =
            collectionOfFuturesToFutureOfCollection(singletonList(completedFuture(result)), toSet());
        assertThat(future.join()).containsExactly(result);
    }

    @Test
    public void single_SetToSet_ReturnsFutureOfSet() {
        final String result = "value1";
        final CompletableFuture<Set<String>> future =
            collectionOfFuturesToFutureOfCollection(singleton(completedFuture(result)), toSet());
        assertThat(future.join()).containsExactly(result);
    }

    @Test
    public void single_SetToList_ReturnsFutureOfList() {
        final String result = "value1";
        final CompletableFuture<List<String>> future =
            collectionOfFuturesToFutureOfCollection(singleton(completedFuture(result)), toList());
        assertThat(future.join()).containsExactly(result);
    }

    /**
     * * List to List : Multiple Values : no duplicates.
     * * List to Set : Multiple Values : no duplicates.
     * * Set to Set : Multiple Values : no duplicates.
     * * Set to List : Multiple Values : no duplicates.
     */

    @Test
    public void multiple_ListToListNoDuplicates_ReturnsFutureOfListOfCompletedValues() {
        final CompletableFuture<List<String>> future =
            collectionOfFuturesToFutureOfCollection(asList(completedFuture("foo"), completedFuture("bar")), toList());
        final List<String> result = future.join();
        assertThat(result).containsExactly("foo", "bar");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    public void multiple_ListToSetNoDuplicates_ReturnsFutureOfSetOfCompletedValues() {
        final CompletableFuture<Set<String>> future =
            collectionOfFuturesToFutureOfCollection(asList(completedFuture("foo"), completedFuture("bar")), toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactlyInAnyOrder("foo", "bar");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToSetNoDuplicates_ReturnsFutureOfSetOfCompletedValues() {
        final Set<CompletableFuture<String>> set = new HashSet<>();
        set.add(completedFuture("foo"));
        set.add(completedFuture("bar"));

        final CompletableFuture<Set<String>> future = collectionOfFuturesToFutureOfCollection(set, toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactlyInAnyOrder("foo", "bar");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToListMappingNoDuplicates_ReturnsFutureOfListOfCompletedValues() {
        final Set<CompletableFuture<String>> set = new HashSet<>();
        set.add(completedFuture("foo"));
        set.add(completedFuture("bar"));

        final CompletableFuture<List<String>> future = collectionOfFuturesToFutureOfCollection(set, toList());
        final List<String> result = future.join();
        assertThat(result).containsExactlyInAnyOrder("foo", "bar");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    /**
     * * List to List : Multiple Values : duplicates.
     * * List to Set : Multiple Values : duplicates.
     * * Set to Set : Multiple Values : duplicates.
     * * Set to List : Multiple Values : duplicates.
     */

    @Test
    public void multiple_ListToListDuplicates_ReturnsFutureOfListOfCompletedValuesDuplicates() {
        final CompletableFuture<List<String>> future =
            collectionOfFuturesToFutureOfCollection(asList(completedFuture("foo"), completedFuture("foo")), toList());
        final List<String> result = future.join();
        assertThat(result).containsExactly("foo", "foo");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }

    @Test
    public void multiple_ListToSetDuplicates_ReturnsFutureOfSetOfCompletedValuesNoDuplicates() {
        final CompletableFuture<Set<String>> future =
            collectionOfFuturesToFutureOfCollection(asList(completedFuture("foo"), completedFuture("foo")), toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactly("foo");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToSetDuplicates_ReturnsFutureOfSetOfCompletedValuesNoDuplicates() {
        final Set<CompletableFuture<String>> set = new HashSet<>();
        set.add(completedFuture("foo"));
        set.add(completedFuture("foo"));

        final CompletableFuture<Set<String>> future = collectionOfFuturesToFutureOfCollection(set, toSet());
        final Set<String> result = future.join();
        assertThat(result).containsExactly("foo");
        assertThat(result).isExactlyInstanceOf(HashSet.class);
    }

    @Test
    public void multiple_SetToListDuplicates_ReturnsFutureOfListOfCompletedValuesDuplicates() {
        final Set<CompletableFuture<String>> set = new HashSet<>();
        set.add(completedFuture("foo"));
        set.add(completedFuture("foo"));

        final CompletableFuture<List<String>> future = collectionOfFuturesToFutureOfCollection(set, toList());
        final List<String> result = future.join();
        assertThat(result).containsExactly("foo", "foo");
        assertThat(result).isExactlyInstanceOf(ArrayList.class);
    }
}
