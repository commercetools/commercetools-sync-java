package com.commercetools.sync.sdk2.products.helpers;

import com.commercetools.api.models.product.ProductSetProductPriceCustomFieldAction;
import com.commercetools.api.models.product.ProductSetProductPriceCustomTypeAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.helpers.GenericCustomActionBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PriceCustomActionBuilder implements GenericCustomActionBuilder {

  @Override
  @Nonnull
  public ProductUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String priceId) {
    return ProductSetProductPriceCustomTypeAction.builder().priceId(priceId).staged(true).build();
  }

  @Override
  @Nonnull
  public ProductUpdateAction buildSetCustomTypeAction(
      @Nullable final Long variantId,
      @Nullable final String priceId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, Object> customFieldsJsonMap) {

    FieldContainerBuilder customFields = null;
    if (customFieldsJsonMap != null) {
      customFields = FieldContainerBuilder.of();
      customFieldsJsonMap.forEach(customFields::addValue);
    }

    return ProductSetProductPriceCustomTypeAction.builder()
        .priceId(priceId)
        .type(TypeResourceIdentifierBuilder.of().id(customTypeId).build())
        .fields(customFields != null ? customFields.build() : null)
        .staged(true)
        .build();
  }

  @Override
  @Nonnull
  public ProductUpdateAction buildSetCustomFieldAction(
      @Nullable final Long variantId,
      @Nullable final String priceId,
      @Nullable final String customFieldName,
      @Nullable final Object customFieldValue) {

    return ProductSetProductPriceCustomFieldAction.builder()
        .priceId(priceId)
        .name(customFieldName)
        .value(customFieldValue)
        .staged(true)
        .build();
  }
}
