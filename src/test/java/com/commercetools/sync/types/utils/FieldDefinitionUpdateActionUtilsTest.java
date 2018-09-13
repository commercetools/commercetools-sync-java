package com.commercetools.sync.types.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.types.EnumFieldType;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.LocalizedEnumFieldType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.AddEnumValue;
import io.sphere.sdk.types.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.types.commands.updateactions.ChangeFieldDefinitionLabel;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.types.FieldDefinitionTestHelper.stringFieldDefinition;
import static com.commercetools.sync.types.utils.FieldDefinitionUpdateActionUtils.buildActions;
import static com.commercetools.sync.types.utils.FieldDefinitionUpdateActionUtils.buildChangeLabelUpdateAction;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;

public class FieldDefinitionUpdateActionUtilsTest {
    private static final String FIELD_NAME_1 = "fieldName1";
    private static final String LABEL_1 = "label1";
    private static final String LABEL_2 = "label2";


    private static FieldDefinition old;
    private static FieldDefinition oldNullValues;
    private static FieldDefinition newSame;
    private static FieldDefinition newDifferent;
    private static FieldDefinition newNullValues;

    private static final EnumValue ENUM_VALUE_A = EnumValue.of("a", "label_a");
    private static final EnumValue ENUM_VALUE_B = EnumValue.of("b", "label_b");

    private static final LocalizedEnumValue LOCALIZED_ENUM_VALUE_A = LocalizedEnumValue.of("a", ofEnglish("label_a"));
    private static final LocalizedEnumValue LOCALIZED_ENUM_VALUE_B = LocalizedEnumValue.of("b", ofEnglish("label_b"));

    /**
     * Initialises test data.
     */
    @BeforeClass
    public static void setup() {

        old = stringFieldDefinition(FIELD_NAME_1, LABEL_1, false, TextInputHint.SINGLE_LINE);
        oldNullValues = stringFieldDefinition(FIELD_NAME_1, LABEL_1, false, null);
        newSame = stringFieldDefinition(FIELD_NAME_1, LABEL_1, false, TextInputHint.SINGLE_LINE);
        newDifferent = stringFieldDefinition(FIELD_NAME_1, LABEL_2, true, TextInputHint.MULTI_LINE);
        newNullValues = stringFieldDefinition(FIELD_NAME_1, LABEL_2, true, null);
    }

    @Test
    public void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Type>> result = buildChangeLabelUpdateAction(old, newDifferent);

        assertThat(result).contains(ChangeFieldDefinitionLabel.of(old.getName(), newDifferent.getLabel()));
    }

    @Test
    public void buildChangeLabelAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Type>> result = buildChangeLabelUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildActions_WithNewDifferentValues_ShouldReturnActions() {
        final List<UpdateAction<Type>> result = buildActions(old, newDifferent);

        assertThat(result).containsExactlyInAnyOrder(
            ChangeFieldDefinitionLabel.of(old.getName(), newDifferent.getLabel())
        );
    }

    @Test
    public void buildActions_WithSameValues_ShouldReturnEmpty() {
        final List<UpdateAction<Type>> result = buildActions(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildActions_WithNewPlainEnum_ShouldReturnAddEnumValueAction() {
        final FieldDefinition oldFieldDefinition = FieldDefinition.of(
                EnumFieldType.of(Arrays.asList(ENUM_VALUE_A)),
                FIELD_NAME_1,
                LocalizedString.ofEnglish(LABEL_1),
                false,
                TextInputHint.SINGLE_LINE);


        final FieldDefinition newFieldDefinition = FieldDefinition.of(
                EnumFieldType.of(Arrays.asList(ENUM_VALUE_A, ENUM_VALUE_B)),
                FIELD_NAME_1,
                LocalizedString.ofEnglish(LABEL_1),
                false,
                TextInputHint.SINGLE_LINE);

        final List<UpdateAction<Type>> result =
                buildActions(oldFieldDefinition, newFieldDefinition);


        assertThat(result).containsExactly(AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_A));
    }


    @Test
    public void buildActions_WithoutOldPlainEnum_ShouldNotReturnAnyValueAction() {
        final FieldDefinition oldFieldDefinition = FieldDefinition.of(
                EnumFieldType.of(Arrays.asList(ENUM_VALUE_A)),
                FIELD_NAME_1,
                LocalizedString.ofEnglish(LABEL_1),
                false,
                TextInputHint.SINGLE_LINE);

        final FieldDefinition newFieldDefinition = FieldDefinition.of(
                EnumFieldType.of(Collections.emptyList()),
                FIELD_NAME_1,
                LocalizedString.ofEnglish(LABEL_1),
                false,
                TextInputHint.SINGLE_LINE);


        //TODO: For type sync there is no remove enum values action for the field definitions.
        final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

        assertThat(result).isEmpty();
    }


    @Test
    public void buildActions_WithNewLocalizedEnum_ShouldReturnAddLocalizedEnumValueAction() {

        final FieldDefinition oldFieldDefinition = FieldDefinition.of(
                LocalizedEnumFieldType.of(Arrays.asList(LOCALIZED_ENUM_VALUE_A)),
                FIELD_NAME_1,
                LocalizedString.ofEnglish(LABEL_1),
                false,
                TextInputHint.SINGLE_LINE);

        final FieldDefinition newFieldDefinition = FieldDefinition.of(
                LocalizedEnumFieldType.of(Arrays.asList(LOCALIZED_ENUM_VALUE_A, LOCALIZED_ENUM_VALUE_B)),
                FIELD_NAME_1,
                LocalizedString.ofEnglish(LABEL_1),
                false,
                TextInputHint.SINGLE_LINE);


        final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

        assertThat(result).containsExactly(AddLocalizedEnumValue.of(FIELD_NAME_1, LOCALIZED_ENUM_VALUE_B));
    }




}
