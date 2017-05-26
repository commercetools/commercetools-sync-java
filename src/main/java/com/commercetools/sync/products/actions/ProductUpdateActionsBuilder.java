package com.commercetools.sync.products.actions;

import com.commercetools.sync.commons.actions.UpdateActionsBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProductUpdateActionsBuilder implements UpdateActionsBuilder<Product, ProductDraft> {

    private static final ProductUpdateActionsBuilder productUpdateActionsBuilder;

    static {
        productUpdateActionsBuilder = new ProductUpdateActionsBuilder();
    }

    private ProductUpdateActionsBuilder() {
    }

    public static ProductUpdateActionsBuilder of() {
        return productUpdateActionsBuilder;
    }

    @Override
    public List<UpdateAction<Product>> buildActions(final Product product, final ProductDraft productDraft) {
        List<UpdateAction<Product>> updateActions = new ArrayList<>();
        if (!Objects.equals(product.getMasterData().getCurrent().getName(), productDraft.getName())) {
            updateActions.add(ChangeName.of(productDraft.getName()));
        }
        return updateActions;
    }
}
