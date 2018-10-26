package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.LocalizedEnumAttributeType;
import org.junit.Test;

import java.util.Collections;

import static com.commercetools.sync.producttypes.utils.AttributeDefinitionValidationUtils.areValid;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionValidationUtils.isValid;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;

public class AttributeDefinitionValidationUtilsTest {

    private static final LocalizedEnumValue LOCALIZED_ENUM_VALUE_A
        = LocalizedEnumValue.of("a", ofEnglish("label_a"));

    @Test
    public void areValid_WithCorrectAttributes_ShouldBeValid() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of(
                "attributeName1",
                LocalizedString.ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(Collections.emptyList()),
                "attributeName1",
                LocalizedString.ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(areValid(attributeDefinition, attributeDefinitionDraft)).isTrue();
    }

    @Test
    public void areValid_WithAttributeDefinitionWithoutName_ShouldNotBeValid() {
        final AttributeDefinition attributeDefinitionWithoutName = AttributeDefinitionBuilder
            .of(
                null,
                LocalizedString.ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(Collections.emptyList()),
                "attributeName1",
                LocalizedString.ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(areValid(attributeDefinitionWithoutName, attributeDefinitionDraft)).isFalse();
    }

    @Test
    public void areValid_WithAttributeDefinitionDraftWithBlankName_ShouldNotBeValid() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of(
                "attributeName1",
                LocalizedString.ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        final AttributeDefinitionDraft attributeDefinitionDraftWithBlankName = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(Collections.emptyList()),
                " ",
                LocalizedString.ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(areValid(attributeDefinition, attributeDefinitionDraftWithBlankName)).isFalse();
    }

    @Test
    public void areValid_WithAttributeDefinitionWithoutType_ShouldNotBeValid() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of(
                "attributeName1",
                LocalizedString.ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();
    }

    @Test
    public void areValid_WithAttributeDefinitionsWithDifferentNames_ShouldNotBeValid() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of(
                "attributeName1",
                LocalizedString.ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();


        final AttributeDefinitionDraft attributeDefinitionDraftWithDifferentName = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(Collections.emptyList()),
                "attributeName2",
                LocalizedString.ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(areValid(attributeDefinition, attributeDefinitionDraftWithDifferentName)).isFalse();
    }

    @Test
    public void isValid_WithCorrectAttributeDefinition_ShouldBeValid() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of(
                "attributeName1",
                LocalizedString.ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(isValid(attributeDefinition)).isTrue();
    }

    @Test
    public void isValid_WithAttributeDefinitionWithoutName_ShouldNotBeValid() {
        final AttributeDefinition attributeDefinitionWithoutName = AttributeDefinitionBuilder
            .of(
                null,
                LocalizedString.ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(isValid(attributeDefinitionWithoutName)).isFalse();
    }

    @Test
    public void isValid_WithAttributeDefinitionWithBlankName_ShouldNotBeValid() {
        final AttributeDefinition attributeDefinitionWithBlankName = AttributeDefinitionBuilder
            .of(
                "   ",
                LocalizedString.ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(isValid(attributeDefinitionWithBlankName)).isFalse();
    }

    @Test
    public void isValid_WithAttributeDefinitionWithoutType_ShouldNotBeValid() {
        final AttributeDefinition attributeDefinitionWithoutType = AttributeDefinitionBuilder
            .of(
                "attribute1",
                LocalizedString.ofEnglish("label1"),
                null)
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(isValid(attributeDefinitionWithoutType)).isFalse();
    }

    @Test
    public void isValid_WithCorrectAttributeDefinitionDraft_ShouldBeValid() {
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(Collections.emptyList()),
                "attributeName1",
                LocalizedString.ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(isValid(attributeDefinitionDraft)).isTrue();
    }

    @Test
    public void isValid_WithAttributeDefinitionDraftWithoutName_ShouldNotBeValid() {
        final AttributeDefinitionDraft attributeDefinitionDraftWithoutName = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(Collections.emptyList()),
                null,
                LocalizedString.ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(isValid(attributeDefinitionDraftWithoutName)).isFalse();
    }

    @Test
    public void isValid_WithAttributeDefinitionDraftWithBlankName_ShouldNotBeValid() {
        final AttributeDefinitionDraft attributeDefinitionDraftWithBlankName = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(Collections.emptyList()),
                "  ",
                LocalizedString.ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(isValid(attributeDefinitionDraftWithBlankName)).isFalse();
    }

    @Test
    public void isValid_WithAttributeDefinitionDraftWithoutType_ShouldNotBeValid() {
        final AttributeDefinitionDraft attributeDefinitionDraftWithoutType = AttributeDefinitionDraftBuilder
            .of(
                null,
                "attributeName1",
                LocalizedString.ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        assertThat(isValid(attributeDefinitionDraftWithoutType)).isFalse();
    }

}
