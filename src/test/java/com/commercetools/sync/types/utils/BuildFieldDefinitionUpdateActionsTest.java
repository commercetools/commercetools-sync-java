package com.commercetools.sync.types.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.types.EnumFieldType;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.LocalizedEnumFieldType;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.SetFieldType;
import io.sphere.sdk.types.StringFieldType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.updateactions.AddEnumValue;
import io.sphere.sdk.types.commands.updateactions.AddFieldDefinition;
import io.sphere.sdk.types.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.types.commands.updateactions.ChangeFieldDefinitionLabel;
import io.sphere.sdk.types.commands.updateactions.ChangeFieldDefinitionOrder;
import io.sphere.sdk.types.commands.updateactions.RemoveFieldDefinition;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.types.utils.FieldDefinitionFixtures.*;
import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildFieldDefinitionsUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildFieldDefinitionUpdateActionsTest {
    private static final String TYPE_KEY = "key";
    private static final LocalizedString TYPE_NAME = ofEnglish("name");
    private static final LocalizedString TYPE_DESCRIPTION = ofEnglish("description");

    private static final TypeDraft TYPE_DRAFT = TypeDraftBuilder.of(
            TYPE_KEY,
            TYPE_NAME,
            ResourceTypeIdsSetBuilder.of().addCategories().build())
            .description(TYPE_DESCRIPTION)
            .build();

    private static final TypeSyncOptions SYNC_OPTIONS = TypeSyncOptionsBuilder
        .of(mock(SphereClient.class))
        .build();

    @Test
    public void buildFieldDefinitionsUpdateActions_WithNullNewFieldDefAndExistingFieldDefs_ShouldBuild3RemoveActions() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
            oldType,
            TYPE_DRAFT,
            SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            RemoveFieldDefinition.of(FIELD_A),
            RemoveFieldDefinition.of(FIELD_B),
            RemoveFieldDefinition.of(FIELD_C)
        );
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithNullNewFieldDefsAndNoOldFieldDefs_ShouldNotBuildActions() {
        final Type oldType = mock(Type.class);
        when(oldType.getFieldDefinitions()).thenReturn(emptyList());
        when(oldType.getKey()).thenReturn("type_key_1");

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
            oldType,
            TYPE_DRAFT,
            SYNC_OPTIONS
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithNewFieldDefsAndNoOldFieldDefinitions_ShouldBuild3AddActions() {
        final Type oldType = mock(Type.class);
        when(oldType.getFieldDefinitions()).thenReturn(emptyList());
        when(oldType.getKey()).thenReturn("type_key_1");

        final TypeDraft newTypeDraft = readObjectFromResource(
            TYPE_WITH_FIELDS_ABC,
            TypeDraft.class
        );

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
            oldType,
            newTypeDraft,
            SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            AddFieldDefinition.of(FIELD_DEFINITION_A),
            AddFieldDefinition.of(FIELD_DEFINITION_B),
            AddFieldDefinition.of(FIELD_DEFINITION_C)
        );
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithIdenticalFieldDefinitions_ShouldNotBuildUpdateActions() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final TypeDraft newTypeDraft = readObjectFromResource(
                TYPE_WITH_FIELDS_ABC,
                TypeDraft.class
        );

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
                oldType,
                newTypeDraft,
                SYNC_OPTIONS
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithDuplicateFieldDefNames_ShouldNotBuildActionsAndTriggerErrorCb() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final TypeDraft newTypeDraft = readObjectFromResource(
                TYPE_WITH_FIELDS_ABB,
                TypeDraft.class
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final TypeSyncOptions syncOptions = TypeSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
            oldType,
            newTypeDraft,
            syncOptions
        );

        assertThat(updateActions).isEmpty();
        assertThat(errorMessages).hasSize(1);
        assertThat(errorMessages.get(0)).matches("Failed to build update actions for the field definitions of the "
            + "type with the key 'key'. Reason: .*DuplicateNameException: Field definitions "
            + "have duplicated names. Duplicated field definition name: 'b'. Field definitions names are "
            + "expected to be unique inside their type.");
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
        assertThat(exceptions.get(0).getMessage()).contains("Field definitions have duplicated names. "
                + "Duplicated field definition name: 'b'. Field definitions names are expected to be unique "
                + "inside their type.");
        assertThat(exceptions.get(0).getCause()).isExactlyInstanceOf(DuplicateNameException.class);
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithOneMissingFieldDefinition_ShouldBuildRemoveFieldDefAction() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final TypeDraft newTypeDraft = readObjectFromResource(
                TYPE_WITH_FIELDS_AB,
                TypeDraft.class
        );

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
                oldType,
                newTypeDraft,
                SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(RemoveFieldDefinition.of("c"));
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithOneExtraFieldDef_ShouldBuildAddFieldDefinitionAction() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final TypeDraft newTypeDraft = readObjectFromResource(
                TYPE_WITH_FIELDS_ABCD,
                TypeDraft.class
        );

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
                oldType,
                newTypeDraft,
                SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            AddFieldDefinition.of(FIELD_DEFINITION_D)
        );
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithOneFieldDefSwitch_ShouldBuildRemoveAndAddFieldDefActions() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final TypeDraft newTypeDraft = readObjectFromResource(
                TYPE_WITH_FIELDS_ABD,
                TypeDraft.class
        );

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
                oldType,
                newTypeDraft,
                SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            RemoveFieldDefinition.of(FIELD_C),
            AddFieldDefinition.of(FIELD_DEFINITION_D)
        );
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithDifferent_ShouldBuildChangeFieldDefinitionOrderAction() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final TypeDraft newTypeDraft = readObjectFromResource(
                TYPE_WITH_FIELDS_CAB,
                TypeDraft.class
        );

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
                oldType,
                newTypeDraft,
                SYNC_OPTIONS
        );


        assertThat(updateActions).containsExactly(
            ChangeFieldDefinitionOrder
                .of(asList(
                        FIELD_DEFINITION_C.getName(),
                        FIELD_DEFINITION_A.getName(),
                        FIELD_DEFINITION_B.getName()
                ))
        );
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithRemovedAndDiffOrder_ShouldBuildChangeOrderAndRemoveActions() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final TypeDraft newTypeDraft = readObjectFromResource(
                TYPE_WITH_FIELDS_CB,
                TypeDraft.class
        );

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
                oldType,
                newTypeDraft,
                SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            RemoveFieldDefinition.of(FIELD_A),
            ChangeFieldDefinitionOrder
                .of(asList(
                        FIELD_DEFINITION_C.getName(),
                        FIELD_DEFINITION_B.getName()
                    )
                )
        );
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final TypeDraft newTypeDraft = readObjectFromResource(
                TYPE_WITH_FIELDS_ACBD,
                TypeDraft.class
        );

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
                oldType,
                newTypeDraft,
                SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            AddFieldDefinition.of(FIELD_DEFINITION_D),
            ChangeFieldDefinitionOrder
                .of(asList(
                        FIELD_DEFINITION_A.getName(),
                        FIELD_DEFINITION_C.getName(),
                        FIELD_DEFINITION_B.getName(),
                        FIELD_DEFINITION_D.getName()
                    )
                )
        );
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithAddedFieldDefInBetween_ShouldBuildChangeOrderAndAddActions() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final TypeDraft newTypeDraft = readObjectFromResource(
                TYPE_WITH_FIELDS_ADBC,
                TypeDraft.class
        );

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
                oldType,
                newTypeDraft,
                SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            AddFieldDefinition.of(FIELD_DEFINITION_D),
            ChangeFieldDefinitionOrder
                .of(asList(
                        FIELD_DEFINITION_A.getName(),
                        FIELD_DEFINITION_D.getName(),
                        FIELD_DEFINITION_B.getName(),
                        FIELD_DEFINITION_C.getName()
                    )
                )
        );
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithMixedFields_ShouldBuildAllThreeMoveFieldDefActions() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final TypeDraft newTypeDraft = readObjectFromResource(
                TYPE_WITH_FIELDS_CBD,
                TypeDraft.class
        );

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
                oldType,
                newTypeDraft,
                SYNC_OPTIONS
        );
        assertThat(updateActions).containsExactly(
            RemoveFieldDefinition.of(FIELD_A),
            AddFieldDefinition.of(FIELD_DEFINITION_D),
            ChangeFieldDefinitionOrder
                .of(asList(
                        FIELD_DEFINITION_C.getName(),
                        FIELD_DEFINITION_B.getName(),
                        FIELD_DEFINITION_D.getName()
                    )
                )
        );
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithDifferentType_ShouldRemoveOldFieldDefAndAddNewFieldDef() {
        final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

        final TypeDraft newTypeDraft = readObjectFromResource(
                TYPE_WITH_FIELDS_ABC_WITH_DIFFERENT_TYPE,
                TypeDraft.class
        );
        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
                oldType,
                newTypeDraft,
                SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            RemoveFieldDefinition.of(FIELD_A),
            AddFieldDefinition.of(FIELD_DEFINITION_A_LOCALIZED_TYPE)
        );
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithNullNewFieldDef_ShouldSkipNullFieldDefs() {
        // preparation
        final Type oldType = mock(Type.class);
        final FieldDefinition oldFieldDefinition = FieldDefinition.of(
            LocalizedEnumFieldType.of(emptyList()),
            "field_1",
            ofEnglish("label1"),
            false,
            TextInputHint.SINGLE_LINE);


        when(oldType.getFieldDefinitions()).thenReturn(singletonList(oldFieldDefinition));

        final FieldDefinition newFieldDefinition = FieldDefinition.of(
            LocalizedEnumFieldType.of(emptyList()),
            "field_1",
            ofEnglish("label2"),
            false,
            TextInputHint.SINGLE_LINE);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("key", ofEnglish("label"), emptySet())
            .fieldDefinitions(asList(null, newFieldDefinition))
            .build();

        // test
        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
            oldType,
            typeDraft,
            SYNC_OPTIONS
        );

        // assertion
        assertThat(updateActions).containsExactly(
            ChangeFieldDefinitionLabel.of(newFieldDefinition.getName(), newFieldDefinition.getLabel()));
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithDefWithNullName_ShouldBuildChangeFieldDefOrderAction() {
        // preparation
        final Type oldType = mock(Type.class);
        final FieldDefinition oldFieldDefinition = FieldDefinition.of(
            LocalizedEnumFieldType.of(emptyList()),
            "field_1",
            ofEnglish("label1"),
            false,
            TextInputHint.SINGLE_LINE);


        when(oldType.getFieldDefinitions()).thenReturn(singletonList(oldFieldDefinition));

        final FieldDefinition newFieldDefinition = FieldDefinition.of(
            LocalizedEnumFieldType.of(emptyList()),
            null,
            ofEnglish("label2"),
            false,
            TextInputHint.SINGLE_LINE);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("key", ofEnglish("label"), emptySet())
            .fieldDefinitions(asList(null, newFieldDefinition))
            .build();

        // test
        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
            oldType,
            typeDraft,
            SYNC_OPTIONS
        );

        // assertion
        assertThat(updateActions).containsExactly(
            RemoveFieldDefinition.of(oldFieldDefinition.getName()),
            AddFieldDefinition.of(newFieldDefinition));
    }

    @Test
    public void buildFieldDefinitionsUpdateActions_WithDefWithNullType_ShouldBuildChangeFieldDefOrderAction() {
        // preparation
        final Type oldType = mock(Type.class);
        final FieldDefinition oldFieldDefinition = FieldDefinition.of(
            LocalizedEnumFieldType.of(emptyList()),
            "field_1",
            ofEnglish("label1"),
            false,
            TextInputHint.SINGLE_LINE);


        when(oldType.getFieldDefinitions()).thenReturn(singletonList(oldFieldDefinition));

        final FieldDefinition newFieldDefinition = FieldDefinition.of(
            null,
            "field_1",
            ofEnglish("label2"),
            false,
            TextInputHint.SINGLE_LINE);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("key", ofEnglish("label"), emptySet())
            .fieldDefinitions(asList(null, newFieldDefinition))
            .build();

        // test
        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(
            oldType,
            typeDraft,
            SYNC_OPTIONS
        );

        // assertion
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildFieldsUpdateActions_WithSetOfText_ShouldBuildActions() {
        final FieldDefinition newDefinition = FieldDefinition
            .of(SetFieldType.of(StringFieldType.of()), "a", ofEnglish("new_label"), true);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("foo", ofEnglish("name"), emptySet())
            .fieldDefinitions(singletonList(newDefinition))
            .build();

        final FieldDefinition oldDefinition = FieldDefinition
            .of(SetFieldType.of(StringFieldType.of()), "a", ofEnglish("old_label"), true);

        final Type type = mock(Type.class);
        when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(type,
            typeDraft, SYNC_OPTIONS);

        assertThat(updateActions)
            .containsExactly(ChangeFieldDefinitionLabel.of(newDefinition.getName(), newDefinition.getLabel()));
    }

    @Test
    public void buildFieldsUpdateActions_WithSetOfSetOfText_ShouldBuildActions() {

        final FieldDefinition newDefinition = FieldDefinition
            .of(SetFieldType.of(SetFieldType.of(StringFieldType.of())), "a", ofEnglish("new_label"), true);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("foo", ofEnglish("name"), emptySet())
            .fieldDefinitions(singletonList(newDefinition))
            .build();

        final FieldDefinition oldDefinition = FieldDefinition
            .of(SetFieldType.of(SetFieldType.of(StringFieldType.of())), "a", ofEnglish("old_label"), true);

        final Type type = mock(Type.class);
        when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(type,
            typeDraft, SYNC_OPTIONS);

        assertThat(updateActions)
            .containsExactly(ChangeFieldDefinitionLabel.of(newDefinition.getName(), newDefinition.getLabel()));

    }

    @Test
    public void buildFieldsUpdateActions_WithSetOfEnumsChanges_ShouldBuildCorrectActions() {
        // preparation
        final FieldDefinition newDefinition = FieldDefinition
            .of(SetFieldType.of(
                EnumFieldType.of(singletonList(EnumValue.of("foo", "bar")))), "a", ofEnglish("new_label"), true);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("foo", ofEnglish("name"), emptySet())
            .fieldDefinitions(singletonList(newDefinition))
            .build();

        final FieldDefinition oldDefinition = FieldDefinition
            .of(SetFieldType.of(
                EnumFieldType.of(emptyList())), "a", ofEnglish("new_label"), true);

        final Type type = mock(Type.class);
        when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

        // test
        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(type,
            typeDraft, SYNC_OPTIONS);

        // assertion
        assertThat(updateActions).containsExactly(AddEnumValue.of("a", EnumValue.of("foo", "bar")));

    }

    @Test
    public void buildFieldsUpdateActions_WithSetOfIdenticalEnums_ShouldNotBuildActions() {
        // preparation
        final FieldDefinition newDefinition = FieldDefinition
            .of(SetFieldType.of(EnumFieldType.of(emptyList())), "a", ofEnglish("new_label"), true);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("foo", ofEnglish("name"), emptySet())
            .fieldDefinitions(singletonList(newDefinition))
            .build();

        final FieldDefinition oldDefinition = FieldDefinition
            .of(SetFieldType.of(
                EnumFieldType.of(emptyList())), "a", ofEnglish("new_label"), true);

        final Type type = mock(Type.class);
        when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

        // test
        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(type,
            typeDraft, SYNC_OPTIONS);

        // assertion
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildFieldsUpdateActions_WithSetOfLEnumsChanges_ShouldBuildCorrectActions() {
        // preparation
        final SetFieldType newSetFieldType = SetFieldType.of(
            LocalizedEnumFieldType.of(singletonList(LocalizedEnumValue.of("foo", ofEnglish("bar")))));

        final FieldDefinition newDefinition = FieldDefinition
            .of(newSetFieldType, "a", ofEnglish("new_label"), true);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("foo", ofEnglish("name"), emptySet())
            .fieldDefinitions(singletonList(newDefinition))
            .build();

        final SetFieldType oldSetFieldType = SetFieldType.of(LocalizedEnumFieldType.of(emptyList()));
        final FieldDefinition oldDefinition = FieldDefinition
            .of(oldSetFieldType, "a", ofEnglish("new_label"), true);

        final Type type = mock(Type.class);
        when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

        // test
        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(type,
            typeDraft, SYNC_OPTIONS);

        // assertion
        assertThat(updateActions)
            .containsExactly(AddLocalizedEnumValue.of("a", LocalizedEnumValue.of("foo", ofEnglish("bar"))));
    }

    @Test
    public void buildFieldsUpdateActions_WithSetOfIdenticalLEnums_ShouldBuildNoActions() {
        // preparation
        final SetFieldType newSetFieldType = SetFieldType.of(SetFieldType.of(LocalizedEnumFieldType.of(emptyList())));

        final FieldDefinition newDefinition = FieldDefinition
            .of(newSetFieldType, "a", ofEnglish("new_label"), true);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("foo", ofEnglish("name"), emptySet())
            .fieldDefinitions(singletonList(newDefinition))
            .build();

        final SetFieldType oldSetFieldType = SetFieldType.of(SetFieldType.of(LocalizedEnumFieldType.of(emptyList())));
        final FieldDefinition oldDefinition = FieldDefinition
            .of(oldSetFieldType, "a", ofEnglish("new_label"), true);

        final Type type = mock(Type.class);
        when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

        // test
        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(type,
            typeDraft, SYNC_OPTIONS);

        // assertion
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildFieldsUpdateActions_WithSetOfLEnumsChangesAndDefinitionLabelChange_ShouldBuildCorrectActions() {
        // preparation
        final SetFieldType newSetFieldType = SetFieldType.of(
            LocalizedEnumFieldType.of(singletonList(LocalizedEnumValue.of("foo", ofEnglish("bar")))));

        final FieldDefinition newDefinition = FieldDefinition
            .of(newSetFieldType, "a", ofEnglish("new_label"), true);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("foo", ofEnglish("name"), emptySet())
            .fieldDefinitions(singletonList(newDefinition))
            .build();

        final SetFieldType oldSetFieldType = SetFieldType.of((LocalizedEnumFieldType.of(emptyList())));
        final FieldDefinition oldDefinition = FieldDefinition
            .of(oldSetFieldType, "a", ofEnglish("old_label"), true);

        final Type type = mock(Type.class);
        when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

        // test
        final List<UpdateAction<Type>> updateActions = buildFieldDefinitionsUpdateActions(type,
            typeDraft, SYNC_OPTIONS);

        // assertion
        assertThat(updateActions)
            .containsExactlyInAnyOrder(
                AddLocalizedEnumValue.of("a", LocalizedEnumValue.of("foo", ofEnglish("bar"))),
                ChangeFieldDefinitionLabel.of("a", ofEnglish("new_label")));
    }
}
