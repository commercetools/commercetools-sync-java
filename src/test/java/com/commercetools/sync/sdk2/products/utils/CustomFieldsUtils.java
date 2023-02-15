package com.commercetools.sync.sdk2.products.utils;

import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;

public class CustomFieldsUtils {

  public static CustomFieldsDraft createCustomFieldsDraft(CustomFields customFields) {
    return CustomFieldsDraftBuilder.of()
        .fields(customFields.getFields())
        .type(customFields.getType().toResourceIdentifier())
        .build();
  }
}
