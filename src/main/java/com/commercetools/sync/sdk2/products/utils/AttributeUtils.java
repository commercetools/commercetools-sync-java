package com.commercetools.sync.sdk2.products.utils;

import static java.util.Collections.*;

import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeAccessor;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public final class AttributeUtils {

  /**
   * Given an attribute this method checks if its value is of type "reference" or "set" of
   * "reference" and transforms its value into a {@link List} of {@link Reference}.
   *
   * @param attribute - Attribute to extract the References if exists.
   * @return - a {@link List} of {@link Reference} extracted from the given attribute or empty list
   *     if the attribute * doesn't contain reference types.
   */
  private static List<Reference> getAttributeReference(@Nonnull final Attribute attribute) {
    final Object attrValue = attribute.getValue();
    if (attrValue instanceof Reference) {
      return singletonList(AttributeAccessor.asReference(attribute));
    }
    if (attrValue instanceof List
        && ((List) attrValue).stream().anyMatch(v -> v instanceof Reference)) {
      return AttributeAccessor.asSetReference(attribute);
    }
    return Collections.emptyList();
  }

  /**
   * Given an attribute this method first checks if its of type "nested" or "set" of "nested" and
   * transforms its value into a {@link List} of {@link Attribute}. The resulting list is then
   * mapped to type "reference" or "set" of "reference" depending on attributes' values types.
   *
   * @param attribute - Attribute to extract the Reference if exists
   * @return - a {@link List} of {@link Reference} extracted from the given attribute or empty list
   *     if the attribute doesn't contain reference types.
   */
  public static List<Reference> getAttributeReferences(@Nonnull final Attribute attribute) {
    final List<Reference> referenceList = getAttributeReference(attribute);
    if (!referenceList.isEmpty()) {
      return referenceList;
    } else {
      final List<Attribute> flattenedAttributes = flatMapNestedAttributes(attribute);
      return flattenedAttributes.stream()
          .map(AttributeUtils::getAttributeReference)
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    }
  }

  /**
   * Takes an object and checks if it's of type {@link List} of {@link Attribute} which is
   * significant for a nested attribute.
   *
   * @param attrValue - value to be checked
   * @return true, if the given param is of type {@link List} of {@link Attribute} otherwise false
   */
  private static boolean isNested(@Nonnull final Object attrValue) {
    return attrValue instanceof List
        && (((List) attrValue).stream().allMatch(v -> v instanceof Attribute));
  }

  /**
   * Given an attribute this method first checks if its of type "nested" or "set" of "nested" and
   * transforms its value into a flattened {@link List} of {@link Attribute} using recursion.
   *
   * @param attribute - Attribute of any value
   * @return - a {@link List} of {@link Attribute} transformed from the given attribute.
   */
  private static List<Attribute> flatMapNestedAttributes(@Nonnull final Attribute attribute) {
    final Object attrValue = attribute.getValue();
    List<Attribute> flattenedAttributes;
    if (isNested(attrValue)) {
      flattenedAttributes = AttributeAccessor.asNested(attribute);
    } else if (attrValue instanceof List
        && ((List) attrValue).stream().allMatch(v -> isNested(v))) {
      final List<List<Attribute>> nestedSet = AttributeAccessor.asSetNested(attribute);
      flattenedAttributes =
          nestedSet.stream().flatMap(Collection::stream).collect(Collectors.toList());
    } else {
      return singletonList(attribute);
    }
    return flattenedAttributes.stream()
        .map(attr -> flatMapNestedAttributes(attr))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
