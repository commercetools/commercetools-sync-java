package com.commercetools.sync.commons.utils;

import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.StreamUtils.map;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class StreamUtilsTest {

    @Test
    public void map_emptyStream_ShouldReturnAnEmptyStream() {
        final Stream<Integer> mappedStream = map(Stream.empty(), String::length);
        final List<Integer> mappedList = mappedStream.collect(toList());
        assertThat(mappedList).isEmpty();
    }

    @Test
    public void map_nonEmptyStream_ShouldReturnMappedStream() {
        final Stream<Integer> mappedStream = map(Stream.of("foo", "bar"), String::length);
        final List<Integer> mappedList = mappedStream.collect(toList());
        assertThat(mappedList).isNotEmpty();
        assertThat(mappedList).hasSize(2);
        assertThat(mappedList).containsExactlyInAnyOrder(3, 3);
    }

    @Test
    public void map_nonEmptyStreamWithNullElements_ShouldReturnMappedStream() {
        final Stream<Integer> mappedStream = map(Stream.of("foo", "bar", null), String::length);
        final List<Integer> mappedList = mappedStream.collect(toList());
        assertThat(mappedList).isNotEmpty();
        assertThat(mappedList).hasSize(2);
        assertThat(mappedList).containsExactlyInAnyOrder(3, 3);
    }
}
