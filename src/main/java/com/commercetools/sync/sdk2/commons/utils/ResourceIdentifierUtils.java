package com.commercetools.sync.sdk2.commons.utils;

import static java.util.Optional.ofNullable;

import com.commercetools.api.models.ResourceIdentifiable;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ResourceIdentifier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ResourceIdentifierUtils {

  /**
   * Given a {@link ResourceIdentifiable} {@code resource}, if it is not null, this method applies
   * the {@link ResourceIdentifiable#toResourceIdentifier()} method to return it as a {@link
   * ResourceIdentifiable}. If it is {@code null}, this method returns {@code null}.
   *
   * @param resource represents the resource to return as a {@link ResourceIdentifier} if not {@code
   *     null}.
   * @return the supplied resource in the as a {@link ResourceIdentifier} if not {@code null}. If it
   *     is {@code null}, this method returns {@code null}.
   */
  @Nullable
  public static <ResourceIdentifiableT extends ResourceIdentifiable>
      ResourceIdentifier toResourceIdentifierIfNotNull(
          @Nullable final ResourceIdentifiableT resource) {
    return ofNullable(resource).map(ResourceIdentifiable::toResourceIdentifier).orElse(null);
  }

  /**
   * Given a {@link Reference} {@code referenceValue} which is the representation of CTP Reference
   * object, this method checks if it has a {@code typeId} with the value equal to {@code
   * referenceTypeId}.
   *
   * @param referenceValue Reference object
   * @param referenceTypeId the typeId to check of the reference is of the same type or not.
   * @return true if the typeId field of the reference has the same value as {@code
   *     referenceTypeId}, otherwise, false.
   */
  public static boolean isReferenceOfType(
      @Nonnull final Reference referenceValue, final String referenceTypeId) {
    return referenceValue.getTypeId().getJsonName().equals(referenceTypeId);
  }

  private ResourceIdentifierUtils() {}
}
