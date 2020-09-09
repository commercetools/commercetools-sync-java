package com.commercetools.sync.customobjects.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomObjectUpdateActionUtilsTest {

    private CustomObject<JsonNode> oldCustomObject;
    private CustomObjectDraft<JsonNode> newCustomObjectdraft;

    final String key = "testkey";
    final String container = "testcontainer";


    @Test
    void verifyIdenticalJsonNode_WithSameFieldAndValueInJsonNode_ShouldBeIdentical() {

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
    void verifyIdenticalJsonNode_WithSameFieldAndDifferentValueInJsonNode_ShouldNotBeIdentical() {

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
    void verifyIdenticalJsonNode_WithSameFieldAndValueInDifferentOrderInJsonNode_ShouldBeIdentical() {

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
    void verifyIdenticalJsonNode_WithSameNestedJsonNode_WithSameAttributeOrderInNestedJson_ShouldBeIdentical() {

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
    void verifyIdenticalJsonNode_WithSameNestedJsonNode_WithDifferentAttributeOrderInNestedJson_ShouldBeIdentical() {

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
    void verifyIdenticalJsonNode_WithDifferentNestedJsonNode_ShouldNotBeIdentical() {

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
}
