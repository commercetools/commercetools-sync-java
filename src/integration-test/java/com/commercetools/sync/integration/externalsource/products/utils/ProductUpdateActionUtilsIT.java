package com.commercetools.sync.integration.externalsource.products.utils;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductUpdateActionUtilsIT {

    private static final String DRAFTS_ROOT = "com/commercetools/sync/integration/externalsource/products/utils/";
    private static final String NEW_PRODUCT = DRAFTS_ROOT + "productDraftNewIT.json";
    private static final String OLD_PRODUCT = DRAFTS_ROOT + "productDraftOldIT.json";

    private static ProductType targetProductType;

    /**
     * Delete all product related test data from target and source projects. Then creates custom types for both
     * CTP projects categories.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        targetProductType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    /**
     * Create old product from the draft.
     */
    @Before
    public void setUp() {
        ProductDraft productDraftOld = SphereJsonUtils.readObjectFromResource(OLD_PRODUCT, ProductDraft.class);
        ProductDraft productDraft = ProductDraftBuilder.of(productDraftOld)
            .productType(ResourceIdentifier.ofKey(targetProductType.getKey()))
            .build();
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
    }

    @Test
    public void buildVariantsUpdateActions_shouldUpdateVariants() {
        final List<String> errors = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback((str, thr) -> errors.add(str))
            .warningCallback(warnings::add)
            .build();

        final ProductSync productSync = new ProductSync(productSyncOptions);

        final ProductDraft productDraftNew = SphereJsonUtils.readObjectFromResource(NEW_PRODUCT, ProductDraft.class);

        ProductDraft productDraft = ProductDraftBuilder.of(productDraftNew)
            .productType(ResourceIdentifier.ofId(targetProductType.getKey()))
            .build();

        final ProductSyncStatistics sync = executeBlocking(productSync.sync(Collections.singletonList(productDraft)));
        assertThat(errors).isEmpty();
        assertThat(warnings).isEmpty();

        assertThat(sync.getCreated()).isEqualTo(0);
        assertThat(sync.getUpdated()).isEqualTo(1);
        assertThat(sync.getProcessed()).isEqualTo(1);
        assertThat(sync.getFailed()).isEqualTo(0);

        final Product synced = executeBlocking(CTP_TARGET_CLIENT.execute(ProductByKeyGet.of("productToSyncKey")));
        final ProductVariant syncedMasterVariant = synced.getMasterData().getStaged().getMasterVariant();
        assertThat(syncedMasterVariant.getKey()).isEqualTo("var-7-key");
        assertThat(syncedMasterVariant.getSku()).isEqualTo("var-7-sku");
        assertThat(syncedMasterVariant.getImages()).hasSize(1);
        final Image masterImage = syncedMasterVariant.getImages().get(0);
        assertThat(masterImage.getUrl()).isEqualTo("https://xxx.ggg/7.png");
        assertThat(masterImage.getHeight()).isEqualTo(10);
        assertThat(syncedMasterVariant.getAttributes()).hasSize(2);
        assertThat(syncedMasterVariant.getAttribute("priceInfo").getValueAsString()).isEqualTo("44/kg");
        assertThat(syncedMasterVariant.getAttribute("size").getValueAsString()).isEqualTo("ca. 7 x 7000 g");

        final List<ProductVariant> syncedVariants = synced.getMasterData().getStaged().getVariants();
        assertThat(syncedVariants).hasSize(3); // "4" is updated, "5" and "6" are added
        ProductVariant var4 = syncedVariants.stream()
            .filter(variant -> "var-4-key".equals(variant.getKey()))
            .findFirst().orElse(null);
        assertThat(var4).isNotNull();
        assertThat(var4.getImages().get(0).getUrl()).isEqualTo("https://yyy.ggg/44.png");
        assertThat(var4.getImages().get(0).getHeight()).isEqualTo(20);
        assertThat(var4.getAttribute("priceInfo").getValueAsString()).isEqualTo("44/kg");
        assertThat(var4.getAttribute("size").getValueAsString()).isEqualTo("ca. 7 x 7000 g");
    }
}
