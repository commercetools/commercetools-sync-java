package com.commercetools.sync.products;

import com.commercetools.sync.products.actions.ProductUpdateActionsBuilder;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.ProductTypeDraftDsl;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.search.SearchKeyword;
import io.sphere.sdk.search.SearchKeywords;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.function.Function;

import static com.commercetools.sync.it.products.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.it.products.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.it.products.SphereClientUtils.fetchAndProcess;
import static com.commercetools.sync.products.ProductTestUtils.de;
import static com.commercetools.sync.products.ProductTestUtils.getProductSyncOptions;
import static com.commercetools.sync.products.ProductTestUtils.getProductType;
import static com.commercetools.sync.products.ProductTestUtils.join;
import static com.commercetools.sync.products.ProductTestUtils.productDraft;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Locale.GERMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ConstantConditions")
public class ProductSyncItTest {

    private static SphereClient client;
    private ProductService service;
    private ProductType productType;
    private ProductSync productSync;

    @Before
    public void setUp() {
        client = CTP_SOURCE_CLIENT;

        InitEnv.delete();
        productType = InitEnv.createProductType();
        InitEnv.createProduct(productType);

        service = spy(ProductService.of(client));
        productSync = new ProductSync(getProductSyncOptions(), service, ProductUpdateActionsBuilder.of(getProductSyncOptions()));
    }

    @AfterClass
    public static void tearDown() {
        // delete();
    }

    @Test
    public void sync_WithNewProduct_ShouldCreateProduct() {
        ProductDraft productDraft = productDraft("product-non-existing.json", productType);

        // no product with given key has been populated during setup
        Assertions.assertThat(join(service.fetch(productDraft.getKey())))
                .isNotPresent();

        join(productSync.sync(singletonList(productDraft)));

        Assertions.assertThat(join(service.fetch(productDraft.getKey())))
                .isPresent();
    }

    @Test
    public void sync_WithEqualProduct_ShouldNotUpdateProduct() {
        ProductDraft productDraft = productDraft("product.json", productType);
        Product product = join(service.fetch(productDraft.getKey())).get();

        join(productSync.sync(singletonList(productDraft)));

        verify(service, never()).update(any(), any());
        Product productAfterSync = join(service.fetch(productDraft.getKey())).get();
        assertThat(productAfterSync.getMasterData().isPublished()).isTrue();
        assertThat(productAfterSync.getMasterData().hasStagedChanges()).isFalse();
        assertThat(productAfterSync).isEqualTo(product);
    }

    @Test
    public void sync_WithChangedProduct_ShouldUpdateProduct() {
        ProductDraft productDraft = productDraft("product-changed.json", productType);
        Product product = join(service.fetch(productDraft.getKey())).get();

        join(productSync.sync(singletonList(productDraft)));

        Product productAfterSync = join(service.fetch(productDraft.getKey())).get();
        verify(service).update(any(), any());
        verifyChange(D::name, product, productAfterSync, "Rehrücken ohne Knochen", "new name");
        verifyChange(D::slug, product, productAfterSync, "rehruecken-o-kn", "rehruecken-o-k1");
        verifyChange(D::masterVariantSku, product, productAfterSync, "3065833", "3065831");
        verifyChange(D::metaDescription, product, productAfterSync, null, "new Meta description");
        verifyChange(D::metaKeywords, product, productAfterSync, null, "key1,key2");
        verifyChange(D::metaTitle, product, productAfterSync, null, "new title");
        verifyChange(D::searchKeywords, product, productAfterSync, SearchKeywords.of(), SearchKeywords.of(GERMAN, asList(SearchKeyword.of("key1"), SearchKeyword.of("key2"))));
        assertThat(productAfterSync.getMasterData().isPublished()).isTrue();
        assertThat(productAfterSync.getMasterData().hasStagedChanges()).isFalse();
        assertThat(productAfterSync).isNotEqualTo(product);
    }

    private <X> void verifyChange(final Function<Product, X> valueProvider,
                                  final Product old, final Product newP,
                                  final X oldvalue, final X newValue) {
        assertThat(valueProvider.apply(old)).isEqualTo(oldvalue);
        assertThat(valueProvider.apply(newP)).isEqualTo(newValue);
    }

    private static class D {
        private static String name(final Product product) {
            return de(ProductTestUtils.staged(product).getName());
        }

        private static String slug(final Product product) {
            return de(ProductTestUtils.staged(product).getSlug());
        }

        private static String masterVariantSku(final Product product) {
            return ProductTestUtils.staged(product).getMasterVariant().getSku();
        }

        private static String metaDescription(final Product product) {
            return de(ProductTestUtils.staged(product).getMetaDescription());
        }

        private static String metaTitle(final Product product) {
            return de(ProductTestUtils.staged(product).getMetaTitle());
        }

        private static String metaKeywords(final Product product) {
            return de(ProductTestUtils.staged(product).getMetaKeywords());
        }

        private static SearchKeywords searchKeywords(final Product product) {
            return ProductTestUtils.staged(product).getSearchKeywords();
        }
    }

    private static class InitEnv {

        static void delete() {
            deleteProducts(client);
            deleteProductTypes(client);
        }

        static ProductType createProductType() {
            ProductTypeDraftDsl build = ProductTypeDraftBuilder.of(getProductType()).build();
            return join(client.execute(ProductTypeCreateCommand.of(build)));
        }

        static void createProduct(final ProductType productType) {
            ProductDraft draft = productDraft("product.json", productType);
            join(client.execute(ProductCreateCommand.of(draft)));
        }

        private static void deleteProductTypes(@Nonnull final SphereClient sphereClient) {
            fetchAndProcess(sphereClient, () -> ProductTypeQuery.of().withLimit(QUERY_MAX_LIMIT),
                    ProductTypeDeleteCommand::of);
        }

        private static void deleteProducts(@Nonnull final SphereClient sphereClient) {
            fetchAndProcess(sphereClient, () -> withLimit(ProductQuery.of()),
                    p -> p.getMasterData().isPublished()
                            ? ProductUpdateCommand.of(p, Unpublish.of())
                            : ProductDeleteCommand.of(p));
            fetchAndProcess(sphereClient, () -> withLimit(ProductQuery.of()),
                    ProductDeleteCommand::of);
        }

        private static <T, C extends QueryDsl<T, C>> C withLimit(final QueryDsl<T, C> of) {
            return of.withLimit(QUERY_MAX_LIMIT);
        }

    }
}
