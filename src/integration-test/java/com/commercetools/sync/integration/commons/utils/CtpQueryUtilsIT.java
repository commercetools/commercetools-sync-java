package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.commons.utils.CtpQueryUtils.queryAll;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class CtpQueryUtilsIT {

    /**
     * Delete all product related test data from target project. Then create a productType and 10 products in the target
     * CTP project.
     */
    @BeforeClass
    public static void setupAllTests() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        final ProductType productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        createProducts(productType, 10);
    }

    private static void createProducts(@Nonnull final ProductType productType, final int numberOfProducts) {
        IntStream.range(0, numberOfProducts).forEach(i -> {
            final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH,
                productType.toReference())
                .key(format("key_%s", i))
                .slug(LocalizedString.ofEnglish(format("slug_%s", i)))
                .masterVariant(ProductVariantDraftBuilder.of().build())
                .taxCategory(null)
                .state(null)
                .categories(Collections.emptySet())
                .categoryOrderHints(null).build();
            executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
        });
    }

    @Test
    public void queryAll_WithPageMapper_ShouldApplyMapperOnAllPages() {
        final Function<List<Product>, Product> productPageMapper = (productPage) -> productPage.get(0);
        final List<Product> result = executeBlocking(queryAll(CTP_TARGET_CLIENT, ProductQuery.of(), productPageMapper));
        assertThat(result).hasSize(1);
    }

    @Test
    public void queryAll_WithPageMapperAndUniformPageSplitting_ShouldApplyMapperOnAllPages() {
        final Function<List<Product>, Product> productPageMapper = (productPage) -> productPage.get(0);
        final List<Product> result = executeBlocking(queryAll(CTP_TARGET_CLIENT, ProductQuery.of(), productPageMapper,
            2));
        assertThat(result).hasSize(5);
    }

    @Test
    public void queryAll_WithPageMapperAndNonUniformPageSplitting_ShouldApplyMapperOnAllPages() {
        final Function<List<Product>, Product> productPageMapper = (productPage) -> productPage.get(0);
        final List<Product> result = executeBlocking(queryAll(CTP_TARGET_CLIENT, ProductQuery.of(), productPageMapper,
            3));
        assertThat(result).hasSize(4);
    }

    @Test
    public void queryAll_WithPageConsumer_ShouldAcceptConsumerOnAllPages() {
        final ArrayList<String> productKeys = new ArrayList<>();
        final Consumer<List<Product>> productPageConsumer =
            (productPage) -> productPage.forEach(product -> productKeys.add(product.getKey()));

        executeBlocking(queryAll(CTP_TARGET_CLIENT, ProductQuery.of(), productPageConsumer));

        final List<String> expectedListOfKeys = IntStream.range(0, 10)
                                                         .mapToObj(i -> format("key_%s", i))
                                                         .collect(Collectors.toList());

        assertThat(productKeys).hasSize(10);
        assertThat(productKeys).containsAll(expectedListOfKeys);
    }

    @Test
    public void queryAll_WithPageConsumerAndUniformPageSplitting_ShouldAcceptConsumerOnAllPages() {
        final ArrayList<String> productKeys = new ArrayList<>();
        final Consumer<List<Product>> productPageConsumer =
            (productPage) -> productPage.forEach(product -> productKeys.add(product.getKey()));

        executeBlocking(queryAll(CTP_TARGET_CLIENT, ProductQuery.of(), productPageConsumer, 2));

        final List<String> expectedListOfKeys = IntStream.range(0, 10)
                                                         .mapToObj(i -> format("key_%s", i))
                                                         .collect(Collectors.toList());

        assertThat(productKeys).hasSize(10);
        assertThat(productKeys).containsAll(expectedListOfKeys);
    }

    @Test
    public void queryAll_WithPageConsumerAndNonUniformPageSplitting_ShouldAcceptConsumerOnAllPages() {
        final ArrayList<String> productKeys = new ArrayList<>();
        final Consumer<List<Product>> productPageConsumer =
            (productPage) -> productPage.forEach(product -> productKeys.add(product.getKey()));

        executeBlocking(queryAll(CTP_TARGET_CLIENT, ProductQuery.of(), productPageConsumer, 3));

        final List<String> expectedListOfKeys = IntStream.range(0, 10)
                                                         .mapToObj(i -> format("key_%s", i))
                                                         .collect(Collectors.toList());

        assertThat(productKeys).hasSize(10);
        assertThat(productKeys).containsAll(expectedListOfKeys);
    }
}
