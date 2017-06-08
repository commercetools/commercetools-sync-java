package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.helpers.ProductSyncUtils;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import io.sphere.sdk.products.commands.updateactions.SetCategoryOrderHint;
import io.sphere.sdk.products.commands.updateactions.SetSku;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
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
            this::setMasterVariantSku
        ), product, productDraft, syncOptions);
        simpleActions.addAll(addToCategory(product, productDraft, syncOptions));
        simpleActions.addAll(setCategoryOrderHints(product, productDraft, syncOptions));
        return simpleActions;
    }

    private Optional<UpdateAction<Product>> setMasterVariantSku(final Product product, final ProductDraft draft,
                                                                final ProductSyncOptions syncOptions) {
        // suppress NPE inspection as null-check is already done in wrapper method
        //noinspection ConstantConditions
        return ActionUtils.actionOnProductData(product, syncOptions,
            productData -> productData.getMasterVariant().getSku(), draft.getMasterVariant().getSku(),
            newSku -> SetSku.of(ProductSyncUtils.masterData(product, syncOptions).getMasterVariant().getId(), newSku,
                syncOptions.isUpdateStaged()));
        // TODO beware that this change is staged and needs to be published
    }

    private List<UpdateAction<Product>> addToCategory(final Product product, final ProductDraft draft,
                                                      final ProductSyncOptions syncOptions) {
        return ActionUtils.actionsOnProductData(product, syncOptions,
            ProductData::getCategories, draft.getCategories(), (oldCategories, newCategories) -> {
                newCategories.removeAll(oldCategories);
                if (!newCategories.isEmpty()) {
                    // TODO only for one category now
                    return singletonList(
                        AddToCategory.of(newCategories.iterator().next(), syncOptions.isUpdateStaged()));
                }
                return emptyList();
            });
    }

    private List<UpdateAction<Product>> setCategoryOrderHints(final Product product, final ProductDraft draft,
                                                              final ProductSyncOptions syncOptions) {
        return ActionUtils.actionsOnProductData(product, syncOptions,
            ProductData::getCategoryOrderHints, draft.getCategoryOrderHints(), (oldHints, newHints) -> {

                List<UpdateAction<Product>> updateActions = new ArrayList<>();

                Map<String, String> newMap = nonNull(newHints) ? newHints.getAsMap() : emptyMap();
                Map<String, String> oldMap = nonNull(oldHints) ? oldHints.getAsMap() : emptyMap();

                // remove category hints present in old product if they are absent in draft
                oldMap.forEach((key, value) -> {
                    if (!newMap.containsKey(key)) {
                        updateActions.add(
                            SetCategoryOrderHint.of(key, null, syncOptions.isUpdateStaged()));
                    }
                });

                // add category hints present in draft if they are absent or changed in old product
                newMap.forEach((key, value) -> {
                    if (!oldMap.containsKey(key) || !Objects.equals(oldMap.get(key), value)) {
                        updateActions.add(SetCategoryOrderHint.of(key, value, syncOptions.isUpdateStaged()));
                    }
                });

                return updateActions;
            });
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
