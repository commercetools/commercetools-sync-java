package com.commercetools.sync.integration.ctpprojectsource.products;

import static com.commercetools.api.models.common.DefaultCurrencyUnits.EUR;
import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createPriceDraft;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.neovisionaries.i18n.CountryCode.DE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.channel.ChannelDraft;
import com.commercetools.api.models.channel.ChannelDraftBuilder;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.channel.ChannelRoleEnum;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.AssetSourceBuilder;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupDraft;
import com.commercetools.api.models.customer_group.CustomerGroupDraftBuilder;
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
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryDraftBuilder;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifierBuilder;
import com.commercetools.api.models.tax_category.TaxRateDraft;
import com.commercetools.api.models.tax_category.TaxRateDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldDefinition;
import com.commercetools.api.models.type.FieldDefinitionBuilder;
import com.commercetools.api.models.type.FieldTypeBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.commercetools.api.models.type.TypeTextInputHint;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.sdk2.products.utils.ProductTransformUtils;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        ProductTypeDraftBuilder.of()
            .key(RESOURCE_KEY)
            .name("sample-product-type")
            .description("a productType for t-shirts")
            .attributes(emptyList())
            .build();

    final ProductType productType =
        CTP_SOURCE_CLIENT
            .productTypes()
            .create(productTypeDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    CTP_TARGET_CLIENT.productTypes().create(productTypeDraft).executeBlocking();

    final FieldDefinition FIELD_DEFINITION_1 =
        FieldDefinitionBuilder.of()
            .type(FieldTypeBuilder::stringBuilder)
            .name("field_name_1")
            .label(ofEnglish("label_1"))
            .required(false)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key(TYPE_KEY)
            .name(ofEnglish("name_1"))
            .resourceTypeIds(
                List.of(
                    ResourceTypeId.PRODUCT_PRICE, ResourceTypeId.ASSET, ResourceTypeId.CATEGORY))
            .description(ofEnglish("description_1"))
            .fieldDefinitions(Arrays.asList(FIELD_DEFINITION_1))
            .build();

    final Type type =
        CTP_SOURCE_CLIENT
            .types()
            .create(typeDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    CTP_TARGET_CLIENT.types().create(typeDraft).executeBlocking();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(ofEnglish("t-shirts"))
            .slug(ofEnglish("t-shirts"))
            .key(RESOURCE_KEY)
            .build();

    CTP_SOURCE_CLIENT.categories().create(categoryDraft).executeBlocking();

    CTP_TARGET_CLIENT.categories().create(categoryDraft).executeBlocking();

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(RESOURCE_KEY)
            .type(StateTypeEnum.PRODUCT_STATE)
            .roles(List.of())
            .description(ofEnglish("State 1"))
            .name(ofEnglish("State 1"))
            .initial(true)
            .transitions(List.of())
            .build();
    final State state =
        CTP_SOURCE_CLIENT
            .states()
            .create(stateDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    CTP_TARGET_CLIENT.states().create(stateDraft).executeBlocking();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("Tax-Rate-Name-1")
            .amount(0.3)
            .includedInPrice(false)
            .country(DE.getAlpha2())
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("Tax-Category-Name-1")
            .rates(List.of(taxRateDraft))
            .description("Tax-Category-Description-1")
            .key(RESOURCE_KEY)
            .build();

    final TaxCategory taxCategory =
        CTP_SOURCE_CLIENT
            .taxCategories()
            .create(taxCategoryDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    CTP_TARGET_CLIENT.taxCategories().create(taxCategoryDraft).executeBlocking();

    final CustomerGroupDraft customerGroupDraft =
        CustomerGroupDraftBuilder.of()
            .groupName("customerGroupName")
            .key("customerGroupKey")
            .build();

    final CustomerGroup customerGroup =
        CTP_SOURCE_CLIENT
            .customerGroups()
            .create(customerGroupDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    CTP_TARGET_CLIENT.customerGroups().create(customerGroupDraft).executeBlocking();

    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(type.getKey()))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(Map.of()))
            .build();

    final ChannelDraft draft =
        ChannelDraftBuilder.of().key("channelKey").roles(ChannelRoleEnum.INVENTORY_SUPPLY).build();
    CTP_SOURCE_CLIENT.channels().create(draft).executeBlocking();
    CTP_TARGET_CLIENT.channels().create(draft).executeBlocking();

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
            .customerGroup(customerGroup.toResourceIdentifier())
            .custom(customFieldsDraft)
            .channel(ChannelResourceIdentifierBuilder.of().key("channelKey").build())
            .build();

    final AssetDraft assetDraft =
        AssetDraftBuilder.of()
            .name(ofEnglish("assetName"))
            .key("assetKey")
            .sources(AssetSourceBuilder.of().uri("sourceUri").build())
            .custom(customFieldsDraft)
            .build();

    final ProductVariantDraft variantDraft1 =
        ProductVariantDraftBuilder.of()
            .key("variantKey")
            .sku("sku1")
            .prices(priceBuilder)
            .assets(assetDraft)
            .build();

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("V-neck Tee"))
            .slug(ofEnglish("v-neck-tee"))
            .masterVariant(
                ProductVariantDraftBuilder.of().key(RESOURCE_KEY).sku(RESOURCE_KEY).build())
            .state(StateResourceIdentifierBuilder.of().id(state.getId()).build())
            .taxCategory(TaxCategoryResourceIdentifierBuilder.of().id(taxCategory.getId()).build())
            .productType(ProductTypeResourceIdentifierBuilder.of().id(productType.getId()).build())
            .variants(variantDraft1)
            .key(RESOURCE_KEY)
            .publish(true)
            .build();

    CTP_SOURCE_CLIENT.products().create(productDraft).executeBlocking();
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
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjectionPagedQueryResponse::getResults)
            .join();

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }
}
