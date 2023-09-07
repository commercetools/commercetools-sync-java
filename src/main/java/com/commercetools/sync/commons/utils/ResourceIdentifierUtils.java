package com.commercetools.sync.commons.utils;

import com.commercetools.api.models.common.Reference;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

public final class ResourceIdentifierUtils {

  public static final String REFERENCE_TYPE_ID_FIELD = "typeId";
  public static final String REFERENCE_ID_FIELD = "id";

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
    return referenceValue.getTypeId() != null
        && StringUtils.isNotBlank(referenceValue.getTypeId().getJsonName())
        && referenceValue.getTypeId().getJsonName().equals(referenceTypeId);
  }

  /**
   * Given a {@link JsonNode} {@code referenceValue} which is the JSON representation of CTP
   * Reference object, this method checks if it has a {@code typeId} with the value equal to {@code
   * referenceTypeId}.
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
