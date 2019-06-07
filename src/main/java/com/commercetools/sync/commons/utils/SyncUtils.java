package com.commercetools.sync.commons.utils;

import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;

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
     * Given a reference to a resource of type {@code T}, this method
     * checks if the reference is expanded. If it is, then it executes the {@code keyInReferenceSupplier} and returns
     * it's result. Otherwise, it returns the supplied reference as is. Since, the reference could be {@code null}, this
     * method could also return null if the reference was not expanded.
     *
     * <p>This method expects the passed supplier to either </p>
     *
     * @param reference              the reference of the resource to check if it's expanded.
     * @param <T>                    the type of the resource.
     * @param keyInReferenceSupplier the supplier to execute and return its result if the {@code reference} was
     *                               expanded.
     *
     * @return returns the result of the {@code keyInReferenceSupplier} if the {@code reference} was expanded.
     *         Otherwise, it returns the supplied reference as is.
     */
    @Nullable
    public static <T> Reference<T> getReferenceWithKeyReplaced(
        @Nullable final Reference<T> reference,
        @Nonnull final Supplier<Reference<T>> keyInReferenceSupplier) {

        if (reference != null && reference.getObj() != null) {
            return keyInReferenceSupplier.get();
        }
        return reference;
    }

    /**
     * Given a reference to a resource of type {@code T}, this method
     * checks if the reference is expanded. If it is, then it executes the {@code keyInReferenceSupplier} and returns
     * it's result. Otherwise, it returns the supplied reference as is. Since, the reference could be {@code null}, this
     * method could also return null if the reference was not expanded.
     *
     * <p>This method expects the passed supplier to either </p>
     *
     * @param reference              the reference of the resource to check if it's expanded.
     * @param <T>                    the type of the resource.
     * @param keyInReferenceSupplier the supplier to execute and return its result if the {@code reference} was
     *                               expanded.
     *
     * @return returns the result of the {@code keyInReferenceSupplier} if the {@code reference} was expanded.
     *         Otherwise, it returns the supplied reference as is.
     */
    @Nullable
    public static <T> ResourceIdentifier<T> getResourceIdentifierWithKeyReplaced(
        @Nullable final Reference<T> reference,
        @Nonnull final Supplier<ResourceIdentifier<T>> keyInReferenceSupplier) {

        if (reference != null && reference.getObj() != null) {
            return keyInReferenceSupplier.get();
        }
        return reference;
    }

    private SyncUtils() {
    }
}
