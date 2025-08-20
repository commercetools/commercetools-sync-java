package com.commercetools.sync.producttypes;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;

import com.commercetools.api.models.product_type.*;
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
        .level(AttributeLevelEnum.VARIANT)
        .label(ofEnglish("x"))
        .type(AttributeTypeBuilder::textBuilder)
        .inputHint(TextInputHint.MULTI_LINE)
        .attributeConstraint(AttributeConstraintEnum.NONE)
        .isRequired(true)
        .isSearchable(false);
  }
}
