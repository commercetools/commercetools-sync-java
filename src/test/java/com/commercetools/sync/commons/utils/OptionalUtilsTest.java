package com.commercetools.sync.commons.utils;

import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class OptionalUtilsTest {

    private String element1 = "element1";
    private String element2 = "element2";

    @Test
    public void filterEmptyOptionals_withPresent_returnsStreamOfOneElement() {
        TestPerson testPerson = new TestPerson();
        assertThat(filterEmptyOptionals(Optional.of(testPerson))).containsExactly(testPerson);
    }

    @Test
    public void filterEmptyOptionals_withEmpty_returnsEmptyStream() {
        assertThat(filterEmptyOptionals(Optional.empty())).isEmpty();
        TestPerson testPerson = null;
        assertThat(filterEmptyOptionals(Optional.ofNullable(testPerson))).isEmpty();
    }

    @Test
    public void filterEmptyOptionals_withGivenElements_whenCollectedToList_ShouldReturnNonEmptyOrderedList() {
        final List<String> result = Stream.of(Optional.of(element1), Optional.of(element2))
            .flatMap(OptionalUtils::filterEmptyOptionals)
            .collect(toList());
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).containsExactly(element1, element2);
    }

    @Test
    public void filterEmptyOptionals_withMixedElements_whenCollectedToList_ShouldReturnOnlyPresentElements() {
        final List<String> result = Stream.<Optional<String>>of(Optional.of(element1), Optional.empty())
            .flatMap(OptionalUtils::filterEmptyOptionals)
            .collect(toList());
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).containsExactly(element1);
    }

    @Test
    public void filterEmptyOptionals_withAllEmptyElements_whenCollectedToList_ShouldReturnEmptyList() {
        final List<?> result = Stream.of(Optional.empty(), Optional.empty())
            .flatMap(OptionalUtils::filterEmptyOptionals)
            .collect(toList());
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void filterEmptyOptionals_withMixedElements_whenCollectedToSet_ShouldReturnOnlyPresentElements() {
        final Set<String> result = Stream.<Optional<String>>of(Optional.of(element1), Optional.empty())
            .flatMap(OptionalUtils::filterEmptyOptionals)
            .collect(toSet());
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).containsExactly(element1);
    }

    @Test
    public void filterEmptyOptionals_withEmptyElements_whenCollectedToSet_ShouldReturnEmptySet() {
        final Set<?> result = Stream.of(Optional.empty(), Optional.empty())
            .flatMap(OptionalUtils::filterEmptyOptionals)
            .collect(toSet());
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void filterEmptyOptionals_onEmptyStream_whenCollectedToSet_ShouldReturnEmptySet() {
        final Set<?> result = Stream.<Optional<?>>of()
            .flatMap(OptionalUtils::filterEmptyOptionals)
            .collect(toSet());
        assertThat(result.isEmpty()).isTrue();
    }

    private static class TestPerson {

    }
}