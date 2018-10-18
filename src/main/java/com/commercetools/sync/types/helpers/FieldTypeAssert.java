package com.commercetools.sync.types.helpers;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import io.sphere.sdk.types.FieldType;


public final class FieldTypeAssert {

    /**
     * A utility method validates an old {@link FieldType} and a new {@link FieldType}
     * and if the value of types {@code null}, {@link BuildUpdateActionException} will be thrown.
     *
     * @param oldFieldType the old field type.
     * @param newFieldType the new field type.
     */
    public static void assertTypesAreNull(final FieldType oldFieldType,
                                          final FieldType newFieldType)
            throws BuildUpdateActionException {

        if (oldFieldType == null && newFieldType == null) {
            throw new BuildUpdateActionException("Field types are not set "
                    + "for both the old and new field definitions.");
        }

        assertOldFieldType(oldFieldType);
        assertNewFieldType(newFieldType);
    }

    /**
     * A utility method validates old {@link FieldType} and
     * if the value of type {@code null}, {@link BuildUpdateActionException} will be thrown.
     *
     * @param oldFieldType the old field type.
     */
    public static void assertOldFieldType(final FieldType oldFieldType) throws BuildUpdateActionException {
        if (oldFieldType == null) {
            throw new BuildUpdateActionException("Field type is not set for the old field definition.");
        }
    }

    /**
     * A utility method validates new {@link FieldType} and
     * if the value of type {@code null}, {@link BuildUpdateActionException} will be thrown.
     *
     * @param newFieldType the new field type.
     */
    public static void assertNewFieldType(final FieldType newFieldType) throws BuildUpdateActionException {
        if (newFieldType == null) {
            throw new BuildUpdateActionException("Field type is not set for the new field definition.");
        }
    }

    private FieldTypeAssert() {
    }
}