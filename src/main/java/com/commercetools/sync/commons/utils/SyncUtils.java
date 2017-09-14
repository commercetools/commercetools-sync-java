package com.commercetools.sync.commons.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.types.Custom;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public final class SyncUtils {
    /**
     * Given a list of resource (e.g. categories, products, etc..) drafts and a {@code batchSize}, this method separates
     * the drafts into batches with the {@code batchSize}. Each batch is represented by a
     * {@link List} of resources and all the batches are grouped and represented by an
     * {@link List}&lt;{@link List}&gt; of resources, which is returned by the method.
     *
     * @param <T> the type of the draft resources.
     * @param drafts    the list of drafts to split into batches.
     * @param batchSize the size of each batch.
     * @return a list of lists where each list represents a batch of resources.
     */
    public static <T> List<List<T>> batchDrafts(@Nonnull final List<T> drafts,
                                                                       final int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < drafts.size() && batchSize > 0; i += batchSize) {
            batches.add(drafts.subList(i, Math.min(i + batchSize, drafts.size())));
        }
        return batches;
    }

    @Nullable
    static <T extends Custom> CustomFieldsDraft replaceCustomTypeIdWithKeys(@Nonnull final T resource) {
        final CustomFields custom = resource.getCustom();
        if (custom != null) {
            if (custom.getType().getObj() != null) {
                return CustomFieldsDraft.ofTypeIdAndJson(custom.getType().getObj().getKey(),
                    custom.getFieldsJsonMap());
            }
            return CustomFieldsDraftBuilder.of(custom).build();
        }
        return null;
    }

    @Nullable
    static <T> Reference<T> replaceReferenceIdWithKey(@Nullable final Reference<T> reference,
                                                      @Nonnull
                                                      final Supplier<Reference<T>> keyInReferenceSupplier) {
        if (reference != null && reference.getObj() != null) {
            return keyInReferenceSupplier.get();
        }
        return reference;
    }


    /**
     * Takes a list of Categories that are supposed to have their custom type and parent category reference expanded
     * in order to be able to fetch the keys and replace the reference ids with the corresponding keys and then return
     * a new list of category drafts with their references containing keys instead of the ids. Note that if the
     * references are not expanded for a category, the reference ids will not be replaced with keys and will still have
     * their ids in place.
     *
     * @param categories the categories to replace their reference ids with keys
     * @return a list of category drafts with keys instead of ids for references.
     */
    @Nonnull
    public static List<CategoryDraft> replaceCategoriesReferenceIdsWithKeys(@Nonnull final List<Category> categories) {
        return categories
            .stream()
            .map(category -> {
                final CustomFieldsDraft customTypeWithKeysInReference = replaceCustomTypeIdWithKeys(category);
                final Reference<Category> parentWithKeyInReference = replaceReferenceIdWithKey(category.getParent(),
                    () -> Category.referenceOfId(category.getParent().getObj().getKey()));
                return CategoryDraftBuilder.of(category)
                                           .custom(customTypeWithKeysInReference)
                                           .parent(parentWithKeyInReference)
                                           .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Takes a list of inventoryEntries that are supposed to have their custom type reference expanded
     * in order to be able to fetch the keys and replace the reference ids with the corresponding keys and then return
     * a new list of inventory entry drafts with their references containing keys instead of the ids.  Note that if the
     * references are not expanded for an inventory entry, the reference ids will not be replaced with keys and will
     * still have their ids in place.
     *
     * @param inventoryEntries the inventory entries to replace their reference ids with keys
     * @return a list of inventoryEntry drafts with keys instead of ids for references.
     */
    @Nonnull
    public static List<InventoryEntryDraft> replaceInventoriesReferenceIdsWithKeys(@Nonnull final List<InventoryEntry>
                                                                                       inventoryEntries) {
        return inventoryEntries
            .stream()
            .map(inventoryEntry -> {
                final CustomFieldsDraft customTypeWithKeysInReference = replaceCustomTypeIdWithKeys(inventoryEntry);
                return InventoryEntryDraftBuilder.of(inventoryEntry)
                                                 .custom(customTypeWithKeysInReference)
                                                 .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Given a {@link Product} this method creates a {@link ProductDraftBuilder} based on the staged projection
     * values of the supplied product.
     *
     * @param product the product to create a {@link ProductDraftBuilder} based on it's staged data.
     * @return a {@link ProductDraftBuilder} based on the staged projection values of the supplied product.
     */
    // TODO: Need to check if a current version should also be checked.
    // TODO: Test
    @Nonnull
    public static ProductDraftBuilder getDraftBuilderFromStagedProduct(@Nonnull final Product product) {
        final ProductData productData = product.getMasterData().getStaged();
        final List<ProductVariantDraft> allVariants = productData
            .getAllVariants().stream()
            .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
            .collect(toList());

        return ProductDraftBuilder
            .of(product.getProductType(), productData.getName(), productData.getSlug(), allVariants)
            .metaDescription(productData.getMetaDescription())
            .metaKeywords(productData.getMetaKeywords())
            .metaTitle(productData.getMetaTitle())
            .description(productData.getDescription())
            .searchKeywords(productData.getSearchKeywords())
            .key(product.getKey())
            .publish(product.getMasterData().isPublished())
            .categories(productData.getCategories())
            .categoryOrderHints(productData.getCategoryOrderHints());
    }
}
