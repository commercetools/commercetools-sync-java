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
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.products.utils.ProductTransformUtils;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.CustomerGroupDraft;
import io.sphere.sdk.customergroups.CustomerGroupDraftBuilder;
import io.sphere.sdk.customergroups.commands.CustomerGroupCreateCommand;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.AssetSourceBuilder;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.StringFieldType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithUnexpandedReferencesIT {

  private static final String RESOURCE_KEY = "foo";
  private static final String TYPE_KEY = "typeKey";
  private static ProductProjectionQuery productQuery;

  private ProductSync productSync;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private ReferenceIdToKeyCache referenceIdToKeyCache;

  @BeforeAll
  static void setupSourceProjectData() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteProductSyncTestData(CTP_SOURCE_CLIENT);
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of(
                RESOURCE_KEY, "sample-product-type", "a productType for t-shirts", emptyList())
            .build();

    final ProductType productType =
        CTP_SOURCE_CLIENT
            .execute(ProductTypeCreateCommand.of(productTypeDraft))
            .toCompletableFuture()
            .join();

    CTP_TARGET_CLIENT
        .execute(ProductTypeCreateCommand.of(productTypeDraft))
        .toCompletableFuture()
        .join();

    final FieldDefinition FIELD_DEFINITION_1 =
        FieldDefinition.of(
            StringFieldType.of(),
            "field_name_1",
            LocalizedString.ofEnglish("label_1"),
            false,
            TextInputHint.SINGLE_LINE);

    final TypeDraft typeDraft =
        TypeDraftBuilder.of(
                TYPE_KEY,
                LocalizedString.ofEnglish("name_1"),
                ResourceTypeIdsSetBuilder.of().addCategories().addPrices().addAssets().build())
            .description(LocalizedString.ofEnglish("description_1"))
            .fieldDefinitions(Arrays.asList(FIELD_DEFINITION_1))
            .build();

    final Type type =
        CTP_SOURCE_CLIENT.execute(TypeCreateCommand.of(typeDraft)).toCompletableFuture().join();

    CTP_TARGET_CLIENT.execute(TypeCreateCommand.of(typeDraft)).toCompletableFuture().join();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(ofEnglish("t-shirts"), ofEnglish("t-shirts"))
            .key(RESOURCE_KEY)
            .build();

    CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(categoryDraft)).toCompletableFuture().join();

    CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(categoryDraft)).toCompletableFuture().join();

    final StateDraft stateDraft =
        StateDraftBuilder.of(RESOURCE_KEY, StateType.PRODUCT_STATE)
            .roles(Collections.emptySet())
            .description(ofEnglish("State 1"))
            .name(ofEnglish("State 1"))
            .initial(true)
            .transitions(Collections.emptySet())
            .build();
    final State state =
        CTP_SOURCE_CLIENT.execute(StateCreateCommand.of(stateDraft)).toCompletableFuture().join();

    CTP_TARGET_CLIENT.execute(StateCreateCommand.of(stateDraft)).toCompletableFuture().join();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("Tax-Rate-Name-1", 0.3, false, CountryCode.DE).build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "Tax-Category-Name-1", singletonList(taxRateDraft), "Tax-Category-Description-1")
            .key(RESOURCE_KEY)
            .build();

    final TaxCategory taxCategory =
        CTP_SOURCE_CLIENT
            .execute(TaxCategoryCreateCommand.of(taxCategoryDraft))
            .toCompletableFuture()
            .join();

    CTP_TARGET_CLIENT
        .execute(TaxCategoryCreateCommand.of(taxCategoryDraft))
        .toCompletableFuture()
        .join();

    final CustomerGroupDraft customerGroupDraft =
        CustomerGroupDraftBuilder.of("customerGroupName").key("customerGroupKey").build();

    CustomerGroup customerGroup =
        CTP_SOURCE_CLIENT
            .execute(CustomerGroupCreateCommand.of(customerGroupDraft))
            .toCompletableFuture()
            .join();

    CTP_TARGET_CLIENT
        .execute(CustomerGroupCreateCommand.of(customerGroupDraft))
        .toCompletableFuture()
        .join();

    CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraft.ofTypeKeyAndJson(type.getKey(), emptyMap());

    final ChannelDraft draft =
        ChannelDraftBuilder.of("channelKey").roles(singleton(ChannelRole.INVENTORY_SUPPLY)).build();
    CTP_SOURCE_CLIENT.execute(ChannelCreateCommand.of(draft)).toCompletableFuture().join();
    CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(draft)).toCompletableFuture().join();

    final PriceDraft priceBuilder =
        PriceDraftBuilder.of(
                createPriceDraft(
                    BigDecimal.valueOf(222),
                    EUR,
                    DE,
                    customerGroup.getId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null))
            .customerGroup(customerGroup)
            .custom(customFieldsDraft)
            .channel(ResourceIdentifier.ofKey("channelKey"))
            .build();

    final AssetDraft assetDraft =
        AssetDraftBuilder.of(emptyList(), LocalizedString.ofEnglish("assetName"))
            .key("assetKey")
            .sources(singletonList(AssetSourceBuilder.ofUri("sourceUri").build()))
            .custom(customFieldsDraft)
            .build();

    final ProductVariantDraft variantDraft1 =
        ProductVariantDraftBuilder.of()
            .key("variantKey")
            .sku("sku1")
            .prices(priceBuilder)
            .assets(asList(assetDraft))
            .build();

    final ProductDraft productDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("V-neck Tee"),
                ofEnglish("v-neck-tee"),
                ProductVariantDraftBuilder.of().key(RESOURCE_KEY).sku(RESOURCE_KEY).build())
            .state(State.referenceOfId(state.getId()))
            .taxCategory(TaxCategory.referenceOfId(taxCategory.getId()))
            .productType(ProductType.referenceOfId(productType.getId()))
            .variants(asList(variantDraft1))
            .key(RESOURCE_KEY)
            .publish(true)
            .build();

    CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();
    productQuery = ProductProjectionQuery.ofStaged();
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

  @AfterAll
  static void tearDownSuite() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteProductSyncTestData(CTP_SOURCE_CLIENT);
  }

  @Test
  void run_WithSyncAsArgumentWithProductsArg_ShouldResolveReferencesAndExecuteProductSyncer() {
    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT.execute(productQuery).toCompletableFuture().join().getResults();

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    // assertion
    AssertionsForStatistics.assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }
}
