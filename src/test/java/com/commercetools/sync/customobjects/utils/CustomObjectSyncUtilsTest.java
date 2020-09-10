package com.commercetools.sync.customobjects.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomObjectSyncUtilsTest {

    private CustomObject<JsonNode> oldCustomObject;
    private CustomObjectDraft<JsonNode> newCustomObjectdraft;

    private static final String key = "testkey";
    private static final String container = "testcontainer";


    @Test
    void hasIdenticalValue_WithSameFieldAndValueInJsonNode_ShouldBeIdentical() {

        ObjectNode oldValue = JsonNodeFactory.instance.objectNode().put("username", "Peter");
        ObjectNode newValue = JsonNodeFactory.instance.objectNode().put("username", "Peter");
        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, newValue);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(oldValue);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isTrue();
    }

    @Test
    void hasIdenticalValue_WithSameFieldAndDifferentValueInJsonNode_ShouldNotBeIdentical() {

        ObjectNode oldValue = JsonNodeFactory.instance.objectNode().put("username", "Peter");
        ObjectNode newValue = JsonNodeFactory.instance.objectNode().put("username", "Joe");
        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, newValue);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(oldValue);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isFalse();
    }

    @Test
    void hasIdenticalValue_WithSameFieldAndValueInDifferentOrderInJsonNode_ShouldBeIdentical() {

        ObjectNode oldValue = JsonNodeFactory.instance.objectNode()
                .put("username", "Peter")
                .put("userId", "123-456-789");

        ObjectNode newValue = JsonNodeFactory.instance.objectNode()
                .put("userId", "123-456-789")
                .put("username", "Peter");

        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, newValue);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(oldValue);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);

        assertThat(oldValue.toString()).isNotEqualTo(newValue.toString());
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isTrue();
    }

    @Test
    void hasIdenticalValue_WithSameNestedJsonNode_WithSameAttributeOrderInNestedJson_ShouldBeIdentical() {

        JsonNode oldNestedJson = JsonNodeFactory.instance.objectNode()
                .put("username", "Peter")
                .put("userId", "123-456-789");
        JsonNode newNestedJson = JsonNodeFactory.instance.objectNode()
                .put("username", "Peter")
                .put("userId", "123-456-789");

        JsonNode oldJsonNode = JsonNodeFactory.instance.objectNode()
                .set("nestedJson", oldNestedJson);

        JsonNode newJsonNode = JsonNodeFactory.instance.objectNode()
                .set("nestedJson", newNestedJson);


        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, newJsonNode);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(oldJsonNode);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);

        assertThat(oldJsonNode.toString()).isEqualTo(newJsonNode.toString());
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isTrue();
    }

    @Test
    void hasIdenticalValue_WithSameNestedJsonNode_WithDifferentAttributeOrderInNestedJson_ShouldBeIdentical() {

        JsonNode oldNestedJson = JsonNodeFactory.instance.objectNode()
                .put("username", "Peter")
                .put("userId", "123-456-789");
        JsonNode newNestedJson = JsonNodeFactory.instance.objectNode()
                .put("userId", "123-456-789")
                .put("username", "Peter");

        JsonNode oldJsonNode = JsonNodeFactory.instance.objectNode()
                .set("nestedJson", oldNestedJson);

        JsonNode newJsonNode = JsonNodeFactory.instance.objectNode()
                .set("nestedJson", newNestedJson);


        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, newJsonNode);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(oldJsonNode);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);

        assertThat(oldJsonNode.toString()).isNotEqualTo(newJsonNode.toString());
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isTrue();
    }

    @Test
    void hasIdenticalValue_WithDifferentNestedJsonNode_ShouldNotBeIdentical() {

        JsonNode oldNestedJson = JsonNodeFactory.instance.objectNode()
                .put("username", "Peter")
                .put("userId", "123-456-789");
        JsonNode newNestedJson = JsonNodeFactory.instance.objectNode()
                .put("userId", "129-382-189")
                .put("username", "Peter");

        JsonNode oldJsonNode = JsonNodeFactory.instance.objectNode()
                .set("nestedJson", oldNestedJson);

        JsonNode newJsonNode = JsonNodeFactory.instance.objectNode()
                .set("nestedJson", newNestedJson);


        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, newJsonNode);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(oldJsonNode);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);

        assertThat(oldJsonNode.toString()).isNotEqualTo(newJsonNode.toString());
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isFalse();
    }

    @Test
    void batchElements_WithValidSize_ShouldReturnCorrectBatches() {
        final int numberOfCustomObjectDrafts = 160;
        final int batchSize = 10;
        final ArrayList<CustomObjectDraft<JsonNode>> customObjectDrafts = new ArrayList<>();

        for (int i = 0; i < numberOfCustomObjectDrafts; i++) {
            customObjectDrafts.add(CustomObjectDraft
                    .ofUnversionedUpsert("container_" + i, "key_" + i,
                            JsonNodeFactory.instance.objectNode().put("field", "field_" + i)));
        }
        final List<List<CustomObjectDraft<JsonNode>>> batches = batchElements(customObjectDrafts, 10);
        assertThat(batches.size()).isEqualTo(numberOfCustomObjectDrafts / batchSize);
    }

    @Test
    void batchElements_WithNegativeSize_ShouldReturnNoBatches() {
        final int numberOfCustomObjectDrafts = 160;
        final ArrayList<CustomObjectDraft<JsonNode>> customObjectDrafts = new ArrayList<>();


        final List<List<CustomObjectDraft<JsonNode>>> batches = batchElements(customObjectDrafts, -100);
        assertThat(batches.size()).isEqualTo(0);
    }

    @Test
    void batchElements_WithEmptyListAndAnySize_ShouldReturnNoBatches() {
        final List<List<CustomObjectDraft<JsonNode>>> batches = batchElements(new ArrayList<>(), 100);
        assertThat(batches.size()).isEqualTo(0);
    }

    @Test
    void batchElements_WithUniformSeparation_ShouldReturnCorrectBatches() {
        batchStringElementsAndAssertAfterBatching(100, 10);
        batchStringElementsAndAssertAfterBatching(3, 1);
    }

    @Test
    void batchElements_WithNonUniformSeparation_ShouldReturnCorrectBatches() {
        batchStringElementsAndAssertAfterBatching(100, 9);
        batchStringElementsAndAssertAfterBatching(3, 2);
    }

    @Nonnull
    private List<String> getPrefixedStrings(final int numberOfElements, @Nonnull final String prefix) {
        return IntStream.range(1, numberOfElements + 1)
                .mapToObj(i -> format("%s#%s", prefix, i))
                .collect(Collectors.toList());
    }

    private void batchStringElementsAndAssertAfterBatching(final int numberOfElements, final int batchSize) {
        final List<String> elements = getPrefixedStrings(numberOfElements, "element");

        final List<List<String>> batches = batchElements(elements, batchSize);

        final int expectedNumberOfBatches = getExpectedNumberOfBatches(numberOfElements, batchSize);
        assertThat(batches.size()).isEqualTo(expectedNumberOfBatches);

        // Assert correct size of elements after batching
        final Integer numberOfElementsAfterBatching = batches.stream()
                .map(List::size)
                .reduce(0,
                    (element1, element2) -> element1 + element2);

        assertThat(numberOfElementsAfterBatching).isEqualTo(numberOfElements);


        // Assert correct splitting of batches
        final int remainder = numberOfElements % batchSize;
        if (remainder == 0) {
            final int numberOfDistinctBatchSizes = batches.stream().collect(Collectors.groupingBy(List::size)).size();
            assertThat(numberOfDistinctBatchSizes).isEqualTo(1);
        } else {
            final List<String> lastBatch = batches.get(batches.size() - 1);
            assertThat(lastBatch).hasSize(remainder);
        }

        // Assert that all elements have been batched in correct order
        final List<String> flatBatches = batches.stream()
                .flatMap(Collection::stream).collect(Collectors.toList());
        IntStream.range(0, flatBatches.size())
                .forEach(index -> assertThat(flatBatches.get(index)).isEqualTo(format("element#%s", index + 1)));
    }

    private int getExpectedNumberOfBatches(int numberOfElements, int batchSize) {
        return (int) (Math.ceil((double)numberOfElements / batchSize));
    }
}
