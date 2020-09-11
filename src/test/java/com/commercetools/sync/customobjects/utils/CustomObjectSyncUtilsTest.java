package com.commercetools.sync.customobjects.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomObjectSyncUtilsTest {

    private CustomObject<JsonNode> oldCustomObject;
    private CustomObjectDraft<JsonNode> newCustomObjectdraft;

    private static final String key = "testkey";
    private static final String container = "testcontainer";

    @Test
    void hasIdenticalValue_WithSameBooleanValue_ShouldBeIdentical() throws JsonProcessingException {
        JsonNode actualObj = new ObjectMapper().readTree("true");
        JsonNode mockedObj = new ObjectMapper().readTree("true");

        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, actualObj);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(mockedObj);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);
        assertThat(actualObj.isBoolean()).isTrue();
        assertThat(mockedObj.isBoolean()).isTrue();
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isTrue();
    }

    @Test
    void hasIdenticalValue_WithDifferentBooleanValue_ShouldNotBeIdentical() throws JsonProcessingException {
        JsonNode actualObj = new ObjectMapper().readTree("true");
        JsonNode mockedObj = new ObjectMapper().readTree("false");

        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, actualObj);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(mockedObj);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);
        assertThat(actualObj.isBoolean()).isTrue();
        assertThat(mockedObj.isBoolean()).isTrue();
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isFalse();
    }

    @Test
    void hasIdenticalValue_WithSameNumberValue_ShouldBeIdentical() throws JsonProcessingException {
        JsonNode actualObj = new ObjectMapper().readTree("2020");
        JsonNode mockedObj = new ObjectMapper().readTree("2020");

        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, actualObj);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(mockedObj);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);
        assertThat(actualObj.isNumber()).isTrue();
        assertThat(mockedObj.isNumber()).isTrue();
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isTrue();
    }

    @Test
    void hasIdenticalValue_WithDifferentNumberValue_ShouldNotBeIdentical() throws JsonProcessingException {
        JsonNode actualObj = new ObjectMapper().readTree("2020");
        JsonNode mockedObj = new ObjectMapper().readTree("2021");

        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, actualObj);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(mockedObj);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);
        assertThat(actualObj.isNumber()).isTrue();
        assertThat(mockedObj.isNumber()).isTrue();
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isFalse();
    }

    @Test
    void hasIdenticalValue_WithSameStringValue_ShouldBeIdentical() throws JsonProcessingException {
        JsonNode actualObj = new ObjectMapper().readTree("\"CommerceTools\"");
        JsonNode mockedObj = new ObjectMapper().readTree("\"CommerceTools\"");

        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, actualObj);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(mockedObj);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);
        assertThat(actualObj.isTextual()).isTrue();
        assertThat(mockedObj.isTextual()).isTrue();
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isTrue();
    }

    @Test
    void hasIdenticalValue_WithDifferentStringValue_ShouldNotBeIdentical() throws JsonProcessingException {
        JsonNode actualObj = new ObjectMapper().readTree("\"CommerceToolsPlatform\"");
        JsonNode mockedObj = new ObjectMapper().readTree("\"CommerceTools\"");

        newCustomObjectdraft = CustomObjectDraft.ofUnversionedUpsert(container, key, actualObj);
        oldCustomObject = mock(CustomObject.class);
        when(oldCustomObject.getValue()).thenReturn(mockedObj);
        when(oldCustomObject.getContainer()).thenReturn(container);
        when(oldCustomObject.getKey()).thenReturn(key);
        assertThat(actualObj.isTextual()).isTrue();
        assertThat(mockedObj.isTextual()).isTrue();
        assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft)).isFalse();
    }

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
}
