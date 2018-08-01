package com.commercetools.sync.commons.exceptions;

import javax.annotation.Nonnull;

/**
 * Exception thrown when the attribute type is different between the {@link io.sphere.sdk.producttypes.ProductType} and
 * {@link io.sphere.sdk.producttypes.ProductTypeDraft}.
 */
public class DifferentTypeException extends RuntimeException {
    public DifferentTypeException(@Nonnull final String message) {
        super(message);
    }

    public DifferentTypeException(@Nonnull final Throwable cause) {
        super(cause);
    }

    public DifferentTypeException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }
}
