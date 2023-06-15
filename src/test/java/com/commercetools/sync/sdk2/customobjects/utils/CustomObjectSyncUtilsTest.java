package com.commercetools.sync.sdk2.customobjects.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.*;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class CustomObjectSyncUtilsTest {

  private CustomObject oldCustomObject;
  private CustomObjectDraft newCustomObjectdraft;

  @SuppressWarnings("unchecked")
  private void prepareMockObjects(
      final Object newCustomObjDraftValue, final Object oldCustomObjValue) {
    final String key = "testkey";
    final String container = "testcontainer";

    newCustomObjectdraft =
        CustomObjectDraftBuilder.of()
            .container(container)
            .key(key)
            .value(newCustomObjDraftValue)
            .build();
    oldCustomObject = mock(CustomObject.class);
    when(oldCustomObject.getValue()).thenReturn(oldCustomObjValue);
    when(oldCustomObject.getContainer()).thenReturn(container);
    when(oldCustomObject.getKey()).thenReturn(key);
  }

  @Test
  void hasIdenticalValue_WithSameBooleanValue_ShouldBeIdentical() {
    final JsonNode newDraftValue = JsonNodeFactory.instance.booleanNode(true);
    final boolean oldValue = true;
    prepareMockObjects(newDraftValue, oldValue);
    assertThat(newDraftValue.isBoolean()).isTrue();
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isTrue();
  }

  @Test
  void hasIdenticalValue_WithDifferentBooleanValue_ShouldNotBeIdentical() {
    final JsonNode newDraftValue = JsonNodeFactory.instance.booleanNode(true);
    final JsonNode oldValue = JsonNodeFactory.instance.booleanNode(false);
    prepareMockObjects(newDraftValue, oldValue);
    assertThat(newDraftValue.isBoolean()).isTrue();
    assertThat(oldValue.isBoolean()).isTrue();
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isFalse();
  }

  @Test
  void hasIdenticalValue_WithSameNumberValue_ShouldBeIdentical() {
    final JsonNode newDraftValue = JsonNodeFactory.instance.numberNode(2020);
    final JsonNode oldValue = JsonNodeFactory.instance.numberNode(2020);
    prepareMockObjects(newDraftValue, oldValue);
    assertThat(newDraftValue.isNumber()).isTrue();
    assertThat(oldValue.isNumber()).isTrue();
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isTrue();
  }

  @Test
  void hasIdenticalValue_WithDifferentNumberValue_ShouldNotBeIdentical() {
    final JsonNode newDraftValue = JsonNodeFactory.instance.numberNode(2020);
    final int oldValue = 2021;
    prepareMockObjects(newDraftValue, oldValue);
    assertThat(newDraftValue.isNumber()).isTrue();
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isFalse();
  }

  @Test
  void hasIdenticalValue_WithSameStringValue_ShouldBeIdentical() {
    final JsonNode newDraftValue = JsonNodeFactory.instance.textNode("\"CommerceTools\"");
    final String oldValue = "\"CommerceTools\"";
    prepareMockObjects(newDraftValue, oldValue);
    assertThat(newDraftValue.isTextual()).isTrue();
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isTrue();
  }

  @Test
  void hasIdenticalValue_WithDifferentStringValue_ShouldNotBeIdentical() {
    final String newDraftValue = "\"CommerceToolsPlatform\"";
    final String oldValue = "\"CommerceTools\"";
    prepareMockObjects(newDraftValue, oldValue);
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isFalse();
  }

  @Test
  void hasIdenticalValue_WithSameFieldAndValueInJsonNode_ShouldBeIdentical() {

    final ObjectNode oldValue = JsonNodeFactory.instance.objectNode().put("username", "Peter");
    final ObjectNode newValue = JsonNodeFactory.instance.objectNode().put("username", "Peter");
    prepareMockObjects(oldValue, newValue);
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isTrue();
  }

  @Test
  void hasIdenticalValue_WithSameFieldAndDifferentValueInJsonNode_ShouldNotBeIdentical() {

    final ObjectNode oldValue = JsonNodeFactory.instance.objectNode().put("username", "Peter");
    final ObjectNode newValue = JsonNodeFactory.instance.objectNode().put("username", "Joe");
    prepareMockObjects(newValue, oldValue);
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isFalse();
  }

  @Test
  void hasIdenticalValue_WithSameFieldAndValueInDifferentOrderInJsonNode_ShouldBeIdentical() {

    final ObjectNode oldValue =
        JsonNodeFactory.instance.objectNode().put("username", "Peter").put("userId", "123-456-789");

    final ObjectNode newValue =
        JsonNodeFactory.instance.objectNode().put("userId", "123-456-789").put("username", "Peter");

    prepareMockObjects(newValue, oldValue);

    assertThat(oldValue.toString()).isNotEqualTo(newValue.toString());
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isTrue();
  }

  @Test
  void hasIdenticalValue_WithSameMoneyValue_ShouldBeIdentical() {
    final Money oldValue =
        CentPrecisionMoneyBuilder.of()
            .centAmount(100L)
            .currencyCode("EUR")
            .fractionDigits(2)
            .build();
    final TypedMoneyDraft newDraftValue =
        TypedMoneyDraftBuilder.of()
            .centPrecisionBuilder()
            .centAmount(100L)
            .currencyCode("EUR")
            .fractionDigits(2)
            .build();

    prepareMockObjects(newDraftValue, oldValue);

    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isTrue();
  }

  @Test
  void hasIdenticalValue_WithDifferentMoneyValue_ShouldNotBeIdentical() {
    final Money oldValue = MoneyBuilder.of().centAmount(100L).currencyCode("EUR").build();
    final ObjectNode newDraftValue =
        JsonNodeFactory.instance.objectNode().put("centAmount", 200L).put("currencyCode", "EUR");

    prepareMockObjects(newDraftValue, oldValue);

    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isFalse();
  }

  @Test
  void hasIdenticalValue_WithDifferentMoneyTypeValues_ShouldNotBeIdentical() {
    final Money oldValue = MoneyBuilder.of().centAmount(100L).currencyCode("EUR").build();
    final HighPrecisionMoneyDraft newDraftValue =
        HighPrecisionMoneyDraftBuilder.of()
            .centAmount(100L)
            .currencyCode("EUR")
            .fractionDigits(2)
            .preciseAmount(101L)
            .build();

    prepareMockObjects(newDraftValue, oldValue);

    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isFalse();
  }

  @Test
  void
      hasIdenticalValue_WithSameNestedJsonNode_WithSameAttributeOrderInNestedJson_ShouldBeIdentical() {

    final JsonNode oldNestedJson =
        JsonNodeFactory.instance.objectNode().put("username", "Peter").put("userId", "123-456-789");
    JsonNode newNestedJson =
        JsonNodeFactory.instance.objectNode().put("username", "Peter").put("userId", "123-456-789");

    JsonNode oldJsonNode = JsonNodeFactory.instance.objectNode().set("nestedJson", oldNestedJson);

    JsonNode newJsonNode = JsonNodeFactory.instance.objectNode().set("nestedJson", newNestedJson);

    prepareMockObjects(oldJsonNode, newJsonNode);

    assertThat(oldJsonNode.toString()).isEqualTo(newJsonNode.toString());
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isTrue();
  }

  @Test
  void
      hasIdenticalValue_WithSameNestedJsonNode_WithDifferentAttributeOrderInNestedJson_ShouldBeIdentical() {

    JsonNode oldNestedJson =
        JsonNodeFactory.instance.objectNode().put("username", "Peter").put("userId", "123-456-789");
    JsonNode newNestedJson =
        JsonNodeFactory.instance.objectNode().put("userId", "123-456-789").put("username", "Peter");

    JsonNode oldJsonNode = JsonNodeFactory.instance.objectNode().set("nestedJson", oldNestedJson);

    JsonNode newJsonNode = JsonNodeFactory.instance.objectNode().set("nestedJson", newNestedJson);

    prepareMockObjects(oldJsonNode, newJsonNode);

    assertThat(oldJsonNode.toString()).isNotEqualTo(newJsonNode.toString());
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isTrue();
  }

  @Test
  void hasIdenticalValue_WithDifferentNestedJsonNode_ShouldNotBeIdentical() {

    JsonNode oldNestedJson =
        JsonNodeFactory.instance.objectNode().put("username", "Peter").put("userId", "123-456-789");
    JsonNode newNestedJson =
        JsonNodeFactory.instance.objectNode().put("userId", "129-382-189").put("username", "Peter");

    JsonNode oldJsonNode = JsonNodeFactory.instance.objectNode().set("nestedJson", oldNestedJson);

    JsonNode newJsonNode = JsonNodeFactory.instance.objectNode().set("nestedJson", newNestedJson);

    prepareMockObjects(oldJsonNode, newJsonNode);

    assertThat(oldJsonNode.toString()).isNotEqualTo(newJsonNode.toString());
    assertThat(CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObjectdraft))
        .isFalse();
  }
}
