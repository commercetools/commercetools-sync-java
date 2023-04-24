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

  public static List<Reference> getAttributeReference(@Nonnull final Attribute attribute) {
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

  public static List<Reference> getAttributeReferences(@Nonnull final Attribute attribute) {
    List<Attribute> attributes;
    Object attrValue = attribute.getValue();
    if (attrValue instanceof List && ((List) attrValue).stream().anyMatch(v -> v instanceof List)) {
      List<List<Attribute>> nestedSet = AttributeAccessor.asSetNested(attribute);
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
