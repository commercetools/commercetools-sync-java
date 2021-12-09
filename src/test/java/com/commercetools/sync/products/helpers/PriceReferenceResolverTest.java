package com.commercetools.sync.products.helpers;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerGroup;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerGroupService;
import static com.commercetools.sync.products.helpers.PriceReferenceResolver.CHANNEL_DOES_NOT_EXIST;
import static com.commercetools.sync.products.helpers.PriceReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE;
import static com.commercetools.sync.products.helpers.PriceReferenceResolver.FAILED_TO_RESOLVE_REFERENCE;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.DefaultCurrencyUnits;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.utils.MoneyImpl;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceReferenceResolverTest {
  private TypeService typeService;
  private ChannelService channelService;
  private CustomerGroupService customerGroupService;
  private ProductSyncOptions syncOptions;

  private static final String CHANNEL_KEY = "channel-key_1";
  private static final String CHANNEL_ID = "1";

  private static final String CUSTOMER_GROUP_KEY = "customer-group-key_1";
  private static final String CUSTOMER_GROUP_ID = "1";

  /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    typeService = getMockTypeService();
    channelService = getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY));
    customerGroupService =
        getMockCustomerGroupService(getMockCustomerGroup(CUSTOMER_GROUP_ID, CUSTOMER_GROUP_KEY));
    syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
  }

  @Test
  void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
    // Preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(ctpClient).build();

    final TypeService typeService = new TypeServiceImpl(productSyncOptions);

    final CompletableFuture<PagedQueryResult<Type>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
    when(ctpClient.execute(any(TypeQuery.class))).thenReturn(futureThrowingSphereException);

    final String customTypeKey = "customTypeKey";
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, new HashMap<>()));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(
            productSyncOptions, typeService, channelService, customerGroupService);

    // Test and assertion
    assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(SphereException.class)
        .hasMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
    final String customTypeKey = "customTypeKey";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, new HashMap<>());
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .custom(customFieldsDraft);

    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    // Test and assertion
    final String expectedExceptionMessage =
        format(FAILED_TO_RESOLVE_CUSTOM_TYPE, priceBuilder.getCountry(), priceBuilder.getValue());
    final String expectedMessageWithCause =
        format(
            "%s Reason: %s", expectedExceptionMessage, format(TYPE_DOES_NOT_EXIST, customTypeKey));
    ;
    assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(expectedMessageWithCause);
  }

  @Test
  void resolveCustomTypeReference_WithEmptyKeyOnCustomTypeResId_ShouldCompleteExceptionally() {
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson("", emptyMap()));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                "Failed to resolve custom type reference on PriceDraft"
                    + " with country:'DE' and value: 'EUR 10.00'. Reason: %s",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void
      resolveCustomTypeReference_WithNonNullIdOnCustomTypeResId_ShouldResolveCustomTypeReference() {
    // Preparation
    final String customTypeId = UUID.randomUUID().toString();
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(customTypeId, new HashMap<>()));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    // Test
    final PriceDraftBuilder resolvedDraftBuilder =
        priceReferenceResolver
            .resolveCustomTypeReference(priceBuilder)
            .toCompletableFuture()
            .join();

    // Assertion
    assertThat(resolvedDraftBuilder.getCustom()).isNotNull();
    assertThat(resolvedDraftBuilder.getCustom().getType().getId()).isEqualTo(customTypeId);
  }

  @Test
  void
      resolveCustomTypeReference_WithNonNullKeyOnCustomTypeResId_ShouldResolveCustomTypeReference() {
    // Preparation
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson("foo", new HashMap<>()));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    // Test
    final PriceDraftBuilder resolvedDraftBuilder =
        priceReferenceResolver
            .resolveCustomTypeReference(priceBuilder)
            .toCompletableFuture()
            .join();
    // Assertion
    assertThat(resolvedDraftBuilder.getCustom()).isNotNull();
    assertThat(resolvedDraftBuilder.getCustom().getType().getId()).isEqualTo("typeId");
  }

  @Test
  void
      resolveChannelReference_WithNonExistingChannelAndNotEnsureChannel_ShouldNotResolveChannelReference() {
    // Preparation
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel(ResourceIdentifier.ofKey("channelKey"));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    // Test and assertion
    assertThat(priceReferenceResolver.resolveChannelReference(priceBuilder))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                FAILED_TO_RESOLVE_REFERENCE,
                Channel.resourceTypeId(),
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                format(CHANNEL_DOES_NOT_EXIST, "channelKey")));
  }

  @Test
  void
      resolveChannelReference_WithNonExistingChannelAndEnsureChannel_ShouldResolveSupplyChannelReference() {
    // Preparation
    final ProductSyncOptions optionsWithEnsureChannels =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).ensurePriceChannels(true).build();
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel(ResourceIdentifier.ofKey("channelKey"));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(
            optionsWithEnsureChannels, typeService, channelService, customerGroupService);

    // Test
    final PriceDraftBuilder resolvedDraftBuilder =
        priceReferenceResolver.resolveChannelReference(priceBuilder).toCompletableFuture().join();

    // Assertion
    assertThat(resolvedDraftBuilder.getChannel()).isNotNull();
    assertThat(resolvedDraftBuilder.getChannel().getId()).isEqualTo(CHANNEL_ID);
  }

  @Test
  void resolveChannelReference_WithEmptyChannelKey_ShouldNotResolveChannelReference() {
    // Preparation
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel(ResourceIdentifier.ofKey(""));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    // Test and assertion
    assertThat(priceReferenceResolver.resolveChannelReference(priceBuilder))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                FAILED_TO_RESOLVE_REFERENCE,
                Channel.resourceTypeId(),
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveChannelReference_WithNullChannelKey_ShouldNotResolveChannelReference() {
    // Preparation
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel(ResourceIdentifier.ofKey(null));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    // Test and assertion
    assertThat(priceReferenceResolver.resolveChannelReference(priceBuilder))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                FAILED_TO_RESOLVE_REFERENCE,
                Channel.resourceTypeId(),
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveChannelReference_WithNonNullChannelKey_ShouldResolveSupplyChannelReference() {
    // Preparation
    final ProductSyncOptions optionsWithEnsureChannels =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel(ResourceIdentifier.ofKey("channelKey"));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(
            optionsWithEnsureChannels, typeService, channelService, customerGroupService);

    // Test
    final PriceDraftBuilder resolvedDraftBuilder =
        priceReferenceResolver.resolveChannelReference(priceBuilder).toCompletableFuture().join();

    // Assertion
    assertThat(resolvedDraftBuilder.getChannel()).isNotNull();
    assertThat(resolvedDraftBuilder.getChannel().getId()).isEqualTo(CHANNEL_ID);
  }

  @Test
  void resolveReferences_WithNoReferences_ShouldNotResolveReferences() {
    final PriceDraft priceDraft =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .build();

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    final PriceDraft referencesResolvedDraft =
        priceReferenceResolver.resolveReferences(priceDraft).toCompletableFuture().join();

    assertThat(referencesResolvedDraft.getCustom()).isNull();
    assertThat(referencesResolvedDraft.getChannel()).isNull();
    assertThat(referencesResolvedDraft.getCustomerGroup()).isNull();
  }

  @Test
  void resolveChannelReference_WithNullChannelReference_ShouldNotResolveReference() {
    final ProductSyncOptions optionsWithEnsureChannels =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel((ResourceIdentifier<Channel>) null);

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(
            optionsWithEnsureChannels, typeService, channelService, customerGroupService);

    // Test
    final PriceDraftBuilder resolvedDraftBuilder =
        priceReferenceResolver.resolveChannelReference(priceBuilder).toCompletableFuture().join();

    // Assertion
    assertThat(resolvedDraftBuilder.getChannel()).isNull();
  }

  @Test
  void resolveChannelReference_WithChannelReferenceWithId_ShouldNotResolveReference() {
    final ProductSyncOptions optionsWithEnsureChannels =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel(ResourceIdentifier.ofId("existing-id"));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(
            optionsWithEnsureChannels, typeService, channelService, customerGroupService);

    // Test
    final PriceDraftBuilder resolvedDraftBuilder =
        priceReferenceResolver.resolveChannelReference(priceBuilder).toCompletableFuture().join();

    // Assertion
    assertThat(resolvedDraftBuilder.getChannel()).isNotNull();
    assertThat(resolvedDraftBuilder.getChannel().getId()).isEqualTo("existing-id");
  }
}
