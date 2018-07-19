package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeDefinitionLabel;
import io.sphere.sdk.producttypes.commands.updateactions.SetInputTip;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeIsSearchable;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeInputHint;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeConstraint;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildChangeLabelUpdateAction;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildSetInputTipUpdateAction;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildChangeIsSearchableUpdateAction;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildChangeInputHintUpdateAction;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildChangeAttributeConstraintUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;

public class AttributeDefinitionUpdateActionUtilsTest {
    private static AttributeDefinition old;
    private static AttributeDefinition oldNullValues;
    private static AttributeDefinitionDraft newSame;
    private static AttributeDefinitionDraft newDifferent;
    private static AttributeDefinitionDraft newNullValues;


    /**
     * Initialises test data.
     */
    @BeforeClass
    public static void setup() {
        old = AttributeDefinitionBuilder
            .of("attributeName1", LocalizedString.ofEnglish("label1"), StringAttributeType.of())
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        oldNullValues = AttributeDefinitionBuilder
            .of("attributeName1", LocalizedString.ofEnglish("label1"), StringAttributeType.of())
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
            .of(StringAttributeType.of(), "attributeName1", LocalizedString.ofEnglish("label2"), true)
            .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
            .inputTip(LocalizedString.ofEnglish("inputTip2"))
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

        assertThat(result).containsInstanceOf(ChangeAttributeDefinitionLabel.class);
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

        assertThat(result).containsInstanceOf(SetInputTip.class);
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

        assertThat(result).containsInstanceOf(SetInputTip.class);
        assertThat(result).contains(SetInputTip.of(oldNullValues.getName(), newDifferent.getInputTip()));
    }

    @Test
    public void buildSetInputTipAction_WithTargetNullValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildSetInputTipUpdateAction(old, newNullValues);

        assertThat(result).containsInstanceOf(SetInputTip.class);
        assertThat(result).contains(SetInputTip.of(old.getName(), newNullValues.getInputTip()));
    }

    @Test
    public void buildChangeIsSearchableAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildChangeIsSearchableUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(ChangeIsSearchable.class);
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

        assertThat(result).containsInstanceOf(ChangeInputHint.class);
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

        assertThat(result).containsInstanceOf(ChangeInputHint.class);
        assertThat(result).contains(ChangeInputHint.of(oldNullValues.getName(), newDifferent.getInputHint()));
    }

    @Test
    public void buildChangeInputHintAction_WithTargetNullValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildChangeInputHintUpdateAction(old, newNullValues);

        assertThat(result).containsInstanceOf(ChangeInputHint.class);
        assertThat(result).contains(ChangeInputHint.of(old.getName(), newNullValues.getInputHint()));
    }

    @Test
    public void buildChangeAttributeConstraintAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result =
            buildChangeAttributeConstraintUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(ChangeAttributeConstraint.class);
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

        assertThat(result).containsInstanceOf(ChangeAttributeConstraint.class);
        assertThat(result).contains(ChangeAttributeConstraint.of(
            oldNullValues.getName(),
            newDifferent.getAttributeConstraint())
        );
    }

    @Test
    public void buildChangeAttributeConstraintAction_WithTargetNullValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result =
            buildChangeAttributeConstraintUpdateAction(old, newNullValues);

        assertThat(result).containsInstanceOf(ChangeAttributeConstraint.class);
        assertThat(result).contains(ChangeAttributeConstraint.of(
            old.getName(),
            newNullValues.getAttributeConstraint())
        );
    }
}
