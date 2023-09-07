package com.commercetools.sync.integration.externalsource.products.templates.beforeupdatecallback;

import static com.commercetools.api.models.common.LocalizedString.of;
import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.templates.beforeupdatecallback.SyncSingleLocale.syncFrenchDataOnly;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.products.templates.beforeupdatecallback.SyncSingleLocale;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SyncSingleLocaleIT {
  private static ProductType productType;
  private ProductSync productSync;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<ProductUpdateAction> updateActionsFromSync;

  /**
   * Delete all product related test data from the target project. Then creates a product type for
   * the target CTP project.
   */
  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  /** Deletes Products from the target CTP project. */
  @BeforeEach
  void setupTest() {
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
    final QuadConsumer<
            SyncException,
            Optional<ProductDraft>,
            Optional<ProductProjection>,
            List<ProductUpdateAction>>
        errorCallBack =
            (exception, newResource, oldResource, updateActions) -> {
              errorCallBackMessages.add(exception.getMessage());
              errorCallBackExceptions.add(exception.getCause());
            };
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallBack =
            (exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage());

    final TriFunction<
            List<ProductUpdateAction>, ProductDraft, ProductProjection, List<ProductUpdateAction>>
        actionsCallBack =
            (updateActions, newDraft, oldProduct) -> {
              final List<ProductUpdateAction> afterCallback =
                  syncFrenchDataOnly(updateActions, newDraft, oldProduct);
              updateActionsFromSync.addAll(afterCallback);
              return afterCallback;
            };

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(errorCallBack)
        .warningCallback(warningCallBack)
        .beforeUpdateCallback(actionsCallBack)
        .beforeCreateCallback(SyncSingleLocale::filterFrenchLocales)
        .build();
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withSingleLocaleBeforeUpdateCallback_ShouldSyncCorrectly() {
    // Preparation
    final LocalizedString nameTarget = ofEnglish("name_target").plus(Locale.FRENCH, "nom_target");
    final LocalizedString nameSource = ofEnglish("name_source").plus(Locale.FRENCH, "nom_source");

    final LocalizedString slugTarget = ofEnglish("slug_target");
    final LocalizedString slugSource =
        ofEnglish("slug_source").plus(Locale.FRENCH, "limace_source");

    final LocalizedString descriptionSource = of(Locale.FRENCH, "description_source");
    final LocalizedString metaDescriptionSource = ofEnglish("md_source");
    final LocalizedString metaKeywordsTarget = ofEnglish("mk_target");
    final LocalizedString metaTitleTarget = of(Locale.FRENCH, "mt_target");

    final ProductDraft targetDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(nameTarget)
            .slug(slugTarget)
            .masterVariant(ProductVariantDraftBuilder.of().key("foo").sku("sku").build())
            .key("existingProduct")
            .metaKeywords(metaKeywordsTarget)
            .metaTitle(metaTitleTarget)
            .build();

    CTP_TARGET_CLIENT.products().create(targetDraft).executeBlocking();

    final ProductDraft matchingDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(nameSource)
            .slug(slugSource)
            .masterVariant(ProductVariantDraftBuilder.of().key("foo").sku("sku").build())
            .key("existingProduct")
            .description(descriptionSource)
            .metaDescription(metaDescriptionSource)
            .publish(true)
            .build();

    final ProductDraft nonMatchingDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(nameSource)
            .slug(LocalizedString.of(Locale.FRENCH, "foo"))
            .masterVariant(ProductVariantDraftBuilder.of().key("foo-new").sku("sku-new").build())
            .key("newProduct")
            .description(descriptionSource)
            .metaDescription(metaDescriptionSource)
            .metaKeywords(ofEnglish("foo"))
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(asList(matchingDraft, nonMatchingDraft)).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(2, 1, 1, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final LocalizedString expectedUpdatedName =
        ofEnglish("name_target").plus(Locale.FRENCH, "nom_source");
    final LocalizedString expectedUpdatedSlug =
        ofEnglish("slug_target").plus(Locale.FRENCH, "limace_source");
    final LocalizedString expectedUpdatedMetaTitle = of();

    assertThat(updateActionsFromSync)
        .containsExactlyInAnyOrder(
            ProductChangeNameActionBuilder.of().name(expectedUpdatedName).build(),
            ProductChangeSlugActionBuilder.of().slug(expectedUpdatedSlug).build(),
            ProductSetDescriptionActionBuilder.of().description(descriptionSource).build(),
            ProductSetMetaTitleActionBuilder.of().metaTitle(expectedUpdatedMetaTitle).build(),
            ProductPublishActionBuilder.of().build());

    ApiHttpResponse<ProductProjectionPagedQueryResponse> response =
        CTP_TARGET_CLIENT
            .productProjections()
            .get()
            .withWhere("masterVariant(sku in (\"sku\",\"sku-new\"))")
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join();

    final Map<String, ProductProjection> projectionMap =
        response.getBody().getResults().stream()
            .collect(Collectors.toMap(ProductProjection::getKey, Function.identity()));

    final ProductProjection updatedProduct = projectionMap.get(matchingDraft.getKey());

    assertThat(updatedProduct).isNotNull();
    assertThat(updatedProduct.getName()).isEqualTo(expectedUpdatedName);
    assertThat(updatedProduct.getSlug()).isEqualTo(expectedUpdatedSlug);
    assertThat(updatedProduct.getMetaTitle()).isNull();
    assertThat(updatedProduct.getMetaDescription()).isNull();
    assertThat(updatedProduct.getDescription()).isEqualTo(descriptionSource);
    assertThat(updatedProduct.getMetaKeywords()).isEqualTo(metaKeywordsTarget);

    final ProductProjection createdProduct = projectionMap.get(nonMatchingDraft.getKey());

    final LocalizedString expectedCreatedName = LocalizedString.of(Locale.FRENCH, "nom_source");
    final LocalizedString expectedCreatedSlug = LocalizedString.of(Locale.FRENCH, "foo");

    assertThat(createdProduct).isNotNull();
    assertThat(createdProduct.getName()).isEqualTo(expectedCreatedName);
    assertThat(createdProduct.getSlug()).isEqualTo(expectedCreatedSlug);
    assertThat(createdProduct.getMetaTitle()).isNull();
    assertThat(createdProduct.getMetaDescription()).isNull();
    assertThat(createdProduct.getDescription()).isEqualTo(descriptionSource);
    assertThat(createdProduct.getMetaKeywords()).isNull();
  }
}
