package com.commercetools.sync.types.utils.typeactionutils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.LocalizedEnumFieldType;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.updateactions.AddFieldDefinition;
import io.sphere.sdk.types.commands.updateactions.ChangeFieldDefinitionLabel;
import io.sphere.sdk.types.commands.updateactions.ChangeFieldDefinitionOrder;
import io.sphere.sdk.types.commands.updateactions.RemoveFieldDefinition;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.types.FieldDefinitionTestHelper.localizedStringFieldDefinition;
import static com.commercetools.sync.types.FieldDefinitionTestHelper.stringFieldDefinition;
import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildFieldDefinitionsUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildFieldDefinitionUpdateActionsTest {
    private static final String RES_ROOT =
        "com/commercetools/sync/types/utils/updatefielddefinitions/fields/";

    private static final String TYPE_WITH_FIELDS_AB =
        RES_ROOT + "type-with-field-definitions-ab.json";
    private static final String TYPE_WITH_FIELDS_ABB =
        RES_ROOT + "type-with-field-definitions-abb.json";
    private static final String TYPE_WITH_FIELDS_ABC =
        RES_ROOT + "type-with-field-definitions-abc.json";
    private static final String TYPE_WITH_FIELDS_ABCD =
        RES_ROOT + "type-with-field-definitions-abcd.json";
    private static final String TYPE_WITH_FIELDS_ABD =
        RES_ROOT + "type-with-field-definitions-abd.json";
    private static final String TYPE_WITH_FIELDS_CAB =
        RES_ROOT + "type-with-field-definitions-cab.json";
    private static final String TYPE_WITH_FIELDS_CB =
        RES_ROOT + "type-with-field-definitions-cb.json";
    private static final String TYPE_WITH_FIELDS_ACBD =
        RES_ROOT + "type-with-field-definitions-acbd.json";
    private static final String TYPE_WITH_FIELDS_ADBC =
        RES_ROOT + "type-with-field-definitions-adbc.json";
    private static final String TYPE_WITH_FIELDS_CBD =
        RES_ROOT + "type-with-field-definitions-cbd.json";
    private static final String TYPE_WITH_FIELDS_ABC_WITH_DIFFERENT_TYPE =
        RES_ROOT + "type-with-field-definitions-abc-with-different-type.json";

    private static final TypeSyncOptions SYNC_OPTIONS = TypeSyncOptionsBuilder
        .of(mock(SphereClient.class))
        .build();

    private static final String FIELD_A = "a";
    private static final String FIELD_B = "b";
    private static final String FIELD_C = "c";
    private static final String FIELD_D = "d";
    private static final String LABEL_EN = "label_en";

    private static final FieldDefinition FIELD_DEFINITION_A =
            stringFieldDefinition(FIELD_A, LABEL_EN, false, TextInputHint.SINGLE_LINE);
    private static final FieldDefinition FIELD_DEFINITION_A_LOCALIZED_TYPE =
            localizedStringFieldDefinition(FIELD_A, LABEL_EN, false, TextInputHint.SINGLE_LINE);
    private static final FieldDefinition FIELD_DEFINITION_B =
            stringFieldDefinition(FIELD_B, LABEL_EN, false, TextInputHint.SINGLE_LINE);
    private static final FieldDefinition FIELD_DEFINITION_C =
            stringFieldDefinition(FIELD_C, LABEL_EN, false, TextInputHint.SINGLE_LINE);
    private static final FieldDefinition FIELD_DEFINITION_D =
            stringFieldDefinition(FIELD_D, LABEL_EN, false, TextInputHint.SINGLE_LINE);

    private static final String TYPE_KEY = "key";
    private static final LocalizedString TYPE_NAME = LocalizedString.ofEnglish("name");
    private static final LocalizedString TYPE_DESCRIPTION = LocalizedString.ofEnglish("description");

    private static final TypeDraft TYPE_DRAFT = TypeDraftBuilder.of(
            TYPE_KEY,
            TYPE_NAME,
            ResourceTypeIdsSetBuilder.of().addCategories().build())
            .description(TYPE_DESCRIPTION)
            .build();

    @Test
    public void buildFieldDefinitionsUpdateActions_WithNullNewFieldDefinitionsAndExistingFieldDefinitions_ShouldBuild3RemoveActions() {
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
    public void buildFieldDefinitionsUpdateActions_WithNullNewFieldDefinitionsAndNoOldFieldDefinitions_ShouldNotBuildActions() {
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
    public void buildFieldDefinitionsUpdateActions_WithNewFieldDefinitionsAndNoOldFieldDefinitions_ShouldBuild3AddActions() {
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
    public void buildFieldDefinitionsUpdateActions_WithDuplicateFieldDefinitionNames_ShouldNotBuildActionsAndTriggerErrorCb() {
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
    public void buildFieldDefinitionsUpdateActions_WithOneMissingFieldDefinition_ShouldBuildRemoveFieldDefinitionAction() {
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
    public void buildFieldDefinitionsUpdateActions_WithOneExtraFieldDefinition_ShouldBuildAddFieldDefinitionAction() {
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
    public void buildFieldDefinitionsUpdateActions_WithOneFieldDefinitionSwitch_ShouldBuildRemoveAndAddFieldDefinitionsActions() {
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
    public void buildFieldDefinitionsUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
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
    public void buildFieldDefinitionsUpdateActions_WithAddedFieldDefinitionInBetween_ShouldBuildChangeOrderAndAddActions() {
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
    public void buildFieldDefinitionsUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveFieldDefinitionActions() {
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
    public void buildFieldDefinitionsUpdateActions_WithDifferentType_ShouldRemoveOldFieldDefinitionAndAddNewFieldDefinition() {
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
    public void buildFieldDefinitionsUpdateActions_WithNullNewFieldDefinition_ShouldSkipNullFieldDefinitions() {
        // preparation
        final Type oldType = mock(Type.class);
        final FieldDefinition oldFieldDefinition = FieldDefinition.of(
            LocalizedEnumFieldType.of(emptyList()),
            "field_1",
            LocalizedString.ofEnglish("label1"),
            false,
            TextInputHint.SINGLE_LINE);


        when(oldType.getFieldDefinitions()).thenReturn(singletonList(oldFieldDefinition));

        final FieldDefinition newFieldDefinition = FieldDefinition.of(
            LocalizedEnumFieldType.of(emptyList()),
            "field_1",
            LocalizedString.ofEnglish("label2"),
            false,
            TextInputHint.SINGLE_LINE);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("key", LocalizedString.ofEnglish("label"), emptySet())
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
    public void buildFieldDefinitionsUpdateActions_WithDefinitionWithNullName_ShouldBuildChangeFieldDefinitionOrderAction() {
        // preparation
        final Type oldType = mock(Type.class);
        final FieldDefinition oldFieldDefinition = FieldDefinition.of(
            LocalizedEnumFieldType.of(emptyList()),
            "field_1",
            LocalizedString.ofEnglish("label1"),
            false,
            TextInputHint.SINGLE_LINE);


        when(oldType.getFieldDefinitions()).thenReturn(singletonList(oldFieldDefinition));

        final FieldDefinition newFieldDefinition = FieldDefinition.of(
            LocalizedEnumFieldType.of(emptyList()),
            null,
            LocalizedString.ofEnglish("label2"),
            false,
            TextInputHint.SINGLE_LINE);

        final TypeDraft typeDraft = TypeDraftBuilder
            .of("key", LocalizedString.ofEnglish("label"), emptySet())
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
}
