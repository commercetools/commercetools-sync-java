package com.commercetools.sync.commons.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValueSetToFutureSet;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesListToFutureOfCompletedValues;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesSetToFutureOfCompletedValues;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureList;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.setOfFuturesToFutureOfSet;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.toListOfCompletableFutures;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.toSetOfCompletableFutures;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CompletableFutureUtilsTest {

    @Test
    public void mapValuesListToFutureOfCompletedValues_WithEmptyList_ShouldReturnAFutureWithAnEmptyList() {
        final CompletableFuture<List<String>> futureList = mapValuesListToFutureOfCompletedValues(
            new ArrayList<String>(), CompletableFuture::completedFuture);
        assertThat(futureList.join()).isEqualTo(emptyList());
    }

    @Test
    public void mapValuesListToFutureOfCompletedValues_SameTypeMapping_ReturnsFutureOfListOfMappedCompletedValues() {
        final CompletableFuture<List<String>> futureList = mapValuesListToFutureOfCompletedValues(
            asList("foo", "bar"), element -> completedFuture(element.concat("POSTFIX")));
        assertThat(futureList.join()).containsExactly("fooPOSTFIX", "barPOSTFIX");
    }

    @Test
    public void mapValuesListToFutureOfCompletedValues_DiffTypeMapping_ReturnsFutureOfListOfMappedCompletedValues() {
        final CompletableFuture<List<Integer>> futureList = mapValuesListToFutureOfCompletedValues(
            asList("foo", "bar"), element -> completedFuture(element.length()));
        assertThat(futureList.join()).containsExactly(3, 3);
    }

    @Test
    public void mapValuesSetToFutureOfCompletedValues_WithEmptySet_ShouldReturnAFutureWithEmptyStream() {
        final CompletableFuture<Stream<Integer>> futureStream = mapValuesSetToFutureOfCompletedValues(
            new HashSet<String>(), element -> completedFuture(element.length()));
        assertThat(futureStream.join().collect(toList())).isEqualTo(emptyList());
    }

    @Test
    public void mapValuesSetToFutureOfCompletedValues_DiffTypeMapping_ReturnsFutureOfStreamOfMappedCompletedVals() {
        final HashSet<String> names = new HashSet<>();
        names.add("fred durst");
        names.add("james hetfield");

        final CompletableFuture<Stream<Integer>> futureStream =
            mapValuesSetToFutureOfCompletedValues(names, element -> completedFuture(element.length()));
        assertThat(futureStream.join().collect(toList())).containsExactlyInAnyOrder(10, 14);
    }

    @Test
    public void mapValuesSetToFutureOfCompletedValues_SameTypeMapping_ReturnsFutureOfStreamOfMappedCompletedVals() {
        final HashSet<String> names = new HashSet<>();
        names.add("fred durst");
        names.add("james hetfield");

        final CompletableFuture<Stream<String>> futureStream =
            mapValuesSetToFutureOfCompletedValues(names, element -> completedFuture(element.concat("POSTFIX")));
        assertThat(futureStream.join().collect(toList()))
            .containsExactlyInAnyOrder("fred durstPOSTFIX", "james hetfieldPOSTFIX");
    }

    @Test
    public void mapValuesToFutureOfCompletedValues_WithEmptyStream_ShouldReturnAFutureWithEmptyList() {
        final CompletableFuture<List<String>> completedMappedValues = mapValuesToFutureOfCompletedValues(
            new ArrayList<String>().stream(), CompletableFuture::completedFuture);
        assertThat(completedMappedValues.join()).isEqualTo(emptyList());
    }

    @Test
    public void mapValuesToFutureOfCompletedValues_SameTypeMapping_ShouldReturnAFutureWithCompletedMappedListValues() {
        final CompletableFuture<List<String>> completedMappedValues = mapValuesToFutureOfCompletedValues(
            Stream.of("foo", "bar"), name -> completedFuture(name.concat("PostFix")));
        assertThat(completedMappedValues.join()).containsExactlyInAnyOrder("fooPostFix", "barPostFix");
    }

    @Test
    public void mapValuesToFutureOfCompletedValues_DifferentTypeMapping_ShouldReturnAFutureWithCompletedMappedListValues() {
        final CompletableFuture<List<Integer>> completedMappedValues = mapValuesToFutureOfCompletedValues(
            Stream.of("foo", "bar"), name -> completedFuture(name.length()));
        assertThat(completedMappedValues.join()).containsExactlyInAnyOrder(3, 3);
    }


    @Test
    public void setOfFuturesToFutureOfSet_WithEmptySet_ShouldReturnAFutureWithEmptySet() {
        final CompletableFuture<Set<String>> future = setOfFuturesToFutureOfSet(emptySet());
        assertThat(future.join()).isEqualTo(emptySet());
    }

    @Test
    public void setOfFuturesToFutureOfSet_WithNonEmptySet_ShouldReturnAFutureWithTheSetAsValue() {
        final Set<CompletionStage<String>> nameFutues = new HashSet<>();
        nameFutues.add(completedFuture("fred durst"));
        nameFutues.add(completedFuture("james hetfield"));

        final HashSet<String> names = new HashSet<>();
        names.add("fred durst");
        names.add("james hetfield");

        final CompletableFuture<Set<String>> future = setOfFuturesToFutureOfSet(nameFutues);
        assertThat(future.join()).isEqualTo(names);
    }


    @Test
    public void mapValuesToFutureList_WithEmptyStream_ShouldReturnAnEmptyList() {
        assertThat(mapValuesToFutureList(Stream.empty(), CompletableFuture::completedFuture)).isEmpty();
    }

    @Test
    public void mapValuesToFutureList_WithNoDuplicatesAndSameTypeMapping_ShouldReturnListOfMappedFutures() {
        final Stream<String> nameStream = Stream.of("james hetfield", "fred durst");

        final Function<String, CompletionStage<String>> removeSpaceFunction =
            name -> completedFuture(name.replaceAll(" ", ""));

        final List<CompletableFuture<String>> futures = mapValuesToFutureList(nameStream, removeSpaceFunction);

        assertThat(futures).isNotEmpty();
        assertThat(futures.stream()
                          .map(CompletableFuture::join)
                          .filter(resultingName -> resultingName.equals("jameshetfield") || resultingName
                              .equals("freddurst"))
                          .collect(toList())).hasSize(2);
        assertThat(futures).hasSize(2);
    }

    @Test
    public void mapValuesToFutureList_WithDuplicatesAndSameTypeMapping_ShouldReturnListOfMappedFutures() {
        final Stream<String> nameStream = Stream.of("james hetfield", "fred durst", "fred durst");

        final Function<String, CompletionStage<String>> removeSpaceFunction =
            name -> completedFuture(name.replaceAll(" ", ""));

        final List<CompletableFuture<String>> futures =
            mapValuesToFutureList(nameStream, removeSpaceFunction);

        assertThat(futures).isNotEmpty();
        assertThat(futures.stream()
                          .map(CompletableFuture::join)
                          .filter(resultingName -> resultingName.equals("jameshetfield") || resultingName
                              .equals("freddurst"))
                          .collect(toList())).hasSize(3);
        assertThat(futures).hasSize(3);
    }


    @Test
    public void mapValuesToFutureList_WithNoDuplicatesAndDiffTypeMapping_ShouldReturnListOfMappedFutures() {
        final Stream<String> nameStream = Stream.of("james hetfield", "fred durst");

        final Function<String, CompletionStage<Integer>> nameLengthFunction =
            name -> completedFuture(name.length());

        final List<CompletableFuture<Integer>> futures =
            mapValuesToFutureList(nameStream, nameLengthFunction);

        assertThat(futures).isNotEmpty();
        assertThat(futures.stream()
                          .map(CompletableFuture::join)
                          .filter(resultingSize -> resultingSize.equals(14) || resultingSize.equals(10))
                          .collect(toList())).hasSize(2);
        assertThat(futures).hasSize(2);
    }

    @Test
    public void mapValuesToFutureList_WithDuplicatesAndDiffTypeMapping_ShouldReturnListOfMappedFutures() {
        final Stream<String> nameStream = Stream.of("james hetfield", "fred durst", "fred durst");

        final Function<String, CompletionStage<Integer>> nameLengthFunction =
            name -> completedFuture(name.length());

        final List<CompletableFuture<Integer>> futures =
            mapValuesToFutureList(nameStream, nameLengthFunction);

        assertThat(futures).isNotEmpty();
        assertThat(futures.stream()
                          .map(CompletableFuture::join)
                          .filter(resultingSize -> resultingSize.equals(14) || resultingSize.equals(10))
                          .collect(toList())).hasSize(3);
        assertThat(futures).hasSize(3);
    }

    @Test
    public void mapValueSetToFutureSet_WithEmptySet_ShouldReturnAnEmptySet() {
        assertThat(mapValueSetToFutureSet(new HashSet<>(), CompletableFuture::completedFuture)).isEmpty();
    }

    @Test
    public void mapValueSetToFutureSet_DifferentTypeMapping_ShouldReturnSetOfMappedFutures() {
        final HashSet<String> names = new HashSet<>();
        names.add("fred durst");
        names.add("james hetfield");

        final Function<String, CompletionStage<Integer>> nameLengthFunction = name -> completedFuture(name.length());
        final Set<CompletableFuture<Integer>> futureSet = mapValueSetToFutureSet(names, nameLengthFunction);

        assertThat(futureSet).isNotEmpty();
        assertThat(futureSet.stream()
                            .map(CompletableFuture::join)
                            .filter(resultingSize -> resultingSize.equals(14) || resultingSize.equals(10))
                            .collect(toList())).hasSize(2);
        assertThat(futureSet).hasSize(2);
    }

    @Test
    public void mapValueSetToFutureSet_SameTypeMapping_ShouldReturnSetOfMappedFutures() {
        final HashSet<String> names = new HashSet<>();
        names.add("fred durst");
        names.add("james hetfield");

        final Set<CompletableFuture<String>> futureSet =
            mapValueSetToFutureSet(names, CompletableFuture::completedFuture);

        assertThat(futureSet).isNotEmpty();
        assertThat(futureSet.stream()
                            .map(CompletableFuture::join)
                            .filter(resultingSize -> resultingSize.equals("fred durst") ||
                                resultingSize.equals("james hetfield"))
                            .collect(toList())).hasSize(2);
        assertThat(futureSet).hasSize(2);
    }

    @Test
    public void toListOfCompletableFutures_WithEmptyStream_ShouldReturnAnEmptyList() {
        assertThat(toListOfCompletableFutures(Stream.empty())).isEmpty();
    }

    @Test
    public void toListOfCompletableFutures_WithStreamWithDuplicates_ShouldReturnListOfDuplicateCompletableFutures() {
        final CompletionStage<String> completionStage = spy(completedFuture("foo"));
        final CompletableFuture<String> barFuture = completedFuture("bar");
        when(completionStage.toCompletableFuture()).thenReturn(barFuture);

        final List<CompletableFuture<String>> futures = toListOfCompletableFutures(
            Stream.of(completionStage, completionStage));

        assertThat(futures).isNotEmpty();
        assertThat(futures).hasSize(2);
        assertThat(futures).containsExactlyInAnyOrder(barFuture, barFuture);
    }

    @Test
    public void toListOfCompletableFutures_WithStreamWithNoDuplicates_ShouldReturnListOfDistinctCompletableFutures() {
        final CompletionStage<String> completionStage1 = spy(completedFuture("foo"));
        final CompletableFuture<String> barFuture = completedFuture("bar");
        when(completionStage1.toCompletableFuture()).thenReturn(barFuture);

        final CompletionStage<String> completionStage2 = spy(completedFuture("foo1"));
        final CompletableFuture<String> bar1Future = completedFuture("bar1");
        when(completionStage2.toCompletableFuture()).thenReturn(bar1Future);

        final List<CompletableFuture<String>> futures = toListOfCompletableFutures(
            Stream.of(completionStage1, completionStage2));

        assertThat(futures).isNotEmpty();
        assertThat(futures).hasSize(2);
        assertThat(futures).containsExactlyInAnyOrder(barFuture, bar1Future);
    }

    @Test
    public void toSetOfCompletableFutures_WithEmptyStream_ShouldReturnAnEmptySet() {
        assertThat(toSetOfCompletableFutures(Stream.empty())).isEmpty();
    }

    @Test
    public void toSetOfCompletableFutures_WithStreamWithDuplicates_ShouldReturnListOfDistinctCompletableFutures() {
        final CompletionStage<String> completionStage = spy(completedFuture("foo"));
        final CompletableFuture<String> barFuture = completedFuture("bar");
        when(completionStage.toCompletableFuture()).thenReturn(barFuture);

        final Set<CompletableFuture<String>> futures = toSetOfCompletableFutures(
            Stream.of(completionStage, completionStage));

        assertThat(futures).isNotEmpty();
        assertThat(futures).hasSize(1);
        assertThat(futures).containsExactly(barFuture);
    }

    @Test
    public void toSetOfCompletableFutures_WithStreamWithNoDuplicates_ShouldReturnSetOfDistinctCompletableFutures() {
        final CompletionStage<String> completionStage1 = spy(completedFuture("foo"));
        final CompletableFuture<String> barFuture = completedFuture("bar");
        when(completionStage1.toCompletableFuture()).thenReturn(barFuture);

        final CompletionStage<String> completionStage2 = spy(completedFuture("foo1"));
        final CompletableFuture<String> bar1Future = completedFuture("bar1");
        when(completionStage2.toCompletableFuture()).thenReturn(bar1Future);

        final Set<CompletableFuture<String>> futures = toSetOfCompletableFutures(
            Stream.of(completionStage1, completionStage2));

        assertThat(futures).isNotEmpty();
        assertThat(futures).hasSize(2);
        assertThat(futures).containsExactlyInAnyOrder(barFuture, bar1Future);
    }
}
