package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddAttributeDefinition;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeOrder;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveAttributeDefinition;

import javax.annotation.Nonnull;
import java.util.List;


public class ProductTypeAttributeActionFactory {
    private BaseSyncOptions syncOptions;

    public ProductTypeAttributeActionFactory(@Nonnull final ProductTypeSyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    /**
     * Takes a matching old attribute definition and a new attribute definition and computes the update actions
     * needed to sync them.
     *
     * @param oldAttributeDefinition      the old attribute definition to compare.
     * @param newAttributeDefinition the matching new attribute definition draft.
     * @return update actions needed to sync the two attribute definitions.
     */
    public List<UpdateAction<ProductType>> buildAttributeDefinitionActions(
            @Nonnull final AttributeDefinition oldAttributeDefinition,
            @Nonnull final AttributeDefinitionDraft newAttributeDefinition) {
        return AttributeDefinitionUpdateActionUtils.buildActions(
            oldAttributeDefinition,
            newAttributeDefinition,
            (ProductTypeSyncOptions) syncOptions
        );
    }

    /**
     * Takes an attribute definition name to build a RemoveAttributeDefinition action.
     *
     * @param attributeDefinitionName the name of the attribute definition used on building the update action.
     * @return the built remove attribute definition update action.
     */
    public UpdateAction<ProductType> buildRemoveAttributeDefinitionAction(
            @Nonnull final String attributeDefinitionName) {
        return RemoveAttributeDefinition.of(attributeDefinitionName);
    }

    /**
     * Takes a list of attribute definitions to build a ChangeAttributeDefinitionOrder action.
     *
     * @param newAttributeDefinitionOrder the new attribute definition order needed to build the action.
     * @return the built update action.
     */
    public UpdateAction<ProductType> buildChangeAttributeDefinitionOrderAction(
            @Nonnull final List<AttributeDefinition> newAttributeDefinitionOrder) {
        return ChangeAttributeOrder.of(newAttributeDefinitionOrder);
    }

    /**
     * Takes an attribute definition to build an AddAttribute action.
     *
     * @param newAttributeDefinition the new attribute definition to create an Add attribute action for.
     * @return the built update action.
     */
    public UpdateAction<ProductType> buildAddAttributeDefinitionAction(
            @Nonnull final AttributeDefinition newAttributeDefinition) {
        return AddAttributeDefinition.of(newAttributeDefinition);
    }
}
