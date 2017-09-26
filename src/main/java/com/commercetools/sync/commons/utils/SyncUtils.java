package com.commercetools.sync.commons.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.types.Custom;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

// TODO: Test coverage and JavaDoc of new methods.
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
    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
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
     * Takes a list of Products that are supposed to have their product type and category references expanded
     * in order to be able to fetch the keys and replace the reference ids with the corresponding keys and then return
     * a new list of product drafts with their references containing keys instead of the ids. Note that if the
     * references are not expanded for a product, the reference ids will not be replaced with keys and will still have
     * their ids in place.
     *
     * @param products the products to replace their reference ids with keys
     * @return a list of products drafts with keys instead of ids for references.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    public static List<ProductDraft> replaceProductsReferenceIdsWithKeys(@Nonnull final List<Product> products) {
        return products
            .stream()
            .map(product -> {
                // Resolve productType reference
                final Reference<ProductType> productType = product.getProductType();
                final Reference<ProductType> productTypeReferenceWithKey = replaceReferenceIdWithKey(productType,
                    () -> ProductType.referenceOfId(productType.getObj().getKey()));

                final ProductDraft productDraft = getDraftBuilderFromStagedProduct(product).build();
                final ProductDraft productDraftWithCategoryKeys =
                    replaceProductDraftCategoryReferenceIdsWithKeys(productDraft);

                return ProductDraftBuilder.of(productDraftWithCategoryKeys)
                                          .productType(productTypeReferenceWithKey)
                                          .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Takes a list of product drafts that are supposed to have their category references expanded in order to be able
     * to fetch the keys and replace the reference ids with the corresponding keys and then return a new list of product
     * drafts with their references containing keys instead of the ids. Note that if the references are not expanded
     * for a product, the reference ids will not be replaced with keys and will still have their ids in place.
     *
     * @param productDrafts the product drafts to replace their reference ids with keys
     * @return a list of products drafts with keys instead of ids for references.
     */
    @Nonnull
    public static List<ProductDraft> replaceProductDraftsCategoryReferenceIdsWithKeys(@Nonnull final List<ProductDraft>
                                                                                      productDrafts) {
        return productDrafts.stream()
                            .map(productDraft ->
                                productDraft != null
                                    ? replaceProductDraftCategoryReferenceIdsWithKeys(productDraft) : null
                            )
                            .collect(Collectors.toList());
    }

    /**
     * Takes a product draft that is supposed to have its category references expanded
     * in order to be able to fetch the keys and replace the reference ids with the corresponding keys and then return
     * a new product drafts with the references containing keys instead of the ids. Note that the category references
     * should be expanded for the productDraft, otherwise the reference ids will not be replaced with keys and will
     * still have their ids in place.
     *
     * @param productDraft the product drafts to replace its reference ids with keys
     * @return a new products draft with keys instead of ids for references.
     */
    @Nonnull
    public static ProductDraft replaceProductDraftCategoryReferenceIdsWithKeys(@Nonnull final ProductDraft
                                                                                        productDraft) {
        final Set<ResourceIdentifier<Category>> categories = productDraft.getCategories();
        List<Reference<Category>> categoryReferencesWithKeys = new ArrayList<>();
        Map<String, String> categoryOrderHintsMapWithKeys = new HashMap<>();
        if (categories != null) {
            categoryReferencesWithKeys = categories
                .stream().map(categoryResourceIdentifier -> {
                    final String categoryId = categoryResourceIdentifier.getId();
                    String categoryKey = categoryResourceIdentifier.getKey();
                    // If the key is null, keep the id instead.
                    // TODO: Should the user be notified if the category key is null?
                    categoryKey = categoryKey == null ? categoryId : categoryKey;

                    // Replace categoryOrderHint id with key.
                    final CategoryOrderHints categoryOrderHints = productDraft.getCategoryOrderHints();
                    if (categoryOrderHints != null) {
                        final String categoryOrderHintValue = categoryOrderHints.get(categoryId);
                        categoryOrderHintsMapWithKeys.put(categoryKey, categoryOrderHintValue);
                    }

                    // Replace category reference id with key.
                    return Category.referenceOfId(categoryKey);
                }).collect(Collectors.toList());
        }
        final CategoryOrderHints categoryOrderHintsWithKeys = CategoryOrderHints.of(categoryOrderHintsMapWithKeys);
        return ProductDraftBuilder.of(productDraft)
                                  .categories(categoryReferencesWithKeys)
                                  .categoryOrderHints(categoryOrderHintsWithKeys)
                                  .build();
    }

    /**
     * Given a {@link Product} this method creates a {@link ProductDraftBuilder} based on the staged projection
     * values of the supplied product.
     *
     * @param product the product to create a {@link ProductDraftBuilder} based on it's staged data.
     * @return a {@link ProductDraftBuilder} based on the staged projection values of the supplied product.
     */
    @Nonnull
    public static ProductDraftBuilder getDraftBuilderFromStagedProduct(@Nonnull final Product product) {
        final ProductData productData = product.getMasterData().getStaged();
        final List<ProductVariantDraft> allVariants = productData
            .getAllVariants().stream()
            .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
            .collect(toList());
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of(product.getMasterData().getStaged().getMasterVariant()).build();

        return ProductDraftBuilder
            .of(product.getProductType(), productData.getName(), productData.getSlug(), allVariants)
            .masterVariant(masterVariant)
            .metaDescription(productData.getMetaDescription())
            .metaKeywords(productData.getMetaKeywords())
            .metaTitle(productData.getMetaTitle())
            .description(productData.getDescription())
            .searchKeywords(productData.getSearchKeywords())
            .key(product.getKey())
            .publish(product.getMasterData().isPublished())
            .categories(getCategoryResourceIdentifiers(productData))
            .categoryOrderHints(productData.getCategoryOrderHints());
    }

    /**
     * Builds a {@link Set} of Category {@link ResourceIdentifier} given a {@link ProductData} instance which contains
     * references to those categories.
     *
     * @param productData the instance containing the category references.
     * @return a set of {@link Category} {@link ResourceIdentifier}
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions") // NPE (categoryReference.getObj().getKey()) is being checked in the filter.
    private static Set<ResourceIdentifier<Category>> getCategoryResourceIdentifiers(@Nonnull final ProductData
                                                                                        productData) {
        final Set<Reference<Category>> categoryReferences = productData.getCategories();
        if (categoryReferences == null) {
            return Collections.emptySet();
        }
        return categoryReferences.stream()
                                 .filter(Objects::nonNull)
                                 .filter(categoryReference -> Objects.nonNull(categoryReference.getObj()))
                                 .map(categoryReference ->
                                     ResourceIdentifier.<Category>ofIdOrKey(categoryReference.getId(),
                                         categoryReference.getObj().getKey(), Category.referenceTypeId()))
                                 .collect(Collectors.toSet());
    }
}
