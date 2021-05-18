package com.commercetools.sync.integration.ctpprojectsource.products;

import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createPriceDraft;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.neovisionaries.i18n.CountryCode.DE;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.products.utils.ProductTransformUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.PriceTier;
import io.sphere.sdk.products.PriceTierBuilder;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductProjectionByKeyGet;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.utils.MoneyImpl;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithPricesIT {

  private static final String RESOURCE_KEY = "foo";
  private static ProductProjectionQuery productQuery;

  private ProductSync productSync;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private ReferenceIdToKeyCache referenceIdToKeyCache;

  @BeforeAll
  static void setupProjectData() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteProductSyncTestData(CTP_SOURCE_CLIENT);
  }

  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();

    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();
    productSync = new ProductSync(syncOptions);
    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
  }

  @AfterEach
  void tearDownSuite() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteProductSyncTestData(CTP_SOURCE_CLIENT);
  }

  @Test
  void sync_withNewProductWithPriceTiers_shouldCreateProduct() {

    createProductType(CTP_TARGET_CLIENT);
    final PriceTier priceTier = PriceTierBuilder.of(2, MoneyImpl.of(BigDecimal.TEN, EUR)).build();

    final List<ProductProjection> products = prepareDataWithPriceTier(priceTier, CTP_SOURCE_CLIENT);

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    // assertion
    AssertionsForStatistics.assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertProductsWithPriceTier(priceTier);
  }

  @Test
  void sync_withMatchingProductWithPriceTierChanges_shouldUpdateProduct() {

    final PriceTier targetPriceTier =
        PriceTierBuilder.of(3, MoneyImpl.of(BigDecimal.ONE, EUR)).build();
    prepareDataWithPriceTier(targetPriceTier, CTP_TARGET_CLIENT);

    final PriceTier sourcePriceTier =
        PriceTierBuilder.of(2, MoneyImpl.of(BigDecimal.TEN, EUR)).build();

    final List<ProductProjection> products =
        prepareDataWithPriceTier(sourcePriceTier, CTP_SOURCE_CLIENT);

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    // assertion
    AssertionsForStatistics.assertThat(syncStatistics).hasValues(1, 0, 1, 0);
    assertProductsWithPriceTier(sourcePriceTier);
  }

  @Nonnull
  private List<ProductProjection> prepareDataWithPriceTier(
      @Nonnull final PriceTier priceTier, @Nonnull final SphereClient client) {
    final ProductType productType = createProductType(client);

    final PriceDraft priceBuilder =
        PriceDraftBuilder.of(
                createPriceDraft(
                    BigDecimal.valueOf(222),
                    EUR,
                    DE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    asList(priceTier)))
            .build();

    final ProductVariantDraft variantDraft1 =
        ProductVariantDraftBuilder.of().key("variantKey").sku("sku1").prices(priceBuilder).build();

    final ProductDraft productDraft =
        ProductDraftBuilder.of(
                productType, ofEnglish("V-neck Tee"), ofEnglish("v-neck-tee"), variantDraft1)
            .productType(ProductType.referenceOfId(productType.getId()))
            .key(RESOURCE_KEY)
            .publish(true)
            .build();

    client.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();
    productQuery = ProductProjectionQuery.ofStaged();
    return client.execute(productQuery).toCompletableFuture().join().getResults();
  }

  @Nonnull
  private ProductType createProductType(@Nonnull final SphereClient client) {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of(
                RESOURCE_KEY, "sample-product-type", "a productType for t-shirts", emptyList())
            .build();
    final ProductType productType =
        client.execute(ProductTypeCreateCommand.of(productTypeDraft)).toCompletableFuture().join();
    return productType;
  }

  private void assertProductsWithPriceTier(PriceTier sourcePriceTier) {
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    // Assert that the target product was created/updated with sourcePriceTiers.
    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(ProductProjectionByKeyGet.of(RESOURCE_KEY, ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();
    final List<Price> createdPrices = productProjection.getMasterVariant().getPrices();
    assertThat(createdPrices)
        .anySatisfy(
            price -> {
              assertThat(price.getTiers()).isNotNull();
              assertThat(price.getTiers().get(0)).isEqualTo(sourcePriceTier);
            });
  }
}
