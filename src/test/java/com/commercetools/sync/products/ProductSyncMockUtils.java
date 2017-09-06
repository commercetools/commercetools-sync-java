package com.commercetools.sync.products;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.commercetools.sync.products.utils.ProductDataUtils.masterData;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.lang.String.valueOf;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductSyncMockUtils {
    public static final String PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH = "product-key-1-published.json";
    public static final String PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH = "product-key-1-unpublished.json";
    public static final String CATEGORY_KEY_1_RESOURCE_PATH = "category-key-1.json";
    public static final String CATEGORY_KEY_2_RESOURCE_PATH = "category-key-2.json";
    public static final String CATEGORY_KEY_3_RESOURCE_PATH = "category-key-3.json";

    /**
     * Wraps provided string in {@link LocalizedString} of English {@link Locale}.
     */
    @Nullable
    public static LocalizedString en(@Nullable final String string) {
        return localizedString(string, ENGLISH);
    }

    @Nullable
    private static LocalizedString localizedString(final @Nullable String string, final Locale locale) {
        return isNull(string)
            ? null
            : LocalizedString.of(locale, string);
    }

    /**
     * Provides {@link ProductDraftBuilder} built on product template read from {@code resourcePath},
     * based on {@code productType} with {@code syncOptions} applied and category order hints set if {@code category}
     * is not null.
     */
    public static ProductDraftBuilder createProductDraftBuilder(@Nonnull final String resourceAsJsonString,
                                                                @Nonnull final ProductType productType,
                                                                @Nonnull final ProductSyncOptions syncOptions) {
        final Product productFromJson = readObjectFromResource(resourceAsJsonString, Product.class);
        final ProductData productData = masterData(productFromJson, syncOptions);

        @SuppressWarnings("ConstantConditions")
        final List<ProductVariantDraft> allVariants = productData.getAllVariants().stream()
            .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
            .collect(toList());

        return ProductDraftBuilder
            .of(productType, productData.getName(), productData.getSlug(), allVariants)
            .metaDescription(productData.getMetaDescription())
            .metaKeywords(productData.getMetaKeywords())
            .metaTitle(productData.getMetaTitle())
            .description(productData.getDescription())
            .searchKeywords(productData.getSearchKeywords())
            .key(productFromJson.getKey())
            .publish(productFromJson.getMasterData().isPublished())
            .categories(productData.getCategories())
            .categoryOrderHints(productData.getCategoryOrderHints());
    }

    /**
     * Given a {@code locale}, {@code name}, {@code slug}, {@code externalId}, {@code description},
     * {@code metaDescription}, {@code metaTitle}, {@code metaKeywords}, {@code orderHint} and
     * {@code parentId}; this method creates a mock of {@link Category} with all those supplied fields. All the supplied
     * arguments are given as {@link String} and the method internally converts them to their required types.
     * For example, for all the fields that require a {@link LocalizedString} as a value type; the method creates an
     * instance of a {@link LocalizedString} with the given {@link String} and {@link Locale}.
     *
     * @param locale          the locale to create with all the {@link LocalizedString} instances.
     * @param name            the name of the category.
     * @return an instance {@link Category} with all the given fields set in the given {@link Locale}.
     */
    public static Product getMockProduct(@Nonnull final Locale locale,
                                           @Nonnull final String name) {
        final Product oldProduct = mock(Product.class);
        when(oldProduct.getMasterData().getCurrent().getName()).thenReturn(LocalizedString.of(locale, name));
        return oldProduct;
    }

    public static CategoryOrderHints buildRandomCategoryOrderHints(@Nonnull final List<Category> categories) {
        final Map<String, String> categoryOrderHints = new HashMap<>();
        categories.forEach(category -> {
            final double randomDouble = ThreadLocalRandom.current().nextDouble(0, 1);
            categoryOrderHints.put(category.getId(), valueOf(randomDouble ));
        });
        return CategoryOrderHints.of(categoryOrderHints);
    }
}
