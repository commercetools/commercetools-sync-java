package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.commons.utils.ResourceIdentifierUtils.*;

import com.commercetools.api.models.common.Reference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AttributeUtils {

  /**
   * Given an attribute this method checks if its value is of type "reference" or "set" of
   * "reference" and transforms its value into a {@link List} of {@link Reference}.
   *
   * @param attrValue - Attribute value to extract the References if exists.
   * @return - a {@link List} of {@link Reference} extracted from the given attribute or empty list
   *     if the attribute * doesn't contain reference types.
   */
  @Nonnull
  public static List<Reference> getAttributeReferences(@Nonnull final Object attrValue) {
    final ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
    final JsonNode attributeDraftValueClone = objectMapper.convertValue(attrValue, JsonNode.class);

    final List<JsonNode> allAttributeReferences =
        attributeDraftValueClone.findParents(REFERENCE_TYPE_ID_FIELD);

    return allAttributeReferences.stream()
        .map(referenceValue -> mapJsonNodeToReferenceType(referenceValue, objectMapper))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Nullable
  private static Reference mapJsonNodeToReferenceType(
      @Nonnull final JsonNode referenceValue, @Nonnull ObjectMapper objectMapper) {
    final JsonNode idField = referenceValue.get(REFERENCE_ID_FIELD);
    if (idField != null && !Objects.equals(idField, NullNode.getInstance())) {
      return objectMapper.convertValue(referenceValue, Reference.class);
    } else {
      return null;
    }
  }
}
