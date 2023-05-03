package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.sdk2.commons.utils.SyncUtils.getResourceIdentifierWithKey;

import com.commercetools.api.models.Customizable;
import com.commercetools.api.models.type.*;
import com.commercetools.sync.sdk2.commons.models.Custom;
import java.util.Optional;
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
  public static <T extends Customizable<T>> CustomFieldsDraft mapToCustomFieldsDraft(
      @Nonnull final T resource, @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    final CustomFields custom = resource.getCustom();
    return mapToCustomFieldsDraft(custom, referenceIdToKeyCache);
  }

  /**
   * Given a custom {@link CustomFields}, this method provides checking to certain resources which
   * do not extends {@link Custom}. If the custom fields are existing (not null) and they are
   * reference cached(reference idToKey value stored in the map). If they are then it returns a
   * {@link CustomFieldsDraft} instance with the custom type key in place of the key of the
   * reference. Otherwise, if it's not reference cached it returns a {@link CustomFieldsDraft}
   * without the key. If the resource has null {@link Custom}, then it returns {@code null}.
   *
   * @param custom the resource to replace its custom type key, if possible.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return an instance of {@link CustomFieldsDraft} instance with the custom type key, if the
   *     custom type reference was existing and reference cached on the resource. Otherwise, if its
   *     not reference cached it returns a {@link CustomFieldsDraft} without a key. If the resource
   *     has no or null {@link Custom}, then it returns {@code null}.
   */
  @Nullable
  public static CustomFieldsDraft mapToCustomFieldsDraft(
      @Nullable final CustomFields custom,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return Optional.ofNullable(custom)
        .map(
            fields ->
                CustomFieldsDraftBuilder.of()
                    .fields(fields.getFields())
                    .type(
                        Optional.ofNullable(fields.getType())
                            .map(
                                typeReference ->
                                    (TypeResourceIdentifier)
                                        getResourceIdentifierWithKey(
                                            typeReference,
                                            referenceIdToKeyCache,
                                            (id, key) -> {
                                              final TypeResourceIdentifierBuilder builder =
                                                  TypeResourceIdentifierBuilder.of();
                                              if (id == null) {

                                                return builder.key(key).build();
                                              } else {
                                                return builder.id(id).build();
                                              }
                                            }))
                            .orElse(null))
                    .build())
        .orElse(null);
  }

  private CustomTypeReferenceResolutionUtils() {}
}
