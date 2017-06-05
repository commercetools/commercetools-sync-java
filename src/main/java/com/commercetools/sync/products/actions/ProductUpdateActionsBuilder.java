package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.products.commands.updateactions.SetSku;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
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

    public List<UpdateAction<Product>> buildActions(final Product product, final ProductDraft productDraft,
                                                    final ProductSyncOptions syncOptions) {
        List<UpdateAction<Product>> simpleActions = flattenOptionals(asList(
                () -> changeName(product, productDraft, syncOptions),
                () -> changeSlug(product, productDraft, syncOptions),
                () -> setMetaDescription(product, productDraft, syncOptions),
                () -> setMetaKeywords(product, productDraft, syncOptions),
                () -> setMetaTitle(product, productDraft, syncOptions),
                () -> setMasterVariantSku(product, productDraft, syncOptions),
                () -> setSearchKeywords(product, productDraft, syncOptions),
                () -> addToCategory(product, productDraft, syncOptions)
        ));
        // simpleActions.addAll(setCategoryOrderHint(product, productDraft));
        return simpleActions;
    }

    private Optional<UpdateAction<Product>> changeName(final Product product, final ProductDraft draft,
                                                       final ProductSyncOptions syncOptions) {
        return updateActionOnProductData(product, syncOptions,
                ProductData::getName,
                draft::getName,
                name -> ChangeName.of(name, syncOptions.isUpdateStaged()));
    }

    private Optional<UpdateAction<Product>> changeSlug(final Product product, final ProductDraft draft,
                                                       final ProductSyncOptions syncOptions) {
        return updateActionOnProductData(product, syncOptions,
                ProductData::getSlug,
                draft::getSlug,
                slug -> ChangeSlug.of(slug, syncOptions.isUpdateStaged()));
    }

    private Optional<UpdateAction<Product>> setMetaDescription(final Product product, final ProductDraft draft,
                                                               final ProductSyncOptions syncOptions) {
        return updateActionOnProductData(product, syncOptions,
                ProductData::getMetaDescription,
                draft::getMetaDescription,
                SetMetaDescription::of);
    }

    private Optional<UpdateAction<Product>> setMetaKeywords(final Product product, final ProductDraft draft,
                                                            final ProductSyncOptions syncOptions) {
        return updateActionOnProductData(product, syncOptions,
                ProductData::getMetaKeywords,
                draft::getMetaKeywords,
                SetMetaKeywords::of);
    }

    private Optional<UpdateAction<Product>> setMetaTitle(final Product product, final ProductDraft draft,
                                                         final ProductSyncOptions syncOptions) {
        return updateActionOnProductData(product, syncOptions,
                ProductData::getMetaTitle,
                draft::getMetaTitle,
                SetMetaTitle::of);
    }

    // TODO only master variant now
    // TODO beware that this change is staged and needs to be published
    private Optional<UpdateAction<Product>> setMasterVariantSku(final Product product, final ProductDraft draft,
                                                                final ProductSyncOptions syncOptions) {
        // suppress possible NPE inspection as null-check is already done in wrapping method
        //noinspection ConstantConditions
        return updateActionOnProductData(product, syncOptions,
                productData -> productData.getMasterVariant().getSku(),
                () -> draft.getMasterVariant().getSku(),
                newSku -> SetSku.of(masterData(product, syncOptions).getMasterVariant().getId(), newSku,
                        syncOptions.isUpdateStaged()));
    }

    private Optional<UpdateAction<Product>> setSearchKeywords(final Product product, final ProductDraft draft,
                                                              final ProductSyncOptions syncOptions) {
        return updateActionOnProductData(product, syncOptions,
                ProductData::getSearchKeywords,
                draft::getSearchKeywords,
                searchKeywords -> SetSearchKeywords.of(searchKeywords, syncOptions.isUpdateStaged()));
    }

    // TODO only for one category now
    private Optional<UpdateAction<Product>> addToCategory(final Product product, final ProductDraft draft,
                                                          final ProductSyncOptions syncOptions) {
        Set<Reference<Category>> draftCategories = draft.getCategories();
        ProductData productData = masterData(product, syncOptions);
        if (isNull(productData)) {
            return Optional.empty();
        }
        draftCategories.removeAll(productData.getCategories());
        if (!draftCategories.isEmpty()) {
            return Optional.of(AddToCategory.of(draftCategories.iterator().next()));
        }
        return Optional.empty();
    }

// --Commented out by Inspection START (05.06.17 12:54):
//    // TODO not used currently
//    private List<UpdateAction<Product>> setCategoryOrderHint(final Product product, final ProductDraft draft) {
//        final CategoryOrderHints oldHints = product.getMasterData().getStaged().getCategoryOrderHints();
//        final CategoryOrderHints newHints = draft.getCategoryOrderHints();
//
//        if (!Objects.equals(oldHints, newHints)) {
//            List<UpdateAction<Product>> updateActions = new ArrayList<>();
//
//            Map<String, String> newMap = nonNull(newHints) ? newHints.getAsMap() : emptyMap();
//            Map<String, String> oldMap = nonNull(oldHints) ? oldHints.getAsMap() : emptyMap();
//
//            // remove category hints present in old product if they are absent in draft
//            oldMap.forEach((key, value) -> {
//                if (!newMap.containsKey(key)) {
//                    updateActions.add(SetCategoryOrderHint.of(key, null));
//                }
//            });
//
//            // add category hints present in draft if they are absent or changed in old product
//            newMap.forEach((key, value) -> {
//                if (!oldMap.containsKey(key) || !Objects.equals(oldMap.get(key), value)) {
//                    updateActions.add(SetCategoryOrderHint.of(key, value));
//                }
//            });
//
//            return updateActions;
//        } else {
//            return emptyList();
//        }
//    }
// --Commented out by Inspection STOP (05.06.17 12:54)

    private <X> Optional<UpdateAction<Product>> updateActionOnProductData(final Product product,
                                                                          final ProductSyncOptions syncOptions,
                                                                          final Function<ProductData, X> productValue,
                                                                          final Supplier<X> draftValue,
                                                                          final Function<X, UpdateAction<Product>>
                                                                                  action) {
        ProductData productData = masterData(product, syncOptions);
        if (isNull(productData)) {
            return Optional.empty();
        }
        return updateAction(() -> productValue.apply(productData), draftValue, action);
    }

    private <X> Optional<UpdateAction<Product>> updateAction(final Supplier<X> productValue,
                                                             final Supplier<X> draftValue,
                                                             final Function<X, UpdateAction<Product>> action) {
        X newValue = draftValue.get();
        // TODO this is broken for nulls
        if (!Objects.equals(productValue.get(), newValue)) {
            return Optional.ofNullable(action.apply(newValue));
        } else {
            return Optional.empty();
        }
    }

    // TODO add test
    @Nullable
    public static ProductData masterData(final Product product, final ProductSyncOptions syncOptions) {
        return syncOptions.isCompareStaged()
                ? product.getMasterData().getStaged()
                : product.getMasterData().getCurrent();
    }

    private List<UpdateAction<Product>> flattenOptionals(final List<Supplier<Optional<UpdateAction<Product>>>>
                                                                 changeMethods) {
        return changeMethods.stream()
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }
}
