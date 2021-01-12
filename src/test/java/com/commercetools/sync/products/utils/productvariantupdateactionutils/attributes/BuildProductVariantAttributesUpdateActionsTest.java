package com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes;

import static com.commercetools.sync.products.utils.ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.NULL_PRODUCT_VARIANT_ATTRIBUTE;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.BOOLEAN_ATTRIBUTE_FALSE;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.TEXT_ATTRIBUTE_BAR;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.TEXT_ATTRIBUTE_FOO;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.TIME_ATTRIBUTE_10_08_46;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.attributes.AttributeDraftBuilder;
import io.sphere.sdk.products.attributes.BooleanAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.products.attributes.TimeAttributeType;
import io.sphere.sdk.products.commands.updateactions.SetAttribute;
import io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BuildProductVariantAttributesUpdateActionsTest {

  private final Product oldProduct = mock(Product.class);
  private final ProductDraft newProductDraft = mock(ProductDraft.class);
  private final ProductVariant oldProductVariant = mock(ProductVariant.class);
  private final ProductVariantDraft newProductVariant = mock(ProductVariantDraft.class);
  private List<String> errorMessages;
  private final ProductSyncOptions syncOptions =
      ProductSyncOptionsBuilder.of(mock(SphereClient.class))
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
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
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
    final List<AttributeDraft> newAttributes =
        asList(AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(), null);
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            SetAttribute.ofUnsetAttribute(
                oldProductVariant.getId(), TEXT_ATTRIBUTE_BAR.getName(), true));
    assertThat(errorMessages)
        .containsExactly(
            format(
                FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                null,
                variantKey,
                productKey,
                NULL_PRODUCT_VARIANT_ATTRIBUTE));
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
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
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
    final List<AttributeDraft> newAttributes =
        asList(
            AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
            AttributeDraftBuilder.of(TEXT_ATTRIBUTE_BAR).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
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
    final List<AttributeDraft> newAttributes =
        asList(
            AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
            AttributeDraftBuilder.of(TEXT_ATTRIBUTE_BAR).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    when(oldProductVariant.getAttributes()).thenReturn(emptyList());
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            SetAttribute.of(oldProductVariant.getId(), newAttributes.get(0), true),
            SetAttribute.of(oldProductVariant.getId(), newAttributes.get(1), true));
  }

  @Test
  void withSomeNonChangedMatchingAttributesAndNewAttributes_ShouldBuildSetAttributeActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<AttributeDraft> newAttributes =
        asList(
            AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
            AttributeDraftBuilder.of(TEXT_ATTRIBUTE_BAR).build(),
            AttributeDraftBuilder.of(TIME_ATTRIBUTE_10_08_46).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    final AttributeDefinition timeAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TIME_ATTRIBUTE_10_08_46.getName(), ofEnglish("label"), TimeAttributeType.of())
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));
    attributesMetaData.put(
        TIME_ATTRIBUTE_10_08_46.getName(), AttributeMetaData.of(timeAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            SetAttribute.of(
                oldProductVariant.getId(),
                AttributeDraftBuilder.of(TIME_ATTRIBUTE_10_08_46).build(),
                true));
  }

  @Test
  void withNullNewAttributes_ShouldBuildUnSetAttributeActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    when(newProductVariant.getAttributes()).thenReturn(null);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            SetAttribute.ofUnsetAttribute(
                oldProductVariant.getId(), BOOLEAN_ATTRIBUTE_TRUE.getName(), true),
            SetAttribute.ofUnsetAttribute(
                oldProductVariant.getId(), TEXT_ATTRIBUTE_BAR.getName(), true));
  }

  @Test
  void withNullNewAttributes_WithSameForAllAttributes_ShouldBuildUnSetAllAttributeActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    when(newProductVariant.getAttributes()).thenReturn(null);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            SetAttributeInAllVariants.ofUnsetAttribute(BOOLEAN_ATTRIBUTE_TRUE.getName(), true),
            SetAttributeInAllVariants.ofUnsetAttribute(TEXT_ATTRIBUTE_BAR.getName(), true));
  }

  @Test
  void withEmptyNewAttributes_ShouldBuildUnSetAttributeActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    when(newProductVariant.getAttributes()).thenReturn(emptyList());
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            SetAttribute.ofUnsetAttribute(
                oldProductVariant.getId(), BOOLEAN_ATTRIBUTE_TRUE.getName(), true),
            SetAttribute.ofUnsetAttribute(
                oldProductVariant.getId(), TEXT_ATTRIBUTE_BAR.getName(), true));
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

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
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
            format(
                FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                TEXT_ATTRIBUTE_BAR.getName(),
                variantKey,
                productKey,
                format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, TEXT_ATTRIBUTE_BAR.getName())),
            format(
                FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                BOOLEAN_ATTRIBUTE_TRUE.getName(),
                variantKey,
                productKey,
                format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, BOOLEAN_ATTRIBUTE_TRUE.getName())));
  }

  @Test
  void withSomeNonChangedMatchingAttributesAndNoNewAttributes_ShouldBuildUnSetAttributeActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<AttributeDraft> newAttributes =
        singletonList(AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            SetAttribute.ofUnsetAttribute(
                oldProductVariant.getId(), TEXT_ATTRIBUTE_BAR.getName(), true));
  }

  @Test
  void withNoMatchingAttributes_ShouldBuildUnsetAndSetActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<AttributeDraft> newAttributes =
        singletonList(AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = singletonList(TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            SetAttribute.ofUnsetAttribute(
                oldProductVariant.getId(), TEXT_ATTRIBUTE_BAR.getName(), true),
            SetAttribute.of(
                oldProductVariant.getId(),
                AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
                true));
  }

  @Test
  void
      withNoMatchingAttributes_WithSomeNoExistingMetaData_ShouldBuildSomeActionsAndTriggerErrorCallback() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<AttributeDraft> newAttributes =
        singletonList(AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = singletonList(TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            SetAttribute.ofUnsetAttribute(
                oldProductVariant.getId(), TEXT_ATTRIBUTE_BAR.getName(), true));
    assertThat(errorMessages)
        .containsExactly(
            format(
                FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                BOOLEAN_ATTRIBUTE_TRUE.getName(),
                variantKey,
                productKey,
                format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, BOOLEAN_ATTRIBUTE_TRUE.getName())));
  }

  @Test
  void
      withNoMatchingAttributes_WithNoExistingMetaData_ShouldBuildNoActionsAndTriggerErrorCallback() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<AttributeDraft> newAttributes =
        singletonList(AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = singletonList(TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
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
            format(
                FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                TEXT_ATTRIBUTE_BAR.getName(),
                variantKey,
                productKey,
                format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, TEXT_ATTRIBUTE_BAR.getName())),
            format(
                FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                BOOLEAN_ATTRIBUTE_TRUE.getName(),
                variantKey,
                productKey,
                format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, BOOLEAN_ATTRIBUTE_TRUE.getName())));
  }

  @Test
  void withAllChangedMatchingAttributes_ShouldBuildSetActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<AttributeDraft> newAttributes =
        asList(
            AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
            AttributeDraftBuilder.of(TEXT_ATTRIBUTE_FOO).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_FALSE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            SetAttribute.of(
                oldProductVariant.getId(),
                AttributeDraftBuilder.of(TEXT_ATTRIBUTE_FOO).build(),
                true),
            SetAttribute.of(
                oldProductVariant.getId(),
                AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
                true));
  }

  @Test
  void
      withAllChangedMatchingAttrs_WithSomeNoExistingMetaData_ShouldBuildSomeActionsAndTriggerErrorCallback() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<AttributeDraft> newAttributes =
        asList(
            AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
            AttributeDraftBuilder.of(TEXT_ATTRIBUTE_FOO).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_FALSE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            SetAttribute.of(
                oldProductVariant.getId(),
                AttributeDraftBuilder.of(TEXT_ATTRIBUTE_FOO).build(),
                true));
    assertThat(errorMessages)
        .containsExactly(
            format(
                FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                BOOLEAN_ATTRIBUTE_TRUE.getName(),
                variantKey,
                productKey,
                format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, BOOLEAN_ATTRIBUTE_TRUE.getName())));
  }

  @Test
  void withSomeChangedMatchingAttributes_ShouldBuildSetActions() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<AttributeDraft> newAttributes =
        asList(
            AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
            AttributeDraftBuilder.of(TEXT_ATTRIBUTE_FOO).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            SetAttribute.of(
                oldProductVariant.getId(),
                AttributeDraftBuilder.of(TEXT_ATTRIBUTE_FOO).build(),
                true));
  }

  @Test
  void
      withSomeChangedMatchingAttrs_WithNoExistingMetaData_ShouldBuildNoActionsAndTriggerErrorCallback() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<AttributeDraft> newAttributes =
        asList(
            AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
            AttributeDraftBuilder.of(TEXT_ATTRIBUTE_FOO).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
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
            format(
                FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                TEXT_ATTRIBUTE_BAR.getName(),
                variantKey,
                productKey,
                format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, TEXT_ATTRIBUTE_BAR.getName())),
            format(
                FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                BOOLEAN_ATTRIBUTE_TRUE.getName(),
                variantKey,
                productKey,
                format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, BOOLEAN_ATTRIBUTE_TRUE.getName())));
  }

  @Test
  void withNoMatchingAttributes_ShouldBuildUnsetAndSetActionsInCorrectOrder() {
    // Preparation
    final String productKey = "foo";
    final String variantKey = "foo";
    when(oldProduct.getKey()).thenReturn(productKey);
    final List<AttributeDraft> newAttributes =
        singletonList(AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build());
    when(newProductVariant.getAttributes()).thenReturn(newAttributes);
    when(newProductVariant.getKey()).thenReturn(variantKey);

    final List<Attribute> oldAttributes = singletonList(TEXT_ATTRIBUTE_BAR);
    when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
    when(oldProductVariant.getKey()).thenReturn(variantKey);

    final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeDefinition booleanAttributeDefinition =
        AttributeDefinitionBuilder.of(
                BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"), BooleanAttributeType.of())
            .build();
    final AttributeDefinition textAttributeDefinition =
        AttributeDefinitionBuilder.of(
                TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"), StringAttributeType.of())
            .build();
    attributesMetaData.put(
        BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
    attributesMetaData.put(
        TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

    // Test
    final List<UpdateAction<Product>> updateActions =
        buildProductVariantAttributesUpdateActions(
            oldProduct,
            newProductDraft,
            oldProductVariant,
            newProductVariant,
            attributesMetaData,
            syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            SetAttribute.ofUnsetAttribute(
                oldProductVariant.getId(), TEXT_ATTRIBUTE_BAR.getName(), true),
            SetAttribute.of(
                oldProductVariant.getId(),
                AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
                true));
  }
}
