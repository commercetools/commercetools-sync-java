package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import io.sphere.sdk.products.commands.updateactions.SetCategoryOrderHint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.commercetools.sync.products.actions.ActionUtils.actionsOnProductData;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;

final class Category {
    private Category() {
    }

    static List<UpdateAction<Product>> addToCategory(final Product product, final ProductDraft draft,
                                                     final ProductSyncOptions syncOptions) {
        return actionsOnProductData(product, syncOptions,
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

    static List<UpdateAction<Product>> setCategoryOrderHints(final Product product, final ProductDraft draft,
                                                             final ProductSyncOptions syncOptions) {
        return actionsOnProductData(product, syncOptions,
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
}
