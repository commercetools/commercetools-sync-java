package com.commercetools.sync.sdk2.commons.utils;

import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ResourceIdentifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

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
   * Given a reference to a resource of type {@code T}, this method checks if the reference id is
   * cached in the map. If it is, then it executes the {@code keyInReferenceSupplier} and returns
   * it's result. Otherwise, it returns the supplied reference as is. Since, the reference could be
   * {@code null}, this method could also return null if the reference id is not in the map.
   *
   * <p>This method expects the passed supplier to either
   *
   * @param reference the reference of the resource to check if it's cached.
   * @param keyInReferenceSupplier the supplier to execute and return its result if the {@code
   *     reference} was cached.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return returns the result of the {@code keyInReferenceSupplier} if the {@code reference} id
   *     was in cache. Otherwise, it returns the supplied reference as is.
   */
  @Nullable
  public static Reference getReferenceWithKeyReplaced(
      @Nullable final Reference reference,
      @Nonnull final Supplier<Reference> keyInReferenceSupplier,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (reference != null) {
      final String id = reference.getId();
      if (referenceIdToKeyCache.containsKey(id)) {
        return keyInReferenceSupplier.get();
      }
    }
    return reference;
  }

  /**
   * Given a reference to a resource of type {@code T}, this method checks if the reference id is
   * cached. If it is, then it returns the resource identifier with key. Otherwise, it returns the
   * resource identifier with id. Since, the reference could be {@code null}, this method could also
   * return null if the reference id was not in the map.
   *
   * @param reference the reference of the resource to check if it's cached.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @param toResourceIdentifierWithIdAndKey
   * @return returns the resource identifier with key if the {@code reference} id was in cache.
   *     Otherwise, it returns the resource identifier with id.
   */
  @Nullable
  public static ResourceIdentifier getResourceIdentifierWithKey(
      @Nullable final Reference reference,
      @Nonnull ReferenceIdToKeyCache referenceIdToKeyCache,
      final @Nonnull BiFunction<String, String, ResourceIdentifier>
              toResourceIdentifierWithIdAndKey) {
    return Optional.ofNullable(reference)
        .map(
            ref -> {
              final String id = ref.getId();
              return getResourceIdentifierWithKey(
                  id, referenceIdToKeyCache.get(id), toResourceIdentifierWithIdAndKey);
            })
        .orElse(null);
  }

  private static ResourceIdentifier getResourceIdentifierWithKey(
      @Nonnull final String id,
      @Nullable final String key,
      final BiFunction<String, String, ResourceIdentifier> toResourceIdentifier) {

    if (!StringUtils.isEmpty(key)) {
      return toResourceIdentifier.apply(null, key);
    }
    return toResourceIdentifier.apply(id, null);
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