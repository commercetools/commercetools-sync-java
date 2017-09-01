package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.RemoveFromCategory;
import io.sphere.sdk.products.commands.updateactions.SetCategoryOrderHint;
import io.sphere.sdk.products.commands.updateactions.SetDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.search.SearchKeywords;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.products.utils.ProductDataUpdateActionUtils.buildProductDataUpdateAction;
import static com.commercetools.sync.products.utils.ProductDataUpdateActionUtils.buildProductDataUpdateActions;
import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;

public final class ProductUpdateActionUtils {
    public static Optional<UpdateAction<Product>> buildChangeNameUpdateAction(final Product product, final ProductDraft draft,
                                                                       final ProductSyncOptions syncOptions) {
        final LocalizedString draftName = draft.getName();
        return buildProductDataUpdateAction(product, syncOptions, ProductData::getName, draftName,
            () -> ChangeName.of(draftName, syncOptions.shouldUpdateStaged()));
    }

    public static Optional<UpdateAction<Product>> buildSetDescriptionUpdateAction(final Product product,
                                                                           final ProductDraft draft,
                                                                           final ProductSyncOptions syncOptions) {
        final LocalizedString draftDescription = draft.getDescription();
        return buildProductDataUpdateAction(product, syncOptions, ProductData::getDescription, draftDescription,
            () -> SetDescription.of(draftDescription, syncOptions.shouldUpdateStaged()));
    }

    public static Optional<UpdateAction<Product>> buildChangeSlugUpdateAction(final Product product, final ProductDraft draft,
                                                                       final ProductSyncOptions syncOptions) {
        final LocalizedString draftSlug = draft.getSlug();
        return buildProductDataUpdateAction(product, syncOptions, ProductData::getSlug, draftSlug,
            () -> ChangeSlug.of(draftSlug, syncOptions.shouldUpdateStaged()));
    }


    public static List<UpdateAction<Product>> buildAddToCategoryUpdateActions(final Product product, final ProductDraft draft,
                                                                       final ProductSyncOptions syncOptions) {
        final Set<Reference<Category>> draftCategories = draft.getCategories();
        return buildProductDataUpdateActions(product, syncOptions,
            ProductData::getCategories, draftCategories, (oldCategories) -> {
                final List<UpdateAction<Product>> updateActions = new ArrayList<>();
                subtract(draftCategories, oldCategories).forEach(c ->
                    updateActions.add(AddToCategory.of(c, syncOptions.shouldUpdateStaged())));
                return updateActions;
            });
    }

    public static List<UpdateAction<Product>> buildSetCategoryOrderHintsUpdateAction(final Product product, final ProductDraft draft,
                                                                              final ProductSyncOptions syncOptions) {
        final CategoryOrderHints draftCategoryOrderHints = draft.getCategoryOrderHints();
        return buildProductDataUpdateActions(product, syncOptions,
            ProductData::getCategoryOrderHints, draftCategoryOrderHints, (oldCategoryOrderHints) -> {

                final Set<String> newCategoryIds = draft.getCategories().stream()
                                                        .map(Reference::getId)
                                                        .collect(toSet());

                final List<UpdateAction<Product>> updateActions = new ArrayList<>();

                final Map<String, String> newMap = nonNull(draftCategoryOrderHints) ? draftCategoryOrderHints.getAsMap() : emptyMap();
                final Map<String, String> oldMap = nonNull(oldCategoryOrderHints) ? oldCategoryOrderHints.getAsMap() : emptyMap();

                // remove category hints present in old product if they are absent in draft but only if product
                // is or will be assigned to given category
                oldMap.forEach((categoryId, value) -> {
                    if (!newMap.containsKey(categoryId) && newCategoryIds.contains(categoryId)) {
                        updateActions.add(SetCategoryOrderHint.of(categoryId, null, syncOptions.shouldUpdateStaged()));
                    }
                });

                // add category hints present in draft if they are absent or changed in old product
                newMap.forEach((key, value) -> {
                    if (!oldMap.containsKey(key) || !Objects.equals(oldMap.get(key), value)) {
                        updateActions.add(SetCategoryOrderHint.of(key, value, syncOptions.shouldUpdateStaged()));
                    }
                });

                return updateActions;
            });
    }

    public static List<UpdateAction<Product>> buildRemoveFromCategoryUpdateActions(final Product product, final ProductDraft draft,
                                                                            final ProductSyncOptions syncOptions) {
        final Set<Reference<Category>> draftCategories = draft.getCategories();
        return buildProductDataUpdateActions(product, syncOptions,
            ProductData::getCategories, draftCategories, (oldCategories) -> {
                final List<UpdateAction<Product>> updateActions = new ArrayList<>();
                subtract(draftCategories, oldCategories).forEach(c ->
                    updateActions.add(AddToCategory.of(c, syncOptions.shouldUpdateStaged())));
                subtract(oldCategories, draftCategories).forEach(c ->
                    updateActions.add(RemoveFromCategory.of(c, syncOptions.shouldUpdateStaged())));
                return updateActions;
            });
    }

    private static Set<Reference<Category>> subtract(final Set<Reference<Category>> set1,
                                                     final Set<Reference<Category>> set2) {
        Set<Reference<Category>> difference = new HashSet<>(set1);
        difference.removeAll(set2);
        return difference;
    }

    public static Optional<UpdateAction<Product>> buildSetSearchKeywordsUpdateAction(final Product product,
                                                                              final ProductDraft draft,
                                                                              final ProductSyncOptions syncOptions) {
        final SearchKeywords draftSearchKeywords = draft.getSearchKeywords();
        return buildProductDataUpdateAction(product, syncOptions, ProductData::getSearchKeywords, draftSearchKeywords,
            () -> SetSearchKeywords.of(draftSearchKeywords, syncOptions.shouldUpdateStaged()));
    }

    public static Optional<UpdateAction<Product>> buildSetMetaDescriptionUpdateAction(final Product product, final ProductDraft draft,
                                                                               final ProductSyncOptions syncOptions) {
        final LocalizedString draftMetaDescription = draft.getMetaDescription();
        return buildProductDataUpdateAction(product, syncOptions, ProductData::getMetaDescription, draftMetaDescription,
            () -> SetMetaDescription.of(draftMetaDescription));
    }

    public static Optional<UpdateAction<Product>> buildSetMetaKeywordsUpdateAction(final Product product, final ProductDraft draft,
                                                                            final ProductSyncOptions syncOptions) {
        final LocalizedString draftMetaKeywords = draft.getMetaKeywords();
        return buildProductDataUpdateAction(product, syncOptions, ProductData::getMetaKeywords, draftMetaKeywords,
            () -> SetMetaKeywords.of(draftMetaKeywords));
    }

    public static Optional<UpdateAction<Product>> buildSetMetaTitleUpdateAction(final Product product, final ProductDraft draft,
                                                                         final ProductSyncOptions syncOptions) {
        final LocalizedString draftMetaTitle = draft.getMetaTitle();
        return buildProductDataUpdateAction(product, syncOptions, ProductData::getMetaTitle, draftMetaTitle,
            () -> SetMetaTitle.of(draftMetaTitle));
    }

    /*static Optional<UpdateAction<Product>> buildSetSkuUpdateAction(final Product product, final ProductDraft draft,
                                                               final ProductSyncOptions syncOptions) {

        final ProductData productData = masterData(product, syncOptions);
        // productData.getAllVariants() TODO CONTINUTE TRAVERSING ALL VARIANTS..
        final String draftProductMasterVariantSku = draft.getMasterVariant().getSku();
        return ProductDataUpdateActionUtils.buildProductDataUpdateAction(product, syncOptions,
            productData -> productData.getMasterVariant().getSku(), draftProductMasterVariantSku,
            () -> SetSku.of(masterVariantId, draftProductMasterVariantSku, syncOptions.shouldUpdateStaged()));
        // TODO beware that this change is staged and needs to be published
    }*/

}
