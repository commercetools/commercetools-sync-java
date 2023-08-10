package com.commercetools.sync.integration.ctpprojectsource.products;

import static com.commercetools.api.models.common.DefaultCurrencyUnits.EUR;
import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createPriceDraft;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.neovisionaries.i18n.CountryCode.DE;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.common.PriceTier;
import com.commercetools.api.models.common.PriceTierDraft;
import com.commercetools.api.models.common.PriceTierDraftBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponse;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.sdk2.products.utils.ProductTransformUtils;
import io.vrap.rmf.base.client.ApiHttpResponse;
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
    final PriceTierDraft priceDraftTier =
        PriceTierDraftBuilder.of()
            .value(moneyBuilder -> moneyBuilder.centAmount(10L).currencyCode(EUR.getCurrencyCode()))
            .minimumQuantity(2L)
            .build();

    final List<ProductProjection> products =
        prepareDataWithPriceTier(priceDraftTier, CTP_SOURCE_CLIENT);

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertProductsWithPriceTier(priceDraftTier);
  }

  @Test
  void sync_withMatchingProductWithPriceTierChanges_shouldUpdateProduct() {

    final PriceTierDraft targetPriceTier =
        PriceTierDraftBuilder.of()
            .minimumQuantity(3L)
            .value(moneyBuilder -> moneyBuilder.currencyCode(EUR.getCurrencyCode()).centAmount(1L))
            .build();
    prepareDataWithPriceTier(targetPriceTier, CTP_TARGET_CLIENT);

    final PriceTierDraft sourcePriceTier =
        PriceTierDraftBuilder.of()
            .minimumQuantity(2L)
            .value(moneyBuilder -> moneyBuilder.centAmount(10L).currencyCode(EUR.getCurrencyCode()))
            .build();

    final List<ProductProjection> products =
        prepareDataWithPriceTier(sourcePriceTier, CTP_SOURCE_CLIENT);

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0);
    assertProductsWithPriceTier(sourcePriceTier);
  }

  @Nonnull
  private List<ProductProjection> prepareDataWithPriceTier(
      @Nonnull final PriceTierDraft priceTierDraft, @Nonnull final ProjectApiRoot client) {
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
                    List.of(priceTierDraft)))
            .build();

    final ProductVariantDraft variantDraft1 =
        ProductVariantDraftBuilder.of().key("variantKey").sku("sku1").prices(priceBuilder).build();

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("V-neck Tee"))
            .slug(ofEnglish("v-neck-tee"))
            .masterVariant(variantDraft1)
            .productType(ProductTypeResourceIdentifierBuilder.of().id(productType.getId()).build())
            .key(RESOURCE_KEY)
            .publish(true)
            .build();

    client.products().create(productDraft).executeBlocking();
    return client
        .productProjections()
        .get()
        .withStaged(true)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .thenApply(ProductProjectionPagedQueryResponse::getResults)
        .join();
  }

  @Nonnull
  private ProductType createProductType(@Nonnull final ProjectApiRoot client) {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(RESOURCE_KEY)
            .name("sample-product-type")
            .description("a productType for t-shirts")
            .attributes(List.of())
            .build();
    return client
        .productTypes()
        .create(productTypeDraft)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .join();
  }

  private void assertProductsWithPriceTier(PriceTierDraft sourcePriceTier) {
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    // Assert that the target product was created/updated with sourcePriceTiers.
    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(RESOURCE_KEY)
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    assertThat(productProjection).isNotNull();
    final List<Price> createdPrices = productProjection.getMasterVariant().getPrices();
    assertThat(createdPrices)
        .anySatisfy(
            price -> {
              assertThat(price.getTiers()).isNotNull();
              final PriceTier priceTier = price.getTiers().get(0);
              assertThat(priceTier.getMinimumQuantity())
                  .isEqualTo(sourcePriceTier.getMinimumQuantity());
              assertThat(priceTier.getValue().getCurrencyCode())
                  .isEqualTo(sourcePriceTier.getValue().getCurrencyCode());
              assertThat(priceTier.getValue().getCentAmount())
                  .isEqualTo(sourcePriceTier.getValue().getCentAmount());
            });
  }
}
