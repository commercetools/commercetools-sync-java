package com.commercetools.sync.products;

import com.commercetools.sync.products.actions.ProductUpdateActionsBuilder;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.commercetools.sync.it.products.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.it.products.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.it.products.SphereClientUtils.fetchAndProcess;
import static com.commercetools.sync.products.ProductTestUtils.join;
import static com.commercetools.sync.products.ProductTestUtils.productDraftBuilder;
import static com.commercetools.sync.products.ProductTestUtils.productType;
import static com.commercetools.sync.products.ProductTestUtils.syncOptions;
import static com.commercetools.sync.products.helpers.ProductSyncUtils.masterData;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ConstantConditions")
public class ProductSyncItTest {

    private ProductService service;
    private ProductType productType;
    private List<Category> categories;
    private ProductUpdateActionsBuilder updateActionsBuilder;
    private Product product;

    /**
     * Initializes environment for integration test of product synchronization against CT platform.
     * <p>
     * <p>It first removes up all related resources. Then creates required product type, categories, products and
     * associates products to categories.
     */
    @Before
    public void setUp() {
        Env.delete();
        productType = Env.createProductType();
        categories = Env.createCategories();
        product = Env.createProduct(productType);

        updateActionsBuilder = ProductUpdateActionsBuilder.of();
        service = ProductService.of(CTP_SOURCE_CLIENT);
    }

    @AfterClass
    public static void tearDown() {
        // Env.delete();
    }

    @Test
    public void sync_withNewProduct_shouldCreateProduct() {
        ProductSyncOptions syncOptions = syncOptions(CTP_SOURCE_CLIENT, true, true);
        ProductDraft productDraft = Env.productDraft("product-non-existing.json", productType, syncOptions);

        // no product with given key has been populated during setup
        assertThat(join(service.fetch(productDraft.getKey())))
            .isNotPresent();

        ProductSync productSync = new ProductSync(syncOptions, service, updateActionsBuilder);
        join(productSync.sync(singletonList(productDraft)));

        assertThat(join(service.fetch(productDraft.getKey())))
            .isPresent();
    }

    @Test
    public void sync_withEqualProduct_shouldNotUpdateProduct() {
        List<Category> firstThreeCategories = Env.addProductToCategories(product, categories);

        ProductSyncOptions syncOptions = syncOptions(CTP_SOURCE_CLIENT, true, false);
        ProductDraft productDraft = Env.productDraft("product.json", productType, syncOptions,
            firstThreeCategories, oldHints(firstThreeCategories));
        final Product product = join(service.fetch(productDraft.getKey())).get();

        // when
        ProductSync productSync = new ProductSync(syncOptions, service, updateActionsBuilder);
        join(productSync.sync(singletonList(productDraft)));

        // then
        Product afterSync = join(service.fetch(productDraft.getKey())).get();
        assertThat(afterSync.getMasterData().isPublished()).isTrue();
        assertThat(afterSync.getMasterData().hasStagedChanges()).isFalse();
        assertThat(afterSync).isEqualTo(product);
    }

    @Test
    public void sync_withChangedProduct_shouldUpdateProduct() {
        Env.addProductToCategories(product, categories);

        ProductSyncOptions options = syncOptions(true, true);
        List<Category> lastThreeCategories = categories.subList(1, 4);
        ProductDraft productDraft = Env.productDraft("product-changed.json", productType, options,
            lastThreeCategories, newHints(lastThreeCategories));
        final Product product = join(service.fetch(productDraft.getKey())).get();

        // when
        ProductSync productSync = new ProductSync(options, service, updateActionsBuilder);
        join(productSync.sync(singletonList(productDraft)));

        // then
        Product afterSync = join(service.fetch(productDraft.getKey())).get();
        new DataVerifier(afterSync, product, options, categories).verify();
    }

    private static CategoryOrderHints newHints(final List<Category> lastThreeCategories) {
        return CategoryOrderHints.of(categoryOrderHintsMap(
            lastThreeCategories.get(0).getId(), String.valueOf(0.53),
            lastThreeCategories.get(1).getId(), String.valueOf(0.83),
            lastThreeCategories.get(2).getId(), String.valueOf(0.93)));
    }

    private static CategoryOrderHints oldHints(final List<Category> firstThreeCategories) {
        return CategoryOrderHints.of(categoryOrderHintsMap(
            firstThreeCategories.get(0).getId(), String.valueOf(0.43),
            firstThreeCategories.get(1).getId(), String.valueOf(0.53),
            firstThreeCategories.get(2).getId(), String.valueOf(0.63)));
    }

    private static Map<String, String> categoryOrderHintsMap(final String... categoryIdOrderHints) {
        Map<String, String> newHints = new HashMap<>();
        for (int i = 0; i < categoryIdOrderHints.length; i += 2) {
            newHints.put(categoryIdOrderHints[i], categoryIdOrderHints[i + 1]);
        }
        return newHints;
    }

    private Set<Reference<Category>> toReferences(final List<Category> lastThreeCategories) {
        return new HashSet<>(lastThreeCategories.stream().map(Category::toReference).collect(toSet()));
    }

    @Nullable
    private String en(@Nullable final LocalizedString localizedString) {
        return isNull(localizedString)
            ? null
            : localizedString.get(ENGLISH);
    }

    private class DataVerifier {
        private final Product afterSync;
        private final Product product;
        private final ProductSyncOptions options;
        private final List<Category> firstThreeCategories;
        private final List<Category> lastThreeCategories;

        DataVerifier(final Product afterSync, final Product product, final ProductSyncOptions options,
                     final List<Category> categories) {
            this.afterSync = afterSync;
            this.product = product;
            this.options = options;
            this.firstThreeCategories = categories.subList(0, 3);
            this.lastThreeCategories = categories.subList(1, 4);
        }

        void verify() {
            verifyChange(this::name, "Rehrücken ohne Knochen", "new name");
            verifyChange(this::slug, "rehruecken-o-kn", "rehruecken-o-k1");
            verifyChange(this::masterVariantSku, "3065833", "3065831");
            verifyChange(this::metaDescription, null, "new Meta description");
            verifyChange(this::metaKeywords, null, "key1,key2");
            verifyChange(this::metaTitle, null, "new title");
            verifyChange(this::searchKeywords, SearchKeywords.of(), newSearchKeywords());
            verifyChange(this::categories, toReferences(firstThreeCategories), toReferences(lastThreeCategories));
            verifyChange(this::categoryOrderHints, oldHints(firstThreeCategories), newHints(lastThreeCategories));
            assertThat(afterSync.getMasterData().isPublished()).isTrue();
            assertThat(afterSync.getMasterData().hasStagedChanges()).isFalse();
            assertThat(afterSync).isNotEqualTo(product);
        }

        SearchKeywords newSearchKeywords() {
            return SearchKeywords.of(ENGLISH, asList(SearchKeyword.of("key1"), SearchKeyword.of("key2")));
        }

        String name(final Product product) {
            return en(masterData(product, options).getName());
        }

        String slug(final Product product) {
            return en(masterData(product, options).getSlug());
        }

        String masterVariantSku(final Product product) {
            return masterData(product, options).getMasterVariant().getSku();
        }

        String metaDescription(final Product product) {
            return en(masterData(product, options).getMetaDescription());
        }

        String metaTitle(final Product product) {
            return en(masterData(product, options).getMetaTitle());
        }

        String metaKeywords(final Product product) {
            return en(masterData(product, options).getMetaKeywords());
        }

        SearchKeywords searchKeywords(final Product product) {
            return masterData(product, options).getSearchKeywords();
        }

        Set<Reference<Category>> categories(final Product product) {
            return masterData(product, options).getCategories();
        }

        CategoryOrderHints categoryOrderHints(final Product product) {
            return masterData(product, options).getCategoryOrderHints();
        }

        <X> void verifyChange(final Function<Product, X> value, final X oldValue, final X newValue) {
            assertThat(value.apply(product)).isEqualTo(oldValue);
            assertThat(value.apply(afterSync)).isEqualTo(newValue);
        }
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

        static ProductDraft productDraft(final String resourcePath, final ProductType productType,
                                         final ProductSyncOptions syncOptions) {
            return productDraftBuilder(resourcePath, productType, syncOptions)
                .categories(emptyList())
                .categoryOrderHints(null)
                .build();
        }

        static ProductDraft productDraft(final String resourcePath, final ProductType productType,
                                         final ProductSyncOptions syncOptions,
                                         final List<Category> categories,
                                         final CategoryOrderHints categoryOrderHints) {
            return productDraftBuilder(resourcePath, productType, syncOptions)
                .categories(categories.stream().map(Category::toReference).collect(toList()))
                .categoryOrderHints(categoryOrderHints)
                .build();
        }

        static List<Category> createCategories() {
            return Stream.of(category("category1.json"), category("category2.json"),
                category("category3.json"), category("category4.json"))
                .map(template -> {
                    CategoryDraftBuilder of = CategoryDraftBuilder.of(template)
                        .name(template.getName())
                        .description(template.getDescription())
                        .slug(template.getSlug());
                    return join(CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(of.build())));
                }).collect(toList());
        }

        static List<Category> addProductToCategories(final Product product, final List<Category> categories) {
            List<Category> sublist = categories.subList(0, 3);
            List<AddToCategory> addToCategories = sublist.stream()
                .map(category -> {
                    String orderHint = String.valueOf(0.43 + sublist.indexOf(category) / 10d);
                    return AddToCategory.of(category, orderHint);
                }).collect(toList());
            Product updated = join(CTP_SOURCE_CLIENT.execute(ProductUpdateCommand.of(product, addToCategories)));
            join(CTP_SOURCE_CLIENT.execute(ProductUpdateCommand.of(updated, Publish.of())));
            return sublist;
        }

        static Category category(final String resourcePath) {
            return readObjectFromResource(resourcePath, Category.typeReference());
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
