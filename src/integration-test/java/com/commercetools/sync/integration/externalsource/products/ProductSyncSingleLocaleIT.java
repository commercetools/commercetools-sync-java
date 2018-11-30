package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.SetDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.products.queries.ProductProjectionByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.templates.beforeupdatecallback.SyncSingleLocale.syncFrenchDataOnly;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.of;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.producttypes.ProductType.referenceOfId;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductSyncSingleLocaleIT {
    private static ProductType productType;
    private ProductSync productSync;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private List<UpdateAction<Product>> updateActionsFromSync;

    /**
     * Delete all product related test data from the target project. Then creates price custom types, customer groups,
     * channels and a product type for the target CTP project.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    /**
     * Deletes Products from the target CTP project.
     */
    @Before
    public void setupTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        productSync = new ProductSync(buildSyncOptions());
    }

    private void clearSyncTestCollections() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
        updateActionsFromSync = new ArrayList<>();
    }

    private ProductSyncOptions buildSyncOptions() {
        final BiConsumer<String, Throwable> errorCallBack = (errorMessage, exception) -> {
            errorCallBackMessages.add(errorMessage);
            errorCallBackExceptions.add(exception);
        };
        final Consumer<String> warningCallBack = warningMessage -> warningCallBackMessages.add(warningMessage);

        final TriFunction<List<UpdateAction<Product>>, ProductDraft, Product, List<UpdateAction<Product>>>
            actionsCallBack = (updateActions, newDraft, oldProduct) -> {
                final List<UpdateAction<Product>> afterCallback =
                        syncFrenchDataOnly(updateActions, newDraft, oldProduct);
                updateActionsFromSync.addAll(afterCallback);
                return afterCallback;
            };

        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(errorCallBack)
                                        .warningCallback(warningCallBack)
                                        .beforeUpdateCallback(actionsCallBack)
                                        .build();
    }

    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    public void sync_withSingleLocaleBeforeUpdateCallback_ShouldSyncCorrectly() {
        // Preparation
        final LocalizedString nameTarget = ofEnglish("name_target").plus(Locale.FRENCH, "nom_target");
        final LocalizedString nameSource = ofEnglish("name_source").plus(Locale.FRENCH, "nom_source");

        final LocalizedString slugTarget = ofEnglish("slug_target");
        final LocalizedString slugSource = ofEnglish("slug_source").plus(Locale.FRENCH, "limace_source");

        final LocalizedString descriptionSource = of(Locale.FRENCH, "description_source");
        final LocalizedString metaDescriptionSource = ofEnglish("md_source");
        final LocalizedString metaKeywordsTarget = ofEnglish("mk_target");
        final LocalizedString metaTitleTarget = of(Locale.FRENCH, "mt_target");


        final ProductDraft targetDraft = ProductDraftBuilder
            .of(productType.toReference(), nameTarget, slugTarget,
                    ProductVariantDraftBuilder.of().key("foo").sku("sku").build())
            .key("existingProduct")
            .metaKeywords(metaKeywordsTarget)
            .metaTitle(metaTitleTarget)
            .build();

        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(targetDraft)));

        final ProductDraft newProductDraft = ProductDraftBuilder
            .of(referenceOfId(productType.getKey()), nameSource, slugSource,
                    ProductVariantDraftBuilder.of().key("foo").sku("sku").build())
                .key("existingProduct")
                .description(descriptionSource)
                .metaDescription(metaDescriptionSource)
                .isPublish(true)
                .build();


        // test
        final ProductSyncStatistics syncStatistics =
            executeBlocking(productSync.sync(singletonList(newProductDraft)));

        // assertion
        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        final LocalizedString expectedName = ofEnglish("name_target").plus(Locale.FRENCH, "nom_source");
        final LocalizedString expectedSlug = ofEnglish("slug_target").plus(Locale.FRENCH, "limace_source");
        final LocalizedString expectedMetaTitle = of();

        assertThat(updateActionsFromSync).containsExactlyInAnyOrder(
                ChangeName.of(expectedName),
                ChangeSlug.of(expectedSlug),
                SetDescription.of(descriptionSource),
                SetMetaTitle.of(expectedMetaTitle),
                Publish.of()
        );

        final ProductProjection productProjection = CTP_TARGET_CLIENT
            .execute(ProductProjectionByKeyGet.of(newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture().join();

        assertThat(productProjection).isNotNull();
        assertThat(productProjection.getName()).isEqualTo(expectedName);
        assertThat(productProjection.getSlug()).isEqualTo(expectedSlug);
        assertThat(productProjection.getMetaTitle()).isNull();
        assertThat(productProjection.getMetaDescription()).isNull();
        assertThat(productProjection.getDescription()).isEqualTo(descriptionSource);
        assertThat(productProjection.getMetaKeywords()).isEqualTo(metaKeywordsTarget);
    }
}
