package com.commercetools.sync.commons.utils;

import io.sphere.sdk.types.Custom;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CustomTypeReferenceReplacementUtils {
    /**
     * Given a resource of type {@code T} that extends {@link Custom} (i.e. it has {@link CustomFields}, this method
     * checks if the custom fields are existing (not null) and they are reference expanded. If they are then
     * it returns a {@link CustomFieldsDraft} instance with the custom type key in place of the id of the reference.
     * Otherwise, if its not reference expanded it returns a {@link CustomFieldsDraft} with the id not replaced. If the
     * resource has no or null {@link Custom}, then it returns {@code null}.
     *
     * @param resource the resource to replace its custom type id, if possible.
     * @param <T> the type of the resource.
     * @return an instance of {@link CustomFieldsDraft} instance with the custom type key in place of the id, if the
     *         custom type reference was existing and reference expanded on the resource. Otherwise, if its not
     *         reference expanded it returns a {@link CustomFieldsDraft} with the id not replaced with key. If the
     *         resource has no or null {@link Custom}, then it returns {@code null}.
     */
    @Nullable
    public static <T extends Custom> CustomFieldsDraft replaceCustomTypeIdWithKeys(@Nonnull final T resource) {
        final CustomFields custom = resource.getCustom();
        if (custom != null) {
            if (custom.getType().getObj() != null) {
                return CustomFieldsDraft.ofTypeIdAndJson(custom.getType().getObj().getKey(),
                    custom.getFieldsJsonMap());
            }
            return CustomFieldsDraftBuilder.of(custom).build();
        }
        return null;
    }

    private CustomTypeReferenceReplacementUtils() {
    }
}
