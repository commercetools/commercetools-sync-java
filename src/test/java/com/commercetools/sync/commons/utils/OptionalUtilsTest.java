package com.commercetools.sync.commons.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;

class OptionalUtilsTest {

    @Test
    void filterEmptyOptionals_withEmptyCollection_ShouldReturnEmptyList() {
        // preparation
        final List<Optional<String>> optionalStrings = emptyList();

        // test
        final List<String> filteredOptionals = filterEmptyOptionals(optionalStrings);

        // assertion
        assertEquals(emptyList(), filteredOptionals);
    }

    @Test
    void filterEmptyOptionals_withNoEmptyOptionals_ShouldReturnOptionalContentsAsList() {
        // preparation
        final List<Optional<String>> optionalStrings = asList(Optional.of("foo"), Optional.of("bar"));

        // test
        final List<String> filteredOptionals = filterEmptyOptionals(optionalStrings);

        // assertion
        assertEquals(asList("foo", "bar"), filteredOptionals);
    }

    @Test
    void filterEmptyOptionals_withAllEmptyOptionals_ShouldReturnEmptyList() {
        // preparation
        final List<Optional<String>> optionalStrings = asList(empty(), empty());

        // test
        final List<String> filteredOptionals = filterEmptyOptionals(optionalStrings);

        // assertion
        assertEquals(emptyList(), filteredOptionals);
    }

    @Test
    void filterEmptyOptionals_withSomeEmptyOptionals_ShouldFilterEmptyOptionals() {
        // preparation
        final List<Optional<String>> optionalStrings = asList(Optional.of("foo"), empty());

        // test
        final List<String> filteredOptionals = filterEmptyOptionals(optionalStrings);

        // assertion
        assertEquals(singletonList("foo"), filteredOptionals);
    }

    @Test
    void filterEmptyOptionals_withNoVarArgs_ShouldReturnEmptyList() {
        // test
        final List<String> filteredOptionals = filterEmptyOptionals();

        // assertion
        assertEquals(emptyList(), filteredOptionals);
    }

    @Test
    void filterEmptyOptionals_withVarArgsAllEmptyOptionals_ShouldReturnEmptyList() {
        // test
        final List<String> filteredOptionals = filterEmptyOptionals(empty(), empty());

        // assertion
        assertEquals(emptyList(), filteredOptionals);
    }

    @Test
    void filterEmptyOptionals_withVarArgsAllNonEmptyOptionals_ShouldReturnAllElementsInList() {
        // test
        final List<String> filteredOptionals = filterEmptyOptionals(of("foo"), of("bar"));

        // assertion
        assertEquals(asList("foo", "bar"), filteredOptionals);
    }

    @Test
    void filterEmptyOptionals_withVarArgsSomeEmptyOptionals_ShouldFilterEmptyOptionals() {
        // test
        final List<String> filteredOptionals = filterEmptyOptionals(of("foo"), empty());

        // assertion
        assertEquals(singletonList("foo"), filteredOptionals);
    }
}