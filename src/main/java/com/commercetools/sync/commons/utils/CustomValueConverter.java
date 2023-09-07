package com.commercetools.sync.commons.utils;

import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.Objects;
import javax.annotation.Nullable;

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
}
