package com.commercetools.sync.producttypes.utils.producttypeactionutils;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeEnumValueOrder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangePlainEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveEnumValues;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.List;

import static com.commercetools.sync.commons.utils.enums.PlainEnumValueTestObjects.*;
import static com.commercetools.sync.producttypes.utils.PlainEnumValueUpdateActionUtils.buildEnumValueUpdateActions;
import static com.commercetools.sync.producttypes.utils.PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class BuildPlainEnumUpdateActionsTest {

    @Test
    public void buildPlainEnumUpdateActions_WithNullNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            null
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("a", "b", "c"))
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithEmptyNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            Collections.emptyList()
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("a", "b", "c"))
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOlEnumValues_ShouldNotBuildActions() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            Collections.emptyList(),
            Collections.emptyList()
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildPlainEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            Collections.emptyList(),
            ENUM_VALUES_ABC
        );

        assertThat(updateActions).containsExactly(
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_A),
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_B),
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_C)
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABC
        );

        assertThat(updateActions).isEmpty();
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Test
    public void buildPlainEnumUpdateActions_WithDuplicatePlainEnumValues_ShouldTriggerDuplicateKeyError() {
        expectedException.expect(DuplicateKeyException.class);
        expectedException.expectMessage("Enum Values have duplicated keys. Attribute definition name: "
            + "'attribute_definition_name_1', Duplicated enum value: 'b'. Enum Values are expected to be unique inside "
            + "their attribute definition.");

        buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABB
        );
    }


    @Test
    public void buildPlainEnumUpdateActions_WithOneMissingPlainEnumValue_ShouldBuildRemoveEnumValueAction() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_AB
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("c"))
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABCD
        );

        assertThat(updateActions).containsExactly(
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D)
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildRemoveAndAddEnumValueActions() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABD
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("c")),
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D)
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithDifferent_ShouldBuildChangeEnumValueOrderAction() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_CAB
        );

        assertThat(updateActions).containsExactly(
            ChangeEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_C,
                ENUM_VALUE_A,
                ENUM_VALUE_B
            ))
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_CB
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("a")),
            ChangeEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_C,
                ENUM_VALUE_B
            ))
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ACBD
        );

        assertThat(updateActions).containsExactly(
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D),
            ChangeEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_A,
                ENUM_VALUE_C,
                ENUM_VALUE_B,
                ENUM_VALUE_D
            ))
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ADBC
        );

        assertThat(updateActions).containsExactly(
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D),
            ChangeEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_A,
                ENUM_VALUE_D,
                ENUM_VALUE_B,
                ENUM_VALUE_C
            ))
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveEnumValueActions() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_CBD
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("a")),
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D),
            ChangeEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_C,
                ENUM_VALUE_B,
                ENUM_VALUE_D
            ))
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithDifferentLabels_ShouldReturnChangeLabelAction() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValueUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUE_A,
            ENUM_VALUE_A_DIFFERENT_LABEL
        );

        assertThat(updateActions).containsAnyOf(
            ChangePlainEnumValueLabel.of("attribute_definition_name_1", ENUM_VALUE_A_DIFFERENT_LABEL)
        );
    }


    @Test
    public void buildPlainEnumUpdateActions_WithSameLabels_ShouldNotReturnChangeLabelAction() {
        final List<UpdateAction<ProductType>> updateActions = buildEnumValueUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUE_A,
            ENUM_VALUE_A
        );

        assertThat(updateActions).doesNotContain(
            ChangePlainEnumValueLabel.of("attribute_definition_name_1", ENUM_VALUE_A)
        );
    }


}
