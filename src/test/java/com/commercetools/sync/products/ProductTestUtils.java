package com.commercetools.sync.products;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.commercetools.sync.products.actions.ProductUpdateActionsBuilder.masterData;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Collections.singletonMap;
import static java.util.Locale.GERMAN;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.mockito.Mockito.mock;

public class ProductTestUtils {
    public static LocalizedString localizedString(final String string) {
        if (string == null) {
            return null;
        }
        return LocalizedString.of(Locale.ENGLISH, string);
    }

    public static ProductType productType() {
        return readObjectFromResource("product-type-main.json", ProductType.typeReference());
    }

    public static ProductDraft productDraft(String resourcePath, ProductType productType,
                                            Category category, ProductSyncOptions syncOptions) {
        Product template = product(resourcePath);
        ProductData productData = masterData(template, syncOptions);

        @SuppressWarnings("ConstantConditions")
        List<ProductVariantDraft> allVariants = productData.getAllVariants().stream()
                .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
                .collect(Collectors.toList());

        ProductDraftBuilder builder = ProductDraftBuilder
                .of(productType, productData.getName(), productData.getSlug(), allVariants)
                .metaDescription(productData.getMetaDescription())
                .metaKeywords(productData.getMetaKeywords())
                .metaTitle(productData.getMetaTitle())
                .searchKeywords(productData.getSearchKeywords())
                .key(template.getKey())
                .publish(template.getMasterData().isPublished());
        if (nonNull(category)) {
            builder = builder.categoryOrderHints(CategoryOrderHints.of(singletonMap(category.getId(), "0.95")));
        }
        return builder.build();
    }

    public static ProductDraft productDraft(String resourcePath, ProductType productType, ProductSyncOptions syncOptions) {
        return productDraft(resourcePath, productType, null, syncOptions);
    }

    public static Product product(final String resourcePath) {
        return readObjectFromResource(resourcePath, Product.typeReference());
    }

    public static String de(final LocalizedString localizedString) {
        if (isNull(localizedString)) {
            return null;
        }
        return localizedString.get(GERMAN);
    }

    public static LocalizedString de(final String string) {
        if (isNull(string)) {
            return null;
        }
        return LocalizedString.of(GERMAN, string);
    }

    public static <X> X join(CompletionStage<X> stage) {
        return stage.toCompletableFuture().join();
    }

    @SuppressWarnings("unchecked")
    public static ProductSyncOptions syncOptions(boolean publish, boolean compareStaged) {
        return syncOptions(mock(SphereClient.class), publish, compareStaged);
    }

    @SuppressWarnings("unchecked")
    public static ProductSyncOptions syncOptions(SphereClient client, boolean publish, boolean compareStaged) {
        BiConsumer errorCallBack = mock(BiConsumer.class);
        Consumer warningCallBack = mock(Consumer.class);
        return ProductSyncOptionsBuilder.of(client, errorCallBack, warningCallBack).publish(publish).compareStaged(compareStaged).build();
    }
}
