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
import io.sphere.sdk.products.attributes.SetAttributeType;
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
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildEnumUpdateActions;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildSetInputTipUpdateAction;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class AttributeDefinitionUpdateActionUtilsTest {
    private static AttributeDefinition old;
    private static AttributeDefinitionDraft newSame;
    private static AttributeDefinitionDraft newDifferent;

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
    }

    @Test
    public void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("y"), null)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result = buildChangeLabelUpdateAction(attributeDefinition, draft);

        //assertion
        assertThat(result).contains(ChangeAttributeDefinitionLabel.of(attributeDefinition.getName(), ofEnglish("x")));
    }

    @Test
    public void buildChangeLabelAction_WithSameValues_ShouldReturnEmptyOptional() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result = buildChangeLabelUpdateAction(attributeDefinition, draft);

        //assertion
        assertThat(result).isEmpty();
    }

    @Test
    public void buildSetInputTipAction_WithDifferentValues_ShouldReturnAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .inputTip(ofEnglish("foo"))
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .inputTip(ofEnglish("bar"))
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result = buildSetInputTipUpdateAction(attributeDefinition, draft);

        //assertion
        assertThat(result).contains(SetInputTip.of(attributeDefinition.getName(), ofEnglish("foo")));
    }

    @Test
    public void buildSetInputTipAction_WithSameValues_ShouldReturnEmptyOptional() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .inputTip(ofEnglish("foo"))
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .inputTip(ofEnglish("foo"))
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result = buildSetInputTipUpdateAction(attributeDefinition, draft);

        //assertion
        assertThat(result).isEmpty();
    }

    @Test
    public void buildSetInputTipAction_WithSourceNullValues_ShouldReturnAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .inputTip(null)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .inputTip(ofEnglish("foo"))
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result = buildSetInputTipUpdateAction(attributeDefinition, draft);

        //assertion
        assertThat(result).contains(SetInputTip.of(attributeDefinition.getName(), null));
    }

    @Test
    public void buildSetInputTipAction_WithTargetNullValues_ShouldReturnAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .inputTip(ofEnglish("foo"))
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .inputTip(null)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result = buildSetInputTipUpdateAction(attributeDefinition, draft);

        //assertion
        assertThat(result).contains(SetInputTip.of(attributeDefinition.getName(), ofEnglish("foo")));
    }

    @Test
    public void buildChangeIsSearchableAction_WithDifferentValues_ShouldReturnAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .isSearchable(true)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .isSearchable(false)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result =
            buildChangeIsSearchableUpdateAction(attributeDefinition, draft);

        assertThat(result).contains(ChangeIsSearchable.of(attributeDefinition.getName(), true));
    }

    @Test
    public void buildChangeIsSearchableAction_WithSameValues_ShouldReturnEmptyOptional() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .isSearchable(true)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .isSearchable(true)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result =
            buildChangeIsSearchableUpdateAction(attributeDefinition, draft);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildChangeIsSearchableAction_WithNullSourceAndNonDefaultTarget_ShouldBuildAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .isSearchable(null)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .isSearchable(false)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result =
            buildChangeIsSearchableUpdateAction(attributeDefinition, draft);

        assertThat(result).contains(ChangeIsSearchable.of("foo", true));
    }

    @Test
    public void buildChangeIsSearchableAction_WithNullSourceAndDefaultTarget_ShouldNotBuildAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .isSearchable(null)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .isSearchable(true)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result =
            buildChangeIsSearchableUpdateAction(attributeDefinition, draft);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildChangeInputHintAction_WithDifferentValues_ShouldReturnAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .inputHint(TextInputHint.MULTI_LINE)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .inputHint(TextInputHint.SINGLE_LINE)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result = buildChangeInputHintUpdateAction(attributeDefinition, draft);

        assertThat(result).contains(ChangeInputHint.of(attributeDefinition.getName(), TextInputHint.MULTI_LINE));
    }

    @Test
    public void buildChangeInputHintAction_WithSameValues_ShouldReturnEmptyOptional() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .inputHint(TextInputHint.MULTI_LINE)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .inputHint(TextInputHint.MULTI_LINE)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result = buildChangeInputHintUpdateAction(attributeDefinition, draft);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildChangeInputHintAction_WithSourceNullValuesAndNonDefaultTargetValue_ShouldBuildAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .inputHint(null)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .inputHint(TextInputHint.MULTI_LINE)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result = buildChangeInputHintUpdateAction(attributeDefinition, draft);

        assertThat(result).contains(ChangeInputHint.of(attributeDefinition.getName(), TextInputHint.SINGLE_LINE));
    }

    @Test
    public void buildChangeInputHintAction_WithSourceNullValuesAndDefaultTargetValue_ShouldNotBuildAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .inputHint(null)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .inputHint(TextInputHint.SINGLE_LINE)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result = buildChangeInputHintUpdateAction(attributeDefinition, draft);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildChangeAttributeConstraintAction_WithDifferentValues_ShouldBuildAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .attributeConstraint(AttributeConstraint.COMBINATION_UNIQUE)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result =
            buildChangeAttributeConstraintUpdateAction(attributeDefinition, draft);

        assertThat(result).contains(
            ChangeAttributeConstraint.of(attributeDefinition.getName(), AttributeConstraint.COMBINATION_UNIQUE));
    }

    @Test
    public void buildChangeAttributeConstraintAction_WithSameValues_ShouldReturnEmptyOptional() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .attributeConstraint(AttributeConstraint.COMBINATION_UNIQUE)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .attributeConstraint(AttributeConstraint.COMBINATION_UNIQUE)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result =
            buildChangeAttributeConstraintUpdateAction(attributeDefinition, draft);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildChangeAttributeConstraintAction_WithSourceNullValuesAndDefaultTarget_ShouldNotBuildAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .attributeConstraint(null)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .attributeConstraint(AttributeConstraint.NONE)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result =
            buildChangeAttributeConstraintUpdateAction(attributeDefinition, draft);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildChangeAttributeConstraintAction_WithSourceNullValuesAndNonDefaultTarget_ShouldBuildAction() {
        // Preparation
        final AttributeDefinitionDraft draft = AttributeDefinitionDraftBuilder
            .of(null, "foo", ofEnglish("x"), null)
            .attributeConstraint(null)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("foo", ofEnglish("x"), null)
            .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
            .build();

        // test
        final Optional<UpdateAction<ProductType>> result =
            buildChangeAttributeConstraintUpdateAction(attributeDefinition, draft);

        assertThat(result).contains(ChangeAttributeConstraint.of(draft.getName(), AttributeConstraint.NONE));
    }

    @Test
    public void buildActions_WithNullOptionalsAndDefaultValues_ShouldBuildNoActions() {
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(StringAttributeType.of(), "attributeName1", ofEnglish("label2"), true)
            .attributeConstraint(null)
            .inputHint(null)
            .isSearchable(null)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label2"), StringAttributeType.of())
            .isRequired(true)
            .build();


        final List<UpdateAction<ProductType>> result = buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildActions_WithNonDefaultValuesForOptionalFields_ShouldBuildActions() {
        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(StringAttributeType.of(), "attributeName1", ofEnglish("label2"), true)
            .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
            .inputHint(TextInputHint.MULTI_LINE)
            .isSearchable(false)
            .build();

        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label2"), StringAttributeType.of())
            .isRequired(true)
            .build();


        final List<UpdateAction<ProductType>> result = buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactlyInAnyOrder(
            ChangeAttributeConstraint.of("attributeName1", AttributeConstraint.SAME_FOR_ALL),
            ChangeInputHint.of("attributeName1", TextInputHint.MULTI_LINE),
            ChangeIsSearchable.of("attributeName1", false)
        );
    }

    @Test
    public void buildActions_WithNewDifferentValues_ShouldReturnActions() {
        final List<UpdateAction<ProductType>> result = buildActions(old, newDifferent);

        assertThat(result).containsExactlyInAnyOrder(
            ChangeAttributeDefinitionLabel.of(old.getName(), newDifferent.getLabel()),
            SetInputTip.of(old.getName(), newDifferent.getInputTip()),
            ChangeAttributeConstraint.of(old.getName(), newDifferent.getAttributeConstraint()),
            ChangeInputHint.of(old.getName(), newDifferent.getInputHint()),
            ChangeIsSearchable.of(old.getName(), newDifferent.isSearchable())
        );
    }

    @Test
    public void buildActions_WithSameValues_ShouldReturnEmpty() {
        final List<UpdateAction<ProductType>> result = buildActions(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildActions_WithStringAttributeTypesWithLabelChanges_ShouldBuildChangeLabelAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"), StringAttributeType.of())
            .build();

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(StringAttributeType.of(), "attributeName1", ofEnglish("label2"), false)
            .build();

        final List<UpdateAction<ProductType>> result =
            buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(
            ChangeAttributeDefinitionLabel.of("attributeName1", attributeDefinitionDraft.getLabel()));
    }

    @Test
    public void buildActions_WithSetOfStringAttributeTypesWithDefinitionLabelChanges_ShouldBuildChangeLabelAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"), SetAttributeType.of(StringAttributeType.of()))
            .build();

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(StringAttributeType.of()), "attributeName1", ofEnglish("label2"), false)
            .build();

        final List<UpdateAction<ProductType>> result =
            buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(
            ChangeAttributeDefinitionLabel.of("attributeName1", attributeDefinitionDraft.getLabel()));
    }

    @Test
    public void buildActions_WithSetOfSetOfStringAttributeTypesWithDefLabelChanges_ShouldBuildChangeLabelAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"),
                SetAttributeType.of(SetAttributeType.of(StringAttributeType.of())))
            .build();

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(SetAttributeType.of(StringAttributeType.of())), "attributeName1", ofEnglish("label2"), false)
            .build();

        final List<UpdateAction<ProductType>> result =
            buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(
            ChangeAttributeDefinitionLabel.of("attributeName1", attributeDefinitionDraft.getLabel()));
    }

    @Test
    public void buildActions_WithSameSetOfEnumsAttributeTypesWithDefLabelChanges_ShouldBuildChangeLabelAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"), SetAttributeType.of(
                EnumAttributeType.of(emptyList())))
            .build();

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(
                EnumAttributeType.of(emptyList())), "attributeName1", ofEnglish("label2"), false)
            .build();

        final List<UpdateAction<ProductType>> result =
            buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(
            ChangeAttributeDefinitionLabel.of("attributeName1", attributeDefinitionDraft.getLabel()));
    }

    @Test
    public void buildActions_WithChangedSetOfEnumAttributeTypes_ShouldBuildEnumActions() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"), SetAttributeType.of(
                EnumAttributeType.of(emptyList())))
            .build();

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(
                EnumAttributeType.of(singletonList(ENUM_VALUE_A))),
                "attributeName1", ofEnglish("label1"), false)
            .build();

        final List<UpdateAction<ProductType>> result =
            buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(
            AddEnumValue.of("attributeName1", ENUM_VALUE_A));
    }

    @Test
    public void buildActions_WithSameSetOfLEnumAttributeTypesWithDefLabelChanges_ShouldBuildChangeLabelAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"), SetAttributeType.of(
                LocalizedEnumAttributeType.of(emptyList())))
            .build();

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(
                LocalizedEnumAttributeType.of(emptyList())), "attributeName1", ofEnglish("label2"), false)
            .build();

        final List<UpdateAction<ProductType>> result =
            buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(
            ChangeAttributeDefinitionLabel.of("attributeName1", attributeDefinitionDraft.getLabel()));
    }

    @Test
    public void buildActions_WithChangedSetOfLocalizedEnumAttributeTypes_ShouldBuildEnumActions() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"), SetAttributeType.of(
                LocalizedEnumAttributeType.of(emptyList())))
            .build();

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(
                LocalizedEnumAttributeType.of(singletonList(LOCALIZED_ENUM_VALUE_A))),
                "attributeName1", ofEnglish("label1"), false)
            .build();

        final List<UpdateAction<ProductType>> result =
            buildActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(
            AddLocalizedEnumValue.of("attributeName1", LOCALIZED_ENUM_VALUE_A));
    }

    @Test
    public void buildActions_WithNewPlainEnum_ShouldReturnAddEnumValueAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"), EnumAttributeType.of(ENUM_VALUE_A))
            .build();


        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                EnumAttributeType.of(ENUM_VALUE_A, ENUM_VALUE_B),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .build();


        final List<UpdateAction<ProductType>> result =
            buildEnumUpdateActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(AddEnumValue.of("attributeName1", ENUM_VALUE_B));
    }

    @Test
    public void buildActions_WithoutOldPlainEnum_ShouldReturnRemoveEnumValueAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"), EnumAttributeType.of(ENUM_VALUE_A))
            .build();


        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                EnumAttributeType.of(emptyList()),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .build();


        final List<UpdateAction<ProductType>> result = buildEnumUpdateActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(RemoveEnumValues.of("attributeName1", "a"));
    }

    @Test
    public void buildActions_WitDifferentPlainEnumValueLabel_ShouldReturnChangeEnumValueLabelAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"), EnumAttributeType.of(ENUM_VALUE_A))
            .build();

        final EnumValue enumValueDiffLabel = EnumValue.of("a", "label_a_different");

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                EnumAttributeType.of(enumValueDiffLabel),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .build();


        final List<UpdateAction<ProductType>> result = buildEnumUpdateActions(attributeDefinition, attributeDefinitionDraft);

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
            .build();


        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A, LOCALIZED_ENUM_VALUE_B),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .build();


        final List<UpdateAction<ProductType>> result = buildEnumUpdateActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(AddLocalizedEnumValue.of("attributeName1", LOCALIZED_ENUM_VALUE_B));
    }

    @Test
    public void buildActions_WithoutOldLocalizedEnum_ShouldReturnRemoveLocalizedEnumValueAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of(
                "attributeName1",
                ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .build();


        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(emptyList()),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .build();


        final List<UpdateAction<ProductType>> result = buildEnumUpdateActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result).containsExactly(RemoveEnumValues.of("attributeName1", "a"));
    }

    @Test
    public void buildActions_WithDifferentLocalizedEnumValueLabel_ShouldReturnChangeLocalizedEnumValueLabelAction() {
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of("attributeName1", ofEnglish("label1"),
                LocalizedEnumAttributeType.of(LOCALIZED_ENUM_VALUE_A))
            .build();

        final LocalizedEnumValue localizedEnumValueDiffLabel = LocalizedEnumValue.of("a", ofEnglish("label_a_diff"));

        final AttributeDefinitionDraft attributeDefinitionDraft = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(localizedEnumValueDiffLabel),
                "attributeName1",
                ofEnglish("label1"),
                false
            )
            .build();

        final List<UpdateAction<ProductType>> result = buildEnumUpdateActions(attributeDefinition, attributeDefinitionDraft);

        assertThat(result)
            .containsExactly(ChangeLocalizedEnumValueLabel.of("attributeName1", localizedEnumValueDiffLabel));
    }
}
