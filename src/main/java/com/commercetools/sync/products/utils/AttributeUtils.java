package com.commercetools.sync.products.utils;

import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.*;

import com.commercetools.api.models.product.Attribute;
import com.fasterxml.jackson.databind.JsonNode;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.List;
import javax.annotation.Nonnull;

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
}
