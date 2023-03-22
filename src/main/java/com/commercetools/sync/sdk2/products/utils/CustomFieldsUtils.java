package com.commercetools.sync.sdk2.products.utils;

import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeReference;
import java.util.Optional;
import javax.annotation.Nullable;

public class CustomFieldsUtils {

  public static CustomFieldsDraft createCustomFieldsDraft(@Nullable CustomFields customFields) {
    return Optional.ofNullable(customFields)
        .map(
            fields ->
                CustomFieldsDraftBuilder.of()
                    .fields(fields.getFields())
                    .type(
                        Optional.ofNullable(fields.getType())
                            .map(TypeReference::toResourceIdentifier)
                            .orElse(null))
                    .build())
        .orElse(null);
  }
}
