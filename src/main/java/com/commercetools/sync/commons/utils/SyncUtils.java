package com.commercetools.sync.commons.utils;

import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.Custom;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class SyncUtils {

    /**
     * Given a list of elements and a {@code batchSize}, this method distributes the elements into batches with the
     * {@code batchSize}. Each batch is represented by a {@link List} of elements and all the batches are grouped and
     * represented by a {@link List}&lt;{@link List}&gt; of elements, which is returned by the method.
     *
     * @param <T>       the type of the draft elements.
     * @param elements  the list of elements to split into batches.
     * @param batchSize the size of each batch.
     * @return a list of lists where each list represents a batch of elements.
     */
    public static <T> List<List<T>> batchElements(@Nonnull final List<T> elements, final int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < elements.size() && batchSize > 0; i += batchSize) {
            batches.add(elements.subList(i, Math.min(i + batchSize, elements.size())));
        }
        return batches;
    }

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

    /**
     * Given a reference to a resource of type {@code T}, this method
     * checks if the reference is expanded. If it is, then it executes the {@code keyInReferenceSupplier} and returns
     * it's result. Otherwise, it returns the supplied reference as is. Since, the reference could be {@code null}, this
     * method could also return null if the reference was not expanded.
     *
     * @param reference              the reference of the resource to check if it's expanded.
     * @param <T>                    the type of the resource.
     * @param keyInReferenceSupplier the supplier to execute and return its result if the {@code reference} was
     *                               expanded.
     * @return returns the result of the {@code keyInReferenceSupplier} if the {@code reference} was expanded.
     *         Otherwise, it returns the supplied reference as is.
     */
    @Nullable
    public static <T> Reference<T> replaceReferenceIdWithKey(@Nullable final Reference<T> reference,
                                                             @Nonnull final Supplier<Reference<T>>
                                                                 keyInReferenceSupplier) {
        if (reference != null && reference.getObj() != null) {
            return keyInReferenceSupplier.get();
        }
        return reference;
    }

    private SyncUtils() {
    }
}
