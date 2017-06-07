package com.commercetools.sync.products;

import com.commercetools.sync.products.actions.ProductUpdateActionsBuilder;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.CategoryOrderHints;
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
import io.sphere.sdk.search.SearchKeyword;
import io.sphere.sdk.search.SearchKeywords;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.function.BiFunction;

import static com.commercetools.sync.it.products.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.it.products.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.it.products.SphereClientUtils.fetchAndProcess;
import static com.commercetools.sync.products.ProductTestUtils.join;
import static com.commercetools.sync.products.ProductTestUtils.productDraft;
import static com.commercetools.sync.products.ProductTestUtils.productType;
import static com.commercetools.sync.products.ProductTestUtils.syncOptions;
import static com.commercetools.sync.products.helpers.ProductSyncUtils.masterData;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Locale.GERMAN;
import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ConstantConditions")
public class ProductSyncItTest {

    private ProductService service;
    private ProductType productType;
    private ProductSync productSync;
    private Category category;
    private ProductUpdateActionsBuilder updateActionsBuilder;

    /**
     * Initializes environment for integration test of product synchronization against CT platform.
     *
     * <p>It first removes up all related resources. Then creates required product type, categories, products and
     * associates products to categories.
     */
    @Before
    public void setUp() {
        Env.delete();
        productType = Env.createProductType();
        category = Env.createCategory();
        Product product = Env.createProduct(productType);
        Env.addProductToCategory(product, category);

        updateActionsBuilder = ProductUpdateActionsBuilder.of();
        service = ProductService.of(CTP_SOURCE_CLIENT);
    }

    @AfterClass
    public static void tearDown() {
        Env.delete();
    }

    @Test
    public void sync_withNewProduct_shouldCreateProduct() {
        ProductSyncOptions syncOptions = syncOptions(CTP_SOURCE_CLIENT, true, true);
        ProductDraft productDraft = productDraft("product-non-existing.json", productType, syncOptions);

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
        ProductSyncOptions syncOptions = syncOptions(CTP_SOURCE_CLIENT, true, false);
        ProductDraft productDraft = productDraft("product.json", productType, syncOptions);
        final Product product = join(service.fetch(productDraft.getKey())).get();

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
        ProductDraft productDraft = productDraft("product-changed.json", productType, options, category);
        final Product product = join(service.fetch(productDraft.getKey())).get();

        productSync = new ProductSync(options, service, updateActionsBuilder);
        join(productSync.sync(singletonList(productDraft)));

        Product afterSync = join(service.fetch(productDraft.getKey())).get();
        verifyChange(this::name, options, product, afterSync, "Rehrücken ohne Knochen", "new name");
        verifyChange(this::slug, options, product, afterSync, "rehruecken-o-kn", "rehruecken-o-k1");
        verifyChange(this::masterVariantSku, options, product, afterSync, "3065833", "3065831");
        verifyChange(this::metaDescription, options, product, afterSync, null, "new Meta description");
        verifyChange(this::metaKeywords, options, product, afterSync, null, "key1,key2");
        verifyChange(this::metaTitle, options, product, afterSync, null, "new title");
        verifyChange(this::searchKeywords, options, product, afterSync,
            SearchKeywords.of(),
            SearchKeywords.of(GERMAN, asList(SearchKeyword.of("key1"), SearchKeyword.of("key2"))));
        verifyChange(this::categoryOrderHints, options, product, afterSync,
            CategoryOrderHints.of(emptyMap()),
            CategoryOrderHints.of(singletonMap(category.getId(), "0.95")));
        assertThat(afterSync.getMasterData().isPublished()).isTrue();
        assertThat(afterSync.getMasterData().hasStagedChanges()).isFalse();
        assertThat(afterSync).isNotEqualTo(product);
    }

    private static <X> void verifyChange(final BiFunction<Product, ProductSyncOptions, X> valueProvider,
                                         final ProductSyncOptions syncOptions,
                                         final Product old, final Product newP,
                                         final X oldValue, final X newValue) {
        assertThat(valueProvider.apply(old, syncOptions)).isEqualTo(oldValue);
        assertThat(valueProvider.apply(newP, syncOptions)).isEqualTo(newValue);
    }

    private String name(final Product product, final ProductSyncOptions syncOptions) {
        return de(masterData(product, syncOptions).getName());
    }

    private String slug(final Product product, final ProductSyncOptions options) {
        return de(masterData(product, options).getSlug());
    }

    private String masterVariantSku(final Product product, final ProductSyncOptions options) {
        return masterData(product, options).getMasterVariant().getSku();
    }

    private String metaDescription(final Product product, final ProductSyncOptions options) {
        return de(masterData(product, options).getMetaDescription());
    }

    private String metaTitle(final Product product, final ProductSyncOptions options) {
        return de(masterData(product, options).getMetaTitle());
    }

    private String metaKeywords(final Product product, final ProductSyncOptions options) {
        return de(masterData(product, options).getMetaKeywords());
    }

    private SearchKeywords searchKeywords(final Product product, final ProductSyncOptions options) {
        return masterData(product, options).getSearchKeywords();
    }

    private CategoryOrderHints categoryOrderHints(final Product product, final ProductSyncOptions options) {
        return masterData(product, options).getCategoryOrderHints();
    }

    private String de(final LocalizedString localizedString) {
        return isNull(localizedString)
            ? null
            : localizedString.get(GERMAN);
    }

    static class Env {

        static void delete() {
            deleteProducts();
            deleteProductTypes();
            deleteCategories();
        }

        static ProductType createProductType() {
            ProductTypeDraftDsl build = ProductTypeDraftBuilder.of(productType()).build();
            return join(CTP_SOURCE_CLIENT.execute(ProductTypeCreateCommand.of(build)));
        }

        static Product createProduct(final ProductType productType) {
            ProductSyncOptions syncOptions = syncOptions(true, true);
            ProductDraft draft = productDraft("product.json", productType, syncOptions);
            return join(CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(draft)));
        }

        static Category createCategory() {
            Category template = category();
            CategoryDraftBuilder of = CategoryDraftBuilder.of(template)
                .name(template.getName())
                .description(template.getDescription())
                .slug(template.getSlug());
            return join(CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(of.build())));
        }

        static void addProductToCategory(final Product product, final Category category) {
            Product updated =
                join(CTP_SOURCE_CLIENT.execute(ProductUpdateCommand.of(product, AddToCategory.of(category))));
            join(CTP_SOURCE_CLIENT.execute(ProductUpdateCommand.of(updated, Publish.of())));
        }

        static Category category() {
            return readObjectFromResource("category.json", Category.typeReference());
        }

        static void deleteProductTypes() {
            fetchAndProcess(CTP_SOURCE_CLIENT, () -> ProductTypeQuery.of().withLimit(QUERY_MAX_LIMIT),
                ProductTypeDeleteCommand::of);
        }

        static void deleteProducts() {
            fetchAndProcess(CTP_SOURCE_CLIENT, () -> ProductQuery.of().withLimit(QUERY_MAX_LIMIT),
                p -> p.getMasterData().isPublished()
                    ? ProductUpdateCommand.of(p, Unpublish.of())
                    : ProductDeleteCommand.of(p));
            fetchAndProcess(CTP_SOURCE_CLIENT, () -> ProductQuery.of().withLimit(QUERY_MAX_LIMIT),
                ProductDeleteCommand::of);
        }

        static void deleteCategories() {
            fetchAndProcess(CTP_SOURCE_CLIENT, () -> CategoryQuery.of().withLimit(QUERY_MAX_LIMIT),
                CategoryDeleteCommand::of);
        }

    }
}
