package com.commercetools.sync.commons.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CompletableFutureUtilsTest {

  /**
   * * List to List : Single Value. * List to Set : Single Value. * Set to Set : Single Value. * Set
   * to List : Single Value.
   */
  @Test
  void single_ListToList_ReturnsFutureOfList() {
    final String result = "value1";
    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(
            singletonList(completedFuture(result)), toList());
    assertThat(future.join()).containsExactly(result);
  }

  @Test
  void single_ListToSet_ReturnsFutureOfSet() {
    final String result = "value1";
    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(
            singletonList(completedFuture(result)), toSet());
    assertThat(future.join()).containsExactly(result);
  }

  @Test
  void single_SetToSet_ReturnsFutureOfSet() {
    final String result = "value1";
    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(
            singleton(completedFuture(result)), toSet());
    assertThat(future.join()).containsExactly(result);
  }

  @Test
  void single_SetToList_ReturnsFutureOfList() {
    final String result = "value1";
    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(
            singleton(completedFuture(result)), toList());
    assertThat(future.join()).containsExactly(result);
  }

  /**
   * * List to List : Multiple Values : no duplicates. * List to Set : Multiple Values : no
   * duplicates. * Set to Set : Multiple Values : no duplicates. * Set to List : Multiple Values :
   * no duplicates.
   */
  @Test
  void multiple_ListToListNoDuplicates_ReturnsFutureOfListOfCompletedValues() {
    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(
            asList(completedFuture("foo"), completedFuture("bar")), toList());
    final List<String> result = future.join();
    assertThat(result).containsExactly("foo", "bar");
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void multiple_ListToSetNoDuplicates_ReturnsFutureOfSetOfCompletedValues() {
    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(
            asList(completedFuture("foo"), completedFuture("bar")), toSet());
    final Set<String> result = future.join();
    assertThat(result).containsExactlyInAnyOrder("foo", "bar");
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void multiple_SetToSetNoDuplicates_ReturnsFutureOfSetOfCompletedValues() {
    final Set<CompletableFuture<String>> set = new HashSet<>();
    set.add(completedFuture("foo"));
    set.add(completedFuture("bar"));

    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(set, toSet());
    final Set<String> result = future.join();
    assertThat(result).containsExactlyInAnyOrder("foo", "bar");
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void multiple_SetToListMappingNoDuplicates_ReturnsFutureOfListOfCompletedValues() {
    final Set<CompletableFuture<String>> set = new HashSet<>();
    set.add(completedFuture("foo"));
    set.add(completedFuture("bar"));

    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(set, toList());
    final List<String> result = future.join();
    assertThat(result).containsExactlyInAnyOrder("foo", "bar");
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  /**
   * * List to List : Multiple Values : duplicates. * List to Set : Multiple Values : duplicates. *
   * Set to Set : Multiple Values : duplicates. * Set to List : Multiple Values : duplicates.
   */
  @Test
  void multiple_ListToListDuplicates_ReturnsFutureOfListOfCompletedValuesDuplicates() {
    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(
            asList(completedFuture("foo"), completedFuture("foo")), toList());
    final List<String> result = future.join();
    assertThat(result).containsExactly("foo", "foo");
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void multiple_ListToSetDuplicates_ReturnsFutureOfSetOfCompletedValuesNoDuplicates() {
    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(
            asList(completedFuture("foo"), completedFuture("foo")), toSet());
    final Set<String> result = future.join();
    assertThat(result).containsExactly("foo");
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void multiple_SetToSetDuplicates_ReturnsFutureOfSetOfCompletedValuesNoDuplicates() {
    final Set<CompletableFuture<String>> set = new HashSet<>();
    set.add(completedFuture("foo"));
    set.add(completedFuture("foo"));

    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(set, toSet());
    final Set<String> result = future.join();
    assertThat(result).containsExactly("foo");
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void multiple_SetToListDuplicates_ReturnsFutureOfListOfCompletedValuesDuplicates() {
    final Set<CompletableFuture<String>> set = new HashSet<>();
    set.add(completedFuture("foo"));
    set.add(completedFuture("foo"));

    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(set, toList());
    final List<String> result = future.join();
    assertThat(result).containsExactly("foo", "foo");
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void empty_ListToList_ReturnsFutureOfEmptyList() {
    final CompletableFuture<List<String>> futureList =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            new ArrayList<String>(), CompletableFuture::completedFuture, toList());
    assertThat(futureList.join()).isEqualTo(emptyList());
  }

  @Test
  void empty_ListToSet_ReturnsFutureOfEmptySet() {
    final CompletableFuture<Set<String>> futureSet =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            new ArrayList<String>(), CompletableFuture::completedFuture, toSet());
    assertThat(futureSet.join()).isEqualTo(emptySet());
  }

  @Test
  void empty_SetToSet_ReturnsFutureOfEmptyList() {
    final CompletableFuture<List<String>> futureList =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            new HashSet<String>(), CompletableFuture::completedFuture, toList());
    assertThat(futureList.join()).isEqualTo(emptyList());
  }

  @Test
  void empty_SetToList_ReturnsFutureOfEmptySet() {
    final CompletableFuture<Set<String>> futureSet =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            new HashSet<String>(), CompletableFuture::completedFuture, toSet());
    assertThat(futureSet.join()).isEqualTo(emptySet());
  }

  /**
   * List to List : single null value. List to Set : single null value. Set to Set : single null
   * value. Set to List : single null value.
   */
  @Test
  void singleNull_ListToList_ReturnsFutureOfEmptyList() {
    final String nullString = null;
    final CompletableFuture<List<String>> futureList =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singletonList(nullString), CompletableFuture::completedFuture, toList());
    assertThat(futureList.join()).isEqualTo(emptyList());
  }

  @Test
  void singleNull_ListToSet_ReturnsFutureOfEmptySet() {
    final String nullString = null;
    final CompletableFuture<Set<String>> futureSet =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singletonList(nullString), CompletableFuture::completedFuture, toSet());
    assertThat(futureSet.join()).isEqualTo(emptySet());
  }

  @Test
  void singleNull_SetToSet_ReturnsFutureOfEmptyList() {
    final String nullString = null;
    final CompletableFuture<List<String>> futureList =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singleton(nullString), CompletableFuture::completedFuture, toList());
    assertThat(futureList.join()).isEqualTo(emptyList());
  }

  @Test
  void singleNull_SetToList_ReturnsFutureOfEmptySet() {
    final String nullString = null;
    final CompletableFuture<Set<String>> futureSet =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singleton(nullString), CompletableFuture::completedFuture, toSet());
    assertThat(futureSet.join()).isEqualTo(emptySet());
  }

  /** List to List : multiple null value. List to Set : multiple null value. */
  @Test
  void multipleNull_ListToList_ReturnsFutureOfEmptyList() {
    final String nullString = null;
    final CompletableFuture<List<String>> futureList =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            asList(nullString, nullString), CompletableFuture::completedFuture, toList());
    assertThat(futureList.join()).isEqualTo(emptyList());
  }

  @Test
  void multipleNull_ListToSet_ReturnsFutureOfEmptySet() {
    final String nullString = null;
    final CompletableFuture<Set<String>> futureSet =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            asList(nullString, nullString), CompletableFuture::completedFuture, toSet());
    assertThat(futureSet.join()).isEqualTo(emptySet());
  }

  /**
   * * List to List : Same Type Mapping : Single Value. * List to Set : Same Type Mapping : Single
   * Value. * Set to Set : Same Type Mapping : Single Value. * Set to List : Same Type Mapping :
   * Single Value.
   *
   * <p>List to List : Diff Type Mapping : Single Value. * List to Set : Diff Type Mapping : Single
   * Value. * Set to Set : Diff Type Mapping : Single Value. * Set to List : Diff Type Mapping :
   * Single Value.
   */
  @Test
  void single_ListToListWithSameTypeMapping_ReturnsFutureOfListOfMappedValue() {
    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singletonList("foo"), element -> completedFuture(element.concat("POSTFIX")), toList());
    final List<String> result = future.join();
    assertThat(result).containsExactly("fooPOSTFIX");
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void single_ListToSetWithSameTypeMapping_ReturnsFutureOfSetOfMappedValue() {
    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singletonList("foo"), element -> completedFuture(element.concat("POSTFIX")), toSet());
    final Set<String> result = future.join();
    assertThat(result).containsExactly("fooPOSTFIX");
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void single_SetToSetWithSameTypeMapping_ReturnsFutureOfSetOfMappedValue() {
    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singleton("foo"), element -> completedFuture(element.concat("POSTFIX")), toSet());
    final Set<String> result = future.join();
    assertThat(result).containsExactly("fooPOSTFIX");
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void single_SetToListWithSameTypeMapping_ReturnsFutureOfListOfMappedValue() {
    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singleton("foo"), element -> completedFuture(element.concat("POSTFIX")), toList());
    final List<String> result = future.join();
    assertThat(result).containsExactly("fooPOSTFIX");
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void single_ListToListWithDiffTypeMapping_ReturnsFutureOfListOfMappedValue() {
    final CompletableFuture<List<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singletonList("foo"), element -> completedFuture(element.length()), toList());
    final List<Integer> result = future.join();
    assertThat(result).containsExactly(3);
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void single_ListToSetWithDiffTypeMapping_ReturnsFutureOfSetOfMappedValue() {
    final CompletableFuture<Set<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singletonList("foo"), element -> completedFuture(element.length()), toSet());
    final Set<Integer> result = future.join();
    assertThat(result).containsExactly(3);
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void single_SetToSetWithDiffTypeMapping_ReturnsFutureOfSetOfMappedValue() {
    final CompletableFuture<Set<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singleton("foo"), element -> completedFuture(element.length()), toSet());
    final Set<Integer> result = future.join();
    assertThat(result).containsExactly(3);
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void single_SetToListWithDiffTypeMapping_ReturnsFutureOfListOfMappedValue() {
    final CompletableFuture<List<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            singleton("foo"), element -> completedFuture(element.length()), toList());
    final List<Integer> result = future.join();
    assertThat(result).containsExactly(3);
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  /**
   * * List to List : Same Type Mapping : Multiple Values : no duplicates. * List to Set : Same Type
   * Mapping : Multiple Values : no duplicates. * Set to Set : Same Type Mapping : Multiple Values :
   * no duplicates. * Set to List : Same Type Mapping : Multiple Values : no duplicates.
   *
   * <p>List to List : Diff Type Mapping : Multiple Values : no duplicates. * List to Set : Diff
   * Type Mapping : Multiple Values : no duplicates. * Set to Set : Diff Type Mapping : Multiple
   * Values : no duplicates. * Set to List : Diff Type Mapping : Multiple Values : no duplicates.
   */
  @Test
  void multiple_ListToListWithSameTypeMappingNoDuplicates_ReturnsFutureOfListOfMappedValues() {
    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            asList("foo", "bar"), element -> completedFuture(element.concat("POSTFIX")), toList());
    final List<String> result = future.join();
    assertThat(result).containsExactly("fooPOSTFIX", "barPOSTFIX");
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void multiple_ListToSetWithSameTypeMappingNoDuplicates_ReturnsFutureOfSetOfMappedValues() {
    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            asList("foo", "bar"), element -> completedFuture(element.concat("POSTFIX")), toSet());
    final Set<String> result = future.join();
    assertThat(result).containsExactlyInAnyOrder("fooPOSTFIX", "barPOSTFIX");
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void multiple_SetToSetWithSameTypeMappingNoDuplicates_ReturnsFutureOfSetOfMappedValues() {
    final Set<String> set = new HashSet<>();
    set.add("foo");
    set.add("bar");

    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            set, element -> completedFuture(element.concat("POSTFIX")), toSet());
    final Set<String> result = future.join();
    assertThat(result).containsExactlyInAnyOrder("fooPOSTFIX", "barPOSTFIX");
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void multiple_SetToListWithSameTypeMappingNoDuplicates_ReturnsFutureOfListOfMappedValues() {
    final Set<String> set = new HashSet<>();
    set.add("foo");
    set.add("bar");

    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            set, element -> completedFuture(element.concat("POSTFIX")), toList());
    final List<String> result = future.join();
    assertThat(result).containsExactlyInAnyOrder("fooPOSTFIX", "barPOSTFIX");
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void multiple_ListToListWithDiffTypeMappingNoDuplicates_ReturnsFutureOfListOfMappedValues() {
    final CompletableFuture<List<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            asList("john", "smith"), element -> completedFuture(element.length()), toList());
    final List<Integer> result = future.join();
    assertThat(result).containsExactly(4, 5);
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void multiple_ListToSetWithDiffTypeMappingNoDuplicates_ReturnsFutureOfSetOfMappedValues() {
    final CompletableFuture<Set<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            asList("john", "smith"), element -> completedFuture(element.length()), toSet());
    final Set<Integer> result = future.join();
    assertThat(result).containsExactlyInAnyOrder(4, 5);
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void multiple_SetToSetWithDiffTypeMappingNoDuplicates_ReturnsFutureOfSetOfMappedValues() {
    final Set<String> set = new HashSet<>();
    set.add("john");
    set.add("smith");

    final CompletableFuture<Set<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            set, element -> completedFuture(element.length()), toSet());
    final Set<Integer> result = future.join();
    assertThat(result).containsExactlyInAnyOrder(4, 5);
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void multiple_SetToListWithDiffTypeMappingNoDuplicates_ReturnsFutureOfListOfMappedValues() {
    final Set<String> set = new HashSet<>();
    set.add("john");
    set.add("smith");

    final CompletableFuture<List<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            set, element -> completedFuture(element.length()), toList());
    final List<Integer> result = future.join();
    assertThat(result).containsExactlyInAnyOrder(4, 5);
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  /**
   * * List to List : Same Type Mapping : Multiple Values : duplicates. * List to Set : Same Type
   * Mapping : Multiple Values : duplicates. * Set to Set : Same Type Mapping : Multiple Values :
   * duplicates. * Set to List : Same Type Mapping : Multiple Values : duplicates.
   *
   * <p>List to List : Diff Type Mapping : Multiple Values : duplicates. * List to Set : Diff Type
   * Mapping : Multiple Values : duplicates. * Set to Set : Diff Type Mapping : Multiple Values :
   * duplicates. * Set to List : Diff Type Mapping : Multiple Values : duplicates.
   */
  @Test
  void
      multiple_ListToListWithSameTypeMappingDuplicates_ReturnsFutureOfListOfMappedValuesWithDuplicates() {
    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            asList("foo", "foo"), element -> completedFuture(element.concat("POSTFIX")), toList());
    final List<String> result = future.join();
    assertThat(result).containsExactly("fooPOSTFIX", "fooPOSTFIX");
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void
      multiple_ListToSetWithSameTypeMappingDuplicates_ReturnsFutureOfSetOfMappedValuesNoDuplicates() {
    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            asList("foo", "foo"), element -> completedFuture(element.concat("POSTFIX")), toSet());
    final Set<String> result = future.join();
    assertThat(result).containsExactly("fooPOSTFIX");
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void
      multiple_SetToSetWithSameTypeMappingDuplicates_ReturnsFutureOfSetOfMappedValuesNoDuplicates() {
    final Set<String> set = new HashSet<>();
    set.add("foo");
    set.add("bar");

    final CompletableFuture<Set<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            set, element -> completedFuture("constantResult"), toSet());
    final Set<String> result = future.join();
    assertThat(result).containsExactly("constantResult");
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void
      multiple_SetToListWithSameTypeMappingDuplicates_ReturnsFutureOfListOfMappedValuesWithDuplicates() {
    final Set<String> set = new HashSet<>();
    set.add("foo");
    set.add("bar");

    final CompletableFuture<List<String>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            set, element -> completedFuture("constantResult"), toList());
    final List<String> result = future.join();
    assertThat(result).containsExactly("constantResult", "constantResult");
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void
      multiple_ListToListWithDiffTypeMappingDuplicates_ReturnsFutureOfListOfMappedValuesWithDuplicates() {
    final CompletableFuture<List<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            asList("john", "john"), element -> completedFuture(element.length()), toList());
    final List<Integer> result = future.join();
    assertThat(result).containsExactly(4, 4);
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }

  @Test
  void
      multiple_ListToSetWithDiffTypeMappingDuplicates_ReturnsFutureOfSetOfMappedValuesNoDuplicates() {
    final CompletableFuture<Set<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            asList("john", "john"), element -> completedFuture(element.length()), toSet());
    final Set<Integer> result = future.join();
    assertThat(result).containsExactly(4);
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void
      multiple_SetToSetWithDiffTypeMappingDuplicates_ReturnsFutureOfSetOfMappedValuesNoDuplicates() {
    final Set<String> set = new HashSet<>();
    set.add("foo");
    set.add("bar");

    final CompletableFuture<Set<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            set, element -> completedFuture(element.length()), toSet());
    final Set<Integer> result = future.join();
    assertThat(result).containsExactly(3);
    assertThat(result).isExactlyInstanceOf(HashSet.class);
  }

  @Test
  void
      multiple_SetToListWithDiffTypeMappingDuplicates_ReturnsFutureOfListOfMappedValuesWithDuplicates() {
    final Set<String> set = new HashSet<>();
    set.add("foo");
    set.add("bar");

    final CompletableFuture<List<Integer>> future =
        CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            set, element -> completedFuture(element.length()), toList());
    final List<Integer> result = future.join();
    assertThat(result).containsExactly(3, 3);
    assertThat(result).isExactlyInstanceOf(ArrayList.class);
  }
}
