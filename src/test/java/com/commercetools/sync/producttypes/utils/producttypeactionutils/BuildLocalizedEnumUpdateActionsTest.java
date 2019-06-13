package com.commercetools.sync.producttypes.utils.producttypeactionutils;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueOrder;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveEnumValues;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.commercetools.sync.commons.utils.LocalizedEnumValueFixtures.*;
import static com.commercetools.sync.producttypes.utils.LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValueUpdateActions;
import static com.commercetools.sync.producttypes.utils.LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuildLocalizedEnumUpdateActionsTest {

    @Test
    void buildLocalizedEnumUpdateActions_WithNullNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            null
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("a", "b", "c"))
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithEmptyNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            Collections.emptyList()
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("a", "b", "c"))
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOldEnumValues_ShouldNotBuildActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            Collections.emptyList(),
            Collections.emptyList()
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            Collections.emptyList(),
            ENUM_VALUES_ABC
        );

        assertThat(updateActions).containsExactly(
            AddLocalizedEnumValue.of("attribute_definition_name_1", ENUM_VALUE_A),
            AddLocalizedEnumValue.of("attribute_definition_name_1", ENUM_VALUE_B),
            AddLocalizedEnumValue.of("attribute_definition_name_1", ENUM_VALUE_C)
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABC
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithDuplicatePlainEnumValues_ShouldTriggerDuplicateKeyError() {
        assertThatThrownBy(() -> buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABB
        ))
            .isInstanceOf(DuplicateKeyException.class)
            .hasMessage("Enum Values have duplicated keys. Definition name: "
                + "'attribute_definition_name_1', Duplicated enum value: 'b'. "
                + "Enum Values are expected to be unique inside their definition.");
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithOneMissingPlainEnumValue_ShouldBuildRemoveEnumValueAction() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_AB
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", singletonList("c"))
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABCD
        );

        assertThat(updateActions).containsExactly(
            AddLocalizedEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D)
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildRemoveAndAddEnumValueActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABD
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", singletonList("c")),
            AddLocalizedEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D)
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithDifferent_ShouldBuildChangeEnumValueOrderAction() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_CAB
        );

        assertThat(updateActions).containsExactly(
            ChangeLocalizedEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_C,
                ENUM_VALUE_A,
                ENUM_VALUE_B
            ))
        );
    }

    @Test
    void buildPlainEnumUpdateActions_WithMixedCase_ShouldBuildChangeEnumValueOrderAction() {
        final String attributeDefinitionName = "attribute_definition_name_1";
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            attributeDefinitionName,
            ENUM_VALUES_BAC,
            ENUM_VALUES_AB_WITH_DIFFERENT_LABEL
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of(attributeDefinitionName, ENUM_VALUE_C.getKey()),
            ChangeLocalizedEnumValueLabel.of(attributeDefinitionName, ENUM_VALUE_A_DIFFERENT_LABEL),
            ChangeLocalizedEnumValueOrder.of(attributeDefinitionName, asList(
                ENUM_VALUE_A_DIFFERENT_LABEL,
                ENUM_VALUE_B
            ))
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_CB
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", singletonList("a")),
            ChangeLocalizedEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_C,
                ENUM_VALUE_B
            ))
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ACBD
        );

        assertThat(updateActions).containsExactly(
            AddLocalizedEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D),
            ChangeLocalizedEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_A,
                ENUM_VALUE_C,
                ENUM_VALUE_B,
                ENUM_VALUE_D
            ))
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ADBC
        );

        assertThat(updateActions).containsExactly(
            AddLocalizedEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D),
            ChangeLocalizedEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_A,
                ENUM_VALUE_D,
                ENUM_VALUE_B,
                ENUM_VALUE_C
            ))
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveEnumValueActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_CBD
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", singletonList("a")),
            AddLocalizedEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D),
            ChangeLocalizedEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_C,
                ENUM_VALUE_B,
                ENUM_VALUE_D
            ))
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithDifferentLabels_ShouldReturnChangeLabelAction() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValueUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUE_A,
            ENUM_VALUE_A_DIFFERENT_LABEL
        );

        assertThat(updateActions).containsAnyOf(
            ChangeLocalizedEnumValueLabel.of("attribute_definition_name_1", ENUM_VALUE_A_DIFFERENT_LABEL)
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithSameLabels_ShouldNotReturnChangeLabelAction() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValueUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUE_A,
            ENUM_VALUE_A
        );

        assertThat(updateActions).doesNotContain(
            ChangeLocalizedEnumValueLabel.of("attribute_definition_name_1", ENUM_VALUE_A)
        );
    }
}
