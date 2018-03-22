package com.commercetools.sync.commons.utils;

import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.StreamUtils.filterNullAndMap;
import static com.commercetools.sync.commons.utils.StreamUtils.getFilterEmptyOptionals;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class StreamUtilsTest {

    private String element1 = "element1";
    private String element2 = "element2";

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
        assertThat(mappedList).containsExactly(3);
    }

    @Test
    public void filterNullAndMap_multipleElementsStream_ShouldReturnMappedStream() {
        final Stream<Integer> mappedStream = filterNullAndMap(Stream.of("james", "hetfield"), String::length);
        final List<Integer> mappedList = mappedStream.collect(toList());
        assertThat(mappedList).isNotEmpty();
        assertThat(mappedList).containsExactly(5, 8);
    }

    @Test
    public void filterNullAndMap_nonEmptyStreamWithNullElements_ShouldReturnMappedStream() {
        final Stream<Integer> mappedStream = filterNullAndMap(Stream.of("james", "hetfield", null), String::length);
        final List<Integer> mappedList = mappedStream.collect(toList());
        assertThat(mappedList).isNotEmpty();
        assertThat(mappedList).containsExactly(5, 8);
    }

    @Test
    public void asList_streamOfNonEmptyOptionalsIsGiven_ShouldReturnNonEmptyList() {
        final List<String> result = Stream.of(Optional.of(element1), Optional.of(element2))
            .flatMap(StreamUtils::filterEmptyOptionals)
            .collect(toList());
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).containsExactly(element1, element2);
    }

    @Test
    public void asList_streamOfOptionalsIsGiven_ShouldReturnNonEmptyList() {
        final List<String> result = Stream.<Optional<String>>of(Optional.of(element1), Optional.empty())
            .flatMap(StreamUtils::filterEmptyOptionals)
            .collect(toList());
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).containsExactly(element1);
    }

    @Test
    public void asList_streamOfEmptyOptionalsIsGiven_ShouldReturnEmptyList() {
        final List<?> result = Stream.of(Optional.empty(), Optional.empty())
            .flatMap(StreamUtils::filterEmptyOptionals)
            .collect(toList());
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void asList_emptyStreamIsGiven_ShouldReturnEmptyList() {
        final List<?> result = Stream.<Optional<?>>of()
            .flatMap(getFilterEmptyOptionals())
            .collect(toList());
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void asSet_streamOfNonEmptyOptionalsIsGiven_ShouldReturnNonEmptySet() {
        final Set<String> result = Stream.of(Optional.of(element1), Optional.of(element2))
            .flatMap(getFilterEmptyOptionals())
            .collect(toSet());
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).containsExactlyInAnyOrder(element1, element2);
    }

    @Test
    public void asSet_streamOfOptionalsIsGiven_ShouldReturnNonEmptySet() {
        final Set<String> result = Stream.<Optional<String>>of(Optional.of(element1), Optional.empty())
            .flatMap(StreamUtils::filterEmptyOptionals)
            .collect(toSet());
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).containsExactly(element1);
    }

    @Test
    public void asSet_streamOfEmptyOptionalsIsGiven_ShouldReturnEmptySet() {
        final Set<?> result = Stream.of(Optional.empty(), Optional.empty())
            .flatMap(StreamUtils::filterEmptyOptionals)
            .collect(toSet());
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void asSet_emptyStreamIsGiven_ShouldReturnEmptySet() {
        final Set<?> result = Stream.<Optional<?>>of()
            .flatMap(StreamUtils::filterEmptyOptionals)
            .collect(toSet());
        assertThat(result.isEmpty()).isTrue();
    }

}
