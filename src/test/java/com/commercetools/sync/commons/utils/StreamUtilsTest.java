package com.commercetools.sync.commons.utils;

import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.StreamUtils.filterNullAndMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class StreamUtilsTest {

    @Test
    public void filterNullAndMap_emptyStream_ShouldReturnAnEmptyStream() {
        final Stream<Integer> mappedStream = filterNullAndMap(Stream.empty(), String::length);
        final List<Integer> mappedList = mappedStream.collect(toList());
        assertThat(mappedList).isEmpty();
    }

    @Test
    public void filterNullAndMap_streamWithOnlyNullElements_ShouldReturnAnEmptyStream() {
        final Stream<Integer> mappedStream = filterNullAndMap(Stream.of(null, null, null), String::length);
        final List<Integer> mappedList = mappedStream.collect(toList());
        assertThat(mappedList).isEmpty();
    }

    @Test
    public void filterNullAndMap_streamWithOneNullElement_ShouldReturnAnEmptyStream() {
        final String nullString = null;
        final Stream<Integer> mappedStream = filterNullAndMap(Stream.of(nullString), String::length);
        final List<Integer> mappedList = mappedStream.collect(toList());
        assertThat(mappedList).isEmpty();
    }

    @Test
    public void filterNullAndMap_singleElementStream_ShouldReturnMappedStream() {
        final Stream<Integer> mappedStream = filterNullAndMap(Stream.of("foo"), String::length);
        final List<Integer> mappedList = mappedStream.collect(toList());
        assertThat(mappedList).isNotEmpty();
        assertThat(mappedList).hasSize(1);
        assertThat(mappedList).containsOnly(3);
    }

    @Test
    public void filterNullAndMap_multipleElementsStream_ShouldReturnMappedStream() {
        final Stream<Integer> mappedStream = filterNullAndMap(Stream.of("james", "hetfield"), String::length);
        final List<Integer> mappedList = mappedStream.collect(toList());
        assertThat(mappedList).isNotEmpty();
        assertThat(mappedList).hasSize(2);
        assertThat(mappedList).containsExactly(5, 8);
    }

    @Test
    public void filterNullAndMap_nonEmptyStreamWithNullElements_ShouldReturnMappedStream() {
        final Stream<Integer> mappedStream = filterNullAndMap(Stream.of("james", "hetfield", null), String::length);
        final List<Integer> mappedList = mappedStream.collect(toList());
        assertThat(mappedList).isNotEmpty();
        assertThat(mappedList).hasSize(2);
        assertThat(mappedList).containsExactly(5, 8);
    }
}
