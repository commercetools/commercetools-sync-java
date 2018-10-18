package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import io.sphere.sdk.products.attributes.AttributeType;

public final class AttributeTypeAssert {

    /**
     * A utility method validates an old {@link AttributeType} and a new {@link AttributeType}
     * and if the value of types {@code null}, {@link BuildUpdateActionException} will be thrown.
     *
     * @param oldAttributeType the old attribute type.
     * @param newAttributeType the new attribute type.
     */
    public static void assertTypesAreNull(final AttributeType oldAttributeType,
                                          final AttributeType newAttributeType)
            throws BuildUpdateActionException {

        if (oldAttributeType == null && newAttributeType == null) {
            throw new BuildUpdateActionException("Attribute types are not set "
                    + "for both the old and new/draft attribute definitions.");
        }

        assertOldAttributeType(oldAttributeType);
        assertNewAttributeType(newAttributeType);
    }

    /**
     * A utility method validates old {@link AttributeType} and
     * if the value of type {@code null}, {@link BuildUpdateActionException} will be thrown.
     *
     * @param oldAttributeType the old attribute type.
     */
    public static void assertOldAttributeType(final AttributeType oldAttributeType) throws BuildUpdateActionException {
        if (oldAttributeType == null) {
            throw new BuildUpdateActionException("Attribute type is not set for the old attribute definition.");
        }
    }

    /**
     * A utility method validates new {@link AttributeType} and
     * if the value of type {@code null}, {@link BuildUpdateActionException} will be thrown.
     *
     * @param newAttributeType the new attribute type.
     */
    public static void assertNewAttributeType(final AttributeType newAttributeType) throws BuildUpdateActionException {
        if (newAttributeType == null) {
            throw new BuildUpdateActionException("Attribute type is not set for the new/draft attribute definition.");
        }
    }

    private AttributeTypeAssert() {
    }
}
