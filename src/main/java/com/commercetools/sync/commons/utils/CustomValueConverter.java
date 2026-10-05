package com.commercetools.sync.commons.utils;

import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeType;

public final class CustomValueConverter {

  /**
   * Takes a value of type Object and converts to JSONNode.
   *
   * <p>This can be helpful to compare values of {@link CustomFields} and {@link CustomFieldsDraft},
   * or to check equality of {@link com.commercetools.api.models.custom_object.CustomObject}'s
   * values and {@link com.commercetools.api.models.custom_object.CustomObjectDraft}'s values.
   *
   * @param data a value of any Object-type
   * @return the given value converted to {@link JsonNode} or null
   */
  @Nullable
  public static JsonNode convertCustomValueObjDataToJsonNode(@Nullable final Object data) {
    if (Objects.isNull(data)) return null;
    final ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
    final JsonNode jsonNode = objectMapper.convertValue(data, JsonNode.class);
    return jsonNode;
  }

  /**
   * Takes a value of type JsonNode and checks if it's a valid string value.
   *
   * @param node - a jsonNode which might contain text
   * @return true if the given node is not null, not blank and does not contain a "null" value.
   */
  public static boolean isValidTextNode(@Nullable JsonNode node) {
    return node != null
        && JsonNodeType.STRING.equals(node.getNodeType())
        && !StringUtils.isBlank(node.asString())
        && !"null".equals(node.asString());
  }
}
