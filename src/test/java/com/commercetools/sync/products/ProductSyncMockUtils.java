package com.commercetools.sync.products;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.stream.Collectors.toList;

public class ProductSyncMockUtils {
    public static final String PRODUCT_KEY_1_RESOURCE_PATH = "product-key-1.json";
    public static final String PRODUCT_KEY_1_CHANGED_RESOURCE_PATH = "product-key-1-changed.json";
    public static final String PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH = "product-key-1-with-prices.json";
    public static final String PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH =
        "product-key-1-changed-with-prices.json";
    public static final String PRODUCT_KEY_2_RESOURCE_PATH = "product-key-2.json";
    public static final String PRODUCT_TYPE_RESOURCE_PATH = "product-type.json";
    public static final String PRODUCT_TYPE_NO_KEY_RESOURCE_PATH = "product-type-no-key.json";
    public static final String CATEGORY_KEY_1_RESOURCE_PATH = "category-key-1.json";

    /**
     * Unfortunately, <a href="http://dev.commercetools.com/http-api-projects-products.html#category-order-hints">
     * <i>Category Order Hints</i></a> in CTP platform is quite picky: it requires number values as a string
     * and only without trailing zeros and only in fixed point format.
     *
     * @see <a href="http://dev.commercetools.com/http-api-projects-products.html#category-order-hints">
     * http://dev.commercetools.com/http-api-projects-products.html#category-order-hints</a>
     */
    private static final DecimalFormat ORDER_HINT_FORMAT;

    static {
        ORDER_HINT_FORMAT = new DecimalFormat();
        ORDER_HINT_FORMAT.setMaximumFractionDigits(Integer.MAX_VALUE);
        ORDER_HINT_FORMAT.setMaximumIntegerDigits(1);
    }

    /**
     * Builds a {@link ProductDraftBuilder} based on the staged projection of the product JSON resource located at the
     * {@code jsonResourcePath} and based on the supplied {@code productType}.
     * TODO: GITHUB ISSUE#152
     *
     * @param jsonResourcePath     the path of the JSON resource to build the product draft from.
     * @param productTypeReference the reference of the product type that the product draft belongs to.
     * @return a {@link ProductDraftBuilder} instance containing the data from the current projection of the specified
     *          JSON resource and the product type.
     */
    public static ProductDraftBuilder createProductDraftBuilder(@Nonnull final String jsonResourcePath,
                                                                @Nonnull final Reference<ProductType>
                                                                    productTypeReference) {
        final Product productFromJson = readObjectFromResource(jsonResourcePath, Product.class);
        final ProductData productData = productFromJson.getMasterData().getStaged();

        @SuppressWarnings("ConstantConditions") final List<ProductVariantDraft> allVariants = productData
            .getAllVariants().stream()
            .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
            .collect(toList());

        return ProductDraftBuilder
            .of(productTypeReference, productData.getName(), productData.getSlug(), allVariants)
            .metaDescription(productData.getMetaDescription())
            .metaKeywords(productData.getMetaKeywords())
            .metaTitle(productData.getMetaTitle())
            .description(productData.getDescription())
            .searchKeywords(productData.getSearchKeywords())
            .taxCategory(productFromJson.getTaxCategory())
            .state(productFromJson.getState())
            .key(productFromJson.getKey())
            .categories(
                productData.getCategories().stream().map(Reference::toResourceIdentifier).collect(Collectors.toSet()))
            .categoryOrderHints(productData.getCategoryOrderHints())
            .publish(productFromJson.getMasterData().isPublished());
    }


    /**
     * Given a {@link List} of {@link Category}, this method returns an instance of {@link CategoryOrderHints}
     * containing a {@link Map}, in which each entry has category id from the supplied {@link List} as a key and a
     * random categoryOrderHint which is a {@link String} containing a random double value between 0 and 1 (exclusive).
     *
     * <p>Note: The random double value is generated by the {@link ThreadLocalRandom#current()} nextDouble method.
     *
     * @param categoryResources list of references of categories to build categoryOrderHints for.
     * @return an instance of {@link CategoryOrderHints} containing a categoryOrderHint for each category in the
     *         supplied list of categories.
     */
    public static CategoryOrderHints createRandomCategoryOrderHints(@Nonnull final List<Reference<Category>>
                                                                        categoryResources) {
        final Map<String, String> categoryOrderHints = new HashMap<>();
        categoryResources.forEach(resourceIdentifier -> {
            final double randomDouble = ThreadLocalRandom.current().nextDouble(1e-8, 1);
            categoryOrderHints.put(resourceIdentifier.getId(), ORDER_HINT_FORMAT.format(randomDouble));
        });
        return CategoryOrderHints.of(categoryOrderHints);
    }

    /**
     * Builds a {@link ProductDraft} based on the current projection of the product JSON resource located at the
     * {@code jsonResourcePath} and based on the supplied {@code productType}, {@code taxCategoryReference} and
     * {@code stateReference}. The method also attaches the created {@link ProductDraft} to all the {@code categories}
     * specified and assigns {@code categoryOrderHints} for it for each category assigned.
     * TODO: GITHUB ISSUE#152
     *
     * @param jsonResourcePath     the path of the JSON resource to build the product draft from.
     * @param productTypeReference the reference of the  product type that the product draft belongs to.
     * @param categoryReferences   the references to the categories to attach this product draft to.
     * @param categoryOrderHints   the categoryOrderHint for each category this product belongs to.
     * @return a {@link ProductDraft} instance containing the data from the current projection of the specified
     *         JSON resource and the product type. The draft would be assigned also to the specified {@code categories}
     *         with the supplied {@code categoryOrderHints}.
     */
    public static ProductDraft createProductDraft(@Nonnull final String jsonResourcePath,
                                                  @Nonnull final Reference<ProductType> productTypeReference,
                                                  @Nullable final Reference<TaxCategory> taxCategoryReference,
                                                  @Nullable final Reference<State> stateReference,
                                                  @Nonnull final List<Reference<Category>> categoryReferences,
                                                  @Nullable final CategoryOrderHints categoryOrderHints) {
        return createProductDraftBuilder(jsonResourcePath, productTypeReference)
            .taxCategory(taxCategoryReference)
            .state(stateReference)
            .categories(categoryReferences)
            .categoryOrderHints(categoryOrderHints)
            .build();
    }

    public static Product createProductFromJson(@Nonnull final String jsonResourcePath) {
        return readObjectFromResource(jsonResourcePath, Product.class);
    }

    public static ProductDraft createProductDraftFromJson(@Nonnull final String jsonResourcePath) {
        return readObjectFromResource(jsonResourcePath, ProductDraft.class);
    }
}
