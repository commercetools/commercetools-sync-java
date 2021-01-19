package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.utils.StreamUtils.filterNullAndMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class StreamUtilsTest {

  @Test
  void filterNullAndMap_emptyStream_ShouldReturnAnEmptyStream() {
    final Stream<Integer> mappedStream = filterNullAndMap(Stream.empty(), String::length);
    final List<Integer> mappedList = mappedStream.collect(toList());
    assertThat(mappedList).isEmpty();
  }

  @Test
  void filterNullAndMap_streamWithOnlyNullElements_ShouldReturnAnEmptyStream() {
    final Stream<Integer> mappedStream =
        filterNullAndMap(Stream.of(null, null, null), String::length);
    final List<Integer> mappedList = mappedStream.collect(toList());
    assertThat(mappedList).isEmpty();
  }

  @Test
  void filterNullAndMap_streamWithOneNullElement_ShouldReturnAnEmptyStream() {
    final String nullString = null;
    final Stream<Integer> mappedStream = filterNullAndMap(Stream.of(nullString), String::length);
    final List<Integer> mappedList = mappedStream.collect(toList());
    assertThat(mappedList).isEmpty();
  }

  @Test
  void filterNullAndMap_singleElementStream_ShouldReturnMappedStream() {
    final Stream<Integer> mappedStream = filterNullAndMap(Stream.of("foo"), String::length);
    final List<Integer> mappedList = mappedStream.collect(toList());
    assertThat(mappedList).isNotEmpty();
    assertThat(mappedList).containsExactly(3);
  }

  @Test
  void filterNullAndMap_multipleElementsStream_ShouldReturnMappedStream() {
    final Stream<Integer> mappedStream =
        filterNullAndMap(Stream.of("james", "hetfield"), String::length);
    final List<Integer> mappedList = mappedStream.collect(toList());
    assertThat(mappedList).isNotEmpty();
    assertThat(mappedList).containsExactly(5, 8);
  }

  @Test
  void filterNullAndMap_nonEmptyStreamWithNullElements_ShouldReturnMappedStream() {
    final Stream<Integer> mappedStream =
        filterNullAndMap(Stream.of("james", "hetfield", null), String::length);
    final List<Integer> mappedList = mappedStream.collect(toList());
    assertThat(mappedList).isNotEmpty();
    assertThat(mappedList).containsExactly(5, 8);
  }
}
