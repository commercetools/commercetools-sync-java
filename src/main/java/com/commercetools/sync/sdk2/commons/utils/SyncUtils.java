package com.commercetools.sync.sdk2.commons.utils;

import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.customer_group.CustomerGroupReference;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifier;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifier;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.api.models.tax_category.TaxCategoryReference;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifier;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifierBuilder;
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
   * Given a reference to a resource, this method checks if the reference id is cached in the map.
   * If it is, then it executes the {@code keyInReferenceSupplier} and returns its result.
   * Otherwise, it returns the supplied reference as is. Since, the reference could be {@code null},
   * this method could also return null if the reference id is not in the map.
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
  public static <T extends Reference> T getReferenceWithKeyReplaced(
      @Nullable final T reference,
      @Nonnull final Supplier<T> keyInReferenceSupplier,
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
   * Given a reference to a resource of type {@code ProductTypeReference}, this method checks if the
   * reference id is cached. If it is, then it returns the resource identifier with key. Otherwise,
   * it returns the resource identifier with id. Since, the reference could be {@code null}, this
   * method could also return null if the reference id was not in the map.
   *
   * @param reference the reference of the resource to check if it's cached.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return returns the resource identifier with key if the {@code reference} id was in cache.
   *     Otherwise, it returns the resource identifier with id.
   */
  @Nullable
  public static ProductTypeResourceIdentifier getResourceIdentifierWithKey(
      @Nullable final ProductTypeReference reference,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (reference != null) {
      final String id = reference.getId();
      if (referenceIdToKeyCache.containsKey(id)) {
        return ProductTypeResourceIdentifierBuilder.of().key(referenceIdToKeyCache.get(id)).build();
      }
      return ProductTypeResourceIdentifierBuilder.of().id(id).build();
    }

    return null;
  }

  /**
   * Given a reference to a resource of type {@code CategoryReference}, this method checks if the
   * reference id is cached. If it is, then it returns the resource identifier with key. Otherwise,
   * it returns the resource identifier with id. Since, the reference could be {@code null}, this
   * method could also return null if the reference id was not in the map.
   *
   * @param reference the reference of the resource to check if it's cached.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return returns the resource identifier with key if the {@code reference} id was in cache.
   *     Otherwise, it returns the resource identifier with id.
   */
  @Nullable
  public static CategoryResourceIdentifier getResourceIdentifierWithKey(
      @Nullable final CategoryReference reference,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (reference != null) {
      final String id = reference.getId();
      if (referenceIdToKeyCache.containsKey(id)) {
        return CategoryResourceIdentifierBuilder.of().key(referenceIdToKeyCache.get(id)).build();
      }
      return CategoryResourceIdentifierBuilder.of().id(id).build();
    }

    return null;
  }

  public static ChannelResourceIdentifier getResourceIdentifierWithKey(
      final ChannelReference reference, final ReferenceIdToKeyCache referenceIdToKeyCache) {
    if (reference != null) {
      final String id = reference.getId();
      if (referenceIdToKeyCache.containsKey(id)) {
        return ChannelResourceIdentifierBuilder.of().key(referenceIdToKeyCache.get(id)).build();
      }
      return ChannelResourceIdentifierBuilder.of().id(id).build();
    }

    return null;
  }

  public static CustomerGroupResourceIdentifier getResourceIdentifierWithKey(
      final CustomerGroupReference reference, final ReferenceIdToKeyCache referenceIdToKeyCache) {
    if (reference != null) {
      final String id = reference.getId();
      if (referenceIdToKeyCache.containsKey(id)) {
        return CustomerGroupResourceIdentifierBuilder.of()
            .key(referenceIdToKeyCache.get(id))
            .build();
      }
      return CustomerGroupResourceIdentifierBuilder.of().id(id).build();
    }

    return null;
  }

  @Nullable
  public static TaxCategoryResourceIdentifier getResourceIdentifierWithKey(
      @Nullable final TaxCategoryReference reference,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (reference != null) {
      final String id = reference.getId();
      if (referenceIdToKeyCache.containsKey(id)) {
        return TaxCategoryResourceIdentifierBuilder.of().key(referenceIdToKeyCache.get(id)).build();
      }
      return TaxCategoryResourceIdentifierBuilder.of().id(id).build();
    }

    return null;
  }

  @Nullable
  public static StateResourceIdentifier getResourceIdentifierWithKey(
      @Nullable final StateReference reference,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (reference != null) {
      final String id = reference.getId();
      if (referenceIdToKeyCache.containsKey(id)) {
        return StateResourceIdentifierBuilder.of().key(referenceIdToKeyCache.get(id)).build();
      }
      return StateResourceIdentifierBuilder.of().id(id).build();
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
