package com.commercetools.sync.producttypes.utils.producttypeactionutils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.LocalizedStringAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.updateactions.AddAttributeDefinition;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeOrder;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveAttributeDefinition;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateActionUtils.buildAttributesUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
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

        // Bug in the commercetools JVM SDK. AddAttributeDefinition should expect an AttributeDefinitionDraft rather
        // than AttributeDefinition.
        // TODO It will be fixed in https://github.com/commercetools/commercetools-jvm-sdk/issues/1786
        assertThat(updateActions).containsExactly(
            AddAttributeDefinition.of(ATTRIBUTE_DEFINITION_A),
            AddAttributeDefinition.of(ATTRIBUTE_DEFINITION_B),
            AddAttributeDefinition.of(ATTRIBUTE_DEFINITION_C)
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithIdenticalAttributes_ShouldNotBuildUpdateActions() {
        final ProductType oldProductType = readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

        final ProductTypeDraft newProductTypeDraft = readObjectFromResource(
            PRODUCT_TYPE_WITH_ATTRIBUTES_ABC,
            ProductTypeDraft.class
        );

        final List<UpdateAction<ProductType>> updateActions = buildAttributesUpdateActions(
            oldProductType,
            newProductTypeDraft,
            SYNC_OPTIONS
        );

        assertThat(updateActions).isEmpty();
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
            AddAttributeDefinition.of(ATTRIBUTE_DEFINITION_D)
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
            AddAttributeDefinition.of(ATTRIBUTE_DEFINITION_D)
        );
    }

    @Test
    public void buildAttributesUpdateActions_WithDifferent_ShouldBuildChangeAttributeOrderAction() {
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
            ChangeAttributeOrder
                .of(asList(
                        ATTRIBUTE_DEFINITION_C,
                        ATTRIBUTE_DEFINITION_A,
                        ATTRIBUTE_DEFINITION_B
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
            ChangeAttributeOrder
                .of(asList(
                        ATTRIBUTE_DEFINITION_C,
                        ATTRIBUTE_DEFINITION_B
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
            AddAttributeDefinition.of(ATTRIBUTE_DEFINITION_D),
            ChangeAttributeOrder
                .of(asList(
                        ATTRIBUTE_DEFINITION_A,
                        ATTRIBUTE_DEFINITION_C,
                        ATTRIBUTE_DEFINITION_B,
                        ATTRIBUTE_DEFINITION_D
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
            AddAttributeDefinition.of(ATTRIBUTE_DEFINITION_D),
            ChangeAttributeOrder
                .of(asList(
                        ATTRIBUTE_DEFINITION_A,
                        ATTRIBUTE_DEFINITION_D,
                        ATTRIBUTE_DEFINITION_B,
                        ATTRIBUTE_DEFINITION_C
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
            AddAttributeDefinition.of(ATTRIBUTE_DEFINITION_D),
            ChangeAttributeOrder
                .of(asList(
                        ATTRIBUTE_DEFINITION_C,
                        ATTRIBUTE_DEFINITION_B,
                        ATTRIBUTE_DEFINITION_D
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
            AddAttributeDefinition.of(ATTRIBUTE_DEFINITION_A_LOCALIZED_TYPE)
        );
    }
}
