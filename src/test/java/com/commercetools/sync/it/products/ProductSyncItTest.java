package com.commercetools.sync.it.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductDraftDsl;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.ProductTypeDraftDsl;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.it.products.ProductSyncItTest.ProductIntegrationTestUtils.deleteProducts;
import static com.commercetools.sync.it.products.ProductSyncItTest.ProductIntegrationTestUtils.getProductSyncOptions;
import static com.commercetools.sync.it.products.ProductSyncItTest.ProductIntegrationTestUtils.getStagedProductData;
import static com.commercetools.sync.it.products.ProductSyncItTest.ProductIntegrationTestUtils.join;
import static com.commercetools.sync.it.products.ProductSyncItTest.ProductIntegrationTestUtils.productDraft;
import static com.commercetools.sync.it.products.ProductSyncItTest.ProductIntegrationTestUtils.populateSourceProject;
import static com.commercetools.sync.it.products.ProductSyncItTest.ProductIntegrationTestUtils.populateTargetProject;
import static com.commercetools.sync.it.products.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.it.products.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.it.products.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.it.products.SphereClientUtils.fetchAndProcess;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Empty.
 */
@SuppressWarnings("ConstantConditions")
public class ProductSyncItTest {

    private static ProductService service = ProductService.of(CTP_SOURCE_CLIENT);
    private static ProductType productType;
    private static ProductSync productSync;

    /**
     * Empty.
     */
    @BeforeClass
    public static void setup() {
        deleteProducts();
        populateSourceProject();
        populateTargetProject();
        productSync = new ProductSync(getProductSyncOptions());
    }

    /**
     * Empty.
     */
    @AfterClass
    public static void delete() {
        // deleteProducts();
    }

    @Test
    public void sync_WithNewProduct_ShouldCreateProduct() {
        ProductDraft productDraft = productDraft("product-non-existing.json");

        // no product with given key has been populated during setup
        assertThat(join(service.fetch(productDraft.getKey())))
                .isNotPresent();

        productSync.sync(singletonList(productDraft));

        assertThat(join(service.fetch(productDraft.getKey())))
                .isPresent();
    }

    @Test
    public void sync_WithEqualProduct_ShouldNotUpdateProduct() {
        ProductDraft productDraft = productDraft("product.json");
        Product product = join(service.fetch(productDraft.getKey())).get();

        productSync.sync(singletonList(productDraft));

        Product productAfterSync = join(service.fetch(productDraft.getKey())).get();
        assertThat(productAfterSync).isEqualTo(product);
    }

    @Test
    public void sync_WithNewProduct_ShouldUpdateProduct() {
        ProductDraft productDraft = productDraft("product-changed-name.json");
        Product product = join(service.fetch(productDraft.getKey())).get();

        productSync.sync(singletonList(productDraft));

        Product productAfterSync = join(service.fetch(productDraft.getKey())).get();
        assertChange(ProductSyncItTest::name, product, productAfterSync,
                "Rehrücken ohne Knochen", "new name");
        assertThat(productAfterSync).isNotEqualTo(product);
    }

    private <X> void assertChange(final Function<Product, X> valueProvider,
                                  final Product product, final Product productAfterSync,
                                  final X original, final X expected) {
        assertThat(valueProvider.apply(product)).isEqualTo(original);
        assertThat(valueProvider.apply(productAfterSync)).isEqualTo(expected);
    }

    private static String name(final Product product) {
        return getStagedProductData(product).getName().get(Locale.GERMAN);
    }

    static class ProductIntegrationTestUtils {

        static void deleteProducts() {
            deleteProducts(CTP_SOURCE_CLIENT);
            deleteProductTypes(CTP_SOURCE_CLIENT);
            deleteProducts(CTP_TARGET_CLIENT);
            deleteProductTypes(CTP_TARGET_CLIENT);
        }

        static void populateSourceProject() {
            populateProductType();
            populateProduct();
        }

        static ProductDraft productDraft(String resourcePath) {
            return getProductDraft(getProductTemplate(resourcePath));
        }

        static void populateTargetProject() {
        }

        static ProductData getStagedProductData(final Product product) {
            return product.getMasterData().getStaged();
        }

        static ProductSyncOptions getProductSyncOptions() {
            return when(mock(ProductSyncOptions.class).getCtpClient()).thenReturn(CTP_SOURCE_CLIENT).getMock();
        }

        static <X> X join(CompletionStage<X> stage) {
            return stage.toCompletableFuture().join();
        }

        private static void deleteProductTypes(@Nonnull final SphereClient sphereClient) {
            fetchAndProcess(sphereClient, ProductIntegrationTestUtils::productTypeQuerySupplier,
                    ProductTypeDeleteCommand::of);
        }

        private static void deleteProducts(@Nonnull final SphereClient sphereClient) {
            fetchAndProcess(sphereClient, ProductIntegrationTestUtils::productQuerySupplier,
                    ProductDeleteCommand::of);
        }

        private static void populateProduct() {
            ProductDraft draft = productDraft("product.json");
            CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(draft)).toCompletableFuture().join();
        }

        private static ProductDraftDsl getProductDraft(Product mockMainProduct) {
            ProductData current = getStagedProductData(mockMainProduct);

            List<ProductVariantDraft> allVariants = current.getAllVariants().stream()
                    .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
                    .collect(Collectors.toList());

            return ProductDraftBuilder
                    .of(productType, current.getName(), current.getSlug(), allVariants)
                    .key(mockMainProduct.getKey())
                    .build();
        }

        private static Product getProductTemplate(final String resourcePath) {
            return readObjectFromResource(resourcePath, Product.typeReference());
        }

        private static void populateProductType() {
            ProductType productType = readObjectFromResource("product-type-main.json", ProductType.typeReference());
            ProductTypeDraftDsl build = ProductTypeDraftBuilder.of(productType).build();
            ProductSyncItTest.productType = CTP_SOURCE_CLIENT.execute(ProductTypeCreateCommand.of(build)).toCompletableFuture().join();
        }

        private static ProductQuery productQuerySupplier() {
            return ProductQuery.of().withLimit(QUERY_MAX_LIMIT);
        }

        private static ProductTypeQuery productTypeQuerySupplier() {
            return ProductTypeQuery.of().withLimit(QUERY_MAX_LIMIT);
        }

        private static LocalizedString en(final String string) {
            return LocalizedString.ofEnglish(string);
        }
    }
}
