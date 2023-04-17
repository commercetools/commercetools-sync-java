package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;

import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import java.util.Optional;
import javax.annotation.Nullable;

public class CustomFieldsUtils {

  public static CustomFieldsDraft createCustomFieldsDraft(
      @Nullable final CustomFields customFields,
      @Nullable final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return referenceIdToKeyCache != null
        ? mapToCustomFieldsDraft(customFields, referenceIdToKeyCache)
        : createCustomFieldsDraft(customFields);
  }

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
