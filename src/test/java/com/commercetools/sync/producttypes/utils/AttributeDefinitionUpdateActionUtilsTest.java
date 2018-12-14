package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.EnumAttributeType;
import io.sphere.sdk.products.attributes.LocalizedEnumAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeConstraint;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeDefinitionLabel;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeInputHint;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeIsSearchable;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.ChangePlainEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveEnumValues;
import io.sphere.sdk.producttypes.commands.updateactions.SetInputTip;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildActions;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildChangeAttributeConstraintUpdateAction;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildChangeInputHintUpdateAction;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildChangeIsSearchableUpdateAction;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildChangeLabelUpdateAction;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildSetInputTipUpdateAction;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class AttributeDefinitionUpdateActionUtilsTest {
    private static AttributeDefinition old;
    private static AttributeDefinition oldNullValues;
    private static AttributeDefinitionDraft newSame;
    private static AttributeDefinitionDraft newDifferent;
    private static AttributeDefinitionDraft newNullValues;

    private static final EnumValue ENUM_VALUE_A = EnumValue.of("a", "label_a");
    private static final EnumValue ENUM_VALUE_B = EnumValue.of("b", "label_b");

    private static final LocalizedEnumValue LOCALIZED_ENUM_VALUE_A = LocalizedEnumValue.of("a", ofEnglish("label_a"));
    private static final LocalizedEnumValue LOCALIZED_ENUM_VALUE_B = LocalizedEnumValue.of("b", ofEnglish("label_b"));

    /**
     * Initialises test data.
     */
    @BeforeClass
    public static void setup() {
        old = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"), StringAttributeType.of())
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        oldNullValues = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"), StringAttributeType.of())
            .isRequired(false)
            .attributeConstraint(null)
            .inputTip(null)
            .inputHint(null)
            .isSearchable(false)
            .build();

        newSame = AttributeDefinitionDraftBuilder
            .of(old)
            .build();

        newDifferent = AttributeDefinitionDraftBuilder
            .of(StringAttributeType.of(), "attributeName1", ofEnglish("label2"), true)
            .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
            .inputTip(ofEnglish("inputTip2"))
            .inputHint(TextInputHint.MULTI_LINE)
            .isSearchable(true)
            .build();

        newNullValues = AttributeDefinitionDraftBuilder
            .of(StringAttributeType.of(), "attributeName1", LocalizedString.ofEnglish("label2"), true)
            .attributeConstraint(null)
            .inputTip(null)
            .inputHint(null)
            .isSearchable(true)
            .build();
    }

    @Test
    public void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildChangeLabelUpdateAction(old, newDifferent);

        assertThat(result).contains(ChangeAttributeDefinitionLabel.of(old.getName(), newDifferent.getLabel()));
    }

    @Test
    public void buildChangeLabelAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<ProductType>> result = buildChangeLabelUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildSetInputTipAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildSetInputTipUpdateAction(old, newDifferent);

        assertThat(result).contains(SetInputTip.of(old.getName(), newDifferent.getInputTip()));
    }

    @Test
    public void buildSetInputTipAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<ProductType>> result = buildSetInputTipUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildSetInputTipAction_WithSourceNullValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildSetInputTipUpdateAction(oldNullValues, newDifferent);

        assertThat(result).contains(SetInputTip.of(oldNullValues.getName(), newDifferent.getInputTip()));
    }

    @Test
    public void buildSetInputTipAction_WithTargetNullValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildSetInputTipUpdateAction(old, newNullValues);

        assertThat(result).contains(SetInputTip.of(old.getName(), newNullValues.getInputTip()));
    }

    @Test
    public void buildChangeIsSearchableAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildChangeIsSearchableUpdateAction(old, newDifferent);

        assertThat(result).contains(ChangeIsSearchable.of(old.getName(), newDifferent.isSearchable()));
    }

    @Test
    public void buildChangeIsSearchableAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<ProductType>> result = buildChangeIsSearchableUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildChangeInputHintAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildChangeInputHintUpdateAction(old, newDifferent);

        assertThat(result).contains(ChangeInputHint.of(old.getName(), newDifferent.getInputHint()));
    }

    @Test
    public void buildChangeInputHintAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<ProductType>> result = buildChangeInputHintUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildChangeInputHintAction_WithSourceNullValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result =
            buildChangeInputHintUpdateAction(oldNullValues, newDifferent);

        assertThat(result).contains(ChangeInputHint.of(oldNullValues.getName(), newDifferent.getInputHint()));
    }

    @Test
    public void buildChangeInputHintAction_WithTargetNullValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildChangeInputHintUpdateAction(old, newNullValues);

        assertThat(result).contains(ChangeInputHint.of(old.getName(), newNullValues.getInputHint()));
    }

    @Test
    public void buildChangeAttributeConstraintAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result =
            buildChangeAttributeConstraintUpdateAction(old, newDifferent);

        assertThat(result).contains(ChangeAttributeConstraint.of(old.getName(), newDifferent.getAttributeConstraint()));
    }

    @Test
    public void buildChangeAttributeConstraintAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<ProductType>> result = buildChangeAttributeConstraintUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildChangeAttributeConstraintAction_WithSourceNullValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result =
            buildChangeAttributeConstraintUpdateAction(oldNullValues, newDifferent);

        assertThat(result).contains(ChangeAttributeConstraint.of(
            oldNullValues.getName(),
            newDifferent.getAttributeConstraint())
        );
    }

    @Test
    public void buildChangeAttributeConstraintAction_WithTargetNullValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result =
            buildChangeAttributeConstraintUpdateAction(old, newNullValues);

        assertThat(result).contains(ChangeAttributeConstraint.of(
            old.getName(),
            newNullValues.getAttributeConstraint())
        );
    }

    @Test
    public void buildActions_WithNewDifferentValues_ShouldReturnActions() {
        final List<UpdateAction<ProductType>> result = buildActions(old, newDifferent);

        assertThat(result).containsExactlyInAnyOrder(
            ChangeAttributeDefinitionLabel.of(old.getName(), newDifferent.getLabel()),
            SetInputTip.of(old.getName(), newDifferent.getInputTip()),
            ChangeAttributeConstraint.of(old.getName(), newDifferent.getAttributeConstraint()),
            ChangeInputHint.of(oldNullValues.getName(), newDifferent.getInputHint()),
            ChangeIsSearchable.of(old.getName(), newDifferent.isSearchable())
        );
    }

    @Test
    public void buildActions_WithSameValues_ShouldReturnEmpty() {
        final List<UpdateAction<ProductType>> result = buildActions(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildActions_WithNewPlainEnum_ShouldReturnAddEnumValueAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", LocalizedString.ofEnglish("label1"), EnumAttributeType.of(ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();


        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                EnumAttributeType.of(ENUM_VALUE_A, ENUM_VALUE_B),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();


        final List<UpdateAction<ProductType>> result = buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(AddEnumValue.of("attributeName1", ENUM_VALUE_B));
    }

    @Test
    public void buildActions_WithoutOldPlainEnum_ShouldReturnRemoveEnumValueAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", LocalizedString.ofEnglish("label1"), EnumAttributeType.of(ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();


        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                EnumAttributeType.of(emptyList()),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();


        final List<UpdateAction<ProductType>> result = buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(RemoveEnumValues.of("attributeName1", "a"));
    }

    @Test
    public void buildActions_WitDifferentPlainEnumValueLabel_ShouldReturnChangeEnumValueLabelAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", LocalizedString.ofEnglish("label1"), EnumAttributeType.of(ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        final EnumValue enumValueDiffLabel = EnumValue.of("a", "label_a_different");

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                EnumAttributeType.of(enumValueDiffLabel),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();


        final List<UpdateAction<ProductType>> result = buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(ChangePlainEnumValueLabel.of("attributeName1", enumValueDiffLabel));
    }

    @Test
    public void buildActions_WithNewLocalizedEnum_ShouldReturnAddLocalizedEnumValueAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of(
                "attributeName1",
                ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A)
            )
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();


        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A, LOCALIZED_ENUM_VALUE_B),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();


        final List<UpdateAction<ProductType>> result = buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(AddLocalizedEnumValue.of("attributeName1", LOCALIZED_ENUM_VALUE_B));
    }

    @Test
    public void buildActions_WithoutOldLocalizedEnum_ShouldReturnRemoveLocalizedEnumValueAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of(
                "attributeName1",
                ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();


        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(emptyList()),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();


        final List<UpdateAction<ProductType>> result = buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(RemoveEnumValues.of("attributeName1", "a"));
    }

    @Test
    public void buildActions_WithDifferentLocalizedEnumValueLabel_ShouldReturnChangeLocalizedEnumValueLabelAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        final LocalizedEnumValue localizedEnumValueDiffLabel = LocalizedEnumValue.of("a", ofEnglish("label_a_diff"));

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(localizedEnumValueDiffLabel),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();


        final List<UpdateAction<ProductType>> result = buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result)
            .containsExactly(ChangeLocalizedEnumValueLabel.of("attributeName1", localizedEnumValueDiffLabel));
    }

}
