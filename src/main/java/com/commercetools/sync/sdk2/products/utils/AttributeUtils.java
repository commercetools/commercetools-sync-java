package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.commons.utils.ResourceIdentifierUtils.*;

import com.commercetools.api.models.product.Attribute;
import com.fasterxml.jackson.databind.JsonNode;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.List;
import javax.annotation.Nonnull;

public final class AttributeUtils {

  /**
   * Given an attribute this method checks if its value is of type "reference" or "set" of
   * "reference" and transforms its value into a {@link List} of {@link JsonNode}.
   *
   * @param attribute - Attribute to extract the References if exists.
   * @return - a {@link List} of {@link JsonNode} extracted from the given attribute or empty list
   *     if the attribute * doesn't contain reference types.
   */
  @Nonnull
  public static List<JsonNode> getAttributeReferencesAsJson(@Nonnull final Attribute attribute) {
    final Object attributeValue = attribute.getValue();
    final JsonNode attributeValueAsJson = JsonUtils.toJsonNode(attributeValue);
    attribute.setValue(attributeValueAsJson);
    return attributeValueAsJson.findParents(REFERENCE_TYPE_ID_FIELD);
  }
}
