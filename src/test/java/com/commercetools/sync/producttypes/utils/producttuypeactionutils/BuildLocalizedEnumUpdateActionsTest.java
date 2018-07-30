package com.commercetools.sync.producttypes.utils.producttuypeactionutils;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveEnumValues;
import io.sphere.sdk.producttypes.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.List;

import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateLocalizedEnumActionUtils.buildLocalizedEnumValuesUpdateActions;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;

public class BuildLocalizedEnumUpdateActionsTest {
    private static final LocalizedEnumValue ENUM_VALUE_A = LocalizedEnumValue.of("a", ofEnglish("label_a"));
    private static final LocalizedEnumValue ENUM_VALUE_B = LocalizedEnumValue.of("b", ofEnglish("label_b"));
    private static final LocalizedEnumValue ENUM_VALUE_C = LocalizedEnumValue.of("c", ofEnglish("label_c"));
    private static final LocalizedEnumValue ENUM_VALUE_D = LocalizedEnumValue.of("d", ofEnglish("label_d"));

    private static final List<LocalizedEnumValue> ENUM_VALUES_ABC = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C);
    private static final List<LocalizedEnumValue> ENUM_VALUES_AB = asList(ENUM_VALUE_A, ENUM_VALUE_B);
    private static final List<LocalizedEnumValue> ENUM_VALUES_ABB = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_B);
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
    public void buildLocalizedEnumUpdateActions_WithNullNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
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
    public void buildLocalizedEnumUpdateActions_WithEmptyNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
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
    public void buildLocalizedEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOlEnumValues_ShouldNotBuildActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            Collections.emptyList(),
            Collections.emptyList()
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
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
    public void buildLocalizedEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABC
        );

        assertThat(updateActions).isEmpty();
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Test
    public void buildLocalizedEnumUpdateActions_WithDuplicatePlainEnumValues_ShouldTriggerDuplicateKeyError() {
        expectedException.expect(DuplicateKeyException.class);
        expectedException.expectMessage("Enum Values have duplicated keys. Attribute definition name: "
            + "'attribute_definition_name_1', Duplicated enum value: 'b'. Enum Values are expected to be unique inside "
            + "their attribute definition.");

        buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABB
        );
    }


    @Test
    public void buildLocalizedEnumUpdateActions_WithOneMissingPlainEnumValue_ShouldBuildRemoveEnumValueAction() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_AB
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("c"))
        );
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
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
    public void buildLocalizedEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildRemoveAndAddEnumValueActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABD
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("c")),
            AddLocalizedEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D)
        );
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithDifferent_ShouldBuildChangeEnumValueOrderAction() {
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
    public void buildLocalizedEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_CB
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("a")),
            ChangeLocalizedEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_C,
                ENUM_VALUE_B
            ))
        );
    }

    @Test
    public void buildLocalizedEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
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
    public void buildLocalizedEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
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
    public void buildLocalizedEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveEnumValueActions() {
        final List<UpdateAction<ProductType>> updateActions = buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1",
            ENUM_VALUES_ABC,
            ENUM_VALUES_CBD
        );

        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", asList("a")),
            AddLocalizedEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D),
            ChangeLocalizedEnumValueOrder.of("attribute_definition_name_1", asList(
                ENUM_VALUE_C,
                ENUM_VALUE_B,
                ENUM_VALUE_D
            ))
        );
    }
}
