package com.commercetools.sync.products.helpers;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyTypesGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.common.DefaultCurrencyUnits;
import com.commercetools.api.models.common.MoneyBuilder;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypePagedQueryResponse;
import com.commercetools.sync.commons.MockUtils;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.inventories.InventorySyncMockUtils;
import com.commercetools.sync.products.ProductSyncMockUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.neovisionaries.i18n.CountryCode;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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
    typeService = MockUtils.getMockTypeService();
    channelService =
        InventorySyncMockUtils.getMockChannelService(
            InventorySyncMockUtils.getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY));
    customerGroupService =
        ProductSyncMockUtils.getMockCustomerGroupService(
            ProductSyncMockUtils.getMockCustomerGroup(CUSTOMER_GROUP_ID, CUSTOMER_GROUP_KEY));
    syncOptions = ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
  }

  @Test
  void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
    // Preparation
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(ctpClient).build();

    final TypeService typeService = new TypeServiceImpl(productSyncOptions);

    final CompletableFuture<ApiHttpResponse<TypePagedQueryResponse>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(new Exception("CTP error on fetch"));

    final ByProjectKeyTypesGet byProjectKeyTypesGet = mock();
    when(ctpClient.types()).thenReturn(mock());
    when(ctpClient.types().get()).thenReturn(mock());
    when(ctpClient.types().get().withWhere(anyString())).thenReturn(byProjectKeyTypesGet);
    when(byProjectKeyTypesGet.withPredicateVar(anyString(), anyCollection()))
        .thenReturn(byProjectKeyTypesGet);
    when(byProjectKeyTypesGet.withLimit(anyInt())).thenReturn(byProjectKeyTypesGet);
    when(byProjectKeyTypesGet.withWithTotal(anyBoolean())).thenReturn(byProjectKeyTypesGet);
    when(byProjectKeyTypesGet.withSort(anyString())).thenReturn(byProjectKeyTypesGet);
    when(byProjectKeyTypesGet.execute()).thenReturn(futureThrowingSphereException);

    final String customTypeKey = "customTypeKey";
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .custom(
                CustomFieldsDraftBuilder.of().type(builder -> builder.key(customTypeKey)).build());

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(
            productSyncOptions, typeService, channelService, customerGroupService);

    // Test and assertion
    assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(Exception.class)
        .withMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
    final String customTypeKey = "customTypeKey";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of().type(builder -> builder.key(customTypeKey)).build();
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .custom(customFieldsDraft);

    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    // Test and assertion
    final String expectedExceptionMessage =
        String.format(
            PriceReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE,
            priceBuilder.getCountry(),
            priceBuilder.getValue().toMonetaryAmount());
    final String expectedMessageWithCause =
        format(
            "%s Reason: %s",
            expectedExceptionMessage,
            String.format(CustomReferenceResolver.TYPE_DOES_NOT_EXIST, customTypeKey));
    ;
    assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(expectedMessageWithCause);
  }

  @Test
  void resolveCustomTypeReference_WithEmptyKeyOnCustomTypeResId_ShouldCompleteExceptionally() {
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(""))
                    .build());

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve custom type reference on PriceDraft"
                    + " with country:'DE' and value: 'EUR 10.00'. Reason: %s",
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void
      resolveCustomTypeReference_WithNonNullIdOnCustomTypeResId_ShouldResolveCustomTypeReference() {
    // Preparation
    final String customTypeId = UUID.randomUUID().toString();
    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .custom(
                CustomFieldsDraftBuilder.of().type(builder -> builder.id(customTypeId)).build());

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
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key("foo"))
                    .build());

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
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .channel(ChannelResourceIdentifierBuilder.of().key("channelKey").build());

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    // Test and assertion
    assertThat(priceReferenceResolver.resolveChannelReference(priceBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                PriceReferenceResolver.FAILED_TO_RESOLVE_REFERENCE,
                ChannelReference.CHANNEL,
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                String.format(PriceReferenceResolver.CHANNEL_DOES_NOT_EXIST, "channelKey")));
  }

  @Test
  void
      resolveChannelReference_WithNonExistingChannelAndEnsureChannel_ShouldResolveSupplyChannelReference() {
    // Preparation
    final ProductSyncOptions optionsWithEnsureChannels =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).ensurePriceChannels(true).build();
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .channel(ChannelResourceIdentifierBuilder.of().key("channelKey").build());

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
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .channel(ChannelResourceIdentifierBuilder.of().key("").build());

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    // Test and assertion
    assertThat(priceReferenceResolver.resolveChannelReference(priceBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                PriceReferenceResolver.FAILED_TO_RESOLVE_REFERENCE,
                ChannelReference.CHANNEL,
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveChannelReference_WithNullChannelKey_ShouldNotResolveChannelReference() {
    // Preparation
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .channel(ChannelResourceIdentifierBuilder.of().key(null).build());

    final PriceReferenceResolver priceReferenceResolver =
        new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

    // Test and assertion
    assertThat(priceReferenceResolver.resolveChannelReference(priceBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                PriceReferenceResolver.FAILED_TO_RESOLVE_REFERENCE,
                ChannelReference.CHANNEL,
                priceBuilder.getCountry(),
                priceBuilder.getValue(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveChannelReference_WithNonNullChannelKey_ShouldResolveSupplyChannelReference() {
    // Preparation
    final ProductSyncOptions optionsWithEnsureChannels =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .channel(ChannelResourceIdentifierBuilder.of().key("channelKey").build());

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
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
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
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .channel((ChannelResourceIdentifier) null);

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
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final PriceDraftBuilder priceBuilder =
        PriceDraftBuilder.of()
            .value(
                MoneyBuilder.of()
                    .centAmount(BigDecimal.TEN.longValue())
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .country(CountryCode.DE.getAlpha2())
            .channel(ChannelResourceIdentifierBuilder.of().id("existing-id").build());

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
