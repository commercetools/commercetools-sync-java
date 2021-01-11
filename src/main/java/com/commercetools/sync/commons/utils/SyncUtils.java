package com.commercetools.sync.commons.utils;

import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.WithKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SyncUtils {
  private static final String UUID_REGEX =
      "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

  /**
   * Given a list of elements and a {@code batchSize}, this method distributes the elements into
   * batches with the {@code batchSize}. Each batch is represented by a {@link List} of elements and
   * all the batches are grouped and represented by a {@link List}&lt;{@link List}&gt; of elements,
   * which is returned by the method.
   *
   * @param <T> the type of the draft elements.
   * @param elements the list of elements to split into batches.
   * @param batchSize the size of each batch.
   * @return a list of lists where each list represents a batch of elements.
   */
  public static <T> List<List<T>> batchElements(
      @Nonnull final List<T> elements, final int batchSize) {
    List<List<T>> batches = new ArrayList<>();
    for (int i = 0; i < elements.size() && batchSize > 0; i += batchSize) {
      batches.add(elements.subList(i, Math.min(i + batchSize, elements.size())));
    }
    return batches;
  }

  /**
   * Given a reference to a resource of type {@code T}, this method checks if the reference is
   * expanded. If it is, then it executes the {@code keyInReferenceSupplier} and returns it's
   * result. Otherwise, it returns the supplied reference as is. Since, the reference could be
   * {@code null}, this method could also return null if the reference was not expanded.
   *
   * <p>This method expects the passed supplier to either
   *
   * @param reference the reference of the resource to check if it's expanded.
   * @param <T> the type of the resource.
   * @param keyInReferenceSupplier the supplier to execute and return its result if the {@code
   *     reference} was expanded.
   * @return returns the result of the {@code keyInReferenceSupplier} if the {@code reference} was
   *     expanded. Otherwise, it returns the supplied reference as is.
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
   * Given a reference to a resource of type {@code T}, this method checks if the reference is
   * expanded. If it is, then it return the resource identifier with key. Otherwise, it returns the
   * resource identifier with id. Since, the reference could be {@code null}, this method could also
   * return null if the reference was not expanded.
   *
   * @param reference the reference of the resource to check if it's expanded.
   * @param <T> the type of the resource.
   * @return returns the resource identifier with key if the {@code reference} was expanded.
   *     Otherwise, it returns the resource identifier with id.
   */
  @Nullable
  public static <T extends WithKey> ResourceIdentifier<T> getResourceIdentifierWithKey(
      @Nullable final Reference<T> reference) {

    if (reference != null) {
      if (reference.getObj() != null) {
        return ResourceIdentifier.ofKey(reference.getObj().getKey());
      }
      return ResourceIdentifier.ofId(reference.getId());
    }

    return null;
  }

  /**
   * Given an id as {@link String}, this method checks whether if it is in UUID format or not.
   *
   * @param id to check if it is in UUID format.
   * @return true if it is in UUID format, otherwise false.
   */
  public static boolean isUuid(@Nonnull final String id) {
    final Pattern regexPattern = Pattern.compile(UUID_REGEX);
    return regexPattern.matcher(id).matches();
  }

  private SyncUtils() {}
}
