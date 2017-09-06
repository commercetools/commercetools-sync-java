package com.commercetools.sync.products;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.products.utils.ProductDataUtils.masterData;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

public class ProductSyncMockUtils {
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
    public static CategoryOrderHints buildRandomCategoryOrderHints(@Nonnull final List<Category> categories) {
        final Map<String, String> categoryOrderHints = new HashMap<>();
        categories.forEach(category -> {
            final double randomDouble = ThreadLocalRandom.current().nextDouble(0, 1);
            categoryOrderHints.put(category.getId(), valueOf(randomDouble ));
        });
        return CategoryOrderHints.of(categoryOrderHints);
    }
}
