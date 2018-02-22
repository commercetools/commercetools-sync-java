package com.commercetools.sync.commons.utils.completablefutureutils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.toCompletableFutures;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ToCompletableFuturesTest {
    /**
     * Stream to List : empty values.
     * Stream to Set : empty values.
     **/

    @Test
    public void empty_StreamToList_ReturnsEmptyList() {
        final List<CompletableFuture<String>> futures =
            toCompletableFutures(Stream.<CompletionStage<String>>empty(), toList());
        assertThat(futures).isEmpty();
    }

    @Test
    public void empty_StreamToSet_ReturnsEmptySet() {
        final Set<CompletableFuture<String>> futures = toCompletableFutures(Stream.<CompletionStage<String>>empty(),
            toSet());
        assertThat(futures).isEmpty();
    }


    /**
     * Stream to List : single null value.
     * Stream to Set : single null value.
     */

    @Test
    public void singleNull_StreamToList_ReturnsEmptyList() {
        final CompletionStage<String> nullStage = null;
        final List<CompletableFuture<String>> futures = toCompletableFutures(Stream.of(nullStage), toList());
        assertThat(futures).isEmpty();
    }

    @Test
    public void singleNull_StreamToSet_ReturnsEmptySet() {
        final CompletionStage<String> nullStage = null;
        final Set<CompletableFuture<String>> futures = toCompletableFutures(Stream.of(nullStage), toSet());
        assertThat(futures).isEmpty();
    }

    /**
     * Stream to List : multiple null value.
     * Stream to Set : multiple null value.
     */
    @Test
    public void multipleNull_StreamToList_ReturnsEmptyList() {
        final CompletionStage<String> nullStage = null;
        final List<CompletableFuture<String>> futures = toCompletableFutures(Stream.of(nullStage, nullStage), toList());
        assertThat(futures).isEmpty();
    }

    @Test
    public void multipleNull_StreamToSet_ReturnsEmptySet() {
        final CompletionStage<String> nullStage = null;
        final Set<CompletableFuture<String>> futures = toCompletableFutures(Stream.of(nullStage, nullStage), toSet());
        assertThat(futures).isEmpty();
    }

    /**
     * * Stream to List : Single Value.
     * * Stream to Set :  Single Value.
     */

    @Test
    public void single_StreamToList_ReturnsListOfMappedValue() {
        final CompletionStage<String> completionStage = spy(completedFuture("foo"));
        final CompletableFuture<String> barFuture = completedFuture("bar");
        when(completionStage.toCompletableFuture()).thenReturn(barFuture);


        final List<CompletableFuture<String>> futures = toCompletableFutures(Stream.of(completionStage), toList());

        assertThat(futures).hasSize(1);
        assertThat(futures).isExactlyInstanceOf(ArrayList.class);
        assertThat(futures.get(0).join()).isEqualTo("bar");
    }

    @Test
    public void single_StreamToSet_ReturnsSetOfMappedValue() {
        final CompletionStage<String> completionStage = spy(completedFuture("foo"));
        final CompletableFuture<String> barFuture = completedFuture("bar");
        when(completionStage.toCompletableFuture()).thenReturn(barFuture);


        final Set<CompletableFuture<String>> futures = toCompletableFutures(Stream.of(completionStage), toSet());

        assertThat(futures).hasSize(1);
        assertThat(futures).isExactlyInstanceOf(HashSet.class);
        assertThat(futures.iterator().next().join()).isEqualTo("bar");
    }

    /**
     * * Stream to List : Multiple Values : no duplicates.
     * * Stream to Set :  Multiple Values : no duplicates.
     */

    @Test
    public void multiple_StreamToListNoDuplicates_ReturnsListOfMappedValues() {
        final CompletionStage<String> completionStage1 = spy(completedFuture("foo"));
        final CompletableFuture<String> barFuture = completedFuture("bar");
        when(completionStage1.toCompletableFuture()).thenReturn(barFuture);

        final CompletionStage<String> completionStage2 = spy(completedFuture("foo1"));
        final CompletableFuture<String> bar1Future = completedFuture("bar1");
        when(completionStage2.toCompletableFuture()).thenReturn(bar1Future);

        final List<CompletableFuture<String>> futures =
            toCompletableFutures(Stream.of(completionStage1, completionStage2), toList());

        assertThat(futures).isNotEmpty();
        assertThat(futures).hasSize(2);
        assertThat(futures).containsExactlyInAnyOrder(barFuture, bar1Future);
    }

    @Test
    public void multiple_StreamToSetNoDuplicates_ReturnsSetOfMappedValues() {
        final CompletionStage<String> completionStage1 = spy(completedFuture("foo"));
        final CompletableFuture<String> barFuture = completedFuture("bar");
        when(completionStage1.toCompletableFuture()).thenReturn(barFuture);

        final CompletionStage<String> completionStage2 = spy(completedFuture("foo1"));
        final CompletableFuture<String> bar1Future = completedFuture("bar1");
        when(completionStage2.toCompletableFuture()).thenReturn(bar1Future);

        final Set<CompletableFuture<String>> futures =
            toCompletableFutures((Stream.of(completionStage1, completionStage2)), toSet());

        assertThat(futures).isNotEmpty();
        assertThat(futures).hasSize(2);
        assertThat(futures).containsExactlyInAnyOrder(barFuture, bar1Future);

    }

    /**
     * * Stream to List : Multiple Values : duplicates.
     * * Stream to Set : Multiple Values : duplicates.
     */

    @Test
    public void multiple_StreamToListDuplicates_ReturnsListOfMappedValuesWithDuplicates() {
        final CompletionStage<String> completionStage = spy(completedFuture("foo"));
        final CompletableFuture<String> barFuture = completedFuture("bar");
        when(completionStage.toCompletableFuture()).thenReturn(barFuture);

        final List<CompletableFuture<String>> futures =
            toCompletableFutures(Stream.of(completionStage, completionStage), toList());

        assertThat(futures).isNotEmpty();
        assertThat(futures).hasSize(2);
        assertThat(futures).containsExactlyInAnyOrder(barFuture, barFuture);
    }

    @Test
    public void multiple_StreamToSetDuplicates_ReturnsSetOfMappedValuesNoDuplicates() {
        final CompletionStage<String> completionStage = spy(completedFuture("foo"));
        final CompletableFuture<String> barFuture = completedFuture("bar");
        when(completionStage.toCompletableFuture()).thenReturn(barFuture);

        final Set<CompletableFuture<String>> futures =
            toCompletableFutures(Stream.of(completionStage, completionStage), toSet());

        assertThat(futures).isNotEmpty();
        assertThat(futures).hasSize(1);
        assertThat(futures).containsExactly(barFuture);
    }
}
