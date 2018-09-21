package com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes;

import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.attributes.AttributeDraftBuilder;
import io.sphere.sdk.products.attributes.BooleanAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.products.attributes.TimeAttributeType;
import io.sphere.sdk.products.commands.updateactions.SetAttribute;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.NULL_PRODUCT_VARIANT_ATTRIBUTE;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.TEXT_ATTRIBUTE_BAR;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.TIME_ATTRIBUTE_10_08_46;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildProductVariantAttributesUpdateActionsTest {



    private final ProductVariant oldProductVariant = mock(ProductVariant.class);
    private final ProductVariantDraft newProductVariant = mock(ProductVariantDraft.class);
    private List<String> errorMessages;
    private final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                            .errorCallback((msg, throwable) ->
                                                                                errorMessages.add(msg))
                                                                            .build();

    @Before
    public void setupMethod() {
        errorMessages = new ArrayList<>();
    }

    @Test
    public void withNullNewAttributesAndEmptyExistingAttributes_ShouldNotBuildActions() {
        // Preparation
        when(newProductVariant.getAttributes()).thenReturn(null);
        when(oldProductVariant.getAttributes()).thenReturn(emptyList());

        // Test
        final List<UpdateAction<Product>> updateActions = buildProductVariantAttributesUpdateActions("foo",
            oldProductVariant, newProductVariant, new HashMap<>(), syncOptions);

        // Assertion
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void withSomeNullNewAttributesAndExistingAttributes_ShouldBuildActionsAndTriggerErrorCallback() {
        // Preparation
        final String productKey = "foo";
        final String variantKey = "foo";
        final List<AttributeDraft> newAttributes = asList(
            AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(), null);
        when(newProductVariant.getAttributes()).thenReturn(newAttributes);
        when(newProductVariant.getKey()).thenReturn(variantKey);

        final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
        when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
        when(oldProductVariant.getKey()).thenReturn(variantKey);

        final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeDefinition attributeDefinition =
            AttributeDefinitionBuilder.of(BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"),
                BooleanAttributeType.of()).build();
        attributesMetaData.put(BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(attributeDefinition));

        // Test
        final List<UpdateAction<Product>> updateActions = buildProductVariantAttributesUpdateActions(productKey,
            oldProductVariant, newProductVariant, attributesMetaData, syncOptions);

        // Assertion
        assertThat(updateActions).containsExactly(SetAttribute.ofUnsetAttribute(oldProductVariant.getId(),
            TEXT_ATTRIBUTE_BAR.getName(), true));
        assertThat(errorMessages).containsExactly(format(FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION, null, productKey,
            variantKey, NULL_PRODUCT_VARIANT_ATTRIBUTE));
    }

    @Test
    public void withEmptyNewAttributesAndEmptyExistingAttributes_ShouldNotBuildActions() {
        // Preparation
        final String productKey = "foo";
        final String variantKey = "foo";
        when(oldProductVariant.getAttributes()).thenReturn(emptyList());
        when(oldProductVariant.getKey()).thenReturn(variantKey);
        when(newProductVariant.getAttributes()).thenReturn(emptyList());
        when(newProductVariant.getKey()).thenReturn(variantKey);

        // Test
        final List<UpdateAction<Product>> updateActions = buildProductVariantAttributesUpdateActions(productKey,
            oldProductVariant, newProductVariant, new HashMap<>(), syncOptions);

        // Assertion
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void withAllMatchingAttributes_ShouldNotBuildActions() {
        // Preparation
        final String productKey = "foo";
        final String variantKey = "foo";
        final List<AttributeDraft> newAttributes = asList(
            AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
            AttributeDraftBuilder.of(TEXT_ATTRIBUTE_BAR).build());
        when(newProductVariant.getAttributes()).thenReturn(newAttributes);
        when(newProductVariant.getKey()).thenReturn(variantKey);

        final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
        when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
        when(oldProductVariant.getKey()).thenReturn(variantKey);

        final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeDefinition booleanAttributeDefinition =
            AttributeDefinitionBuilder.of(BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"),
                BooleanAttributeType.of()).build();
        final AttributeDefinition textAttributeDefinition =
            AttributeDefinitionBuilder.of(TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"),
                StringAttributeType.of()).build();
        attributesMetaData.put(BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
        attributesMetaData.put(TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

        // Test
        final List<UpdateAction<Product>> updateActions = buildProductVariantAttributesUpdateActions(productKey,
            oldProductVariant, newProductVariant, attributesMetaData, syncOptions);

        // Assertion
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void withNonEmptyNewAttributesButEmptyExistingAttributes_ShouldBuildSetAttributeActions() {
        // Preparation
        final String productKey = "foo";
        final String variantKey = "foo";
        final List<AttributeDraft> newAttributes = asList(
            AttributeDraftBuilder.of(BOOLEAN_ATTRIBUTE_TRUE).build(),
            AttributeDraftBuilder.of(TEXT_ATTRIBUTE_BAR).build());
        when(newProductVariant.getAttributes()).thenReturn(newAttributes);
        when(newProductVariant.getKey()).thenReturn(variantKey);

        when(oldProductVariant.getAttributes()).thenReturn(emptyList());
        when(oldProductVariant.getKey()).thenReturn(variantKey);

        final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeDefinition booleanAttributeDefinition =
            AttributeDefinitionBuilder.of(BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"),
                BooleanAttributeType.of()).build();
        final AttributeDefinition textAttributeDefinition =
            AttributeDefinitionBuilder.of(TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"),
                StringAttributeType.of()).build();
        attributesMetaData.put(BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
        attributesMetaData.put(TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

        // Test
        final List<UpdateAction<Product>> updateActions = buildProductVariantAttributesUpdateActions(productKey,
            oldProductVariant, newProductVariant, attributesMetaData, syncOptions);


        // Assertion
        assertThat(updateActions).containsExactlyInAnyOrder(
            SetAttribute.of(oldProductVariant.getId(), newAttributes.get(0), true),
            SetAttribute.of(oldProductVariant.getId(), newAttributes.get(1), true)
        );
    }

    @Test
    public void withSomeNonChangedMatchingAttributesAndNewAttributes_ShouldBuildSetAttributeActions()  {
        // Preparation
        final String productKey = "foo";
        final String variantKey = "foo";
        final List<AttributeDraft> newAttributes = asList(
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
            AttributeDefinitionBuilder.of(BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"),
                BooleanAttributeType.of()).build();
        final AttributeDefinition textAttributeDefinition =
            AttributeDefinitionBuilder.of(TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"),
                StringAttributeType.of()).build();
        final AttributeDefinition timeAttributeDefinition =
            AttributeDefinitionBuilder.of(TIME_ATTRIBUTE_10_08_46.getName(), ofEnglish("label"),
                TimeAttributeType.of()).build();
        attributesMetaData.put(BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
        attributesMetaData.put(TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));
        attributesMetaData.put(TIME_ATTRIBUTE_10_08_46.getName(), AttributeMetaData.of(timeAttributeDefinition));

        // Test
        final List<UpdateAction<Product>> updateActions = buildProductVariantAttributesUpdateActions(productKey,
            oldProductVariant, newProductVariant, attributesMetaData, syncOptions);

        // Assertion
        assertThat(updateActions).containsExactly(
            SetAttribute.of(oldProductVariant.getId(), AttributeDraftBuilder.of(TIME_ATTRIBUTE_10_08_46).build(), true)
        );
    }

    @Test
    public void withNullNewAttributes_ShouldBuildUnSetAttributeActions() {
        // Preparation
        final String productKey = "foo";
        final String variantKey = "foo";
        when(newProductVariant.getAttributes()).thenReturn(null);
        when(newProductVariant.getKey()).thenReturn(variantKey);

        final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
        when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
        when(oldProductVariant.getKey()).thenReturn(variantKey);

        final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeDefinition booleanAttributeDefinition =
            AttributeDefinitionBuilder.of(BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"),
                BooleanAttributeType.of()).build();
        final AttributeDefinition textAttributeDefinition =
            AttributeDefinitionBuilder.of(TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"),
                StringAttributeType.of()).build();
        attributesMetaData.put(BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
        attributesMetaData.put(TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

        // Test
        final List<UpdateAction<Product>> updateActions = buildProductVariantAttributesUpdateActions(productKey,
            oldProductVariant, newProductVariant, attributesMetaData, syncOptions);

        // Assertion
        assertThat(updateActions).containsExactly(
            SetAttribute.ofUnsetAttribute(oldProductVariant.getId(), BOOLEAN_ATTRIBUTE_TRUE.getName(), true),
            SetAttribute.ofUnsetAttribute(oldProductVariant.getId(), TEXT_ATTRIBUTE_BAR.getName(), true)
        );
    }

    @Test
    public void withEmptyNewAttributes_ShouldBuildUnSetAttributeActions() {
        // Preparation
        final String productKey = "foo";
        final String variantKey = "foo";
        when(newProductVariant.getAttributes()).thenReturn(emptyList());
        when(newProductVariant.getKey()).thenReturn(variantKey);

        final List<Attribute> oldAttributes = asList(BOOLEAN_ATTRIBUTE_TRUE, TEXT_ATTRIBUTE_BAR);
        when(oldProductVariant.getAttributes()).thenReturn(oldAttributes);
        when(oldProductVariant.getKey()).thenReturn(variantKey);

        final HashMap<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeDefinition booleanAttributeDefinition =
            AttributeDefinitionBuilder.of(BOOLEAN_ATTRIBUTE_TRUE.getName(), ofEnglish("label"),
                BooleanAttributeType.of()).build();
        final AttributeDefinition textAttributeDefinition =
            AttributeDefinitionBuilder.of(TEXT_ATTRIBUTE_BAR.getName(), ofEnglish("label"),
                StringAttributeType.of()).build();
        attributesMetaData.put(BOOLEAN_ATTRIBUTE_TRUE.getName(), AttributeMetaData.of(booleanAttributeDefinition));
        attributesMetaData.put(TEXT_ATTRIBUTE_BAR.getName(), AttributeMetaData.of(textAttributeDefinition));

        // Test
        final List<UpdateAction<Product>> updateActions = buildProductVariantAttributesUpdateActions(productKey,
            oldProductVariant, newProductVariant, attributesMetaData, syncOptions);

        // Assertion
        assertThat(updateActions).containsExactly(
            SetAttribute.ofUnsetAttribute(oldProductVariant.getId(), BOOLEAN_ATTRIBUTE_TRUE.getName(), true),
            SetAttribute.ofUnsetAttribute(oldProductVariant.getId(), TEXT_ATTRIBUTE_BAR.getName(), true)
        );
    }

}
