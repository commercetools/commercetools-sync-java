package com.commercetools.sync.products;

import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;

import javax.annotation.Nonnull;

/**
 * Custom container for product variant attribute information: its name, whether a value is required or not and
 * whether it has the constraint "SameForAll" or not.
 */
public class ProductVariantAttribute {
    private String name;
    private boolean isRequired;
    private boolean isSameForAll;

    ProductVariantAttribute(@Nonnull final String name, final boolean isRequired, final boolean isSameForAll) {
        this.name = name;
        this.isRequired = isRequired;
        this.isSameForAll = isSameForAll;
    }

    /**
     * Uses the supplied {@link AttributeDefinition} instance to infer the name, whether a value is required or not and
     * whether it has the constraint "SameForAll" or not, to instantiate a new {@link ProductVariantAttribute}
     * containing the aforementioned information.
     *
     * @param attributeDefinition the instance for which the needed information is used.
     * @return a new instance of {@link ProductVariantAttribute}.
     */
    public static ProductVariantAttribute of(@Nonnull final AttributeDefinition attributeDefinition) {
        boolean isSameForAll = attributeDefinition.getAttributeConstraint()
                                                  .equals(AttributeConstraint.SAME_FOR_ALL);
        return new
            ProductVariantAttribute(attributeDefinition.getName(), attributeDefinition.isRequired(), isSameForAll);
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
     * Is a value required for the attribute or not.
     *
     * @return boolean flag specifying whether a value is required for the attribute or not.
     */
    public boolean isRequired() {
        return isRequired;
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
