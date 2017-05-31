package com.commercetools.sync.products;

import io.sphere.sdk.models.LocalizedString;
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
import java.util.stream.Collectors;

import static com.commercetools.sync.it.products.SphereClientUtils.CTP_SOURCE_CLIENT;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Locale.GERMAN;
import static java.util.Objects.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductTestUtils {
    /**
     * Empty.
     */
    public static LocalizedString localizedString(final String string) {
        if (string == null) {
            return null;
        }
        return LocalizedString.of(Locale.ENGLISH, string);
    }

    public static ProductSyncOptions getProductSyncOptions() {
        ProductSyncOptions syncOptions = mock(ProductSyncOptions.class);
        when(syncOptions.getCtpClient()).thenReturn(CTP_SOURCE_CLIENT);
        when(syncOptions.isPublish()).thenReturn(true); // TODO test other things
        return syncOptions;
    }

    public static ProductType getProductType() {
        return readObjectFromResource("product-type-main.json", ProductType.typeReference());
    }

    public static ProductDraft productDraft(String resourcePath, ProductType productType) {
        Product template = getProduct(resourcePath);
        ProductData staged = staged(template);

        List<ProductVariantDraft> allVariants = staged.getAllVariants().stream()
                .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
                .collect(Collectors.toList());

        return ProductDraftBuilder
                .of(productType, staged.getName(), staged.getSlug(), allVariants)
                .metaDescription(staged(template).getMetaDescription())
                .metaKeywords(staged(template).getMetaKeywords())
                .metaTitle(staged(template).getMetaTitle())
                .searchKeywords(staged(template).getSearchKeywords())
                .key(template.getKey())
                .publish(template.getMasterData().isPublished())
                .build();
    }

    public static Product getProduct(final String resourcePath) {
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

    public static ProductData staged(final Product product) {
        return product.getMasterData().getStaged();
    }
}
