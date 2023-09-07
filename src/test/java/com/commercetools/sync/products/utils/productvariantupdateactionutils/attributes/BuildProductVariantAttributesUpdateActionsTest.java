package com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductSetAttributeActionBuilder;
import com.commercetools.api.models.product.ProductSetAttributeInAllVariantsActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product_type.AttributeBooleanTypeBuilder;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionBuilder;
import com.commercetools.api.models.product_type.AttributeTextTypeBuilder;
import com.commercetools.api.models.product_type.AttributeTimeTypeBuilder;
import com.commercetools.api.models.product_type.TextInputHint;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.utils.ProductVariantAttributeUpdateActionUtils;
import com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BuildProductVariantAttributesUpdateActionsTest {

  private final ProductProjection oldProduct = mock(ProductProjection.class);
  private final ProductDraft newProductDraft = mock(ProductDraft.class);
  private final ProductVariant oldProductVariant = mock(ProductVariant.class);
  private final ProductVariantDraft newProductVariant = mock(ProductVariantDraft.class);
  private List<String> errorMessages;
  private final ProductSyncOptions syncOptions =
      ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
          .errorCallback(
              (exception, oldResource, newResource, updateActions) ->
                  errorMessages.add(exception.getMessage()))
          .build();

  @BeforeEach
  void setupMethod() {
    errorMessages = new ArrayList<>();
  }

  @Test
  void withNullNewAttributesAndEmptyExistingAttributes_ShouldNotBuildActions() {
    // Preparation
    when(newProductVariant.getAttributes()).thenReturn(null);
    when(oldProductVariant.getAttributes()).thenReturn(emptyList());

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            new HashMap<>(),
            syncOptions);

    // Assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void withSomeNullNewAttributesAndExistingAttributes_ShouldBuildActionsAndTriggerErrorCallback() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes = asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, null);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
                .staged(true)
                .build());
    assertThat(errorMessages)
        .containsExactly(
            String.format(
                ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                null,
                variantKey,
                productKey,
                ProductVariantUpdateActionUtils.NULL_PRODUCT_VARIANT_ATTRIBUTE));
  }

  @Test
  void withEmptyNewAttributesAndEmptyExistingAttributes_ShouldNotBuildActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    when(oldProductVariant.getAttributes()).thenReturn(emptyList());
    when(oldProductVariant.getKey()).thenReturn(variantKey);
    when(newProductVariant.getAttributes()).thenReturn(emptyList());
    when(newProductVariant.getKey()).thenReturn(variantKey);

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            new HashMap<>(),
            syncOptions);

    // Assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void withAllMatchingAttributes_ShouldNotBuildActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void withNonEmptyNewAttributesButEmptyExistingAttributes_ShouldBuildSetAttributeActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    when(oldProductVariant.getAttributes()).thenReturn(emptyList());
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(newAttributes.get(0).getName())
                .value(newAttributes.get(0).getValue())
                .staged(true)
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(newAttributes.get(1).getName())
                .value(newAttributes.get(1).getValue())
                .staged(true)
                .build());
  }

  @Test
  void withSomeNonChangedMatchingAttributesAndNewAttributes_ShouldBuildSetAttributeActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes =
        asList(
            AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE,
            AttributeFixtures.TEXT_ATTRIBUTE_BAR,
            AttributeFixtures.TIME_ATTRIBUTE_10_08_46);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition timeAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TIME_ATTRIBUTE_10_08_46.getName())
            .label(ofEnglish("label"))
            .type(AttributeTimeTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TIME_ATTRIBUTE_10_08_46.getName(),
        AttributeMetaData.of(timeAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.TIME_ATTRIBUTE_10_08_46.getName())
                .value(AttributeFixtures.TIME_ATTRIBUTE_10_08_46.getValue())
                .staged(true)
                .build());
  }

  @Test
  void withNullNewAttributes_ShouldBuildUnSetAttributeActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    when(newProductVariant.getAttributes()).thenReturn(null);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
                .staged(true)
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
                .staged(true)
                .build());
  }

  @Test
  void withNullNewAttributes_WithSameForAllAttributes_ShouldBuildUnSetAllAttributeActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    when(newProductVariant.getAttributes()).thenReturn(null);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
                .staged(true)
                .build());
  }

  @Test
  void withEmptyNewAttributes_ShouldBuildUnSetAttributeActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    when(newProductVariant.getAttributes()).thenReturn(emptyList());
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
                .staged(true)
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
                .staged(true)
                .build());
  }

  @Test
  void
      withEmptyNewAttributes_WithNoExistingAttributeInMetaData_ShouldBuildNoActionsAndTriggerErrorCallback() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    when(newProductVariant.getAttributes()).thenReturn(emptyList());
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions).isEmpty();
    assertThat(errorMessages)
        .containsExactlyInAnyOrder(
            String.format(
                ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
                variantKey,
                productKey,
                String.format(
                    ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA,
                    AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())),
            String.format(
                ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
                variantKey,
                productKey,
                String.format(
                    ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA,
                    AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())));
  }

  @Test
  void withSomeNonChangedMatchingAttributesAndNoNewAttributes_ShouldBuildUnSetAttributeActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes = singletonList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
                .staged(true)
                .build());
  }

  @Test
  void withNoMatchingAttributes_ShouldBuildUnsetAndSetActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes = singletonList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = singletonList(AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
                .staged(true)
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
                .value(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getValue())
                .staged(true)
                .build());
  }

  @Test
  void
      withNoMatchingAttributes_WithSomeNoExistingMetaData_ShouldBuildSomeActionsAndTriggerErrorCallback() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes = singletonList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = singletonList(AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
                .staged(true)
                .build());
    assertThat(errorMessages)
        .containsExactly(
            String.format(
                ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
                variantKey,
                productKey,
                String.format(
                    ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA,
                    AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())));
  }

  @Test
  void
      withNoMatchingAttributes_WithNoExistingMetaData_ShouldBuildNoActionsAndTriggerErrorCallback() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes = singletonList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = singletonList(AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions).isEmpty();
    assertThat(errorMessages)
        .containsExactlyInAnyOrder(
            String.format(
                ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
                variantKey,
                productKey,
                String.format(
                    ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA,
                    AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())),
            String.format(
                ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
                variantKey,
                productKey,
                String.format(
                    ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA,
                    AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())));
  }

  @Test
  void withAllChangedMatchingAttributes_ShouldBuildSetActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_FOO);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_FALSE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.TEXT_ATTRIBUTE_FOO.getName())
                .value(AttributeFixtures.TEXT_ATTRIBUTE_FOO.getValue())
                .staged(true)
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
                .value(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getValue())
                .staged(true)
                .build());
  }

  @Test
  void
      withAllChangedMatchingAttrs_WithSomeNoExistingMetaData_ShouldBuildSomeActionsAndTriggerErrorCallback() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_FOO);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_FALSE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.TEXT_ATTRIBUTE_FOO.getName())
                .value(AttributeFixtures.TEXT_ATTRIBUTE_FOO.getValue())
                .staged(true)
                .build());
    assertThat(errorMessages)
        .containsExactly(
            String.format(
                ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
                variantKey,
                productKey,
                String.format(
                    ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA,
                    AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())));
  }

  @Test
  void withSomeChangedMatchingAttributes_ShouldBuildSetActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_FOO);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.TEXT_ATTRIBUTE_FOO.getName())
                .value(AttributeFixtures.TEXT_ATTRIBUTE_FOO.getValue())
                .staged(true)
                .build());
  }

  @Test
  void
      withSomeChangedMatchingAttrs_WithNoExistingMetaData_ShouldBuildNoActionsAndTriggerErrorCallback() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_FOO);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes =
        asList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE, AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions).isEmpty();
    assertThat(errorMessages)
        .containsExactlyInAnyOrder(
            String.format(
                ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
                variantKey,
                productKey,
                String.format(
                    ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA,
                    AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())),
            String.format(
                ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
                variantKey,
                productKey,
                String.format(
                    ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA,
                    AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())));
  }

  @Test
  void withNoMatchingAttributes_ShouldBuildUnsetAndSetActionsInCorrectOrder() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<Attribute> newAttributes = singletonList(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = singletonList(AttributeFixtures.TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
            .label(ofEnglish("label"))
            .type(AttributeBooleanTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of()
            .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
            .label(ofEnglish("label"))
            .type(AttributeTextTypeBuilder.of().build())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(true)
            .isRequired(false)
            .build();
    attributesMetaData.put(
        AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName(),
        AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName(),
        AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<ProductUpdateAction> updateActions =
        ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.TEXT_ATTRIBUTE_BAR.getName())
                .staged(true)
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .name(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getName())
                .value(AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE.getValue())
                .staged(true)
                .build());
  }
}
