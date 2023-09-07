package com.commercetools.sync.producttypes;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;

import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinitionBuilder;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductTypeBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.TextInputHint;
import java.time.ZonedDateTime;

public class MockBuilderUtils {

  public static ProductTypeBuilder createMockProductTypeBuilder() {
    return ProductTypeBuilder.of()
        .key("foo")
        .id("test")
        .version(1L)
        .createdAt(ZonedDateTime.now())
        .lastModifiedAt(ZonedDateTime.now())
        .name("foo")
        .description("foo");
  }

  public static ProductTypeDraftBuilder createMockProductTypeDraftBuilder() {
    return ProductTypeDraftBuilder.of().name("foo").description("foo");
  }

  public static AttributeDefinitionDraftBuilder createMockAttributeDefinitionDraftBuilder() {
    return AttributeDefinitionDraftBuilder.of()
        .name("foo")
        .label(ofEnglish("x"))
        .type(AttributeTypeBuilder::textBuilder)
        .inputHint(TextInputHint.MULTI_LINE)
        .attributeConstraint(AttributeConstraintEnum.NONE)
        .isRequired(true)
        .isSearchable(false);
  }

  public static AttributeDefinitionBuilder createMockAttributeDefinitionBuilder() {
    return AttributeDefinitionBuilder.of()
        .name("foo")
        .label(ofEnglish("x"))
        .type(AttributeTypeBuilder::textBuilder)
        .inputHint(TextInputHint.MULTI_LINE)
        .attributeConstraint(AttributeConstraintEnum.NONE)
        .isRequired(true)
        .isSearchable(false);
  }
}
