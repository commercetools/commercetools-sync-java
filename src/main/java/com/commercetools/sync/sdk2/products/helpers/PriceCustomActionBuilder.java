package com.commercetools.sync.sdk2.products.helpers;

import com.commercetools.api.models.product.*;
import com.commercetools.sync.sdk2.commons.helpers.GenericCustomActionBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PriceCustomActionBuilder implements GenericCustomActionBuilder<ProductUpdateAction> {

  @Override
  @Nonnull
  public ProductUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String priceId) {
    return ProductSetProductPriceCustomTypeActionBuilder.of().priceId(priceId).build();
  }

  @Override
  @Nonnull
  public ProductUpdateAction buildSetCustomTypeAction(
      @Nullable final Long variantId,
      @Nullable final String priceId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, Object> customFieldsJsonMap) {

    return ProductSetProductPriceCustomTypeActionBuilder.of()
        .priceId(priceId)
        .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(customTypeId))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(customFieldsJsonMap))
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

    return ProductSetProductPriceCustomFieldActionBuilder.of()
        .priceId(priceId)
        .name(customFieldName)
        .value(customFieldValue)
        .staged(true)
        .build();
  }
}
