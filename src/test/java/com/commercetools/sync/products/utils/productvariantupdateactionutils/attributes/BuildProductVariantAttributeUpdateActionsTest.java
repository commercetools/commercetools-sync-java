package com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.ProductSetAttributeActionBuilder;
import com.commercetools.api.models.product.ProductSetAttributeInAllVariantsActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionBuilder;
import com.commercetools.api.models.product_type.AttributeTextTypeBuilder;
import com.commercetools.api.models.product_type.TextInputHint;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.utils.ProductVariantAttributeUpdateActionUtils;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BuildProductVariantAttributeUpdateActionsTest {

  @Test
  void withNullOldAndNonNullNew_ShouldBuildSetAction() throws BuildUpdateActionException {

    // Preparation
    final long variantId = 1L;
    final Attribute oldAttribute = null;
    final Attribute newAttribute =
        AttributeBuilder.of().name("foo").value(JsonNodeFactory.instance.objectNode()).build();
    final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition attributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(newAttribute.getName())
            .label(ofEnglish("foo"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(newAttribute.getName(), AttributeMetaData.of(attributeDefinition));

    // Test
    final Optional<ProductUpdateAction> actionOptional =
        ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction(
            variantId, oldAttribute, newAttribute, attributesMetaData);

    // Assertion
    assertThat(actionOptional)
        .contains(
            ProductSetAttributeActionBuilder.of()
                .variantId(variantId)
                .name(newAttribute.getName())
                .value(newAttribute.getValue())
                .staged(true)
                .build());
  }

  @Test
  void withNullOldAndNonNullNew_WithSameForAllAttribute_ShouldBuildSetAllAction()
      throws BuildUpdateActionException {

    // Preparation
    final Attribute oldAttribute = null;
    final Attribute newAttribute =
        AttributeBuilder.of().name("foo").value(JsonNodeFactory.instance.objectNode()).build();
    final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition attributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(newAttribute.getName())
            .label(ofEnglish("foo"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(newAttribute.getName(), AttributeMetaData.of(attributeDefinition));

    // Test
    final Optional<ProductUpdateAction> actionOptional =
        ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction(
            1, oldAttribute, newAttribute, attributesMetaData);

    // Assertion
    assertThat(actionOptional)
        .contains(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name(newAttribute.getName())
                .value(newAttribute.getValue())
                .staged(true)
                .build());
  }

  @Test
  void withNullOldAndNonNullNew_WithNoExistingAttributeInMetaData_ShouldThrowException() {

    // Preparation
    final Attribute oldAttribute = null;
    final Attribute newAttribute =
        AttributeBuilder.of().name("foo").value(JsonNodeFactory.instance.objectNode()).build();
    final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();

    // Test and assertion
    assertThatThrownBy(
            () ->
                ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction(
                    1, oldAttribute, newAttribute, attributesMetaData))
        .hasMessage(
            String.format(
                ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA,
                newAttribute.getName()))
        .isExactlyInstanceOf(BuildUpdateActionException.class);
  }

  @Test
  void withDifferentValues_ShouldBuildSetAction() throws BuildUpdateActionException {
    // Preparation
    final Long variantId = 1L;
    final Attribute oldAttribute =
        AttributeBuilder.of().name("foo").value(JsonNodeFactory.instance.textNode("bar")).build();
    final Attribute newAttribute =
        AttributeBuilder.of()
            .name("foo")
            .value(JsonNodeFactory.instance.textNode("other-bar"))
            .build();
    final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition attributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(newAttribute.getName())
            .label(ofEnglish("foo"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(newAttribute.getName(), AttributeMetaData.of(attributeDefinition));

    // Test
    final Optional<ProductUpdateAction> actionOptional =
        ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction(
            variantId, oldAttribute, newAttribute, attributesMetaData);

    // Assertion
    assertThat(actionOptional)
        .contains(
            ProductSetAttributeActionBuilder.of()
                .variantId(variantId)
                .name(newAttribute.getName())
                .value(newAttribute.getValue())
                .staged(true)
                .build());
  }

  @Test
  void withSameValues_ShouldNotBuildAction() throws BuildUpdateActionException {
    // Preparation
    final Attribute oldAttribute =
        AttributeBuilder.of().name("foo").value(JsonNodeFactory.instance.textNode("foo")).build();
    final Attribute newAttribute =
        AttributeBuilder.of().name("foo").value(JsonNodeFactory.instance.textNode("foo")).build();
    final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition attributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(newAttribute.getName())
            .label(ofEnglish("foo"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(newAttribute.getName(), AttributeMetaData.of(attributeDefinition));

    // Test
    final Optional<ProductUpdateAction> actionOptional =
        ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction(
            1, oldAttribute, newAttribute, attributesMetaData);

    // Assertion
    assertThat(actionOptional).isEmpty();
  }

  @Test
  void withDifferentValues_WithNoExistingAttributeInMetaData_ShouldThrowException() {
    // Preparation
    final Attribute oldAttribute =
        AttributeBuilder.of().name("foo").value(JsonNodeFactory.instance.textNode("bar")).build();
    final Attribute newAttribute =
        AttributeBuilder.of()
            .name("foo")
            .value(JsonNodeFactory.instance.textNode("other-bar"))
            .build();
    final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();

    // Test and assertion
    assertThatThrownBy(
            () ->
                ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction(
                    1, oldAttribute, newAttribute, attributesMetaData))
        .hasMessage(
            String.format(
                ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA,
                newAttribute.getName()))
        .isExactlyInstanceOf(BuildUpdateActionException.class);
  }

  @Test
  void withSameValues_WithNoExistingAttributeInMetaData_ShouldThrowException() {
    // Preparation
    final Attribute oldAttribute =
        AttributeBuilder.of().name("foo").value(JsonNodeFactory.instance.textNode("foo")).build();
    final Attribute newAttribute =
        AttributeBuilder.of().name("foo").value(JsonNodeFactory.instance.textNode("foo")).build();
    final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();

    // Test and assertion
    assertThatThrownBy(
            () ->
                ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction(
                    1, oldAttribute, newAttribute, attributesMetaData))
        .hasMessage(
            String.format(
                ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA,
                newAttribute.getName()))
        .isExactlyInstanceOf(BuildUpdateActionException.class);
  }
}
