package com.commercetools.sync.commons.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeEnumValueOrder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangePlainEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveEnumValues;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils.buildActions;
import static com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils.buildAddEnumValuesUpdateActions;
import static com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils.buildChangeEnumValuesOrderUpdateAction;
import static com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils.buildMatchingEnumValuesUpdateActions;
import static com.commercetools.sync.commons.utils.EnumValuesUpdateActionUtils.buildRemoveEnumValuesUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.commons.utils.PlainEnumValueFixtures.*;
import static com.commercetools.sync.producttypes.utils.PlainEnumValueUpdateActionUtils.buildChangeLabelAction;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class EnumValuesUpdateActionUtilsTest {

    private static final String attributeDefinitionName = "attribute_definition_name_1";

    @Test
    void buildActions_WithoutCallbacks_ShouldNotBuildActions() {
        final List<UpdateAction<ProductType>> updateActions = buildActions(attributeDefinitionName,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABCD,
            null,
            null,
            null,
            null,
            null);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildActions_WithoutRemoveCallback_ShouldNotBuildRemoveAction() {
        final List<UpdateAction<ProductType>> updateActions = buildActions(attributeDefinitionName,
            ENUM_VALUES_ABC,
            null,
            null,
            null,
            null,
            null,
            null);

        assertThat(updateActions).doesNotContain(
            RemoveEnumValues.of(attributeDefinitionName, asList("a", "b", "c"))
        );
    }

    @Test
    void buildActions_WithNullNewEnumValues_ShouldJustBuildRemoveActions() {
        final List<UpdateAction<ProductType>> updateActions = buildActions(attributeDefinitionName,
            ENUM_VALUES_ABC,
            null,
            RemoveEnumValues::of,
            null,
            AddEnumValue::of,
            null,
            null);

        assertThat(updateActions).containsAnyOf(
            RemoveEnumValues.of(attributeDefinitionName, asList("a", "b", "c"))
        );

        assertThat(updateActions).doesNotContain(
            AddEnumValue.of(attributeDefinitionName, ENUM_VALUE_A)
        );
    }

    @Test
    void buildActions_WithEmptyNewEnumValues_ShouldJustBuildRemoveActions() {
        final List<UpdateAction<ProductType>> updateActions = buildActions(attributeDefinitionName,
            ENUM_VALUES_ABC,
            Collections.emptyList(),
            RemoveEnumValues::of,
            null,
            AddEnumValue::of,
            null,
            null);

        assertThat(updateActions).containsAnyOf(
            RemoveEnumValues.of(attributeDefinitionName, asList("a", "b", "c"))
        );

        assertThat(updateActions).doesNotContain(
            AddEnumValue.of(attributeDefinitionName, ENUM_VALUE_A)
        );
    }

    @Test
    void buildActions_WithRemoveCallback_ShouldBuildRemoveAction() {
        final List<UpdateAction<ProductType>> updateActions = buildActions(attributeDefinitionName,
            ENUM_VALUES_ABC,
            null,
            RemoveEnumValues::of,
            null,
            null,
            null,
            null);

        assertThat(updateActions).containsAnyOf(
            RemoveEnumValues.of(attributeDefinitionName, asList("a", "b", "c"))
        );
    }

    @Test
    void buildActions_WithoutMatchingEnumCallback_ShouldNotBuildMatchingActions() {
        final List<UpdateAction<ProductType>> updateActions = buildActions(attributeDefinitionName,
            ENUM_VALUES_AB,
            null,
            null,
            null,
            null,
            null,
            null);

        assertThat(updateActions).doesNotContain(
            ChangePlainEnumValueLabel.of(attributeDefinitionName, ENUM_VALUE_A),
            ChangePlainEnumValueLabel.of(attributeDefinitionName, ENUM_VALUE_B)
        );
    }

    @Test
    void buildActions_WithMatchingEnumCallback_ShouldBuildMatchingActions() {
        final List<UpdateAction<ProductType>> updateActions = buildActions(attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_AB_WITH_DIFFERENT_LABEL,
            null,
            getMatchingValueFunction(),
            null,
            null,
            null);

        assertThat(updateActions).containsAnyOf(
            ChangePlainEnumValueLabel.of(attributeDefinitionName, ENUM_VALUE_A_DIFFERENT_LABEL)
        );
    }

    @Test
    void buildActions_WithoutAddEnumCallback_ShouldNotBuildAddEnumActions() {
        final List<UpdateAction<ProductType>> updateActions = buildActions(attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_ABC,
            null,
            null,
            null,
            null,
            null);

        assertThat(updateActions).doesNotContain(
            AddEnumValue.of(attributeDefinitionName, ENUM_VALUE_C)
        );
    }

    @Test
    void buildActions_WithAddEnumCallback_ShouldBuildAddEnumActions() {
        final List<UpdateAction<ProductType>> updateActions = buildActions(attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_ABC,
            null,
            null,
            AddEnumValue::of,
            null,
            null);

        assertThat(updateActions).containsAnyOf(
            AddEnumValue.of(attributeDefinitionName, ENUM_VALUE_C)
        );
    }

    @Test
    void buildActions_WithoutChangeOrderEnumCallback_ShouldNotBuildChangeOrderEnumActions() {
        final List<UpdateAction<ProductType>> updateActions = buildActions(attributeDefinitionName,
            ENUM_VALUES_ABC,
            ENUM_VALUES_CAB,
            null,
            null,
            null,
            null,
            null);

        assertThat(updateActions).doesNotContain(
            ChangeEnumValueOrder.of(attributeDefinitionName, asList(
                ENUM_VALUE_C,
                ENUM_VALUE_A,
                ENUM_VALUE_B
            ))
        );
    }

    @Test
    void buildActions_WithChangeOrderEnumCallback_ShouldBuildChangeOrderEnumActions() {
        final List<UpdateAction<ProductType>> updateActions = buildActions(attributeDefinitionName,
            ENUM_VALUES_ABC,
            ENUM_VALUES_CAB,
            null,
            null,
            null,
            ChangeEnumValueOrder::of,
            null);

        assertThat(updateActions).containsAnyOf(
            ChangeEnumValueOrder.of(attributeDefinitionName, asList(
                ENUM_VALUE_C,
                ENUM_VALUE_A,
                ENUM_VALUE_B
            ))
        );
    }

    @Test
    void buildRemoveEnumValuesUpdateAction_WithNullNewEnumValues_ShouldBuildRemoveActions() {
        final Optional<UpdateAction<ProductType>> updateAction = buildRemoveEnumValuesUpdateAction(
            attributeDefinitionName,
            ENUM_VALUES_ABC,
            null,
            RemoveEnumValues::of);

        assertThat(updateAction).isNotEmpty();
        assertThat(updateAction).isEqualTo(
            Optional.of(RemoveEnumValues.of(attributeDefinitionName, asList("a", "b", "c"))));
    }

    @Test
    void buildMatchingEnumValuesUpdateActions_WithDifferentEnumValues_ShouldBuildChangeLabelActions() {
        final List<UpdateAction<ProductType>> updateActions = buildMatchingEnumValuesUpdateActions(
            attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_AB_WITH_DIFFERENT_LABEL,
            getMatchingValueFunction());

        assertThat(updateActions).containsAnyOf(
            ChangePlainEnumValueLabel.of(attributeDefinitionName, ENUM_VALUE_A_DIFFERENT_LABEL)
        );
    }

    @Test
    void buildMatchingEnumValuesUpdateActions_WithSameNewEnumValues_ShouldNotBuildChangeLabelActions() {
        final List<UpdateAction<ProductType>> updateActions = buildMatchingEnumValuesUpdateActions(
            attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_AB,
            getMatchingValueFunction());

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddEnumValuesUpdateActions_WithNewEnumValues_ShouldBuildAddActions() {
        final List<UpdateAction<ProductType>> updateActions = buildAddEnumValuesUpdateActions(attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_ABC,
            AddEnumValue::of);

        assertThat(updateActions).containsAnyOf(
            AddEnumValue.of(attributeDefinitionName, ENUM_VALUE_C)
        );
    }

    @Test
    void buildAddEnumValuesUpdateActions_WithSameEnumValues_ShouldNotBuildAddActions() {
        final List<UpdateAction<ProductType>> updateActions = buildAddEnumValuesUpdateActions(attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_AB,
            AddEnumValue::of);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildChangeEnumValuesOrderUpdateAction_WithNewEnumValues_ShouldBuildAddActions() {
        final Optional<UpdateAction<ProductType>> updateAction =
            buildChangeEnumValuesOrderUpdateAction(attributeDefinitionName,
                ENUM_VALUES_ABC,
                ENUM_VALUES_CAB,
                ChangeEnumValueOrder::of);

        assertThat(updateAction).isNotEmpty();
        assertThat(updateAction).isEqualTo(
            Optional.of(ChangeEnumValueOrder.of(attributeDefinitionName, asList(
                ENUM_VALUE_C,
                ENUM_VALUE_A,
                ENUM_VALUE_B))));
    }

    @Test
    void buildChangeEnumValuesOrderUpdateAction_WithSameEnumValues_ShouldNotBuildAddActions() {
        final List<UpdateAction<ProductType>> updateActions = buildAddEnumValuesUpdateActions(attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_AB,
            AddEnumValue::of);

        assertThat(updateActions).isEmpty();
    }

    @Nonnull
    private TriFunction<String, EnumValue, EnumValue, List<UpdateAction<ProductType>>> getMatchingValueFunction() {
        return (definitionName, oldEnumValue, newEnumValue) -> {

            if (oldEnumValue == null || newEnumValue == null) {
                return Collections.emptyList();
            }

            return filterEmptyOptionals(
                buildChangeLabelAction(attributeDefinitionName,
                    oldEnumValue,
                    newEnumValue
                ));
        };
    }
}
