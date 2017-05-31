package com.commercetools.sync.products.actions;

import com.commercetools.sync.commons.actions.UpdateActionsBuilder;
import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.products.commands.updateactions.SetSku;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public final class ProductUpdateActionsBuilder implements UpdateActionsBuilder<Product, ProductDraft> {

    private static final ProductUpdateActionsBuilder productUpdateActionsBuilder;
    private static ProductSyncOptions syncOptions;

    // singleton
    static {
        productUpdateActionsBuilder = new ProductUpdateActionsBuilder();
    }

    private ProductUpdateActionsBuilder() {
    }

    public static ProductUpdateActionsBuilder of(final ProductSyncOptions syncOptions) {
        ProductUpdateActionsBuilder.syncOptions = syncOptions;
        return productUpdateActionsBuilder;
    }

    @Override
    public List<UpdateAction<Product>> buildActions(final Product product, final ProductDraft productDraft) {
        List<UpdateAction<Product>> simpleActions = fromOptionals(asList(
                changeName(product, productDraft),
                changeSlug(product, productDraft),
                setMetaDescription(product, productDraft),
                setMetaKeywords(product, productDraft),
                setMetaTitle(product, productDraft),
                setMasterVariantSku(product, productDraft),
                setSearchKeywords(product, productDraft)
        ));
        boolean publish = true; // TODO get from sync options
        if (publish && (!simpleActions.isEmpty() || !product.getMasterData().isPublished() || product.getMasterData().hasStagedChanges())) {
            simpleActions.add(Publish.of());
        }
        return simpleActions;
    }

    private Optional<UpdateAction<Product>> changeName(final Product product, final ProductDraft productDraft) {
        return updateAction(() -> product.getMasterData().getStaged().getName(),
                productDraft::getName,
                ChangeName::of);
    }

    private Optional<UpdateAction<Product>> changeSlug(final Product product, final ProductDraft productDraft) {
        return updateAction(() -> product.getMasterData().getStaged().getSlug(),
                productDraft::getSlug,
                ChangeSlug::of);
    }

    private Optional<UpdateAction<Product>> setMetaDescription(final Product product, final ProductDraft productDraft) {
        return updateAction(() -> product.getMasterData().getStaged().getMetaDescription(),
                productDraft::getMetaDescription,
                SetMetaDescription::of);
    }

    private Optional<UpdateAction<Product>> setMetaKeywords(final Product product, final ProductDraft productDraft) {
        return updateAction(() -> product.getMasterData().getStaged().getMetaKeywords(),
                productDraft::getMetaKeywords,
                SetMetaKeywords::of);
    }

    private Optional<UpdateAction<Product>> setMetaTitle(final Product product, final ProductDraft productDraft) {
        return updateAction(() -> product.getMasterData().getStaged().getMetaTitle(),
                productDraft::getMetaTitle,
                SetMetaTitle::of);
    }

    // TODO only master variant now
    private Optional<UpdateAction<Product>> setMasterVariantSku(final Product product, final ProductDraft productDraft) {
        ProductVariant masterVariant = product.getMasterData().getStaged().getMasterVariant();
        return updateAction(masterVariant::getSku,
                () -> productDraft.getMasterVariant().getSku(),
                newSku -> SetSku.of(masterVariant.getId(), newSku));
    }

    private Optional<UpdateAction<Product>> setSearchKeywords(final Product product, final ProductDraft productDraft) {
        return updateAction(() -> product.getMasterData().getStaged().getSearchKeywords(),
                productDraft::getSearchKeywords,
                SetSearchKeywords::of);
    }

    private List<UpdateAction<Product>> setCategoryOrderHint(final Product product, final ProductDraft productDraft) {
        final CategoryOrderHints oldHints = product.getMasterData().getStaged().getCategoryOrderHints();
        final CategoryOrderHints newHints = productDraft.getCategoryOrderHints();
        if (!Objects.equals(oldHints, newHints)) {
            List<UpdateAction<Product>> updateActions = new ArrayList<>();
            Map<String, String> oldMap = oldHints.getAsMap();
            Map<String, String> newMap = newHints.getAsMap();
            Map<String, String> map = new HashMap<>();
            map.putAll(oldMap);
            map.putAll(newMap);
            map.entrySet().removeAll(oldMap.entrySet());
            return updateActions;
        } else {
            return emptyList();
        }
    }

    private <X> Optional<UpdateAction<Product>> updateAction(final Supplier<X> productValue,
                                                             final Supplier<X> productDraftValue,
                                                             final Function<X, UpdateAction<Product>> action) {
        X newValue = productDraftValue.get();
        // TODO this is broken for nulls
        if (!Objects.equals(productValue.get(), newValue)) {
            return Optional.ofNullable(action.apply(newValue));
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
