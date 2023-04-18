package com.commercetools.sync.sdk2.products.utils;

import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeAccessor;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public final class AttributeUtils {

  public static List<Reference> getAttributeReference(@Nonnull final Attribute attribute) {
    final Object attrValue = attribute.getValue();
    if (attrValue instanceof Reference) {
      return Collections.singletonList(AttributeAccessor.asReference(attribute));
    }
    if (attrValue instanceof List
        && ((List) attrValue).stream().anyMatch(v -> v instanceof Reference)) {
      return AttributeAccessor.asSetReference(attribute);
    }
    return Collections.emptyList();
  }
}
