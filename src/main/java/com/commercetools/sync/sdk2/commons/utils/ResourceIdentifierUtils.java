package com.commercetools.sync.sdk2.commons.utils;

import static java.util.Optional.ofNullable;

import com.commercetools.api.models.ResourceIdentifiable;
import com.commercetools.api.models.common.ResourceIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ResourceIdentifierUtils {
  public static final String REFERENCE_TYPE_ID_FIELD = "typeId";
  public static final String REFERENCE_ID_FIELD = "id";

  /**
   * Given a {@link io.sphere.sdk.models.Referenceable} {@code resource} of the type {@code T}, if
   * it is not null, this method applies the {@link
   * io.sphere.sdk.models.Referenceable#toResourceIdentifier()} method to return it as a {@link
   * io.sphere.sdk.models.ResourceIdentifier} of the type {@code T}. If it is {@code null}, this
   * method returns {@code null}.
   *
   * @param resource represents the resource to return as a {@link
   *     io.sphere.sdk.models.ResourceIdentifier} if not {@code null}.
   * @param <T> type of the resource supplied.
   * @param <S> represents the type of the {@link io.sphere.sdk.models.ResourceIdentifier} returned.
   * @return the supplied resource in the as a {@link io.sphere.sdk.models.ResourceIdentifier} if
   *     not {@code null}. If it is {@code null}, this method returns {@code null}.
   */
  @Nullable
  public static <T extends ResourceIdentifiable, S>
      ResourceIdentifier toResourceIdentifierIfNotNull(@Nullable final T resource) {
    return ofNullable(resource).map(ResourceIdentifiable::toResourceIdentifier).orElse(null);
  }

  /**
   * Given a {@link com.fasterxml.jackson.databind.JsonNode} {@code referenceValue} which is the
   * JSON representation of CTP Reference object, this method checks if it is has a {@code typeId}
   * with the value equal to {@code referenceTypeId}.
   *
   * @param referenceValue JSON representation of CTP reference object
   * @param referenceTypeId the typeId to check of the reference is of the same type or not.
   * @return true if the typeId field of the reference has the same value as {@code
   *     referenceTypeId}, otherwise, false.
   */
  public static boolean isReferenceOfType(
      @Nonnull final JsonNode referenceValue, final String referenceTypeId) {
    return getReferenceTypeId(referenceValue)
        .map(resolvedReferenceTypeId -> Objects.equals(resolvedReferenceTypeId, referenceTypeId))
        .orElse(false);
  }

  @Nonnull
  private static Optional<String> getReferenceTypeId(@Nonnull final JsonNode referenceValue) {
    final JsonNode typeId = referenceValue.get(REFERENCE_TYPE_ID_FIELD);
    return Optional.ofNullable(typeId).map(JsonNode::asText);
  }

  private ResourceIdentifierUtils() {}
}
