package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.SetCategoryOrderHint;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.products.commands.updateactions.SetSku;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
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

    public List<UpdateAction<Product>> buildActions(final Product product, final ProductDraft productDraft,
                                                    final ProductSyncOptions syncOptions) {
        List<UpdateAction<Product>> simpleActions = flattenOptionals(asList(
                this::changeName,
                this::changeSlug,
                this::setMetaDescription,
                this::setMetaKeywords,
                this::setMetaTitle,
                this::setMasterVariantSku,
                this::setSearchKeywords
        ), product, productDraft, syncOptions);
        simpleActions.addAll(addToCategory(product, productDraft, syncOptions));
        simpleActions.addAll(setCategoryOrderHints(product, productDraft, syncOptions));
        return simpleActions;
    }

    private Optional<UpdateAction<Product>> changeName(final Product product, final ProductDraft draft,
                                                       final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
                ProductData::getName, draft.getName(),
                name -> ChangeName.of(name, syncOptions.isUpdateStaged()));
    }

    private Optional<UpdateAction<Product>> changeSlug(final Product product, final ProductDraft draft,
                                                       final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
                ProductData::getSlug, draft.getSlug(),
                slug -> ChangeSlug.of(slug, syncOptions.isUpdateStaged()));
    }

    private Optional<UpdateAction<Product>> setMetaDescription(final Product product, final ProductDraft draft,
                                                               final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
                ProductData::getMetaDescription, draft.getMetaDescription(),
                SetMetaDescription::of);
    }

    private Optional<UpdateAction<Product>> setMetaKeywords(final Product product, final ProductDraft draft,
                                                            final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
                ProductData::getMetaKeywords, draft.getMetaKeywords(),
                SetMetaKeywords::of);
    }

    private Optional<UpdateAction<Product>> setMetaTitle(final Product product, final ProductDraft draft,
                                                         final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
                ProductData::getMetaTitle, draft.getMetaTitle(),
                SetMetaTitle::of);
    }

    private Optional<UpdateAction<Product>> setMasterVariantSku(final Product product, final ProductDraft draft,
                                                                final ProductSyncOptions syncOptions) {
        // suppress NPE inspection as null-check is already done in wrapper method
        //noinspection ConstantConditions
        return actionOnProductData(product, syncOptions,
                productData -> productData.getMasterVariant().getSku(), draft.getMasterVariant().getSku(),
                newSku -> SetSku.of(masterData(product, syncOptions).getMasterVariant().getId(), newSku,
                        syncOptions.isUpdateStaged()));
        // TODO beware that this change is staged and needs to be published
    }

    private Optional<UpdateAction<Product>> setSearchKeywords(final Product product, final ProductDraft draft,
                                                              final ProductSyncOptions syncOptions) {
        return actionOnProductData(product, syncOptions,
                ProductData::getSearchKeywords, draft.getSearchKeywords(),
                searchKeywords -> SetSearchKeywords.of(searchKeywords, syncOptions.isUpdateStaged()));
    }

    private List<UpdateAction<Product>> addToCategory(final Product product, final ProductDraft draft,
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

    private List<UpdateAction<Product>> setCategoryOrderHints(final Product product, final ProductDraft draft,
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

    private <X> List<UpdateAction<Product>> actionsOnProductData(final Product product,
                                                                 final ProductSyncOptions syncOptions,
                                                                 final Function<ProductData, X> productValue,
                                                                 final X draftValue,
                                                                 final BiFunction<X, X, List<UpdateAction<Product>>>
                                                                         actions) {
        ProductData productData = masterData(product, syncOptions);
        return isNull(productData)
                ? emptyList()
                : actions(productValue.apply(productData), draftValue, actions);
    }

    private <X> List<UpdateAction<Product>> actions(final X oldValue,
                                                    final X newValue,
                                                    final BiFunction<X, X, List<UpdateAction<Product>>>
                                                            actions) {
        return !Objects.equals(oldValue, newValue)
                ? actions.apply(oldValue, newValue)
                : emptyList();
    }

    private <X> Optional<UpdateAction<Product>> actionOnProductData(final Product product,
                                                                    final ProductSyncOptions syncOptions,
                                                                    final Function<ProductData, X> productValue,
                                                                    final X draftValue,
                                                                    final Function<X, UpdateAction<Product>> action) {
        ProductData productData = masterData(product, syncOptions);
        return isNull(productData)
                ? Optional.empty()
                : action(productValue.apply(productData), draftValue, action);
    }

    private <X> Optional<UpdateAction<Product>> action(final X oldValue,
                                                       final X newValue,
                                                       final Function<X, UpdateAction<Product>> action) {
        return !Objects.equals(oldValue, newValue)
                ? Optional.of(action.apply(newValue))
                : Optional.empty();
    }

    @Nullable
    public static ProductData masterData(final Product product, final ProductSyncOptions syncOptions) {
        return syncOptions.isCompareStaged()
                ? product.getMasterData().getStaged()
                : product.getMasterData().getCurrent();
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
