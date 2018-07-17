package com.commercetools.sync.producttypes.utils;

import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.producttypes.ProductType;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;


public final class AttributeDefinitionUpdateActionUtils {
    /**
     * Compares all the fields of an {@link AttributeDefinition} and an {@link AttributeDefinitionDraft} and returns
     * a list of {@link UpdateAction}&lt;{@link ProductType}&gt; as a result. If both the {@link AttributeDefinition}
     * and the {@link AttributeDefinitionDraft} have identical fields, then no update action is needed and hence an
     * empty {@link List} is returned.
     *
     * @param oldAttributeDefinition        the attribute definition which should be updated.
     * @param newAttributeDefinitionDraft   the attribute definition draft where we get the new fields.
     * @param syncOptions responsible for supplying the sync options to the sync utility method. It is used for
     *                    triggering the error callback within the utility, in case of errors.
     * @return A list with the update actions or an empty list if the attribute definition fields are identical.
     */
    @Nonnull
    public static List<UpdateAction<ProductType>> buildActions(
        @Nonnull final AttributeDefinition oldAttributeDefinition,
        @Nonnull final AttributeDefinitionDraft newAttributeDefinitionDraft,
        @Nonnull final ProductTypeSyncOptions syncOptions) {

        // TODO Add Attribute Definition Update Actions

        return Collections.emptyList();
    }
}
