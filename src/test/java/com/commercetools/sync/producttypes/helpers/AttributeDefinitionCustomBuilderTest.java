package com.commercetools.sync.producttypes.helpers;

import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.MoneyAttributeType;
import org.junit.Test;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;

public class AttributeDefinitionCustomBuilderTest {

    @Test
    public void of_withNullRequiredValues_ShouldCreateAttributeDefinitionWithNullRequiredValues() {
        // preparation
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(null, null, null, true)
            .build();

        // test
        final AttributeDefinition attributeDefinition = AttributeDefinitionCustomBuilder.of(attributeDefinitionDraft);

        // assertions
        assertThat(attributeDefinition).isNotNull();
        assertThat(attributeDefinition.getAttributeType()).isNull();
        assertThat(attributeDefinition.getName()).isNull();
        assertThat(attributeDefinition.getLabel()).isNull();
    }

    @Test
    public void of_withNonNullRequiredValues_ShouldCreateAttributeDefinitionWithRequiredValues() {
        // preparation
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(MoneyAttributeType.of(), "foo", ofEnglish("bar"), true)
            .build();

        // test
        final AttributeDefinition attributeDefinition = AttributeDefinitionCustomBuilder.of(attributeDefinitionDraft);

        // assertions
        assertThat(attributeDefinition).isNotNull();
        assertThat(attributeDefinition.getAttributeType()).isEqualTo(MoneyAttributeType.of());
        assertThat(attributeDefinition.getName()).isEqualTo("foo");
        assertThat(attributeDefinition.getLabel()).isEqualTo(ofEnglish("bar"));
        assertThat(attributeDefinition.isRequired()).isTrue();
    }

    @Test
    public void of_withAllValuesSet_ShouldCreateAttributeDefinitionWithAllValuesSet() {
        // preparation
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(MoneyAttributeType.of(), "foo", ofEnglish("bar"), true)
            .searchable(true)
            .attributeConstraint(AttributeConstraint.COMBINATION_UNIQUE)
            .inputTip(ofEnglish("foo"))
            .inputHint(TextInputHint.MULTI_LINE)
            .build();

        // test
        final AttributeDefinition attributeDefinition = AttributeDefinitionCustomBuilder.of(attributeDefinitionDraft);

        // assertions
        assertThat(attributeDefinition).isNotNull();
        assertThat(attributeDefinition.getAttributeType()).isEqualTo(MoneyAttributeType.of());
        assertThat(attributeDefinition.getName()).isEqualTo("foo");
        assertThat(attributeDefinition.getLabel()).isEqualTo(ofEnglish("bar"));
        assertThat(attributeDefinition.isRequired()).isTrue();
        assertThat(attributeDefinition.isSearchable()).isTrue();
        assertThat(attributeDefinition.getAttributeConstraint()).isEqualTo(AttributeConstraint.COMBINATION_UNIQUE);
        assertThat(attributeDefinition.getInputTip()).isEqualTo(ofEnglish("foo"));
        assertThat(attributeDefinition.getInputHint()).isEqualTo(TextInputHint.MULTI_LINE);
    }

    @Test
    public void of_withNullIsRequiredValue_ShouldCreateAttributeDefinitionWithNullIsRequiredValue() {
        // preparation
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(MoneyAttributeType.of(), "foo", ofEnglish("bar"), null)
            .build();

        // test
        final AttributeDefinition attributeDefinition = AttributeDefinitionCustomBuilder.of(attributeDefinitionDraft);

        // assertions
        assertThat(attributeDefinition).isNotNull();
        assertThat(attributeDefinition.isRequired()).isFalse();
    }

    @Test
    public void of_withNullAttributeConstraint_ShouldCreateAttributeDefinitionWithNoneConstraint() {
        // preparation
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(MoneyAttributeType.of(), "foo", ofEnglish("bar"), true)
            .build();

        // test
        final AttributeDefinition attributeDefinition = AttributeDefinitionCustomBuilder.of(attributeDefinitionDraft);

        // assertions
        assertThat(attributeDefinition).isNotNull();
        assertThat(attributeDefinition.getAttributeConstraint()).isEqualTo(AttributeConstraint.NONE);
    }

    @Test
    public void of_withNullInputTip_ShouldCreateAttributeDefinitionWithNullInputTip() {
        // preparation
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(MoneyAttributeType.of(), "foo", ofEnglish("bar"), true)
            .build();

        // test
        final AttributeDefinition attributeDefinition = AttributeDefinitionCustomBuilder.of(attributeDefinitionDraft);

        // assertions
        assertThat(attributeDefinition).isNotNull();
        assertThat(attributeDefinition.getInputTip()).isNull();
    }

    @Test
    public void of_withNullInputHint_ShouldCreateAttributeDefinitionWithSingleLineHint() {
        // preparation
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(MoneyAttributeType.of(), "foo", ofEnglish("bar"), true)
            .build();

        // test
        final AttributeDefinition attributeDefinition = AttributeDefinitionCustomBuilder.of(attributeDefinitionDraft);

        // assertions
        assertThat(attributeDefinition).isNotNull();
        assertThat(attributeDefinition.getInputHint()).isEqualTo(TextInputHint.SINGLE_LINE);
    }

    @Test
    public void of_withNullSearchable_ShouldCreateAttributeDefinitionWithFalseSearchable() {
        // preparation
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(MoneyAttributeType.of(), "foo", ofEnglish("bar"), true)
            .build();

        // test
        final AttributeDefinition attributeDefinition = AttributeDefinitionCustomBuilder.of(attributeDefinitionDraft);

        // assertions
        assertThat(attributeDefinition).isNotNull();
        assertThat(attributeDefinition.isSearchable()).isFalse();
    }
}