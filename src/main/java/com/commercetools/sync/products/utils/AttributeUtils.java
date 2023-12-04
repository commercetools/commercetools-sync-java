package com.commercetools.sync.products.utils;

import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.SELF_REFERENCING_ID_PLACE_HOLDER;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.*;

import com.commercetools.api.models.product.Attribute;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AttributeUtils {

  /**
   * Given an attribute this method converts it's value into {@link JsonNode} and set's the
   * converted value in the attribute.
   *
   * @param attribute - Attribute to replace it's value with a JSON representation
   * @return - a {@link JsonNode} representing the attribute's value. extracted from the given
   *     attribute or empty list if the attribute * doesn't contain reference types.
   */
  @Nonnull
  public static JsonNode replaceAttributeValueWithJsonAndReturnValue(
      @Nonnull final Attribute attribute) {
    final Object attributeValue = attribute.getValue();
    final JsonNode attributeValueAsJson = JsonUtils.toJsonNode(attributeValue);
    attribute.setValue(attributeValueAsJson);
    return attributeValueAsJson;
  }

  /**
   * Given a {@link JsonNode} this method extracts the nodes containing a "typeId" field which is
   * representing a reference type.
   *
   * @param attributeValueAsJson - JsonNode to find the "reference" nodes
   * @return a {@link List} of {@link JsonNode} extracted from the given JSON or empty list if the
   *     value doesn't contain reference types.
   */
  public static List<JsonNode> getAttributeReferences(
      @Nonnull final JsonNode attributeValueAsJson) {
    return attributeValueAsJson.findParents(REFERENCE_TYPE_ID_FIELD);
  }

  /**
   * Given a {@link JsonNode} representing an attribute value of a given {@link Attribute} this
   * method removes all nodes containing invalid data, which is a null or {@code
   * SELF_REFERENCING_ID_PLACE_HOLDER}. The method set's the cleaned up value in the attribute and
   * returns cleaned up value.
   *
   * <p>Note: This might return an empty JsonNode.
   *
   * @param attributeValueAsJson - a JsonNode representing an attribute value
   * @param attribute - Attribute to replace it's value with cleaned-up value
   * @return - a {@link JsonNode} attribute's value which contains valid data for further
   *     processing, it might be an empty node in case all data was removed.
   */
  @Nonnull
  public static JsonNode cleanupAttributeValue(
      @Nonnull JsonNode attributeValueAsJson, @Nonnull final Attribute attribute) {
    if (!attributeValueAsJson.isArray()) {
      return cleanupAttributeValue(
          JsonNodeFactory.instance.arrayNode().add(attributeValueAsJson), attribute);
    }
    final Iterator<JsonNode> entryIterator = attributeValueAsJson.elements();
    while (entryIterator.hasNext()) {
      final JsonNode entry = entryIterator.next();
      final List<JsonNode> nodeValues = entry.findValues("value");
      final JsonNode firstValue = nodeValues.isEmpty() ? entry : nodeValues.get(0);
      if (firstValue.isArray()) {
        return cleanupAttributeValue(firstValue, attribute);
      }
      if (isAttributeEntryValueNullOrInvalid(firstValue)) {
        entryIterator.remove();
      }
    }
    return attributeValueAsJson;
  }

  // A helper to check if a json string contains "SELF_REFERENCE_ID"
  // It returns true if the given Json has serialization errors
  private static boolean isAttributeEntryValueNullOrInvalid(
      @Nullable final JsonNode attributeEntryValue) {
    try {
      return attributeEntryValue == null
          || JsonUtils.toJsonString(attributeEntryValue).contains(SELF_REFERENCING_ID_PLACE_HOLDER);
    } catch (JsonProcessingException e) {
      return true;
    }
  }
}
