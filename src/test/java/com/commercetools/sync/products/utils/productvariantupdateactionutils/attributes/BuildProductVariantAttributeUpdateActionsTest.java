package com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.products.AttributeMetaData;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.products.commands.updateactions.SetAttribute;
import io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.commercetools.sync.products.utils.ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA;
import static com.commercetools.sync.products.utils.ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuildProductVariantAttributeUpdateActionsTest {

    @Test
    void withNullOldAndNonNullNew_ShouldBuildSetAction() throws BuildUpdateActionException {

        // Preparation
        final int variantId = 1;
        final Attribute oldAttribute = null;
        final AttributeDraft newAttribute = AttributeDraft.of("foo", JsonNodeFactory.instance.objectNode());
        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeDefinition attributeDefinition =
            AttributeDefinitionBuilder.of(newAttribute.getName(), ofEnglish("foo"), StringAttributeType.of())
                                      .build();
        attributesMetaData.put(newAttribute.getName(), AttributeMetaData.of(attributeDefinition));

        // Test
        final Optional<UpdateAction<Product>> actionOptional = buildProductVariantAttributeUpdateAction(variantId,
            oldAttribute, newAttribute, attributesMetaData);

        // Assertion
        assertThat(actionOptional)
            .contains(SetAttribute.of(variantId, newAttribute.getName(), newAttribute.getValue(), true));
    }

    @Test
    void withNullOldAndNonNullNew_WithSameForAllAttribute_ShouldBuildSetAllAction()
        throws BuildUpdateActionException {

        // Preparation
        final Attribute oldAttribute = null;
        final AttributeDraft newAttribute = AttributeDraft.of("foo", JsonNodeFactory.instance.objectNode());
        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeDefinition attributeDefinition =
            AttributeDefinitionBuilder.of(newAttribute.getName(), ofEnglish("foo"), StringAttributeType.of())
                                      .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
                                      .build();
        attributesMetaData.put(newAttribute.getName(), AttributeMetaData.of(attributeDefinition));

        // Test
        final Optional<UpdateAction<Product>> actionOptional = buildProductVariantAttributeUpdateAction(1,
            oldAttribute, newAttribute, attributesMetaData);

        // Assertion
        assertThat(actionOptional)
            .contains(SetAttributeInAllVariants.of(newAttribute.getName(), newAttribute.getValue(), true));
    }

    @Test
    void withNullOldAndNonNullNew_WithNoExistingAttributeInMetaData_ShouldThrowException() {

        // Preparation
        final Attribute oldAttribute = null;
        final AttributeDraft newAttribute = AttributeDraft.of("foo", JsonNodeFactory.instance.objectNode());
        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();

        // Test and assertion
        assertThatThrownBy(() -> buildProductVariantAttributeUpdateAction(1, oldAttribute, newAttribute,
            attributesMetaData))
            .hasMessage(format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, newAttribute.getName()))
            .isExactlyInstanceOf(BuildUpdateActionException.class);
    }

    @Test
    void withDifferentValues_ShouldBuildSetAction() throws BuildUpdateActionException {
        // Preparation
        final Integer variantId = 1;
        final Attribute oldAttribute = Attribute.of("foo", JsonNodeFactory.instance.textNode("bar"));
        final AttributeDraft newAttribute = AttributeDraft.of("foo", JsonNodeFactory.instance.textNode("other-bar"));
        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeDefinition attributeDefinition =
            AttributeDefinitionBuilder.of(newAttribute.getName(), ofEnglish("foo"), StringAttributeType.of())
                                      .build();
        attributesMetaData.put(newAttribute.getName(), AttributeMetaData.of(attributeDefinition));

        // Test
        final Optional<UpdateAction<Product>> actionOptional = buildProductVariantAttributeUpdateAction(variantId,
            oldAttribute, newAttribute, attributesMetaData);

        // Assertion
        assertThat(actionOptional)
            .contains(SetAttribute.of(variantId, newAttribute.getName(), newAttribute.getValue(), true));
    }

    @Test
    void withSameValues_ShouldNotBuildAction() throws BuildUpdateActionException {
        // Preparation
        final Attribute oldAttribute = Attribute.of("foo", JsonNodeFactory.instance.textNode("foo"));
        final AttributeDraft newAttribute = AttributeDraft.of("foo", JsonNodeFactory.instance.textNode("foo"));
        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeDefinition attributeDefinition =
            AttributeDefinitionBuilder.of(newAttribute.getName(), ofEnglish("foo"), StringAttributeType.of())
                                      .build();
        attributesMetaData.put(newAttribute.getName(), AttributeMetaData.of(attributeDefinition));

        // Test
        final Optional<UpdateAction<Product>> actionOptional = buildProductVariantAttributeUpdateAction(1, oldAttribute,
            newAttribute, attributesMetaData);

        // Assertion
        assertThat(actionOptional).isEmpty();
    }

    @Test
    void withDifferentValues_WithNoExistingAttributeInMetaData_ShouldThrowException() {
        // Preparation
        final Attribute oldAttribute = Attribute.of("foo", JsonNodeFactory.instance.textNode("bar"));
        final AttributeDraft newAttribute = AttributeDraft.of("foo", JsonNodeFactory.instance.textNode("other-bar"));
        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();

        // Test and assertion
        assertThatThrownBy(() -> buildProductVariantAttributeUpdateAction(1, oldAttribute, newAttribute,
            attributesMetaData))
            .hasMessage(format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, newAttribute.getName()))
            .isExactlyInstanceOf(BuildUpdateActionException.class);
    }

    @Test
    void withSameValues_WithNoExistingAttributeInMetaData_ShouldThrowException() {
        // Preparation
        final Attribute oldAttribute = Attribute.of("foo", JsonNodeFactory.instance.textNode("foo"));
        final AttributeDraft newAttribute = AttributeDraft.of("foo", JsonNodeFactory.instance.textNode("foo"));
        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();

        // Test and assertion
        assertThatThrownBy(() -> buildProductVariantAttributeUpdateAction(1, oldAttribute, newAttribute,
            attributesMetaData))
            .hasMessage(format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, newAttribute.getName()))
            .isExactlyInstanceOf(BuildUpdateActionException.class);
    }
}
