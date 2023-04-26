package com.commercetools.sync.sdk2.products.utils;

import static java.util.Collections.singletonList;

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
   * "reference" and transforms it's value into a {@link List} of {@link Reference}.
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
   * transforms it's value into a {@link List} of {@link Attribute}. The resulting list is then
   * mapped to type "reference" or "set" of "reference" depending on attributes' values types.
   *
   * @param attribute - Attribute to extract the Reference if exists
   * @return - a {@link List} of {@link Reference} extracted from the given attribute or empty list
   *     if the attribute doesn't contain reference types.
   */
  public static List<Reference> getAttributeReferences(@Nonnull final Attribute attribute) {
    List<Attribute> attributes;
    final Object attrValue = attribute.getValue();
    if (attrValue instanceof List && ((List) attrValue).stream().anyMatch(v -> v instanceof List)) {
      final List<List<Attribute>> nestedSet = AttributeAccessor.asSetNested(attribute);
      attributes = nestedSet.stream().flatMap(Collection::stream).collect(Collectors.toList());
    } else if (attribute.getValue() instanceof List
        && ((List) attribute.getValue()).stream().anyMatch(v -> v instanceof Attribute)) {
      attributes = AttributeAccessor.asNested(attribute);
    } else {
      attributes = singletonList(attribute);
    }

    return attributes.stream()
        .map(AttributeUtils::getAttributeReference)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
