package com.commercetools.sync.sdk2.products.helpers;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.sdk2.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.sdk2.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.common.*;
import com.commercetools.api.models.custom_object.CustomObjectReference;
import com.commercetools.api.models.customer.CustomerReference;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.helpers.DefaultCurrencyUnits;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.services.ChannelService;
import com.commercetools.sync.sdk2.services.CustomerGroupService;
import com.commercetools.sync.sdk2.services.TypeService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VariantReferenceResolverTest {

  private static final String CHANNEL_KEY = "channel-key_1";
  private static final String CHANNEL_ID = UUID.randomUUID().toString();
  private static final String PRODUCT_ID = UUID.randomUUID().toString();
  private static final String PRODUCT_TYPE_ID = UUID.randomUUID().toString();
  private static final String CATEGORY_ID = UUID.randomUUID().toString();
  private static final String CUSTOM_OBJECT_ID = UUID.randomUUID().toString();
  private static final String STATE_ID = UUID.randomUUID().toString();
  private static final String CUSTOMER_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  @BeforeEach
  void setup() {
    final TypeService typeService = getMockTypeService();
    final ChannelService channelService =
        getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY));
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    referenceResolver =
        new VariantReferenceResolver(
            syncOptions,
            typeService,
            channelService,
            mock(CustomerGroupService.class),
            getMockProductService(PRODUCT_ID),
            getMockProductTypeService(PRODUCT_TYPE_ID),
            getMockCategoryService(CATEGORY_ID),
            getMockCustomObjectService(CUSTOM_OBJECT_ID),
            getMockStateService(STATE_ID),
            getMockCustomerService(CUSTOMER_ID));
  }

  @Test
  void resolveReferences_WithNoAttributes_ShouldReturnEqualDraft() {
    // preparation
    final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder.of().build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithEmptyAttributes_ShouldReturnEqualDraft() {
    // preparation
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(emptyList()).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithANullAttribute_ShouldReturnDraftWithoutNullAttribute() {
    // preparation
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes((Attribute) null).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft.getAttributes()).isEmpty();
  }

  @Test
  void resolveReferences_WithTextAttribute_ShouldReturnEqualDraft() {
    // preparation
    final Attribute textAttribute =
        AttributeBuilder.of().name("attributeName").value("attributeValue").build();
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(textAttribute).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithEmptyJsonNodeAttributeValue_ShouldReturnEqualDraft() {
    // preparation
    final Attribute attributeWithEmptyValue =
        AttributeBuilder.of()
            .name("attributeName")
            .value(JsonNodeFactory.instance.objectNode())
            .build();

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeWithEmptyValue).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithEmptySetAttribute_ShouldReturnEqualDraft() {
    // preparation
    final Attribute attributeWithEmptyValue =
        AttributeBuilder.of()
            .name("attributeName")
            .value(JsonNodeFactory.instance.arrayNode())
            .build();

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeWithEmptyValue).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithNoPriceReferences_ShouldResolveAttributeReferences() {
    // preparation
    final ProductReference productReferenceWithRandomId = getProductReferenceWithRandomId();
    final Attribute productReferenceSetAttribute =
        getReferenceSetAttributeDraft("foo", productReferenceWithRandomId);

    final Attribute textAttribute =
        AttributeBuilder.of().name("attributeName").value("textValue").build();

    final List<Attribute> attributeDrafts =
        Arrays.asList(productReferenceSetAttribute, textAttribute);

    final ProductVariantDraft variantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeDrafts).build();

    // test
    final ProductVariantDraft resolvedDraft =
        referenceResolver.resolveReferences(variantDraft).toCompletableFuture().join();

    // assertions
    final List<PriceDraft> resolvedDraftPrices = resolvedDraft.getPrices();
    assertThat(resolvedDraftPrices).isNull();

    final List<Attribute> resolvedBuilderAttributes = resolvedDraft.getAttributes();
    assertThat(resolvedBuilderAttributes).hasSize(2);
    assertThat(resolvedBuilderAttributes).contains(textAttribute);

    final Attribute resolvedProductReferenceSetAttribute = resolvedBuilderAttributes.get(0);
    assertThat(resolvedProductReferenceSetAttribute).isNotNull();

    final List<Reference> resolvedProductReferenceSetValue =
        AttributeAccessor.asSetReference(resolvedProductReferenceSetAttribute);
    assertThat(resolvedProductReferenceSetValue).isNotNull();

    final Reference resolvedProductReferenceValue = resolvedProductReferenceSetValue.get(0);
    assertThat(resolvedProductReferenceValue).isNotNull();

    final String resolvedProductReferenceId = resolvedProductReferenceValue.getId();
    assertThat(resolvedProductReferenceId).isNotNull();
    assertThat(resolvedProductReferenceId).isEqualTo(PRODUCT_ID);
  }

  @Test
  void resolveReferences_WithMixedReferences_ShouldResolveReferenceAttributes() {
    // preparation
    final ProductReference productReferenceWithRandomId = getProductReferenceWithRandomId();
    final Attribute productReferenceSetAttribute =
        getReferenceSetAttributeDraft("foo", productReferenceWithRandomId);

    final Reference categoryReference = createReferenceObject("foo", CategoryReference.CATEGORY);
    final Reference productTypeReference =
        createReferenceObject("foo", ProductTypeReference.PRODUCT_TYPE);
    final Reference customerReference = createReferenceObject("foo", CustomerReference.CUSTOMER);
    final Reference customObjectReference =
        createReferenceObject("container|key", CustomObjectReference.KEY_VALUE_DOCUMENT);
    final Reference stateReference = createReferenceObject("foo", StateReference.STATE);

    final Attribute categoryReferenceAttribute =
        AttributeBuilder.of().name("cat-ref").value(categoryReference).build();
    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of().name("productType-ref").value(productTypeReference).build();
    final Attribute customerReferenceAttribute =
        AttributeBuilder.of().name("customer-ref").value(customerReference).build();
    final Attribute textAttribute =
        AttributeBuilder.of().name("attributeName").value("textValue").build();
    final Attribute customObjectReferenceAttribute =
        AttributeBuilder.of().name("customObject-ref").value(customObjectReference).build();
    final Attribute stateReferenceAttribute =
        AttributeBuilder.of().name("state-ref").value(stateReference).build();

    final List<Attribute> attributeDrafts =
        Arrays.asList(
            productReferenceSetAttribute,
            categoryReferenceAttribute,
            productTypeReferenceAttribute,
            customerReferenceAttribute,
            textAttribute,
            customObjectReferenceAttribute,
            stateReferenceAttribute);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeDrafts).build();

    // test
    final ProductVariantDraft resolvedBuilder =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    final List<Attribute> resolvedBuilderAttributes = resolvedBuilder.getAttributes();
    assertThat(resolvedBuilderAttributes).hasSize(7);
    assertThat(resolvedBuilderAttributes).contains(textAttribute);

    final Attribute resolvedProductReferenceSetAttribute = resolvedBuilderAttributes.get(0);
    assertThat(resolvedProductReferenceSetAttribute).isNotNull();

    final List<Reference> resolvedProductReferenceSetValue =
        AttributeAccessor.asSetReference(resolvedProductReferenceSetAttribute);
    assertThat(resolvedProductReferenceSetValue).isNotNull();

    final Reference resolvedProductReferenceValue = resolvedProductReferenceSetValue.get(0);
    assertThat(resolvedProductReferenceValue).isNotNull();

    final String resolvedProductReferenceId = resolvedProductReferenceValue.getId();
    assertThat(resolvedProductReferenceId).isNotNull();
    assertThat(resolvedProductReferenceId).isEqualTo(PRODUCT_ID);

    final Attribute resolvedCategoryReferenceAttribute = resolvedBuilderAttributes.get(1);
    assertThat(resolvedCategoryReferenceAttribute).isNotNull();

    final Reference resolvedCategoryReferenceAttributeValue =
        AttributeAccessor.asReference(resolvedCategoryReferenceAttribute);
    assertThat(resolvedCategoryReferenceAttributeValue).isNotNull();

    assertThat(resolvedCategoryReferenceAttributeValue.getId()).isEqualTo(CATEGORY_ID);
    assertThat(resolvedCategoryReferenceAttributeValue.getTypeId().getJsonName())
        .isEqualTo(CategoryReference.CATEGORY);

    final Attribute resolvedProductTypeReferenceAttribute = resolvedBuilderAttributes.get(2);
    assertThat(resolvedProductTypeReferenceAttribute).isNotNull();

    final Reference resolvedProductTypeReferenceAttributeValue =
        AttributeAccessor.asReference(resolvedProductTypeReferenceAttribute);
    assertThat(resolvedProductTypeReferenceAttributeValue).isNotNull();

    assertThat(resolvedProductTypeReferenceAttributeValue.getId()).isEqualTo(PRODUCT_TYPE_ID);
    assertThat(resolvedProductTypeReferenceAttributeValue.getTypeId().getJsonName())
        .isEqualTo(ProductTypeReference.PRODUCT_TYPE);

    final Attribute resolvedCustomerReferenceAttribute = resolvedBuilderAttributes.get(3);
    assertThat(resolvedCustomerReferenceAttribute).isNotNull();

    final Reference resolvedCustomerReferenceAttributeValue =
        AttributeAccessor.asReference(resolvedCustomerReferenceAttribute);
    assertThat(resolvedCustomerReferenceAttributeValue).isNotNull();

    assertThat(resolvedCustomerReferenceAttributeValue.getId()).isEqualTo(CUSTOMER_ID);
    assertThat(resolvedCustomerReferenceAttributeValue.getTypeId().getJsonName())
        .isEqualTo(CustomerReference.CUSTOMER);

    final Attribute resolvedCustomObjectReferenceAttribute = resolvedBuilderAttributes.get(5);
    assertThat(resolvedCustomObjectReferenceAttribute).isNotNull();

    final Reference resolvedCustomObjectAttributeValue =
        AttributeAccessor.asReference(resolvedCustomObjectReferenceAttribute);
    assertThat(resolvedCustomObjectAttributeValue).isNotNull();

    assertThat(resolvedCustomObjectAttributeValue.getId()).isEqualTo(CUSTOM_OBJECT_ID);
    assertThat(resolvedCustomObjectAttributeValue.getTypeId().getJsonName())
        .isEqualTo(CustomObjectReference.KEY_VALUE_DOCUMENT);

    final Attribute resolvedStateReferenceAttribute = resolvedBuilderAttributes.get(6);
    assertThat(resolvedStateReferenceAttribute).isNotNull();

    final Reference resolvedStateReferenceAttributeValue =
        AttributeAccessor.asReference(resolvedStateReferenceAttribute);
    assertThat(resolvedStateReferenceAttributeValue.getId()).isEqualTo(STATE_ID);
    assertThat(resolvedStateReferenceAttributeValue.getTypeId().getJsonName())
        .isEqualTo(StateReference.STATE);
  }

  @Test
  void resolveReferences_WithNullReferenceInSetAttribute_ShouldResolveReferences() {
    // preparation
    final ProductReference productReference = getProductReferenceWithRandomId();
    final Attribute productReferenceAttribute =
        getReferenceSetAttributeDraft("foo", productReference, null);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();

    final Spliterator<Reference> attributeReferencesIterator =
        AttributeAccessor.asSetReference(resolvedAttributeDraft).spliterator();
    assertThat(attributeReferencesIterator).isNotNull();
    final Set<Reference> resolvedSet =
        StreamSupport.stream(attributeReferencesIterator, false).collect(Collectors.toSet());

    assertThat(resolvedSet).isNotEmpty();
    final ProductReference resolvedReference =
        ReferenceBuilder.of().productBuilder().id(PRODUCT_ID).build();
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference, null);
  }

  @Test
  void resolveAssetsReferences_WithEmptyAssets_ShouldNotResolveAssets() {
    final ProductVariantDraftBuilder productVariantDraftBuilder =
        ProductVariantDraftBuilder.of().assets(emptyList());

    final ProductVariantDraftBuilder resolvedBuilder =
        referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder)
            .toCompletableFuture()
            .join();

    final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
    assertThat(resolvedBuilderAssets).isEmpty();
  }

  @Test
  void resolveAssetsReferences_WithNullAssets_ShouldNotResolveAssets() {
    final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder.of();

    final ProductVariantDraftBuilder resolvedBuilder =
        referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder)
            .toCompletableFuture()
            .join();

    final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
    assertThat(resolvedBuilderAssets).isNull();
  }

  @Test
  void resolveAssetsReferences_WithANullAsset_ShouldNotResolveAssets() {
    final ProductVariantDraftBuilder productVariantDraftBuilder =
        ProductVariantDraftBuilder.of().assets(singletonList(null));

    final ProductVariantDraftBuilder resolvedBuilder =
        referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder)
            .toCompletableFuture()
            .join();

    final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
    assertThat(resolvedBuilderAssets).isEmpty();
  }

  @Test
  void resolveAssetsReferences_WithAssetReferences_ShouldResolveAssets() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeResourceIdentifierBuilder.of().id("customTypeId").build())
            .build();

    final AssetDraft assetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(ofEnglish("assetName"))
            .custom(customFieldsDraft)
            .build();

    final ProductVariantDraftBuilder productVariantDraftBuilder =
        ProductVariantDraftBuilder.of().assets(singletonList(assetDraft));

    final ProductVariantDraftBuilder resolvedBuilder =
        referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder)
            .toCompletableFuture()
            .join();

    final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
    assertThat(resolvedBuilderAssets).isNotEmpty();
    final AssetDraft resolvedAssetDraft = resolvedBuilderAssets.get(0);
    assertThat(resolvedAssetDraft).isNotNull();
    assertThat(resolvedAssetDraft.getCustom()).isNotNull();
    assertThat(resolvedAssetDraft.getCustom().getType().getId()).isEqualTo("customTypeId");
  }

  @Test
  void resolvePricesReferences_WithNullPrices_ShouldNotResolvePrices() {
    final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder.of();

    final ProductVariantDraftBuilder resolvedBuilder =
        referenceResolver
            .resolvePricesReferences(productVariantDraftBuilder)
            .toCompletableFuture()
            .join();

    final List<PriceDraft> resolvedBuilderPrices = resolvedBuilder.getPrices();
    assertThat(resolvedBuilderPrices).isNull();
  }

  @Test
  void resolvePricesReferences_WithANullPrice_ShouldNotResolvePrices() {
    final ProductVariantDraftBuilder productVariantDraftBuilder =
        ProductVariantDraftBuilder.of().prices((PriceDraft) null);

    final ProductVariantDraftBuilder resolvedBuilder =
        referenceResolver
            .resolvePricesReferences(productVariantDraftBuilder)
            .toCompletableFuture()
            .join();

    final List<PriceDraft> resolvedBuilderPrices = resolvedBuilder.getPrices();
    assertThat(resolvedBuilderPrices).isEmpty();
  }

  @Test
  void resolvePricesReferences_WithPriceReferences_ShouldResolvePrices() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeResourceIdentifierBuilder.of().id("customTypeId").build())
            .build();

    final PriceDraft priceDraft =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .custom(customFieldsDraft)
            .customerGroup(
                CustomerGroupResourceIdentifierBuilder.of().id("customerGroupId").build())
            .build();

    final ProductVariantDraftBuilder productVariantDraftBuilder =
        ProductVariantDraftBuilder.of().prices(priceDraft);

    final ProductVariantDraftBuilder resolvedBuilder =
        referenceResolver
            .resolvePricesReferences(productVariantDraftBuilder)
            .toCompletableFuture()
            .join();

    final List<PriceDraft> resolvedBuilderPrices = resolvedBuilder.getPrices();
    assertThat(resolvedBuilderPrices).isNotEmpty();
    final PriceDraft resolvedPriceDraft = resolvedBuilderPrices.get(0);
    assertThat(resolvedPriceDraft).isNotNull();
    assertThat(resolvedPriceDraft.getCustomerGroup().getId()).isEqualTo("customerGroupId");
    assertThat(resolvedPriceDraft.getCustom()).isNotNull();
    assertThat(resolvedPriceDraft.getCustom().getType().getId()).isEqualTo("customTypeId");
  }
}
