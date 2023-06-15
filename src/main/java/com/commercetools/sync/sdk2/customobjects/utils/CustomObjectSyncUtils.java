package com.commercetools.sync.sdk2.customobjects.utils;

import static com.commercetools.sync.sdk2.commons.utils.CustomValueConverter.convertCustomValueObjDataToJsonNode;

import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.fasterxml.jackson.databind.JsonNode;
import javax.annotation.Nonnull;

public class CustomObjectSyncUtils {

  /**
   * Compares the value of a {@link CustomObject} to the value of a {@link CustomObjectDraft}. It
   * returns a boolean whether the values are identical or not.
   *
   * @param oldCustomObject the {@link CustomObject} which should be synced.
   * @param newCustomObject the {@link CustomObjectDraft} with the new data.
   * @return A boolean whether the value of the CustomObject and CustomObjectDraft is identical or
   *     not.
   */
  public static boolean hasIdenticalValue(
      @Nonnull final CustomObject oldCustomObject,
      @Nonnull final CustomObjectDraft newCustomObject) {
    // Values are JSON standard types Number, String, Boolean, Array, Object, and common API data
    // types.
    final Object oldValue = oldCustomObject.getValue();
    final Object newValue = newCustomObject.getValue();

    final JsonNode oldValueJsonNode = convertCustomValueObjDataToJsonNode(oldValue);
    final JsonNode newValueJsonNode = convertCustomValueObjDataToJsonNode(newValue);

    return oldValueJsonNode.equals(newValueJsonNode);
  }
}
