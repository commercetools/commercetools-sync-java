package com.commercetools.sync.producttypes.utils.producttypeactionutils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.EnumAttributeType;
import io.sphere.sdk.products.attributes.LocalizedEnumAttributeType;
import io.sphere.sdk.products.attributes.LocalizedStringAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.updateactions.AddAttributeDefinition;
import io.sphere.sdk.producttypes.commands.updateactions.AddEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeConstraint;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeDefinitionLabel;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeOrderByName;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeEnumValueOrder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeInputHint;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeIsSearchable;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueOrder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangePlainEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveAttributeDefinition;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveEnumValues;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateActionUtils.buildAttributesUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildAttributeDefinitionUpdateActionsTest {
    private static final String RES_ROOT =
        "com/commercetools/sync/producttypes/utils/updateattributedefinitions/attributes/";

    private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_AB =
        RES_ROOT + "product-type-with-attribute-definitions-ab.json";
    private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ABB =
        RES_ROOT + "product-type-with-attribute-definitions-abb.json";
    private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ABC =
        RES_ROOT + "product-type-with-attribute-definitions-abc.json";
    private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ABCD =
        RES_ROOT + "product-type-with-attribute-definitions-abcd.json";
    private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ABD =
        RES_ROOT + "product-type-with-attribute-definitions-abd.json";
    private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_CAB =
        RES_ROOT + "product-type-with-attribute-definitions-cab.json";
    private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_CB =
        RES_ROOT + "product-type-with-attribute-definitions-cb.json";
    private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ACBD =
        RES_ROOT + "product-type-with-attribute-definitions-acbd.json";
    private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ADBC =
        RES_ROOT + "product-type-with-attribute-definitions-adbc.json";
    private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_CBD =
        RES_ROOT + "product-type-with-attribute-definitions-cbd.json";
    private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ABC_WITH_DIFFERENT_TYPE =
        RES_ROOT + "product-type-with-attribute-definitions-abc-with-different-type.json";

    private static final ProductTypeSyncOptions SYNC_OPTIONS = ProductTypeSyncOptionsBuilder
        .of(mock(SphereClient.class))
        .build();

    private static final AttributeDefinition ATTRIBUTE_DEFINITION_A = AttributeDefinitionBuilder
        .of("a", ofEnglish("label_en"), StringAttributeType.of())
        .build();

    private static final AttributeDefinition ATTRIBUTE_DEFINITION_A_LOCALIZED_TYPE = AttributeDefinitionBuilder
        .of("a", ofEnglish("label_en"), LocalizedStringAttributeType.of())
        .build();

    private static final AttributeDefinition ATTRIBUTE_DEFINITION_B = AttributeDefinitionBuilder
        .of("b", ofEnglish("label_en"), StringAttributeType.of())
        .build();

    private static final AttributeDefinition ATTRIBUTE_DEFINITION_C = AttributeDefinitionBuilder
        .of("c", ofEnglish("label_en"), StringAttributeType.of())
        .build();

    private static final AttributeDefinition ATTRIBUTE_DEFINITION_D = AttributeDefinitionBuilder
        .of("d", ofEnglish("label_en"), StringAttributeType.of())
        .build();

    @Test
    public void buildAttributesUpdateActions_WithNullNewAttributesAndExistingAttributes_ShouldBuild3RemoveActions() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            ProductTypeDraftBuilder.of("key", "name", "key", null).build(),
            SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            RemoveAttributeDefinition.of("a"),
            RemoveAttributeDefinition.of("b"),
            RemoveAttributeDefinition.of("c")
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithNullNewAttributesAndNoOldAttributes_ShouldNotBuildActions() {
        final ProductType oldProductType = mock(ProductType.class);
        when(oldProductType.getAttributes()).thenReturn(emptyList());
        when(oldProductType.getKey()).thenReturn("product_type_key_1");

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            ProductTypeDraftBuilder.of("key", "name", "key", null).build(),
            SYNC_OPTIONS
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildAttributesUpdateActions_WithNewAttributesAndNoOldAttributes_ShouldBuild3AddActions() {
        final ProductType oldProductType = mock(ProductType.class);
        when(oldProductType.getAttributes()).thenReturn(emptyList());
        when(oldProductType.getKey()).thenReturn("product_type_key_1");

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_ABC,
            ProductTypeDraft.class
        );

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            newProductTypeDraft,
            SYNC_OPTIONS
        );

        assertThat(updateActions).hasSize(3);
        assertThat(updateActions).allSatisfy(action -> {
            assertThat(action).isExactlyInstanceOf(AddAttributeDefinition.class);
            final AddAttributeDefinition addAttributeDefinition = (AddAttributeDefinition) action;
            final AttributeDefinitionDraft attribute = addAttributeDefinition.getAttribute();
            assertThat(newProductTypeDraft.getAttributes()).contains(attribute);
        });
    }

    @Test
    public void buildAttributesUpdateActions_WithIdenticalAttributes_ShouldNotBuildUpdateActions() {
        final ProductType oldProductType = mock(ProductType.class);
        when(oldProductType.getAttributes())
            .thenReturn(asList(ATTRIBUTE_DEFINITION_A, ATTRIBUTE_DEFINITION_B, ATTRIBUTE_DEFINITION_C));

        final AttributeDefinitionDraft attributeA = AttributeDefinitionDraftBuilder
            .of(ATTRIBUTE_DEFINITION_A.getAttributeType(), ATTRIBUTE_DEFINITION_A.getName(),
                ATTRIBUTE_DEFINITION_A.getLabel(), ATTRIBUTE_DEFINITION_A.isRequired())
            .build();

        final AttributeDefinitionDraft attributeB = AttributeDefinitionDraftBuilder
            .of(ATTRIBUTE_DEFINITION_B.getAttributeType(), ATTRIBUTE_DEFINITION_B.getName(),
                ATTRIBUTE_DEFINITION_B.getLabel(), ATTRIBUTE_DEFINITION_B.isRequired())
            .build();

        final AttributeDefinitionDraft attributeC = AttributeDefinitionDraftBuilder
            .of(ATTRIBUTE_DEFINITION_C.getAttributeType(), ATTRIBUTE_DEFINITION_C.getName(),
                ATTRIBUTE_DEFINITION_C.getLabel(), ATTRIBUTE_DEFINITION_C.isRequired())
            .build();

        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of(oldProductType.getKey(), oldProductType.getName(), oldProductType.getDescription(),
                asList(attributeA, attributeB, attributeC))
            .build();

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            productTypeDraft,
            SYNC_OPTIONS
        );

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildAttributesUpdateActions_WithChangedAttributes_ShouldBuildUpdateActions() {
        final ProductType oldProductType = mock(ProductType.class);
        when(oldProductType.getAttributes())
            .thenReturn(asList(ATTRIBUTE_DEFINITION_A, ATTRIBUTE_DEFINITION_B, ATTRIBUTE_DEFINITION_C));

        final AttributeDefinitionDraft attributeA = AttributeDefinitionDraftBuilder
            .of(ATTRIBUTE_DEFINITION_A.getAttributeType(), ATTRIBUTE_DEFINITION_A.getName(),
                ATTRIBUTE_DEFINITION_A.getLabel(), ATTRIBUTE_DEFINITION_A.isRequired())
            .isSearchable(false)
            .build();

        final AttributeDefinitionDraft attributeB = AttributeDefinitionDraftBuilder
            .of(ATTRIBUTE_DEFINITION_B.getAttributeType(), ATTRIBUTE_DEFINITION_B.getName(),
                ofEnglish("newLabel"), ATTRIBUTE_DEFINITION_B.isRequired())
            .inputHint(TextInputHint.MULTI_LINE)
            .build();

        final AttributeDefinitionDraft attributeC = AttributeDefinitionDraftBuilder
            .of(ATTRIBUTE_DEFINITION_C.getAttributeType(), ATTRIBUTE_DEFINITION_C.getName(),
                ATTRIBUTE_DEFINITION_C.getLabel(), ATTRIBUTE_DEFINITION_C.isRequired())
            .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
            .build();

        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of(oldProductType.getKey(), oldProductType.getName(), oldProductType.getDescription(),
                asList(attributeA, attributeB, attributeC))
            .build();

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            productTypeDraft,
            SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactlyInAnyOrder(
            ChangeIsSearchable.of(ATTRIBUTE_DEFINITION_A.getName(), false),
            ChangeInputHint.of(ATTRIBUTE_DEFINITION_B.getName(), TextInputHint.MULTI_LINE),
            ChangeAttributeConstraint.of(ATTRIBUTE_DEFINITION_C.getName(), AttributeConstraint.SAME_FOR_ALL),
            ChangeAttributeDefinitionLabel.of(ATTRIBUTE_DEFINITION_B.getName(), ofEnglish("newLabel"))
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithDuplicateAttributeNames_ShouldNotBuildActionsAndTriggerErrorCb() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_ABB,
            ProductTypeDraft.class
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            newProductTypeDraft,
            syncOptions
        );

        assertThat(updateActions).isEmpty();
        assertThat(errorMessages).hasSize(1);
        assertThat(errorMessages.get(0)).matches("Failed to build update actions for the attributes definitions of the "
            + "product type with the key 'key'. Reason: .*DuplicateNameException: Attribute definitions drafts "
            + "have duplicated names. Duplicated attribute definition name: 'b'. Attribute definitions names are "
            + "expected to be unique inside their product type.");
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
        assertThat(exceptions.get(0).getMessage()).contains("Attribute definitions drafts have duplicated names. "
                + "Duplicated attribute definition name: 'b'. Attribute definitions names are expected to be unique "
                + "inside their product type.");
        assertThat(exceptions.get(0).getCause()).isExactlyInstanceOf(DuplicateNameException.class);
    }

    @Test
    public void buildAttributesUpdateActions_WithOneMissingAttribute_ShouldBuildRemoveAttributeAction() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_AB,
            ProductTypeDraft.class
        );

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
                oldProductType,
                newProductTypeDraft,
                SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(RemoveAttributeDefinition.of("c"));
    }

    @Test
    public void buildAttributesUpdateActions_WithOneExtraAttribute_ShouldBuildAddAttributesAction() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_ABCD,
            ProductTypeDraft.class
        );

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            newProductTypeDraft,
            SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            AddAttributeDefinition.of(AttributeDefinitionDraftBuilder
                .of(ATTRIBUTE_DEFINITION_D.getAttributeType(), ATTRIBUTE_DEFINITION_D.getName(),
                    ATTRIBUTE_DEFINITION_D.getLabel(), ATTRIBUTE_DEFINITION_D.isRequired())
                .isSearchable(true)
                .build())
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithOneAttributeSwitch_ShouldBuildRemoveAndAddAttributesActions() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_ABD,
            ProductTypeDraft.class
        );

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            newProductTypeDraft,
            SYNC_OPTIONS
        );


        assertThat(updateActions).containsExactly(
            RemoveAttributeDefinition.of("c"),
            AddAttributeDefinition.of(AttributeDefinitionDraftBuilder
                .of(ATTRIBUTE_DEFINITION_D.getAttributeType(), ATTRIBUTE_DEFINITION_D.getName(),
                    ATTRIBUTE_DEFINITION_D.getLabel(), ATTRIBUTE_DEFINITION_D.isRequired())
                .isSearchable(true)
                .build())
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithDifferentOrder_ShouldBuildChangeAttributeOrderAction() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_CAB,
            ProductTypeDraft.class
        );

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            newProductTypeDraft,
            SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            ChangeAttributeOrderByName
                .of(asList(
                    ATTRIBUTE_DEFINITION_C.getName(),
                    ATTRIBUTE_DEFINITION_A.getName(),
                    ATTRIBUTE_DEFINITION_B.getName()
                ))
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_CB,
            ProductTypeDraft.class
        );


        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            newProductTypeDraft,
            SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            RemoveAttributeDefinition.of("a"),
            ChangeAttributeOrderByName
                .of(asList(
                    ATTRIBUTE_DEFINITION_C.getName(),
                    ATTRIBUTE_DEFINITION_B.getName()
                    )
                )
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_ACBD,
            ProductTypeDraft.class
        );

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            newProductTypeDraft,
            SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            AddAttributeDefinition.of(AttributeDefinitionDraftBuilder
                .of(ATTRIBUTE_DEFINITION_D.getAttributeType(),
                    ATTRIBUTE_DEFINITION_D.getName(), ATTRIBUTE_DEFINITION_D.getLabel(),
                    ATTRIBUTE_DEFINITION_D.isRequired())
                .isSearchable(true)
                .inputHint(TextInputHint.SINGLE_LINE)
                .attributeConstraint(AttributeConstraint.NONE)
                .build()),
            ChangeAttributeOrderByName
                .of(asList(
                    ATTRIBUTE_DEFINITION_A.getName(),
                    ATTRIBUTE_DEFINITION_C.getName(),
                    ATTRIBUTE_DEFINITION_B.getName(),
                    ATTRIBUTE_DEFINITION_D.getName()
                    )
                )
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithAddedAttributeInBetween_ShouldBuildChangeOrderAndAddActions() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_ADBC,
            ProductTypeDraft.class
        );

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            newProductTypeDraft,
            SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            AddAttributeDefinition.of(AttributeDefinitionDraftBuilder
                .of(ATTRIBUTE_DEFINITION_D.getAttributeType(),
                    ATTRIBUTE_DEFINITION_D.getName(), ATTRIBUTE_DEFINITION_D.getLabel(),
                    ATTRIBUTE_DEFINITION_D.isRequired())
                .isSearchable(true)
                .build()),
            ChangeAttributeOrderByName
                .of(asList(
                    ATTRIBUTE_DEFINITION_A.getName(),
                    ATTRIBUTE_DEFINITION_D.getName(),
                    ATTRIBUTE_DEFINITION_B.getName(),
                    ATTRIBUTE_DEFINITION_C.getName()
                    )
                )
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveAttributeActions() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_CBD,
            ProductTypeDraft.class
        );

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            newProductTypeDraft,
            SYNC_OPTIONS
        );

        assertThat(updateActions).containsExactly(
            RemoveAttributeDefinition.of("a"),
            AddAttributeDefinition.of(AttributeDefinitionDraftBuilder
                .of(ATTRIBUTE_DEFINITION_D.getAttributeType(),
                    ATTRIBUTE_DEFINITION_D.getName(), ATTRIBUTE_DEFINITION_D.getLabel(),
                    ATTRIBUTE_DEFINITION_D.isRequired())
                .isSearchable(true).build()),
            ChangeAttributeOrderByName
                .of(asList(
                    ATTRIBUTE_DEFINITION_C.getName(),
                    ATTRIBUTE_DEFINITION_B.getName(),
                    ATTRIBUTE_DEFINITION_D.getName()
                    )
                )
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithDifferentAttributeType_ShouldRemoveOldAttributeAndAddNewAttribute() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_ABC_WITH_DIFFERENT_TYPE,
            ProductTypeDraft.class
        );

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            newProductTypeDraft,
            syncOptions
        );

        assertThat(updateActions).containsExactly(
            RemoveAttributeDefinition.of("a"),
            AddAttributeDefinition.of(AttributeDefinitionDraftBuilder
                .of(ATTRIBUTE_DEFINITION_A_LOCALIZED_TYPE.getAttributeType(),
                    ATTRIBUTE_DEFINITION_A_LOCALIZED_TYPE.getName(), ATTRIBUTE_DEFINITION_A_LOCALIZED_TYPE.getLabel(),
                    ATTRIBUTE_DEFINITION_A_LOCALIZED_TYPE.isRequired())
                .isSearchable(true)
                .build())
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithANullAttributeDefinitionDraft_ShouldSkipNullAttributes() {
        // preparation
        final ProductType oldProductType = mock(ProductType.class);
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
            .of(
                "attributeName1",
                LocalizedString.ofEnglish("label1"),
                LocalizedEnumAttributeType.of(Collections.emptyList()))
            .isRequired(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        when(oldProductType.getAttributes()).thenReturn(singletonList(attributeDefinition));

        final AttributeDefinitionDraft attributeDefinitionDraftWithDifferentLabel = AttributeDefinitionDraftBuilder
            .of(
                LocalizedEnumAttributeType.of(Collections.emptyList()),
                "attributeName1",
                LocalizedString.ofEnglish("label2"),
                false
            )
            .attributeConstraint(AttributeConstraint.NONE)
            .inputTip(LocalizedString.ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("key", "name", "key", asList(null, attributeDefinitionDraftWithDifferentLabel))
            .build();

        // test
        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            productTypeDraft,
            SYNC_OPTIONS
        );

        // assertion
        assertThat(updateActions).containsExactly(
            ChangeAttributeDefinitionLabel.of(attributeDefinitionDraftWithDifferentLabel.getName(),
                attributeDefinitionDraftWithDifferentLabel.getLabel()));
    }

    @Test
    public void buildAttributesUpdateActions_WithSetOfText_ShouldBuildActions() {

        final AttributeDefinitionDraft newDefinition = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(StringAttributeType.of()), "a", ofEnglish("new_label"), true)
            .build();
        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("foo", "name", "desc", singletonList(newDefinition))
            .build();

        final AttributeDefinition oldDefinition = AttributeDefinitionBuilder
            .of("a", ofEnglish("old_label"), SetAttributeType.of(StringAttributeType.of()))
            .build();
        final ProductType productType = mock(ProductType.class);
        when(productType.getAttributes()).thenReturn(singletonList(oldDefinition));

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(productType,
            productTypeDraft, SYNC_OPTIONS);

        assertThat(updateActions)
            .containsExactly(ChangeAttributeDefinitionLabel.of(newDefinition.getName(), newDefinition.getLabel()));

    }

    @Test
    public void buildAttributesUpdateActions_WithSetOfSetOfText_ShouldBuildActions() {

        final AttributeDefinitionDraft newDefinition = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(SetAttributeType.of(StringAttributeType.of())), "a", ofEnglish("new_label"), true)
            .build();
        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("foo", "name", "desc", singletonList(newDefinition))
            .build();

        final AttributeDefinition oldDefinition = AttributeDefinitionBuilder
            .of("a", ofEnglish("old_label"), SetAttributeType.of(SetAttributeType.of(StringAttributeType.of())))
            .build();
        final ProductType productType = mock(ProductType.class);
        when(productType.getAttributes()).thenReturn(singletonList(oldDefinition));

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(productType,
            productTypeDraft, SYNC_OPTIONS);

        assertThat(updateActions)
            .containsExactly(ChangeAttributeDefinitionLabel.of(newDefinition.getName(), newDefinition.getLabel()));

    }

    @Test
    public void buildAttributesUpdateActions_WithSetOfEnumsChanges_ShouldBuildCorrectActions() {
        // preparation
        final SetAttributeType newSetOfEnumType = SetAttributeType.of(
            EnumAttributeType.of(
                asList(
                    EnumValue.of("a", "a"),
                    EnumValue.of("b", "newB"),
                    EnumValue.of("c", "c")
                )));

        final AttributeDefinitionDraft newDefinition = AttributeDefinitionDraftBuilder
            .of(newSetOfEnumType, "a", ofEnglish("new_label"), true)
            .build();
        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("foo", "name", "desc", singletonList(newDefinition))
            .build();


        final SetAttributeType oldSetOfEnumType = SetAttributeType.of(
            EnumAttributeType.of(
                asList(
                    EnumValue.of("d", "d"),
                    EnumValue.of("b", "b"),
                    EnumValue.of("a", "a")
                )));
        final AttributeDefinition oldDefinition = AttributeDefinitionBuilder
            .of("a", ofEnglish("new_label"), oldSetOfEnumType)
            .build();
        final ProductType productType = mock(ProductType.class);
        when(productType.getAttributes()).thenReturn(singletonList(oldDefinition));

        // test
        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(productType,
            productTypeDraft, SYNC_OPTIONS);

        // assertion
        assertThat(updateActions).containsExactly(
            RemoveEnumValues.of("a", "d"),
            ChangePlainEnumValueLabel.of("a", EnumValue.of("b", "newB")),
            AddEnumValue.of("a", EnumValue.of("c", "c")),
            ChangeEnumValueOrder.of("a", asList(
                EnumValue.of("a", "a"),
                EnumValue.of("b", "newB"),
                EnumValue.of("c", "c")
            )));
    }

    @Test
    public void buildAttributesUpdateActions_WithSetOfIdenticalEnums_ShouldNotBuildActions() {
        // preparation
        final SetAttributeType newSetOfEnumType = SetAttributeType.of(
            EnumAttributeType.of(singletonList(EnumValue.of("foo", "bar"))));

        final AttributeDefinitionDraft newDefinition = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(newSetOfEnumType), "a", ofEnglish("new_label"), true)
            .build();
        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("foo", "name", "desc", singletonList(newDefinition))
            .build();


        final SetAttributeType oldSetOfEnumType = SetAttributeType.of(EnumAttributeType.of(EnumValue.of("foo", "bar")));
        final AttributeDefinition oldDefinition = AttributeDefinitionBuilder
            .of("a", ofEnglish("new_label"), oldSetOfEnumType)
            .build();
        final ProductType productType = mock(ProductType.class);
        when(productType.getAttributes()).thenReturn(singletonList(oldDefinition));

        // test
        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(productType,
            productTypeDraft, SYNC_OPTIONS);

        // assertion
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildAttributesUpdateActions_WithSetOfLEnumsChanges_ShouldBuildCorrectActions() {
        // preparation
        final SetAttributeType newSetOfLenumType = SetAttributeType.of(
            LocalizedEnumAttributeType.of(singletonList(LocalizedEnumValue.of("foo", ofEnglish("bar")))));

        final AttributeDefinitionDraft newDefinition = AttributeDefinitionDraftBuilder
            .of(newSetOfLenumType, "a", ofEnglish("new_label"), true)
            .build();
        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("foo", "name", "desc", singletonList(newDefinition))
            .build();

        final SetAttributeType oldSetOfLenumType = SetAttributeType.of(
            LocalizedEnumAttributeType.of(emptyList()));

        final AttributeDefinition oldDefinition = AttributeDefinitionBuilder
            .of("a", ofEnglish("new_label"), oldSetOfLenumType)
            .build();
        final ProductType productType = mock(ProductType.class);
        when(productType.getAttributes()).thenReturn(singletonList(oldDefinition));

        // test
        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(productType,
            productTypeDraft, SYNC_OPTIONS);

        // assertion
        assertThat(updateActions)
            .containsExactly(AddLocalizedEnumValue.of("a", LocalizedEnumValue.of("foo", ofEnglish("bar"))));
    }

    @Test
    public void buildAttributesUpdateActions_WithSetOfIdenticalLEnums_ShouldBuildNoActions() {
        // preparation
        final SetAttributeType newSetOfLenumType = SetAttributeType.of(
            LocalizedEnumAttributeType.of(singletonList(LocalizedEnumValue.of("foo", ofEnglish("bar")))));

        final AttributeDefinitionDraft newDefinition = AttributeDefinitionDraftBuilder
            .of(SetAttributeType.of(newSetOfLenumType), "a", ofEnglish("new_label"), true)
            .build();
        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("foo", "name", "desc", singletonList(newDefinition))
            .build();

        final SetAttributeType oldSetOfLenumType = SetAttributeType.of(SetAttributeType.of(
            LocalizedEnumAttributeType.of(singletonList(LocalizedEnumValue.of("foo", ofEnglish("bar"))))));

        final AttributeDefinition oldDefinition = AttributeDefinitionBuilder
            .of("a", ofEnglish("new_label"), oldSetOfLenumType)
            .build();
        final ProductType productType = mock(ProductType.class);
        when(productType.getAttributes()).thenReturn(singletonList(oldDefinition));

        // test
        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(productType,
            productTypeDraft, SYNC_OPTIONS);

        // assertion
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildAttributesUpdateActions_WithSetOfLEnumsChangesAndDefLabelChange_ShouldBuildCorrectActions() {
        // preparation
        final SetAttributeType newSetOfLenumType = SetAttributeType.of(
            LocalizedEnumAttributeType.of(
                asList(
                    LocalizedEnumValue.of("a", ofEnglish("a")),
                    LocalizedEnumValue.of("b", ofEnglish("newB")),
                    LocalizedEnumValue.of("c", ofEnglish("c"))
                )
            ));

        final AttributeDefinitionDraft newDefinition = AttributeDefinitionDraftBuilder
            .of(newSetOfLenumType, "a", ofEnglish("new_label"), true)
            .build();
        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
            .of("foo", "name", "desc", singletonList(newDefinition))
            .build();

        final SetAttributeType oldSetOfLenumType = SetAttributeType.of(
            LocalizedEnumAttributeType.of(
                asList(
                    LocalizedEnumValue.of("d", ofEnglish("d")),
                    LocalizedEnumValue.of("b", ofEnglish("b")),
                    LocalizedEnumValue.of("a", ofEnglish("a"))
                )
            ));

        final AttributeDefinition oldDefinition = AttributeDefinitionBuilder
            .of("a", ofEnglish("old_label"), oldSetOfLenumType)
            .build();
        final ProductType productType = mock(ProductType.class);
        when(productType.getAttributes()).thenReturn(singletonList(oldDefinition));

        // test
        final List<UpdateAction<ProductType>> updateActions =
            buildAttributesUpdateActions(productType, productTypeDraft, SYNC_OPTIONS);

        // assertion
        assertThat(updateActions)
            .containsExactlyInAnyOrder(
                RemoveEnumValues.ofLocalizedEnumValue("a", LocalizedEnumValue.of("d", ofEnglish("d"))),
                ChangeLocalizedEnumValueLabel.of("a", LocalizedEnumValue.of("b", ofEnglish("newB"))),
                AddLocalizedEnumValue.of("a", LocalizedEnumValue.of("c", ofEnglish("c"))),
                ChangeLocalizedEnumValueOrder.of("a", asList(
                    LocalizedEnumValue.of("a", ofEnglish("a")),
                    LocalizedEnumValue.of("b", ofEnglish("newB")),
                    LocalizedEnumValue.of("c", ofEnglish("c"))
                )),
                ChangeAttributeDefinitionLabel.of("a", ofEnglish("new_label")));
    }
}
