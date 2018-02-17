package com.commercetools.sync.commons.utils;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValueSetToFuturesOfDifferentType;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesListToFutureOfCompletedSameTypes;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesSetToFutureOfCompletedDifferentTypes;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedSameTypes;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFuturesOfDifferentType;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFuturesOfSameType;
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
    public void mapValuesListToFutureOfCompletedSameTypes_WithEmptyList_ShouldReturnAFutureWithEmptyList() {
        final CompletableFuture<List<String>> futureList = mapValuesListToFutureOfCompletedSameTypes(
            emptyList(), CompletableFuture::completedFuture);
        assertThat(futureList.join()).isEqualTo(emptyList());
    }

    @Test
    public void mapValuesListToFutureOfCompletedSameTypes_NonEmptyList_ReturnsFutureOfListOfMappedCompletedValues() {
        final CompletableFuture<List<String>> futureList = mapValuesListToFutureOfCompletedSameTypes(
            asList("foo", "bar"), element -> completedFuture(element.concat("POSTFIX")));
        assertThat(futureList.join()).isEqualTo(asList("fooPOSTFIX", "barPOSTFIX"));
    }

    @Test
    public void mapValuesSetToFutureOfCompletedDifferentTypes_WithEmptySet_ShouldReturnAFutureWithEmptyStream() {
        final CompletableFuture<Stream<Integer>> futureStream = mapValuesSetToFutureOfCompletedDifferentTypes(
            new HashSet<String>(), element -> completedFuture(element.length()));
        assertThat(futureStream.join().collect(toList())).isEqualTo(emptyList());
    }

    @Test
    public
        void mapValuesSetToFutureOfCompletedDifferentTypes_NonEmptyStream_ReturnsFutureOfStreamOfMappedCompletedVals() {
        final HashSet<String> names = new HashSet<>();
        names.add("fred durst");
        names.add("james hetfield");

        final CompletableFuture<Stream<Integer>> futureStream =
            mapValuesSetToFutureOfCompletedDifferentTypes(names, element -> completedFuture(element.length()));
        assertThat(futureStream.join().collect(toList())).isEqualTo(asList(10, 14));
    }

    @Test
    public void mapValuesToFutureOfCompletedDifferentTypes_WithStrings_ReturnsFutureWithCompletedMappedListValues() {
        final CompletableFuture<List<String>> completedMappedValues = mapValuesToFutureOfCompletedSameTypes(
            Stream.of("foo", "bar"), name -> completedFuture(name.concat("PostFix")));
        assertThat(completedMappedValues.join()).isEqualTo(asList("fooPostFix", "barPostFix"));
    }


    @Test
    public void mapValuesToFutureOfCompletedSameTypes_WithEmptyStream_ShouldReturnAFutureWithEmptyList() {
        final CompletableFuture<List<String>> completedMappedValues = mapValuesToFutureOfCompletedSameTypes(
            Stream.empty(), CompletableFuture::completedFuture);
        assertThat(completedMappedValues.join()).isEqualTo(emptyList());
    }

    @Test
    public void mapValuesToFutureOfCompletedSameTypes_WithStrings_ShouldReturnAFutureWithCompletedMappedListValues() {
        final CompletableFuture<List<String>> completedMappedValues = mapValuesToFutureOfCompletedSameTypes(
            Stream.of("foo", "bar"), name -> completedFuture(name.concat("PostFix")));
        assertThat(completedMappedValues.join()).isEqualTo(asList("fooPostFix", "barPostFix"));
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
    public void mapValuesToFuturesOfSameType_WithEmptyStream_ShouldReturnAnEmptyList() {
        assertThat(mapValuesToFuturesOfSameType(Stream.empty(), CompletableFuture::completedFuture)).isEmpty();
    }

    @Test
    public void mapValuesToFuturesOfSameType_WithNonEmptyStreamWithNoDuplicates_ShouldReturnListOfMappedFutures() {
        final Stream<String> nameStream = Stream.of("james hetfield", "fred durst");

        final Function<String, CompletionStage<String>> removeSpaceFunction =
            name -> completedFuture(name.replaceAll(" ", ""));

        final List<CompletableFuture<String>> futures =
            mapValuesToFuturesOfSameType(nameStream, removeSpaceFunction);

        assertThat(futures).isNotEmpty();
        assertThat(futures.stream()
                          .map(CompletableFuture::join)
                          .filter(resultingName -> resultingName.equals("jameshetfield") || resultingName
                              .equals("freddurst"))
                          .collect(toList())).hasSize(2);
        assertThat(futures).hasSize(2);
    }

    @Test
    public void mapValuesToFuturesOfSameType_WithNonEmptyStreamWithDuplicates_ShouldReturnListOfMappedFutures() {
        final Stream<String> nameStream = Stream.of("james hetfield", "fred durst", "fred durst");

        final Function<String, CompletionStage<String>> removeSpaceFunction =
            name -> completedFuture(name.replaceAll(" ", ""));

        final List<CompletableFuture<String>> futures =
            mapValuesToFuturesOfSameType(nameStream, removeSpaceFunction);

        assertThat(futures).isNotEmpty();
        assertThat(futures.stream()
                          .map(CompletableFuture::join)
                          .filter(resultingName -> resultingName.equals("jameshetfield") || resultingName
                              .equals("freddurst"))
                          .collect(toList())).hasSize(3);
        assertThat(futures).hasSize(3);
    }

    @Test
    public void mapValuesToFuturesOfDifferentType_WithEmptyStream_ShouldReturnAnEmptyList() {
        assertThat(mapValuesToFuturesOfDifferentType(Stream.empty(), CompletableFuture::completedFuture)).isEmpty();
    }

    @Test
    public void mapValuesToFuturesOfDifferentType_WithNonEmptyStreamWithNoDuplicates_ShouldReturnListOfMappedFutures() {
        final Stream<String> nameStream = Stream.of("james hetfield", "fred durst");

        final Function<String, CompletionStage<Integer>> nameLengthFunction =
            name -> completedFuture(name.length());

        final List<CompletableFuture<Integer>> futures =
            mapValuesToFuturesOfDifferentType(nameStream, nameLengthFunction);

        assertThat(futures).isNotEmpty();
        assertThat(futures.stream()
                          .map(CompletableFuture::join)
                          .filter(resultingSize -> resultingSize.equals(14) || resultingSize.equals(10))
                          .collect(toList())).hasSize(2);
        assertThat(futures).hasSize(2);
    }

    @Test
    public void mapValuesToFuturesOfDifferentType_WithNonEmptyStreamWithDuplicates_ShouldReturnListOfMappedFutures() {
        final Stream<String> nameStream = Stream.of("james hetfield", "fred durst", "fred durst");

        final Function<String, CompletionStage<Integer>> nameLengthFunction =
            name -> completedFuture(name.length());

        final List<CompletableFuture<Integer>> futures =
            mapValuesToFuturesOfDifferentType(nameStream, nameLengthFunction);

        assertThat(futures).isNotEmpty();
        assertThat(futures.stream()
                          .map(CompletableFuture::join)
                          .filter(resultingSize -> resultingSize.equals(14) || resultingSize.equals(10))
                          .collect(toList())).hasSize(3);
        assertThat(futures).hasSize(3);
    }

    @Test
    public void mapValueSetToFuturesOfDifferentType_WithEmptySet_ShouldReturnAnEmptySet() {
        assertThat(mapValueSetToFuturesOfDifferentType(new HashSet<>(), CompletableFuture::completedFuture)).isEmpty();
    }

    @Test
    public void mapValueSetToFuturesOfDifferentType_WithNonEmptySet_ShouldReturnSetOfMappedFutures() {
        final HashSet<String> names = new HashSet<>();
        names.add("fred durst");
        names.add("james hetfield");

        final Function<String, CompletionStage<Integer>> nameLengthFunction =
            name -> completedFuture(name.length());

        final Set<CompletableFuture<Integer>> futureSet =
            mapValueSetToFuturesOfDifferentType(names, nameLengthFunction);

        assertThat(futureSet).isNotEmpty();
        assertThat(futureSet.stream()
                          .map(CompletableFuture::join)
                          .filter(resultingSize -> resultingSize.equals(14) || resultingSize.equals(10))
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
