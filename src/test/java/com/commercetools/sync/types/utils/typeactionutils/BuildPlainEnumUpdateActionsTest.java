package com.commercetools.sync.types.utils.typeactionutils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.AddEnumValue;
import io.sphere.sdk.types.commands.updateactions.ChangeEnumValueOrder;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.commercetools.sync.types.utils.PlainEnumUpdateActionUtils.buildEnumValuesUpdateActions;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class BuildPlainEnumUpdateActionsTest {
    private static final String FIELD_NAME_1 = "field1";

    private static final EnumValue ENUM_VALUE_A = EnumValue.of("a", "label_a");
    private static final EnumValue ENUM_VALUE_B = EnumValue.of("b", "label_b");
    private static final EnumValue ENUM_VALUE_C = EnumValue.of("c", "label_c");
    private static final EnumValue ENUM_VALUE_D = EnumValue.of("d", "label_d");

    private static final List<EnumValue> ENUM_VALUES_ABC = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C);
    private static final List<EnumValue> ENUM_VALUES_ABD = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_D);
    private static final List<EnumValue> ENUM_VALUES_ABCD = asList(
        ENUM_VALUE_A,
        ENUM_VALUE_B,
        ENUM_VALUE_C,
        ENUM_VALUE_D
    );
    private static final List<EnumValue> ENUM_VALUES_CAB = asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B);
    private static final List<EnumValue> ENUM_VALUES_CB = asList(ENUM_VALUE_C, ENUM_VALUE_B);
    private static final List<EnumValue> ENUM_VALUES_ACBD = asList(
        ENUM_VALUE_A,
        ENUM_VALUE_C,
        ENUM_VALUE_B,
        ENUM_VALUE_D
    );
    private static final List<EnumValue> ENUM_VALUES_ADBC = asList(
        ENUM_VALUE_A,
        ENUM_VALUE_D,
        ENUM_VALUE_B,
        ENUM_VALUE_C
    );
    private static final List<EnumValue> ENUM_VALUES_CBD = asList(ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D);



    @Test
    public void buildPlainEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOlEnumValues_ShouldNotBuildActions() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            Collections.emptyList(),
            Collections.emptyList()
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildPlainEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            Collections.emptyList(),
            ENUM_VALUES_ABC
        );

        assertThat(updateActions).containsExactly(
            AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_A),
            AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_B),
            AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_C)
        );
    }

    @Test
    public void buildPlainEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
        final List<UpdateAction<Type>> updateActions = buildEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABC
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildPlainEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
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
    public void buildPlainEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildRemoveAndAddEnumValueActions() {
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
    public void buildPlainEnumUpdateActions_WithDifferent_ShouldBuildChangeEnumValueOrderAction() {
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
    public void buildPlainEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
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
    public void buildPlainEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
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
    public void buildPlainEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
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
    public void buildPlainEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveEnumValueActions() {
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
}
