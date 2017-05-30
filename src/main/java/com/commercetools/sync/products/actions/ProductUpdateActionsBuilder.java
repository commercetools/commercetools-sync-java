package com.commercetools.sync.products.actions;

import com.commercetools.sync.commons.actions.UpdateActionsBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public final class ProductUpdateActionsBuilder implements UpdateActionsBuilder<Product, ProductDraft> {

    private static final ProductUpdateActionsBuilder productUpdateActionsBuilder;

    // singleton
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
        List<Optional<UpdateAction<Product>>> updateActions = asList(
                changeName(product, productDraft));
        return fromOptionals(updateActions);
    }

    private Optional<UpdateAction<Product>> changeName(final Product product, final ProductDraft productDraft) {
        if (!Objects.equals(product.getMasterData().getStaged().getName(), productDraft.getName())) {
            return Optional.of(ChangeName.of(productDraft.getName()));
        } else {
            return Optional.empty();
        }
    }

    private List<UpdateAction<Product>> fromOptionals(final List<Optional<UpdateAction<Product>>> optionalList) {
        return optionalList.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }
}
