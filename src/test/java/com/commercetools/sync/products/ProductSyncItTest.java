package com.commercetools.sync.products;

import com.commercetools.sync.products.actions.ProductUpdateActionsBuilder;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import io.sphere.sdk.products.commands.updateactions.Publish;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Function;

import static com.commercetools.sync.it.products.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.it.products.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.it.products.SphereClientUtils.fetchAndProcess;
import static com.commercetools.sync.products.ProductTestUtils.de;
import static com.commercetools.sync.products.ProductTestUtils.join;
import static com.commercetools.sync.products.ProductTestUtils.productDraft;
import static com.commercetools.sync.products.ProductTestUtils.productType;
import static com.commercetools.sync.products.ProductTestUtils.syncOptions;
import static com.commercetools.sync.products.actions.ProductUpdateActionsBuilder.masterData;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Locale.GERMAN;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ConstantConditions")
public class ProductSyncItTest {

    private static SphereClient client;
    private ProductService service;
    private ProductType productType;
    private ProductSync productSync;
    private Category category;
    private ProductUpdateActionsBuilder updateActionsBuilder;

    @Before
    public void setUp() {
        client = CTP_SOURCE_CLIENT;

        Env.delete();
        productType = Env.createProductType();
        category = Env.createCategory();
        Product product = Env.createProduct(productType);
        Env.addProductToCategory(product, category);

        updateActionsBuilder = ProductUpdateActionsBuilder.of();
        service = ProductService.of(client);
    }

    @After
    public void tearDown() {
        Env.delete();
    }

    @Test
    public void sync_withNewProduct_shouldCreateProduct() {
        ProductSyncOptions syncOptions = syncOptions(client, true, true);
        ProductDraft productDraft = productDraft("product-non-existing.json", productType, category, syncOptions);

        // no product with given key has been populated during setup
        assertThat(join(service.fetch(productDraft.getKey())))
                .isNotPresent();

        productSync = new ProductSync(syncOptions, service, updateActionsBuilder);
        join(productSync.sync(singletonList(productDraft)));

        assertThat(join(service.fetch(productDraft.getKey())))
                .isPresent();
    }

    @Test
    public void sync_withEqualProduct_shouldNotUpdateProduct() {
        ProductSyncOptions syncOptions = syncOptions(client, true, false);
        ProductDraft productDraft = productDraft("product.json", productType, category, syncOptions);
        Product product = join(service.fetch(productDraft.getKey())).get();

        productSync = new ProductSync(syncOptions, service, updateActionsBuilder);
        join(productSync.sync(singletonList(productDraft)));

        Product afterSync = join(service.fetch(productDraft.getKey())).get();
        assertThat(afterSync.getMasterData().isPublished()).isTrue();
        assertThat(afterSync.getMasterData().hasStagedChanges()).isFalse();
        assertThat(afterSync).isEqualTo(product);
    }

    @Test
    public void sync_withChangedProduct_shouldUpdateProduct() {
        ProductSyncOptions options = syncOptions(true, true);
        ProductDraft productDraft = productDraft("product-changed.json", productType, category, options);
        Product product = join(service.fetch(productDraft.getKey())).get();

        productSync = new ProductSync(options, service, updateActionsBuilder);
        join(productSync.sync(singletonList(productDraft)));

        Product afterSync = join(service.fetch(productDraft.getKey())).get();
        verifyChange(p -> name(p, options), product, afterSync, "Rehrücken ohne Knochen", "new name");
        verifyChange(p -> slug(p, options), product, afterSync, "rehruecken-o-kn", "rehruecken-o-k1");
        verifyChange(p -> masterVariantSku(p, options), product, afterSync, "3065833", "3065831");
        verifyChange(p -> metaDescription(p, options), product, afterSync, null, "new Meta description");
        verifyChange(p -> metaKeywords(p, options), product, afterSync, null, "key1,key2");
        verifyChange(p -> metaTitle(p, options), product, afterSync, null, "new title");
        verifyChange(p -> searchKeywords(p, options), product, afterSync,
                SearchKeywords.of(),
                SearchKeywords.of(GERMAN, asList(SearchKeyword.of("key1"), SearchKeyword.of("key2"))));
        // verifyChange(D::categoryOrderHints, product, afterSync,
        //         CategoryOrderHints.of(emptyMap()),
        //         CategoryOrderHints.of(singletonMap(category.getId(), "0.95")));
        assertThat(afterSync.getMasterData().isPublished()).isTrue();
        assertThat(afterSync.getMasterData().hasStagedChanges()).isFalse();
        assertThat(afterSync).isNotEqualTo(product);
    }

    @Test
    public void sync_withChangedProduct_shouldUpdateProductAndPublish() {
        ProductSyncOptions options = syncOptions(client, true, true);
        ProductDraft productDraft = productDraft("product-changed.json", productType, category, options);
        Product product = join(service.fetch(productDraft.getKey())).get();

        productSync = new ProductSync(options, service, updateActionsBuilder);
        join(productSync.sync(singletonList(productDraft)));

        Product afterSync = join(service.fetch(productDraft.getKey())).get();
        verifyChange(p -> name(p, options), product, afterSync, "Rehrücken ohne Knochen", "new name");
        verifyChange(p -> slug(p, options), product, afterSync, "rehruecken-o-kn", "rehruecken-o-k1");
        verifyChange(p -> masterVariantSku(p, options), product, afterSync, "3065833", "3065831");
        verifyChange(p -> metaDescription(p, options), product, afterSync, null, "new Meta description");
        verifyChange(p -> metaKeywords(p, options), product, afterSync, null, "key1,key2");
        verifyChange(p -> metaTitle(p, options), product, afterSync, null, "new title");
        verifyChange(p -> searchKeywords(p, options), product, afterSync,
                SearchKeywords.of(),
                SearchKeywords.of(GERMAN, asList(SearchKeyword.of("key1"), SearchKeyword.of("key2"))));
        // verifyChange(D::categoryOrderHints, product, afterSync,
        //         CategoryOrderHints.of(emptyMap()),
        //         CategoryOrderHints.of(singletonMap(category.getId(), "0.95")));
        assertThat(afterSync.getMasterData().isPublished()).isTrue();
        assertThat(afterSync.getMasterData().hasStagedChanges()).isFalse();
        assertThat(afterSync).isNotEqualTo(product);
    }

    private static <X> void verifyChange(final Function<Product, X> valueProvider,
                                         final Product old, final Product newP,
                                         final X oldValue, final X newValue) {
        assertThat(valueProvider.apply(old)).isEqualTo(oldValue);
        assertThat(valueProvider.apply(newP)).isEqualTo(newValue);
    }

    private static String name(final Product product, final ProductSyncOptions syncOptions) {
        return de(masterData(product, syncOptions).getName());
    }

    private static String slug(final Product product, final ProductSyncOptions options) {
        return de(masterData(product, options).getSlug());
    }

    private static String masterVariantSku(final Product product, final ProductSyncOptions options) {
        return masterData(product, options).getMasterVariant().getSku();
    }

    private static String metaDescription(final Product product, final ProductSyncOptions options) {
        return de(masterData(product, options).getMetaDescription());
    }

    private static String metaTitle(final Product product, final ProductSyncOptions options) {
        return de(masterData(product, options).getMetaTitle());
    }

    private static String metaKeywords(final Product product, final ProductSyncOptions options) {
        return de(masterData(product, options).getMetaKeywords());
    }

    private static SearchKeywords searchKeywords(final Product product, final ProductSyncOptions options) {
        return masterData(product, options).getSearchKeywords();
    }

//        private static CategoryOrderHints categoryOrderHints(final Product product) {
//            return masterData(product, options).getCategoryOrderHints();
//        }

    private static class Env {

        static void delete() {
            deleteProducts();
            deleteProductTypes();
            deleteCategories();
        }

        static ProductType createProductType() {
            ProductTypeDraftDsl build = ProductTypeDraftBuilder.of(productType()).build();
            return join(client.execute(ProductTypeCreateCommand.of(build)));
        }

        static Product createProduct(final ProductType productType) {
            ProductSyncOptions syncOptions = syncOptions(true, true);
            ProductDraft draft = productDraft("product.json", productType, syncOptions);
            return join(client.execute(ProductCreateCommand.of(draft)));
        }

        static Category createCategory() {
            Category template = category();
            CategoryDraftBuilder of = CategoryDraftBuilder.of(template)
                    .name(template.getName())
                    .description(template.getDescription())
                    .slug(template.getSlug());
            return join(client.execute(CategoryCreateCommand.of(of.build())));
        }

        static void addProductToCategory(final Product product, final Category category) {
            Product updated = join(client.execute(ProductUpdateCommand.of(product, AddToCategory.of(category))));
            join(client.execute(ProductUpdateCommand.of(updated, Publish.of())));
        }

        private static Category category() {
            return readObjectFromResource("category.json", Category.typeReference());
        }

        private static void deleteProductTypes() {
            fetchAndProcess(client, () -> ProductTypeQuery.of().withLimit(QUERY_MAX_LIMIT),
                    ProductTypeDeleteCommand::of);
        }

        private static void deleteProducts() {
            fetchAndProcess(client, () -> withLimit(ProductQuery.of()),
                    p -> p.getMasterData().isPublished()
                            ? ProductUpdateCommand.of(p, Unpublish.of())
                            : ProductDeleteCommand.of(p));
            fetchAndProcess(client, () -> withLimit(ProductQuery.of()),
                    ProductDeleteCommand::of);
        }

        private static void deleteCategories() {
            fetchAndProcess(client, () -> CategoryQuery.of().withLimit(QUERY_MAX_LIMIT),
                    CategoryDeleteCommand::of);
        }

        private static <T, C extends QueryDsl<T, C>> C withLimit(final QueryDsl<T, C> of) {
            return of.withLimit(QUERY_MAX_LIMIT);
        }
    }
}
