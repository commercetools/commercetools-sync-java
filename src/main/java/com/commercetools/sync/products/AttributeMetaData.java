package com.commercetools.sync.products;

import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;

import javax.annotation.Nonnull;

/**
 * Custom container for product variant attribute information: its name and whether it has the constraint "SameForAll"
 * or not.
 */
public final class AttributeMetaData {
    private String name;
    private boolean isSameForAll;

    private AttributeMetaData(@Nonnull final String name, final boolean isSameForAll) {
        this.name = name;
        this.isSameForAll = isSameForAll;
    }

    /**
     * Uses the supplied {@link AttributeDefinition} instance to infer the name and whether it has the constraint
     * "SameForAll" or not, to instantiate a new {@link AttributeMetaData} containing the aforementioned information.
     *
     * @param attributeDefinition the instance for which the needed information is used.
     * @return a new instance of {@link AttributeMetaData}.
     */
    public static AttributeMetaData of(@Nonnull final AttributeDefinition attributeDefinition) {
        boolean isSameForAll = attributeDefinition.getAttributeConstraint()
                                                  .equals(AttributeConstraint.SAME_FOR_ALL);
        return new AttributeMetaData(attributeDefinition.getName(), isSameForAll);
    }

    /**
     * Gets the name of the attribute.
     *
     * @return the name of the attribute.
     */
    public String getName() {
        return name;
    }

    /**
     * Does the attribute have a "SameForAll" constraint or not.
     *
     * @return boolean flag specifying whether the attribute has a "SameForAll" constraint or not.
     */
    public boolean isSameForAll() {
        return isSameForAll;
    }
}
