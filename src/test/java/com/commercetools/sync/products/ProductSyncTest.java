package com.commercetools.sync.products;

import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftFromJson;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductTypeService;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductSyncTest {

    private static final String RES_ROOT = "com/commercetools/sync/products/utils/productVariantUpdateActionUtils/";
    private static final String OLD_PROD_WITH_VARIANTS = RES_ROOT + "productOld.json";

    private static final String NEW_PROD_DRAFT_WITHOUT_MV =
        RES_ROOT + "productDraftNew_noMasterVariant.json";
    private static final String OLD_PROD_DRAFT_WITH_VARIANTS = RES_ROOT + "productDraftUpdate.json";

    private CategoryService categoryService;
    private TypeService typeService;
    private ChannelService channelService;
    private CustomerGroupService customerGroupService;
    private TaxCategoryService taxCategoryService;
    private StateService stateService;
    private ProductService productService;

    private Product item;
    private List<UpdateAction<Product>> updateActions;

    private Map<String, String> cache;
    private Set<Product> matching;
    private Set<Product> result;

    @Before
    public void setup() {
        categoryService = mock(CategoryService.class);
        typeService = mock(TypeService.class);
        channelService = mock(ChannelService.class);
        customerGroupService = mock(CustomerGroupService.class);
        taxCategoryService = mock(TaxCategoryService.class);
        stateService = mock(StateService.class);
        productService = mock(ProductService.class);

        cache = new HashMap<>();
        matching = new HashSet<>();
        result = new HashSet<>();

        when(productService.cacheKeysToIds(any())).thenReturn(completedFuture(cache));
        when(productService.fetchMatchingProductsByKeys(any()))
            .thenReturn(completedFuture(matching));
        when(productService.createProducts(any())).thenReturn(completedFuture(result));

        item = null;
        updateActions = new ArrayList<>();
    }

    @Test
    public void sync_WithNotExistingProduct_ShouldCallAfterCreateCallback() {
        final ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITHOUT_MV);

        ProductTypeService productTypeService = getProductTypeService("id");

        ProductSyncOptions options = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
            .afterCreateCallback(created -> item = created)
            .build();

        ProductSync productSync = new ProductSync(options, productService, productTypeService,
            categoryService, typeService, channelService, customerGroupService, taxCategoryService,
            stateService);

        result.add(mock(Product.class));

        productSync.sync(singletonList(productDraftNew));

        assertThat(item).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void sync_WithExistingProduct_ShouldCallAfterUpdateCallback() {
        final ProductDraft productDraftUpdate = createProductDraftFromJson(
            OLD_PROD_DRAFT_WITH_VARIANTS);

        ProductTypeService productTypeService = getProductTypeService(
            productDraftUpdate.getProductType().getId());

        ProductSyncOptions options = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
            .afterUpdateCallback((updated, actions) -> {
                item = updated;
                updateActions = actions;
            })
            .build();

        final ProductSync productSync = new ProductSync(options, productService, productTypeService,
            categoryService, typeService, channelService, customerGroupService, taxCategoryService,
            stateService);

        cache.put(productDraftUpdate.getKey(), productDraftUpdate.getKey());

        matching.add(createProductFromJson(OLD_PROD_WITH_VARIANTS));

        Optional<Map<String, AttributeMetaData>> opt = Optional.of(new HashMap<>());

        when(categoryService.fetchMatchingCategoriesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>()));
        when(productTypeService.fetchCachedProductAttributeMetaDataMap(any()))
            .thenReturn(completedFuture(opt));
        when(productService.updateProduct(any(), any()))
            .thenReturn(completedFuture(mock(Product.class)));

        productSync.sync(singletonList(productDraftUpdate));

        assertThat(item).isNotNull();
        assertThat(updateActions).isNotEmpty();
    }

    private ProductTypeService getProductTypeService(final String id) {
        return getMockProductTypeService(id);
    }

}
