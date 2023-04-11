package com.commercetools.sync.sdk2.commons.utils;

import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.sync.sdk2.commons.models.Custom;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class CustomTypeReferenceResolutionUtils {

  /**
   * Given a resource of type {@code T} that extends {@link Custom} (i.e. it has {@link
   * CustomFields}, this method checks if the custom fields are existing (not null) and they are
   * reference cached(reference idToKey value stored in the map). If they are then it returns a
   * {@link CustomFieldsDraft} instance with the custom type key in place of the key of the
   * reference. Otherwise, if it's not reference cached it returns a {@link CustomFieldsDraft}
   * without the key. If the resource has null {@link Custom}, then it returns {@code null}.
   *
   * @param resource the resource to replace its custom type key, if possible.
   * @param <T> the type of the resource.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return an instance of {@link CustomFieldsDraft} instance with the custom type key, if the
   *     custom type reference was existing and reference cached on the resource. Otherwise, if its
   *     not reference cached it returns a {@link CustomFieldsDraft} without a key. If the resource
   *     has no or null {@link Custom}, then it returns {@code null}.
   */
  @Nullable
  public static <T extends Custom> CustomFieldsDraft mapToCustomFieldsDraft(
      @Nonnull final T resource, @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    final CustomFields custom = resource.getCustom();
    return mapToCustomFieldsDraft(custom, referenceIdToKeyCache);
  }

  @Nullable
  public static CustomFieldsDraft mapToCustomFieldsDraft(
      @Nullable final CustomFields custom,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    if (custom != null) {
      final String typeId = custom.getType().getId();
      if (referenceIdToKeyCache.containsKey(typeId)) {
        return CustomFieldsDraftBuilder.of()
            .type(
                typeResourceIdentifierBuilder ->
                    typeResourceIdentifierBuilder.key(referenceIdToKeyCache.get(typeId)))
            .fields(custom.getFields())
            .build();
      }
      return CustomFieldsDraftBuilder.of()
          .fields(custom.getFields())
          .type(custom.getType().toResourceIdentifier())
          .build();
    }
    return null;
  }

  private CustomTypeReferenceResolutionUtils() {}
}
