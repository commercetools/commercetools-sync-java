package com.commercetools.sync.types.utils;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.AddEnumValue;
import io.sphere.sdk.types.commands.updateactions.ChangeEnumValueOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.commercetools.sync.commons.utils.PlainEnumValueFixtures.*;
import static com.commercetools.sync.types.utils.PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuildPlainEnumUpdateActionsTest {
    private static final String FIELD_NAME_1 = "field1";

    @Test
    void buildPlainEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOldEnumValues_ShouldNotBuildActions() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            emptyList(),
            emptyList()
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildPlainEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            emptyList(),
            ENUM_VALUES_ABC
        );

        assertThat(updateActions).containsExactly(
            AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_A),
            AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_B),
            AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_C)
        );
    }

    @Test
    void buildPlainEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABC
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildPlainEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABCD
        );

        assertThat(updateActions).containsExactly(
            AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D)
        );
    }

    @Test
    void buildPlainEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildAddEnumValueActions() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABD
        );

        assertThat(updateActions).containsExactly(
            AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D)
        );
    }

    @Test
    void buildPlainEnumUpdateActions_WithDifferentOrder_ShouldBuildChangeEnumValueOrderAction() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_CAB
        );

        assertThat(updateActions).containsExactly(
            ChangeEnumValueOrder.of(FIELD_NAME_1, asList(
                ENUM_VALUE_C.getKey(),
                ENUM_VALUE_A.getKey(),
                ENUM_VALUE_B.getKey()
            ))
        );
    }

    @Test
    void buildPlainEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAction() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_CB
        );

        assertThat(updateActions).containsExactly(
            ChangeEnumValueOrder.of(FIELD_NAME_1, asList(
                ENUM_VALUE_C.getKey(),
                ENUM_VALUE_B.getKey()
            ))
        );
    }

    @Test
    void buildPlainEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ACBD
        );

        assertThat(updateActions).containsExactly(
            AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D),
            ChangeEnumValueOrder.of(FIELD_NAME_1, asList(
                ENUM_VALUE_A.getKey(),
                ENUM_VALUE_C.getKey(),
                ENUM_VALUE_B.getKey(),
                ENUM_VALUE_D.getKey()
            ))
        );
    }

    @Test
    void buildPlainEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ADBC
        );

        assertThat(updateActions).containsExactly(
            AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D),
            ChangeEnumValueOrder.of(FIELD_NAME_1, asList(
                ENUM_VALUE_A.getKey(),
                ENUM_VALUE_D.getKey(),
                ENUM_VALUE_B.getKey(),
                ENUM_VALUE_C.getKey()
            ))
        );
    }

    @Test
    void buildPlainEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAddAndChangeOrderActions() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_CBD
        );

        assertThat(updateActions).containsExactly(
            AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D),
            ChangeEnumValueOrder.of(FIELD_NAME_1, asList(
                ENUM_VALUE_C.getKey(),
                ENUM_VALUE_B.getKey(),
                ENUM_VALUE_D.getKey()
            ))
        );
    }

    @Test
    void buildLocalizedEnumUpdateActions_WithDuplicateEnumValues_ShouldTriggerDuplicateKeyError() {
        assertThatThrownBy(() -> buildEnumValuesUpdateActions(
            "field_definition_name",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABB
        )).isInstanceOf(DuplicateKeyException.class)
          .hasMessage("Enum Values have duplicated keys. Definition name: "
              + "'field_definition_name', Duplicated enum value: 'b'. "
              + "Enum Values are expected to be unique inside their definition.");
    }
}
