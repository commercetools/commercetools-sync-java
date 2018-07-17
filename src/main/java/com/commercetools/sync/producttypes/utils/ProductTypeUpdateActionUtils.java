package com.commercetools.sync.producttypes.utils;


import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeDescription;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeName;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

public final class ProductTypeUpdateActionUtils {

    /**
     * Compares the {@code name} values of a {@link ProductType} and a {@link ProductTypeDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeName"}
     * {@link UpdateAction}. If both {@link ProductType} and {@link ProductTypeDraft} have the same
     * {@code name} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldProductType the product type that should be updated.
     * @param newProductType the product type draft which contains the new name.
     * @return optional containing update action or empty optional if names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ProductType>> buildChangeNameAction(
        @Nonnull final ProductType oldProductType,
        @Nonnull final ProductTypeDraft newProductType) {
        return buildUpdateAction(oldProductType.getName(), newProductType.getName(),
            () -> ChangeName.of(newProductType.getName()));
    }

    /**
     * Compares the {@code description} values of a {@link ProductType} and a {@link ProductTypeDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeDescription"}
     * {@link UpdateAction}. If both {@link ProductType} and {@link ProductTypeDraft} have the same
     * {@code description} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldProductType the product type that should be updated.
     * @param newProductType the product type draft which contains the new description.
     * @return optional containing update action or empty optional if descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ProductType>> buildChangeDescriptionAction(
        @Nonnull final ProductType oldProductType,
        @Nonnull final ProductTypeDraft newProductType) {
        return buildUpdateAction(oldProductType.getDescription(), newProductType.getDescription(),
            () -> ChangeDescription.of(newProductType.getDescription()));
    }

    private ProductTypeUpdateActionUtils() { }
}
