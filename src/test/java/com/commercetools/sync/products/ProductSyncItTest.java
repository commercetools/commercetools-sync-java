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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

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
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ConstantConditions")
public class ProductSyncItTest {

    private ProductService service;
    private ProductType productType;
    private ProductSync productSync;
    private List<Category> categories;
    private ProductUpdateActionsBuilder updateActionsBuilder;
    private List<Category> firstThreeCategories;

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
        categories = Env.createCategories();
        Product product = Env.createProduct(productType);
        firstThreeCategories = Env.addProductToCategories(product, categories);

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
        ProductDraft productDraft = productDraft("product-non-existing.json", productType, syncOptions,
            singletonList(categories.get(0)));

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
        ProductDraft productDraft = productDraft("product.json", productType, syncOptions,
            firstThreeCategories, false);
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
        List<Category> lastThreeCategories = categories.subList(1, 4);
        ProductDraft productDraft = productDraft("product-changed.json", productType, options,
            lastThreeCategories);
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
            SearchKeywords.of(ENGLISH, asList(SearchKeyword.of("key1"), SearchKeyword.of("key2"))));
        verifyChange(this::categories, options, product, afterSync,
            toReferences(firstThreeCategories), toReferences(lastThreeCategories));
        verifyChange(this::categoryOrderHints, options, product, afterSync,
            CategoryOrderHints.of(emptyMap()),
            CategoryOrderHints.of(singletonMap(categories.get(1).getId(), "0.95")));
        assertThat(afterSync.getMasterData().isPublished()).isTrue();
        assertThat(afterSync.getMasterData().hasStagedChanges()).isFalse();
        assertThat(afterSync).isNotEqualTo(product);
    }

    private Set<Reference<Category>> toReferences(final List<Category> lastThreeCategories) {
        return new HashSet<>(lastThreeCategories.stream().map(Category::toReference).collect(toSet()));
    }

    private static <X> void verifyChange(final BiFunction<Product, ProductSyncOptions, X> valueProvider,
                                         final ProductSyncOptions syncOptions,
                                         final Product old, final Product newP,
                                         final X oldValue, final X newValue) {
        assertThat(valueProvider.apply(old, syncOptions)).isEqualTo(oldValue);
        assertThat(valueProvider.apply(newP, syncOptions)).isEqualTo(newValue);
    }

    private String name(final Product product, final ProductSyncOptions syncOptions) {
        return en(masterData(product, syncOptions).getName());
    }

    private String slug(final Product product, final ProductSyncOptions options) {
        return en(masterData(product, options).getSlug());
    }

    private String masterVariantSku(final Product product, final ProductSyncOptions options) {
        return masterData(product, options).getMasterVariant().getSku();
    }

    private String metaDescription(final Product product, final ProductSyncOptions options) {
        return en(masterData(product, options).getMetaDescription());
    }

    private String metaTitle(final Product product, final ProductSyncOptions options) {
        return en(masterData(product, options).getMetaTitle());
    }

    private String metaKeywords(final Product product, final ProductSyncOptions options) {
        return en(masterData(product, options).getMetaKeywords());
    }

    private SearchKeywords searchKeywords(final Product product, final ProductSyncOptions options) {
        return masterData(product, options).getSearchKeywords();
    }

    private Set<Reference<Category>> categories(final Product product, final ProductSyncOptions options) {
        return masterData(product, options).getCategories();
    }

    private CategoryOrderHints categoryOrderHints(final Product product, final ProductSyncOptions options) {
        return masterData(product, options).getCategoryOrderHints();
    }

    @Nullable
    private String en(@Nullable final LocalizedString localizedString) {
        return isNull(localizedString)
            ? null
            : localizedString.get(ENGLISH);
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
            ProductDraft draft = productDraft("product.json", productType, syncOptions, emptyList());
            return join(CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(draft)));
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
            List<AddToCategory> addToCategories = sublist.stream().map(AddToCategory::of).collect(toList());
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
