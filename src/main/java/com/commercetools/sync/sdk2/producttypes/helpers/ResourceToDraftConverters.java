package com.commercetools.sync.sdk2.producttypes.helpers;

import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import javax.annotation.Nonnull;

public class ResourceToDraftConverters {

  public static ProductTypeDraft toProductTypeDraft(@Nonnull final ProductType productType) {
    return toProductTypeDraftBuilder(productType).build();
  }

  @Nonnull
  public static ProductTypeDraftBuilder toProductTypeDraftBuilder(
      @Nonnull final ProductType productType) {
    return ProductTypeDraftBuilder.of()
        .description(productType.getDescription())
        .key(productType.getKey())
        .name(productType.getName())
        .attributes(
            productType.getAttributes().stream()
                .map(
                    attributeDefinition ->
                        toAttributeDefinitionDraftBuilder(attributeDefinition).build())
                .collect(toList()));
  }

  @Nonnull
  public static AttributeDefinitionDraftBuilder toAttributeDefinitionDraftBuilder(
      @Nonnull final AttributeDefinition attributeDefinition) {
    return AttributeDefinitionDraftBuilder.of()
        .type(attributeDefinition.getType())
        .name(attributeDefinition.getName())
        .label(attributeDefinition.getLabel())
        .isSearchable(attributeDefinition.getIsSearchable())
        .inputTip(attributeDefinition.getInputTip())
        .isRequired(attributeDefinition.getIsSearchable())
        .attributeConstraint(attributeDefinition.getAttributeConstraint())
        .inputHint(attributeDefinition.getInputHint());
  }
}
