package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;

import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinitionBuilder;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductTypeBuilder;
import com.commercetools.api.models.product_type.TextInputHint;
import java.time.ZonedDateTime;

public class ProductTypeMockUtils {

  public static ProductTypeBuilder getProductTypeBuilder() {
    return ProductTypeBuilder.of()
        .key("foo")
        .attributes(emptyList())
        .id("id")
        .version(1L)
        .createdAt(ZonedDateTime.now())
        .lastModifiedAt(ZonedDateTime.now())
        .name("name")
        .description("description");
  }

  public static AttributeDefinitionBuilder getAttributeDefinitionBuilder() {
    return AttributeDefinitionBuilder.of()
        .name("a")
        .label(ofEnglish("a"))
        .type(AttributeTypeBuilder::textBuilder)
        .isRequired(false)
        .attributeConstraint(AttributeConstraintEnum.NONE)
        .inputHint(TextInputHint.SINGLE_LINE)
        .isSearchable(false);
  }
}
