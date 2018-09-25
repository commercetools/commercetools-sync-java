package com.commercetools.sync.types.utils.typeactionutils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.types.commands.updateactions.ChangeLocalizedEnumValueOrder;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.commercetools.sync.types.utils.TypeUpdateLocalizedEnumActionUtils.buildLocalizedEnumValuesUpdateActions;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class BuildLocalizedEnumUpdateActionsTest {
    private static final String FIELD_NAME_1 = "field1";

    private static final LocalizedEnumValue ENUM_VALUE_A = LocalizedEnumValue.of("a", ofEnglish("label_a"));
    private static final LocalizedEnumValue ENUM_VALUE_B = LocalizedEnumValue.of("b", ofEnglish("label_b"));
    private static final LocalizedEnumValue ENUM_VALUE_C = LocalizedEnumValue.of("c", ofEnglish("label_c"));
    private static final LocalizedEnumValue ENUM_VALUE_D = LocalizedEnumValue.of("d", ofEnglish("label_d"));

    private static final List<LocalizedEnumValue> ENUM_VALUES_ABC = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C);
    private static final List<LocalizedEnumValue> ENUM_VALUES_ABD = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_D);
    private static final List<LocalizedEnumValue> ENUM_VALUES_ABCD = asList(
        ENUM_VALUE_A,
        ENUM_VALUE_B,
        ENUM_VALUE_C,
        ENUM_VALUE_D
    );
    private static final List<LocalizedEnumValue> ENUM_VALUES_CAB = asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B);
    private static final List<LocalizedEnumValue> ENUM_VALUES_CB = asList(ENUM_VALUE_C, ENUM_VALUE_B);
    private static final List<LocalizedEnumValue> ENUM_VALUES_ACBD = asList(
        ENUM_VALUE_A,
        ENUM_VALUE_C,
        ENUM_VALUE_B,
        ENUM_VALUE_D
    );
    private static final List<LocalizedEnumValue> ENUM_VALUES_ADBC = asList(
        ENUM_VALUE_A,
        ENUM_VALUE_D,
        ENUM_VALUE_B,
        ENUM_VALUE_C
    );
    private static final List<LocalizedEnumValue> ENUM_VALUES_CBD = asList(ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D);

    @Test
    public void buildLocalizedEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOlEnumValues_ShouldNotBuildActions() {
        final List<UpdateAction<Type>> updateActions = buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1,
            Collections.emptyList(),
            Collections.emptyList()
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
        final List<UpdateAction<Type>> updateActions = buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1,
            Collections.emptyList(),
            ENUM_VALUES_ABC
        );

        assertThat(updateActions).containsExactly(
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_A),
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_B),
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_C)
        );
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
        final List<UpdateAction<Type>> updateActions = buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABC
        );

        assertThat(updateActions).isEmpty();
    }


    @Test
    public void buildLocalizedEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
        final List<UpdateAction<Type>> updateActions = buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABCD
        );

        assertThat(updateActions).containsExactly(
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D)
        );
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildAddEnumValueActions() {
        final List<UpdateAction<Type>> updateActions = buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABD
        );

        // remove enum value actions not exists for type resources
        assertThat(updateActions).containsExactly(
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D)
        );
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithDifferent_ShouldBuildChangeEnumValueOrderAction() {
        final List<UpdateAction<Type>> updateActions = buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_CAB
        );

        assertThat(updateActions).containsExactly(
            ChangeLocalizedEnumValueOrder.of(FIELD_NAME_1, asList(
                ENUM_VALUE_C.getKey(),
                ENUM_VALUE_A.getKey(),
                ENUM_VALUE_B.getKey()
            ))
        );
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderActions() {
        final List<UpdateAction<Type>> updateActions = buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_CB
        );

        // remove enum value actions not exists for type resources
        assertThat(updateActions).containsExactly(
            ChangeLocalizedEnumValueOrder.of(FIELD_NAME_1, asList(
                ENUM_VALUE_C.getKey(),
                ENUM_VALUE_B.getKey()
            ))
        );
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
        final List<UpdateAction<Type>> updateActions = buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ACBD
        );

        assertThat(updateActions).containsExactly(
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D),
            ChangeLocalizedEnumValueOrder.of(FIELD_NAME_1, asList(
                ENUM_VALUE_A.getKey(),
                ENUM_VALUE_C.getKey(),
                ENUM_VALUE_B.getKey(),
                ENUM_VALUE_D.getKey()
            ))
        );
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
        final List<UpdateAction<Type>> updateActions = buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ADBC
        );

        assertThat(updateActions).containsExactly(
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D),
            ChangeLocalizedEnumValueOrder.of(FIELD_NAME_1, asList(
                ENUM_VALUE_A.getKey(),
                ENUM_VALUE_D.getKey(),
                ENUM_VALUE_B.getKey(),
                ENUM_VALUE_C.getKey()
            ))
        );
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAddAndOrderEnumValueActions() {
        final List<UpdateAction<Type>> updateActions = buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1,
            ENUM_VALUES_ABC,
            ENUM_VALUES_CBD
        );

        // remove enum value actions not exists for type resources
        assertThat(updateActions).containsExactly(
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D),
            ChangeLocalizedEnumValueOrder.of(FIELD_NAME_1, asList(
                ENUM_VALUE_C.getKey(),
                ENUM_VALUE_B.getKey(),
                ENUM_VALUE_D.getKey()
            ))
        );
    }
}
