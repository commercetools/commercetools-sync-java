package com.commercetools.sync.sdk2.producttypes;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;

import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinitionBuilder;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.TextInputHint;

public class TestBuilderUtils {
  public static AttributeDefinitionDraftBuilder createDefaultAttributeDefinitionDraftBuilder() {
    return AttributeDefinitionDraftBuilder.of()
        .name("foo")
        .label(ofEnglish("x"))
        .type(AttributeTypeBuilder::textBuilder)
        .inputHint(TextInputHint.MULTI_LINE)
        .attributeConstraint(AttributeConstraintEnum.NONE)
        .isRequired(true)
        .isSearchable(false);
  }

  public static AttributeDefinitionBuilder createDefaultAttributeDefinitionBuilder() {
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
