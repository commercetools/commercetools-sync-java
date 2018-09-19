package com.commercetools.sync.commons.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

class OptionalUtilsTest {

    @Test
    void filterEmptyOptionals_withEmptyCollection_ShouldReturnEmptyCollection() {
        // preparation
        final List<Optional<String>> optionalStrings = emptyList();

        // test
        final List<String> filteredOptionals = filterEmptyOptionals(optionalStrings);

        //assertion
        assertEquals(emptyList(), filteredOptionals);
    }

    @Test
    void filterEmptyOptionals_withNoEmptyOptionals_ShouldReturnOptionalContentsAsList() {
        // preparation
        final List<Optional<String>> optionalStrings = asList(Optional.of("foo"), Optional.of("bar"));

        // test
        final List<String> filteredOptionals = filterEmptyOptionals(optionalStrings);

        //assertion
        assertEquals(asList("foo", "bar"), filteredOptionals);
    }

    @Test
    void filterEmptyOptionals_withAllEmptyOptionals_ShouldReturnEmptyCollection() {
        // preparation
        final List<Optional<String>> optionalStrings = asList(Optional.empty(), Optional.empty());

        // test
        final List<String> filteredOptionals = filterEmptyOptionals(optionalStrings);

        //assertion
        assertEquals(emptyList(), filteredOptionals);
    }

    @Test
    void filterEmptyOptionals_withSomeEmptyOptionals_ShouldReturnEmptyCollection() {
        // preparation
        final List<Optional<String>> optionalStrings = asList(Optional.of("foo"), Optional.empty());

        // test
        final List<String> filteredOptionals = filterEmptyOptionals(optionalStrings);

        //assertion
        assertEquals(singletonList("foo"), filteredOptionals);
    }
}