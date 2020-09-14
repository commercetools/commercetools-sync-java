package com.commercetools.sync.products.helpers;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.DefaultCurrencyUnits;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCategoryService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductTypeService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceWithRandomId;
import static com.commercetools.sync.products.ProductSyncMockUtils.getReferenceSetAttributeDraft;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VariantReferenceResolverTest {

    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = UUID.randomUUID().toString();
    private static final String PRODUCT_ID = UUID.randomUUID().toString();
    private static final String PRODUCT_TYPE_ID = UUID.randomUUID().toString();
    private static final String CATEGORY_ID = UUID.randomUUID().toString();
    private VariantReferenceResolver referenceResolver;

    @BeforeEach
    void setup() {
        final TypeService typeService = getMockTypeService();
        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY));
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new VariantReferenceResolver(syncOptions, typeService, channelService,
            mock(CustomerGroupService.class),
            getMockProductService(PRODUCT_ID),
            getMockProductTypeService(PRODUCT_TYPE_ID),
            getMockCategoryService(CATEGORY_ID));
    }

    @Test
    void resolveReferences_WithNoAttributes_ShouldReturnEqualDraft() {
        // preparation
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithEmptyAttributes_ShouldReturnEqualDraft() {
        // preparation
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(emptyList())
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithANullAttribute_ShouldReturnDraftWithoutNullAttribute() {
        // preparation
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes((AttributeDraft) null)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft.getAttributes()).isEmpty();
    }

    @Test
    void resolveReferences_WithTextAttribute_ShouldReturnEqualDraft() {
        // preparation
        final AttributeDraft textAttribute = AttributeDraft.of("attributeName", "attributeValue");
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(textAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithNullAttributeValue_ShouldReturnEqualDraft() {
        // preparation
        final AttributeDraft textAttribute = AttributeDraft.of("attributeName", "attributeValue");
        final AttributeDraft nullAttributeValue = AttributeDraft.of("attributeName", null);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(textAttribute, nullAttributeValue)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithEmptyJsonNodeAttributeValue_ShouldReturnEqualDraft() {
        // preparation
        final AttributeDraft attributeWithEmptyValue =
            AttributeDraft.of("attributeName", JsonNodeFactory.instance.objectNode());

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(attributeWithEmptyValue)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithEmptySetAttribute_ShouldReturnEqualDraft() {
        // preparation
        final AttributeDraft attributeWithEmptyValue =
            AttributeDraft.of("attributeName", JsonNodeFactory.instance.arrayNode());

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(attributeWithEmptyValue)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithNoPriceReferences_ShouldResolveAttributeReferences() {
        // preparation
        final ObjectNode productReferenceWithRandomId = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceSetAttribute =
            getReferenceSetAttributeDraft("foo", productReferenceWithRandomId);

        final AttributeDraft textAttribute = AttributeDraft.of("attributeName", "textValue");

        final List<AttributeDraft> attributeDrafts =
            Arrays.asList(productReferenceSetAttribute, textAttribute);


        final ProductVariantDraft variantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(attributeDrafts)
            .build();

        // test
        final ProductVariantDraft resolvedDraft = referenceResolver.resolveReferences(variantDraft)
                                                                   .toCompletableFuture().join();

        // assertions
        final List<PriceDraft> resolvedDraftPrices = resolvedDraft.getPrices();
        assertThat(resolvedDraftPrices).isNull();

        final List<AttributeDraft> resolvedBuilderAttributes = resolvedDraft.getAttributes();
        assertThat(resolvedBuilderAttributes).hasSize(2);
        assertThat(resolvedBuilderAttributes).contains(textAttribute);

        final AttributeDraft resolvedProductReferenceSetAttribute = resolvedBuilderAttributes.get(0);
        assertThat(resolvedProductReferenceSetAttribute).isNotNull();

        final JsonNode resolvedProductReferenceSetValue = resolvedProductReferenceSetAttribute.getValue();
        assertThat(resolvedProductReferenceSetValue).isNotNull();

        final JsonNode resolvedProductReferenceValue = resolvedProductReferenceSetValue.get(0);
        assertThat(resolvedProductReferenceValue).isNotNull();

        final JsonNode resolvedProductReferenceIdTextNode = resolvedProductReferenceValue.get(REFERENCE_ID_FIELD);
        assertThat(resolvedProductReferenceIdTextNode).isNotNull();
        assertThat(resolvedProductReferenceIdTextNode.asText()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void resolveReferences_WithMixedReferences_ShouldResolveReferenceAttributes() {
        // preparation
        final ObjectNode productReferenceWithRandomId = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceSetAttribute =
            getReferenceSetAttributeDraft("foo", productReferenceWithRandomId);

        final ObjectNode categoryReference = createReferenceObject("foo", Category.referenceTypeId());
        final ObjectNode productTypeReference = createReferenceObject("foo", ProductType.referenceTypeId());
        final ObjectNode customerReference = createReferenceObject("foo", Customer.referenceTypeId());

        final AttributeDraft categoryReferenceAttribute = AttributeDraft
            .of("cat-ref", categoryReference);
        final AttributeDraft productTypeReferenceAttribute = AttributeDraft
            .of("productType-ref", productTypeReference);
        final AttributeDraft customerReferenceAttribute = AttributeDraft
            .of("customer-ref", customerReference);
        final AttributeDraft textAttribute = AttributeDraft
            .of("attributeName", "textValue");

        final List<AttributeDraft> attributeDrafts =
            Arrays.asList(productReferenceSetAttribute,
                categoryReferenceAttribute,
                productTypeReferenceAttribute,
                customerReferenceAttribute,
                textAttribute);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(attributeDrafts)
            .build();

        // test
        final ProductVariantDraft resolvedBuilder = referenceResolver
            .resolveReferences(productVariantDraft)
            .toCompletableFuture().join();

        // assertions
        final List<AttributeDraft> resolvedBuilderAttributes = resolvedBuilder.getAttributes();
        assertThat(resolvedBuilderAttributes).hasSize(5);
        assertThat(resolvedBuilderAttributes).contains(textAttribute);

        final AttributeDraft resolvedProductReferenceSetAttribute = resolvedBuilderAttributes.get(0);
        assertThat(resolvedProductReferenceSetAttribute).isNotNull();

        final JsonNode resolvedProductReferenceSetValue = resolvedProductReferenceSetAttribute.getValue();
        assertThat(resolvedProductReferenceSetValue).isNotNull();

        final JsonNode resolvedProductReferenceValue = resolvedProductReferenceSetValue.get(0);
        assertThat(resolvedProductReferenceValue).isNotNull();

        final JsonNode resolvedProductReferenceIdTextNode = resolvedProductReferenceValue.get(REFERENCE_ID_FIELD);
        assertThat(resolvedProductReferenceIdTextNode).isNotNull();
        assertThat(resolvedProductReferenceIdTextNode.asText()).isEqualTo(PRODUCT_ID);

        final AttributeDraft resolvedCategoryReferenceAttribute = resolvedBuilderAttributes.get(1);
        assertThat(resolvedCategoryReferenceAttribute).isNotNull();

        final JsonNode resolvedCategoryReferenceAttributeValue = resolvedCategoryReferenceAttribute.getValue();
        assertThat(resolvedCategoryReferenceAttributeValue).isNotNull();

        assertThat(resolvedCategoryReferenceAttributeValue.get(REFERENCE_ID_FIELD).asText()).isEqualTo(CATEGORY_ID);
        assertThat(resolvedCategoryReferenceAttributeValue.get(REFERENCE_TYPE_ID_FIELD).asText())
            .isEqualTo(Category.referenceTypeId());

        final AttributeDraft resolvedProductTypeReferenceAttribute = resolvedBuilderAttributes.get(2);
        assertThat(resolvedProductTypeReferenceAttribute).isNotNull();

        final JsonNode resolvedProductTypeReferenceAttributeValue = resolvedProductTypeReferenceAttribute.getValue();
        assertThat(resolvedProductTypeReferenceAttributeValue).isNotNull();

        assertThat(resolvedProductTypeReferenceAttributeValue.get(REFERENCE_ID_FIELD).asText())
            .isEqualTo(PRODUCT_TYPE_ID);
        assertThat(resolvedProductTypeReferenceAttributeValue.get(REFERENCE_TYPE_ID_FIELD).asText())
            .isEqualTo(ProductType.referenceTypeId());

        final AttributeDraft resolvedCustomerReferenceAttribute = resolvedBuilderAttributes.get(3);
        assertThat(resolvedCustomerReferenceAttribute).isNotNull();

        final JsonNode resolvedCustomerReferenceAttributeValue = resolvedCustomerReferenceAttribute.getValue();
        assertThat(resolvedCustomerReferenceAttributeValue).isNotNull();

        assertThat(resolvedCustomerReferenceAttributeValue.get(REFERENCE_ID_FIELD))
            .isEqualTo(customerReference.get(REFERENCE_ID_FIELD));
        assertThat(resolvedCustomerReferenceAttributeValue.get(REFERENCE_TYPE_ID_FIELD).asText())
            .isEqualTo(Customer.referenceTypeId());
    }

    @Test
    void resolveReferences_WithNullReferenceInSetAttribute_ShouldResolveReferences() {
        // preparation
        final ObjectNode productReference = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceAttribute =
            getReferenceSetAttributeDraft("foo", productReference, null);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(productReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedProductVariantDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture().join();

        // assertions
        assertThat(resolvedProductVariantDraft).isNotNull();
        assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

        final AttributeDraft resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());

        assertThat(resolvedSet).isNotEmpty();
        final ObjectNode resolvedReference = JsonNodeFactory.instance.objectNode();
        resolvedReference.put(REFERENCE_TYPE_ID_FIELD, Product.referenceTypeId());
        resolvedReference.put(REFERENCE_ID_FIELD, PRODUCT_ID);
        assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference, JsonNodeFactory.instance.nullNode());
    }

    @Test
    void resolveAssetsReferences_WithEmptyAssets_ShouldNotResolveAssets() {
        final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder
            .of()
            .assets(emptyList());

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isEmpty();
    }

    @Test
    void resolveAssetsReferences_WithNullAssets_ShouldNotResolveAssets() {
        final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder.of();

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isNull();
    }

    @Test
    void resolveAssetsReferences_WithANullAsset_ShouldNotResolveAssets() {
        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of().assets(singletonList(null));

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isEmpty();
    }

    @Test
    void resolveAssetsReferences_WithAssetReferences_ShouldResolveAssets() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeKeyAndJson("customTypeId", new HashMap<>());

        final AssetDraft assetDraft = AssetDraftBuilder
            .of(emptyList(), ofEnglish("assetName"))
            .custom(customFieldsDraft)
            .build();

        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of().assets(singletonList(assetDraft));

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder).toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isNotEmpty();
        final AssetDraft resolvedAssetDraft = resolvedBuilderAssets.get(0);
        assertThat(resolvedAssetDraft).isNotNull();
        assertThat(resolvedAssetDraft.getCustom()).isNotNull();
        assertThat(resolvedAssetDraft.getCustom().getType().getId()).isEqualTo(REFERENCE_TYPE_ID_FIELD);
    }

    @Test
    void resolvePricesReferences_WithNullPrices_ShouldNotResolvePrices() {
        final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder.of();

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolvePricesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<PriceDraft> resolvedBuilderPrices = resolvedBuilder.getPrices();
        assertThat(resolvedBuilderPrices).isNull();
    }

    @Test
    void resolvePricesReferences_WithANullPrice_ShouldNotResolvePrices() {
        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of().prices((PriceDraft) null);

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolvePricesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<PriceDraft> resolvedBuilderPrices = resolvedBuilder.getPrices();
        assertThat(resolvedBuilderPrices).isEmpty();
    }

    @Test
    void resolvePricesReferences_WithPriceReferences_ShouldResolvePrices() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeKeyAndJson("customTypeId", new HashMap<>());

        final PriceDraft priceDraft = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .custom(customFieldsDraft).build();

        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of().prices(priceDraft);

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolvePricesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<PriceDraft> resolvedBuilderPrices = resolvedBuilder.getPrices();
        assertThat(resolvedBuilderPrices).isNotEmpty();
        final PriceDraft resolvedPriceDraft = resolvedBuilderPrices.get(0);
        assertThat(resolvedPriceDraft).isNotNull();
        assertThat(resolvedPriceDraft.getCustom()).isNotNull();
        assertThat(resolvedPriceDraft.getCustom().getType().getId()).isEqualTo(REFERENCE_TYPE_ID_FIELD);
    }
}
