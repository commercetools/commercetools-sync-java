package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class ProductUpdateActionsBuilder {

    private static final ProductUpdateActionsBuilder productUpdateActionsBuilder;

    // eager singleton
    static {
        productUpdateActionsBuilder = new ProductUpdateActionsBuilder();
    }

    private ProductUpdateActionsBuilder() {
    }

    public static ProductUpdateActionsBuilder of() {
        return productUpdateActionsBuilder;
    }

    /**
     * Provides the {@link UpdateAction} list that needs to be executed on {@code product} resource so that it is
     * synchronized with {@code productDraft}.
     *
     * @param product      the product resource for which update actions are built
     * @param productDraft the product resource draft containing delta in regard to {@code product}
     * @param syncOptions  the configuration of synchronization
     * @return the {@link UpdateAction} list that needs to be executed on {@code product} resource so that it is
     *     synchronized with {@code productDraft}
     */
    public List<UpdateAction<Product>> buildActions(final Product product, final ProductDraft productDraft,
                                                    final ProductSyncOptions syncOptions) {
        List<UpdateAction<Product>> simpleActions = flattenOptionals(asList(
            Base::changeName,
            Base::changeSlug,
            Base::setDescription,
            Base::setSearchKeywords,
            Meta::setMetaDescription,
            Meta::setMetaKeywords,
            Meta::setMetaTitle,
            Variants::setMasterVariantSku
        ), product, productDraft, syncOptions);
        simpleActions.addAll(Categories.mapCategories(product, productDraft, syncOptions));
        simpleActions.addAll(Categories.setCategoryOrderHints(product, productDraft, syncOptions));
        return simpleActions;
    }

    private List<UpdateAction<Product>> flattenOptionals(final List<UpdateCommand> changeMethods,
                                                         final Product product, final ProductDraft draft,
                                                         final ProductSyncOptions syncOptions) {
        return changeMethods.stream()
            .map(command -> command.supplyAction(product, draft, syncOptions))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList());
    }

    @FunctionalInterface
    private interface UpdateCommand {
        Optional<UpdateAction<Product>> supplyAction(final Product product, final ProductDraft draft,
                                                     final ProductSyncOptions syncOptions);
    }
}
